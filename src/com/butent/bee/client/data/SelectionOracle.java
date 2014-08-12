package com.butent.bee.client.data;

import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
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
import com.butent.bee.shared.data.value.ValueType;
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
    private final Operator searchType;

    private final int offset;
    private final int limit;

    public Request(String query, Operator searchType, int offset, int limit) {
      this.query = query;
      this.searchType = searchType;
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
          && Objects.equals(getSearchType(), r.getSearchType())
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

    public Operator getSearchType() {
      return searchType;
    }

    @Override
    public int hashCode() {
      return Objects.hash(BeeUtils.trim(getQuery()), getSearchType(), getOffset(), getLimit());
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

  private static final String TRANSLATOR_COLUMN = "Original";

  private final DataInfo dataInfo;

  private final List<IsColumn> searchColumns = new ArrayList<>();

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

  private BeeRowSet translator;

  private Filter additionalFilter;
  private final Set<Long> exclusions = new HashSet<>();

  public SelectionOracle(Relation relation, DataInfo dataInfo) {
    Assert.notNull(relation);
    Assert.notNull(dataInfo);

    this.dataInfo = dataInfo;

    for (String colName : relation.getSearchableColumns()) {
      IsColumn column = DataUtils.getColumn(colName, dataInfo.getColumns());
      if (column != null) {
        this.searchColumns.add(column);
      }
    }

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

  public void createTranslator(List<String> values, int startIndex) {
    Assert.notEmpty(values);

    setTranslator(new BeeRowSet(new BeeColumn(ValueType.TEXT, TRANSLATOR_COLUMN)));
    for (int i = 0; i < values.size(); i++) {
      getTranslator().addRow(i + startIndex, new String[] {values.get(i)});
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
    for (HandlerRegistration entry : handlerRegistry) {
      if (entry != null) {
        entry.removeHandler();
      }
    }

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

  public void setAdditionalFilter(Filter additionalFilter) {
    if (Objects.equals(additionalFilter, this.additionalFilter)) {
      return;
    }
    this.additionalFilter = additionalFilter;

    clearData();
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

  private Filter getQueryFilter(String query, Operator searchType) {
    if (BeeUtils.isEmpty(query)) {
      return null;
    }

    if (hasTranslator()) {
      return translateQuery(query, searchType, searchColumns.get(0));
    }

    Filter filter = null;
    for (IsColumn column : searchColumns) {
      Filter flt = Filter.compareWithValue(column, searchType, query);
      if (flt == null) {
        continue;
      }
      if (filter == null) {
        filter = flt;
      } else {
        filter = Filter.or(filter, flt);
      }
    }
    return filter;
  }

  private BeeRowSet getRequestData() {
    return requestData;
  }

  private BeeRowSet getTranslator() {
    return translator;
  }

  private boolean hasTranslator() {
    return getTranslator() != null && !getTranslator().isEmpty();
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

  private boolean prepareData(final Request request) {
    if (getLastRequest() != null) {
      if (isCachingEnabled()) {
        if (BeeUtils.equalsTrim(request.getQuery(), getLastRequest().getQuery())
            && request.getSearchType() == getLastRequest().getSearchType()) {
          return true;
        }
      } else if (request.equals(getLastRequest())) {
        return true;
      }
    }

    final Filter filter = getQueryFilter(request.getQuery(), request.getSearchType());

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

      if (filter == null) {
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
        List<BeeColumn> columns = getViewData().getColumns();
        for (BeeRow row : getViewData().getRows()) {
          if (filter.isMatch(columns, row) && !exclusions.contains(row.getId())) {
            getRequestData().addRow(row);
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

    Queries.getRowSet(getViewName(), null, getFilter(filter, true), viewOrder,
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

  private void setTranslator(BeeRowSet translator) {
    this.translator = translator;
  }

  private void setViewData(BeeRowSet viewData) {
    this.viewData = viewData;
  }

  private Filter translateQuery(String query, Operator searchType, IsColumn targetColumn) {
    Filter filter = Filter.compareWithValue(getTranslator().getColumn(0), searchType, query);
    if (filter == null) {
      return null;
    }
    CompoundFilter result = Filter.or();

    List<BeeColumn> columns = getTranslator().getColumns();
    for (BeeRow row : getTranslator().getRows()) {
      if (filter.isMatch(columns, row)) {
        result.add(Filter.compareWithValue(targetColumn, Operator.EQ,
            BeeUtils.toString(row.getId())));
      }
    }

    if (result.isEmpty()) {
      return null;
    }
    return result;
  }
}