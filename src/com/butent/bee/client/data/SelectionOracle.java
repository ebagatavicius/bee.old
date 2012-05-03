package com.butent.bee.client.data;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

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
          && Objects.equal(getSearchType(), r.getSearchType())
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
      return Objects.hashCode(BeeUtils.trim(getQuery()), getSearchType(), getOffset(), getLimit());
    }
  }

  /**
   * Contains fields and methods to handle suggestion related data responses.
   */

  public static class Response {

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

  private class PendingRequest {
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

  private final DataInfo viewInfo;

  private final List<IsColumn> searchColumns = Lists.newArrayList();

  private final Filter viewFilter;
  private final Order viewOrder;

  private final Relation.Caching caching;

  private BeeRowSet viewData = null;
  private BeeRowSet requestData = null;

  private Request lastRequest = null;
  private PendingRequest pendingRequest = null;

  private final List<HandlerRegistration> handlerRegistry = Lists.newArrayList();

  private boolean dataInitialized = false;

  public SelectionOracle(Relation relation, DataInfo viewInfo) {
    Assert.notNull(relation);
    Assert.notNull(viewInfo);
    
    this.viewInfo = viewInfo;

    for (String colName : relation.getSearchableColumns()) {
      IsColumn column = DataUtils.getColumn(colName, viewInfo.getColumns());
      if (column != null) {
        this.searchColumns.add(column);
      }
    }

    this.viewFilter = relation.getFilter();
    this.viewOrder = relation.getOrder();

    this.caching = (relation.getCaching() == null) ? DEFAULT_CACHING : relation.getCaching();

    this.handlerRegistry.addAll(BeeKeeper.getBus().registerDataHandler(this));
  }

  public String getViewName() {
    return viewInfo.getViewName();
  }

  public void onCellUpdate(CellUpdateEvent event) {
    if (isEventRelevant(event)
        && getViewData().updateCell(event.getRowId(), event.getColumnIndex(), event.getValue())) {
      setLastRequest(null);
    }
  }

  public void onMultiDelete(MultiDeleteEvent event) {
    if (isEventRelevant(event)) {
      for (RowInfo rowInfo : event.getRows()) {
        getViewData().removeRowById(rowInfo.getId());
      }
      setLastRequest(null);
    }
  }

  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event) && getViewData().removeRowById(event.getRowId())) {
      setLastRequest(null);
    }
  }

  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event) && !getViewData().containsRow(event.getRowId())) {
      getViewData().addRow(event.getRow());
      setLastRequest(null);
    }
  }

  public void onRowUpdate(RowUpdateEvent event) {
    if (isEventRelevant(event) && getViewData().updateRow(event.getRow())) {
      setLastRequest(null);
    }
  }

  public void onUnload() {
    for (HandlerRegistration entry : handlerRegistry) {
      if (entry != null) {
        entry.removeHandler();
      }
    }
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

  private void checkPendingRequest() {
    if (getPendingRequest() != null) {
      Request request = getPendingRequest().getRequest();
      Callback callback = getPendingRequest().getCallback();
      setPendingRequest(null);
      requestSuggestions(request, callback);
    }
  }

  private Filter getFilter(String query, Operator searchType) {
    if (BeeUtils.isEmpty(query)) {
      return null;
    }
    Filter filter = null;

    for (IsColumn column : searchColumns) {
      Filter flt = ComparisonFilter.compareWithValue(column, searchType, query);
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

  private Request getLastRequest() {
    return lastRequest;
  }

  private PendingRequest getPendingRequest() {
    return pendingRequest;
  }

  private BeeRowSet getRequestData() {
    return requestData;
  }

  private BeeRowSet getViewData() {
    return viewData;
  }

  private void initViewData() {
    CachingPolicy cachingPolicy =
        Relation.Caching.GLOBAL.equals(caching) ? CachingPolicy.FULL : CachingPolicy.NONE;

    Queries.getRowSet(getViewName(), null, viewFilter, viewOrder, cachingPolicy,
        new Queries.RowSetCallback() {
          public void onSuccess(BeeRowSet result) {
            setViewData(result);
            checkPendingRequest();
          }
        });
  }

  private boolean isCachingEnabled() {
    return !Relation.Caching.NONE.equals(caching);
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

    final Filter filter = getFilter(request.getQuery(), request.getSearchType());
    if (filter == null && !isCachingEnabled()) {
      setRequestData(null);
      return true;
    }

    if (isFullCaching()) {
      if (getViewData() == null) {
        return false;
      }

      if (getRequestData() == null) {
        setRequestData(new BeeRowSet(getViewData().getColumns()));
      } else {
        getRequestData().getRows().clear();
      }

      for (BeeRow row : getViewData().getRows()) {
        if (filter == null || filter.isMatch(getViewData().getColumns(), row)) {
          getRequestData().addRow(row);
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

    Queries.getRowSet(getViewName(), null, Filter.and(viewFilter, filter), viewOrder,
        offset, limit, new Queries.RowSetCallback() {
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

    List<Suggestion> suggestions = Lists.newArrayList();
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