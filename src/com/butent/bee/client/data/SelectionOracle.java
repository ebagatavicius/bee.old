package com.butent.bee.client.data;

import com.google.common.base.Splitter;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Settings;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.Relation.Caching;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Provides suggestions data management functionality for data changing events.
 */
public class SelectionOracle implements HandlesAllDataEvents, HasViewName {

  @FunctionalInterface
  public interface Callback {
    void onSuggestionsReady(Request request, Response response);
  }

  public static class Request {

    private final String query;

    private final int offset;
    private final int limit;

    public Request(String query, int offset, int limit) {
      this.query = query;
      this.offset = offset;
      this.limit = limit;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Request)) {
        return false;
      }
      Request r = (Request) obj;
      return BeeUtils.equalsTrim(getQuery(), r.getQuery())
          && getOffset() == r.getOffset() && getLimit() == r.getLimit();
    }

    public int getLimit() {
      return limit;
    }

    public int getOffset() {
      return offset;
    }

    public String getQuery() {
      return query;
    }

    @Override
    public int hashCode() {
      return Objects.hash(BeeUtils.trim(getQuery()), getOffset(), getLimit());
    }

    public boolean isEmpty() {
      return BeeUtils.isEmpty(query);
    }
  }

  /**
   * Contains fields and methods to handle suggestion related data responses.
   */

  public static final class Response {

    private final Collection<Suggestion> suggestions;
    private final boolean moreSuggestions;

    private Response(Collection<Suggestion> suggestions, boolean moreSuggestions) {
      this.suggestions = suggestions;
      this.moreSuggestions = moreSuggestions;
    }

    public Collection<Suggestion> getSuggestions() {
      return suggestions;
    }

    public boolean hasMoreSuggestions() {
      return moreSuggestions;
    }

    public boolean isEmpty() {
      return suggestions == null || suggestions.isEmpty();
    }
  }

  /**
   * Handles a single row of suggestions.
   */

  public static class Suggestion {
    private final BeeRow row;

    public Suggestion(BeeRow row) {
      this.row = row;
    }

    public BeeRow getRow() {
      return row;
    }
  }

  /**
   * Manages suggestion requests, which are not yet processed, stores their request and callback
   * information.
   */

  private final class PendingRequest {
    private final Request request;
    private final Callback callback;

    private PendingRequest(Request request, Callback callback) {
      this.request = request;
      this.callback = callback;
    }

    private Callback getCallback() {
      return callback;
    }

    private Request getRequest() {
      return request;
    }
  }

  public static final Caching DEFAULT_CACHING = Caching.GLOBAL;
  private static final int DEFAULT_MAX_ROW_COUNT_FOR_CACHING = 1_000;

  private static Caching determineCaching(Relation relation, int dataSize) {
    if (relation.getCaching() == null) {
      if (dataSize > 0) {
        Integer max = Settings.getDataSelectorCachingMaxRows();
        if (max == null) {
          max = DEFAULT_MAX_ROW_COUNT_FOR_CACHING;
        }

        if (dataSize <= max) {
          return DEFAULT_CACHING;
        }
      }

      return Caching.NONE;

    } else {
      return relation.getCaching();
    }
  }

  private final DataInfo dataInfo;

  private final int[] searchIndexes;
  private final List<IsColumn> searchColumns = new ArrayList<>();

  private final Operator searchType;

  private final Filter immutableFilter;
  private final Order viewOrder;

  private Caching caching;

  private BeeRowSet viewData;
  private BeeRowSet requestData;

  private Request lastRequest;
  private PendingRequest pendingRequest;

  private final List<HandlerRegistration> handlerRegistry = new ArrayList<>();

  private final Set<Consumer<Integer>> rowCountChangeHandlers = new HashSet<>();
  private final Set<Consumer<Long>> rowDeleteHandlers = new HashSet<>();
  private final Set<Consumer<BeeRowSet>> dataReceivedHandlers = new HashSet<>();

  private boolean dataInitialized;

  private Filter additionalFilter;
  private final Set<Long> exclusions = new HashSet<>();
  private Filter responseFilter;

  public SelectionOracle(Relation relation, DataInfo dataInfo) {
    Assert.notNull(relation);
    Assert.notNull(dataInfo);

    this.dataInfo = dataInfo;

    List<Integer> indexes = new ArrayList<>();
    for (String colName : relation.getSearchableColumns()) {
      int index = dataInfo.getColumnIndex(colName);
      if (!BeeConst.isUndef(index) && !indexes.contains(index)) {
        indexes.add(index);
      }
    }

    this.searchIndexes = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); i++) {
      int index = indexes.get(i);

      this.searchIndexes[i] = index;
      this.searchColumns.add(dataInfo.getColumns().get(index));
    }

    this.searchType = relation.nvlOperator();

    String cuf = relation.getCurrentUserFilter();
    this.immutableFilter = BeeUtils.isEmpty(cuf) ? relation.getFilter()
        : Filter.and(relation.getFilter(), BeeKeeper.getUser().getFilter(cuf));

    this.viewOrder = relation.getOrder();

    this.handlerRegistry.addAll(BeeKeeper.getBus().registerDataHandler(this, false));
  }

  public void addDataReceivedHandler(Consumer<BeeRowSet> handler) {
    if (handler != null) {
      dataReceivedHandlers.add(handler);
    }
  }

  public void addRowCountChangeHandler(Consumer<Integer> handler) {
    if (handler != null) {
      rowCountChangeHandlers.add(handler);
    }
  }

  public void addRowDeleteHandler(Consumer<Long> handler) {
    if (handler != null) {
      rowDeleteHandlers.add(handler);
    }
  }

  public void clearData() {
    if (isFullCaching()) {
      setViewData(null);
      setDataInitialized(false);
    }

    resetState();
  }

  public void clearExclusions() {
    if (!exclusions.isEmpty()) {
      exclusions.clear();
      resetState();
    }
  }

  public Filter getAdditionalFilter() {
    return additionalFilter;
  }

  public BeeRow getCachedRow(long rowId) {
    if (getViewData() == null) {
      return null;
    } else {
      return getViewData().getRowById(rowId);
    }
  }

  public DataInfo getDataInfo() {
    return dataInfo;
  }

  public Set<Long> getExclusions() {
    return exclusions;
  }

  public BeeRowSet getViewData() {
    return viewData;
  }

  @Override
  public String getViewName() {
    return dataInfo.getViewName();
  }

  public Order getViewOrder() {
    return viewOrder;
  }

  public void init(Relation relation, int dataSize) {
    setCaching(determineCaching(relation, dataSize));
  }

  public boolean isCachingEnabled() {
    return getCaching() != null && getCaching() != Caching.NONE;
  }

  public boolean isFullCaching() {
    return getCaching() == Caching.LOCAL || getCaching() == Caching.GLOBAL;
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isEventRelevant(event) && event.applyTo(getViewData())) {
      resetState();
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (isEventRelevant(event)) {
      clearData();
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (isEventRelevant(event)) {
      boolean changed = false;
      for (RowInfo rowInfo : event.getRows()) {
        if (getViewData().removeRowById(rowInfo.getId())) {
          changed = true;
        }
      }
      if (changed) {
        resetState();
        onRowCountChange(getViewData().getNumberOfRows());
      }
    }

    if (event != null && event.hasView(getViewName()) && !rowDeleteHandlers.isEmpty()) {
      event.getRows().forEach(rowInfo -> {
        long id = rowInfo.getId();
        rowDeleteHandlers.forEach(handler -> handler.accept(id));
      });
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event) && getViewData().removeRowById(event.getRowId())) {
      resetState();
      onRowCountChange(getViewData().getNumberOfRows());
    }

    if (event != null && event.hasView(getViewName())) {
      long id = event.getRowId();
      rowDeleteHandlers.forEach(handler -> handler.accept(id));
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event) && isFullCaching() && !getViewData().containsRow(event.getRowId())
        && matches(immutableFilter, event.getRow())
        && matches(getAdditionalFilter(), event.getRow())) {

      getViewData().addRow(event.getRow());
      resetState();
      onRowCountChange(getViewData().getNumberOfRows());
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isEventRelevant(event) && getViewData().updateRow(event.getRow())) {
      resetState();
    }
  }

  public void onUnload() {
    EventUtils.clearRegistry(handlerRegistry);

    rowCountChangeHandlers.clear();
    rowDeleteHandlers.clear();
    dataReceivedHandlers.clear();
  }

  public void requestSuggestions(Request request, Callback callback) {
    Assert.notNull(request);
    Assert.notNull(callback);

    if (!prepareData(request)) {
      setPendingRequest(new PendingRequest(request, callback));
      if (isFullCaching() && !isDataInitialized()) {
        setDataInitialized(true);
        initViewData();
      }
      return;
    }

    setLastRequest(request);
    processRequest(request, callback);
  }

  public boolean setAdditionalFilter(Filter filter, boolean force) {
    if (force || !Objects.equals(filter, this.additionalFilter)) {
      this.additionalFilter = filter;
      clearData();

      return true;

    } else {
      return false;
    }
  }

  public void setExclusions(Collection<Long> rowIds) {
    if (BeeUtils.isEmpty(rowIds)) {
      clearExclusions();

    } else if (!exclusions.containsAll(rowIds) || !rowIds.containsAll(exclusions)) {
      exclusions.clear();
      exclusions.addAll(rowIds);
      resetState();
    }
  }

  public void setResponseFilter(Filter responseFilter) {
    if (!Objects.equals(getResponseFilter(), responseFilter)) {
      this.responseFilter = responseFilter;
      resetState();
    }
  }

  private void checkPendingRequest() {
    if (getPendingRequest() != null) {
      Request request = getPendingRequest().getRequest();
      Callback callback = getPendingRequest().getCallback();
      setPendingRequest(null);
      requestSuggestions(request, callback);
    }
  }

  private boolean filterResponse(List<BeeColumn> columns, BeeRow row) {
    if (!exclusions.isEmpty() && exclusions.contains(row.getId())) {
      return false;
    }
    if (getResponseFilter() != null && !getResponseFilter().isMatch(columns, row)) {
      return false;
    }

    return true;
  }

  private Caching getCaching() {
    return caching;
  }

  private Filter getFilter(Filter queryFilter, boolean addResponseFilters) {
    CompoundFilter result = Filter.and();

    result.add(immutableFilter);
    result.add(getAdditionalFilter());
    result.add(queryFilter);

    if (addResponseFilters) {
      if (!exclusions.isEmpty()) {
        result.add(Filter.idNotIn(exclusions));
      }

      if (getResponseFilter() != null) {
        result.add(getResponseFilter());
      }
    }

    return result;
  }

  private Request getLastRequest() {
    return lastRequest;
  }

  private PendingRequest getPendingRequest() {
    return pendingRequest;
  }

  private Filter getQueryFilter(List<String> parts) {
    Filter filter = null;
    DateOrdering dateOrdering = Format.getDefaultDateOrdering();

    for (String part : parts) {
      Filter sub = null;
      for (IsColumn column : searchColumns) {
        sub = Filter.or(sub, Filter.compareWithValue(column, searchType, part, dateOrdering));
      }

      filter = Filter.and(filter, sub);
    }

    return filter;
  }

  private BeeRowSet getRequestData() {
    return requestData;
  }

  private Filter getResponseFilter() {
    return responseFilter;
  }

  private boolean hasResponseFilters() {
    return !exclusions.isEmpty() || getResponseFilter() != null;
  }

  private void initViewData() {
    CachingPolicy cachingPolicy = (getCaching() == Caching.GLOBAL && getAdditionalFilter() == null)
        ? CachingPolicy.FULL : CachingPolicy.NONE;

    Queries.getRowSet(getViewName(), null, getFilter(null, false), viewOrder, cachingPolicy,
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            setViewData(result);
            onDataReceived(getViewData());
            checkPendingRequest();
          }
        });
  }

  private boolean isDataInitialized() {
    return dataInitialized;
  }

  private boolean isEventRelevant(DataEvent event) {
    return event != null && event.hasView(getViewName())
        && getViewData() != null && isCachingEnabled();
  }

  private boolean matches(Filter filter, IsRow row) {
    if (row == null || getViewData() == null) {
      return false;
    } else if (filter == null) {
      return true;
    } else {
      return filter.isMatch(getViewData().getColumns(), row);
    }
  }

  private void onDataReceived(BeeRowSet rowSet) {
    for (Consumer<BeeRowSet> handler : dataReceivedHandlers) {
      handler.accept(rowSet);
    }
  }

  private void onRowCountChange(int count) {
    for (Consumer<Integer> handler : rowCountChangeHandlers) {
      handler.accept(count);
    }
  }

  private static List<String> parseQuery(String query) {
    List<String> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(query)) {
      if (BeeUtils.isQuoted(query)) {
        result.add(BeeUtils.notEmpty(BeeUtils.unquote(query), query));
      } else if (query.indexOf(BeeConst.CHAR_SPACE) > 0) {
        result.addAll(Splitter.on(BeeConst.CHAR_SPACE).splitToList(query));
      } else {
        result.add(query);
      }
    }

    return result;
  }

  private boolean prepareData(final Request request) {
    if (getLastRequest() != null && isCachingEnabled()
        && BeeUtils.equalsTrim(request.getQuery(), getLastRequest().getQuery())) {
      return true;
    }

    List<String> queryParts = parseQuery(request.getQuery());

    if (isFullCaching()) {
      if (getViewData() == null) {
        return false;
      }

      if (getRequestData() == null) {
        setRequestData(new BeeRowSet(getViewData().getColumns()));
      } else if (!getRequestData().isEmpty()) {
        getRequestData().getRows().clear();
      }

      if (getViewData().isEmpty()) {
        return true;
      }

      List<BeeColumn> columns = getViewData().getColumns();

      if (queryParts.isEmpty()) {
        if (hasResponseFilters()) {
          for (BeeRow row : getViewData()) {
            if (filterResponse(columns, row)) {
              getRequestData().addRow(row);
            }
          }

        } else {
          getRequestData().setRows(getViewData().getRows());
        }

      } else {
        if (searchType == Operator.CONTAINS) {
          int qc = queryParts.size();
          String[] values = new String[qc];

          for (int i = 0; i < qc; i++) {
            values[i] = queryParts.get(i).toLowerCase();
          }

          if (qc == 1 && !hasResponseFilters()) {
            String v = values[0];

            for (BeeRow row : getViewData()) {
              for (int index : searchIndexes) {
                String s = row.getString(index);
                if (s != null && s.toLowerCase().contains(v)) {
                  getRequestData().addRow(row);
                  break;
                }
              }
            }

          } else {
            boolean ok = false;

            for (BeeRow row : getViewData()) {
              for (String v : values) {
                ok = false;
                for (int index : searchIndexes) {
                  String s = row.getString(index);
                  if (s != null && s.toLowerCase().contains(v)) {
                    ok = true;
                    break;
                  }
                }

                if (!ok) {
                  break;
                }
              }

              if (ok && filterResponse(columns, row)) {
                getRequestData().addRow(row);
              }
            }
          }

        } else {
          Filter filter = getQueryFilter(queryParts);

          for (BeeRow row : getViewData()) {
            if (filter.isMatch(columns, row) && filterResponse(columns, row)) {
              getRequestData().addRow(row);
            }
          }
        }
      }
      return true;
    }

    int offset;
    int limit;
    if (isCachingEnabled()) {
      offset = BeeConst.UNDEF;
      limit = BeeConst.UNDEF;
    } else {
      offset = request.getOffset();
      limit = request.getLimit() + 1;
    }

    Queries.getRowSet(getViewName(), null, getFilter(getQueryFilter(queryParts), true), viewOrder,
        offset, limit, new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (getPendingRequest() == null) {
              return;
            }
            if (request.equals(getPendingRequest().getRequest())) {
              setRequestData(result);
              setLastRequest(request);
              Callback callback = getPendingRequest().getCallback();
              setPendingRequest(null);
              processRequest(request, callback);
            } else {
              checkPendingRequest();
            }
          }
        });

    return false;
  }

  private void processRequest(Request request, Callback callback) {
    int offset = request.getOffset();
    int limit = request.getLimit();

    List<Suggestion> suggestions = new ArrayList<>();
    boolean hasMore = false;

    if (getRequestData() != null && !getRequestData().isEmpty()) {
      int rowCount = getRequestData().getNumberOfRows();
      int start = isCachingEnabled() ? Math.max(offset, 0) : 0;
      int end = (limit > 0) ? Math.min(start + limit, rowCount) : rowCount;

      if (start < end) {
        for (int i = start; i < end; i++) {
          BeeRow row = getRequestData().getRow(i);
          suggestions.add(new Suggestion(row));
        }
        hasMore = end < rowCount;
      }
    }

    Response response = new Response(suggestions, hasMore);
    callback.onSuggestionsReady(request, response);
  }

  private void resetState() {
    setLastRequest(null);
  }

  private void setCaching(Caching caching) {
    this.caching = caching;
  }

  private void setDataInitialized(boolean dataInitialized) {
    this.dataInitialized = dataInitialized;
  }

  private void setLastRequest(Request lastRequest) {
    this.lastRequest = lastRequest;
  }

  private void setPendingRequest(PendingRequest pendingRequest) {
    this.pendingRequest = pendingRequest;
  }

  private void setRequestData(BeeRowSet requestData) {
    this.requestData = requestData;
  }

  private void setViewData(BeeRowSet viewData) {
    this.viewData = viewData;
  }
}