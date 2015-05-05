package com.butent.bee.client.data;

import com.google.common.base.Splitter;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsColumn;
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
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Provides suggestions data management functionality for data changing events.
 */

public class SelectionOracle implements HandlesAllDataEvents, HasViewName {

  /**
   * Requires implementing classes to have a method to handle suggestions events with requests and
   * responses.
   */

  public interface Callback {
    void onSuggestionsReady(Request request, Response response);
  }

  /**
   * Contains fields and methods to handle suggestion related data queries.
   */

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

  public static final Relation.Caching DEFAULT_CACHING = Relation.Caching.GLOBAL;

  private final DataInfo dataInfo;

  private final int[] searchIndexes;
  private final List<IsColumn> searchColumns = new ArrayList<>();

  private final Operator searchType;

  private final Filter immutableFilter;
  private final Order viewOrder;

  private final Relation.Caching caching;

  private BeeRowSet viewData;
  private BeeRowSet requestData;

  private Request lastRequest;
  private PendingRequest pendingRequest;

  private final List<HandlerRegistration> handlerRegistry = new ArrayList<>();
  private final Set<Consumer<Integer>> rowCountChangeHandlers = new HashSet<>();
  private final Set<Consumer<BeeRowSet>> dataReceivedHandlers = new HashSet<>();

  private boolean dataInitialized;

  private Filter additionalFilter;
  private final Set<Long> exclusions = new HashSet<>();

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

    this.caching = (relation.getCaching() == null) ? DEFAULT_CACHING : relation.getCaching();

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

  public boolean isCachingEnabled() {
    return !Relation.Caching.NONE.equals(caching);
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
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event) && getViewData().removeRowById(event.getRowId())) {
      resetState();
      onRowCountChange(getViewData().getNumberOfRows());
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event) && !getViewData().containsRow(event.getRowId())) {
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

  private void checkPendingRequest() {
    if (getPendingRequest() != null) {
      Request request = getPendingRequest().getRequest();
      Callback callback = getPendingRequest().getCallback();
      setPendingRequest(null);
      requestSuggestions(request, callback);
    }
  }

  private Filter getFilter(Filter queryFilter, boolean checkExclusions) {
    CompoundFilter result = Filter.and();

    result.add(immutableFilter);
    result.add(getAdditionalFilter());
    result.add(queryFilter);

    if (checkExclusions && !exclusions.isEmpty()) {
      result.add(Filter.idNotIn(exclusions));
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

    for (String part : parts) {
      Filter sub = null;
      for (IsColumn column : searchColumns) {
        sub = Filter.or(sub, Filter.compareWithValue(column, searchType, part));
      }

      filter = Filter.and(filter, sub);
    }

    return filter;
  }

  private BeeRowSet getRequestData() {
    return requestData;
  }

  private void initViewData() {
    CachingPolicy cachingPolicy =
        Relation.Caching.GLOBAL.equals(caching) ? CachingPolicy.FULL : CachingPolicy.NONE;

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
    return event != null && BeeUtils.same(event.getViewName(), getViewName())
        && getViewData() != null && isCachingEnabled();
  }

  private boolean isFullCaching() {
    return Relation.Caching.LOCAL.equals(caching) || Relation.Caching.GLOBAL.equals(caching);
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
    if (getLastRequest() != null) {
      if (isCachingEnabled()) {
        if (BeeUtils.equalsTrim(request.getQuery(), getLastRequest().getQuery())) {
          return true;
        }
      } else if (request.equals(getLastRequest())) {
        return true;
      }
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

      if (queryParts.isEmpty()) {
        if (exclusions.isEmpty()) {
          getRequestData().setRows(getViewData().getRows());
        } else {
          for (BeeRow row : getViewData().getRows()) {
            if (!exclusions.contains(row.getId())) {
              getRequestData().addRow(row);
            }
          }
        }

      } else {
        if (searchType == Operator.CONTAINS) {
          int qc = queryParts.size();
          String[] values = new String[qc];

          for (int i = 0; i < qc; i++) {
            values[i] = queryParts.get(i).toLowerCase();
          }

          if (qc == 1 && exclusions.isEmpty()) {
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

              if (ok && !exclusions.contains(row.getId())) {
                getRequestData().addRow(row);
              }
            }
          }

        } else {
          Filter filter = getQueryFilter(queryParts);
          List<BeeColumn> columns = getViewData().getColumns();

          for (BeeRow row : getViewData()) {
            if (filter.isMatch(columns, row) && !exclusions.contains(row.getId())) {
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