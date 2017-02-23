package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.modules.documents.RelatedDocumentsHandler;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.Map;

class TransportDocumentsGrid extends RelatedDocumentsHandler {

  private static final BeeLogger logger = LogUtils.getLogger(TransportDocumentsGrid.class);

  private String parentColumn;
  private final String childColumn;

  private Long pendingId;

  TransportDocumentsGrid(String childColumn) {
    super();
    this.childColumn = childColumn;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    maybeRefresh(presenter, getPendingId());
    setPendingId(null);
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    return Provider.createDefaultParentFilters(getFilter(getPendingId()));
  }

  @Override
  public GridInterceptor getInstance() {
    return new TransportDocumentsGrid(childColumn);
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (BeeUtils.isEmpty(getParentColumn())) {
      switch (Data.getViewTable(event.getViewName())) {
        case TBL_ORDER_CARGO:
          setParentColumn(COL_CARGO);
          break;
        case TBL_ORDERS:
          setParentColumn(COL_TRANSPORTATION_ORDER);
          break;
        case TBL_TRIPS:
          setParentColumn(COL_TRIP);
          break;
        default:
          logger.severe(NameUtils.getName(this), event.getViewName(), "not supported");
      }
    }

    if (getGridPresenter() == null) {
      setPendingId(event.getRowId());
    } else {
      maybeRefresh(getGridPresenter(), event.getRowId());
    }
  }

  private Filter getFilter(Long parentId) {
    if (DataUtils.isId(parentId) && !BeeUtils.isEmpty(getParentColumn())) {
      switch (getParentColumn()) {
        case COL_CARGO:
          switch (childColumn) {
            case COL_TRANSPORTATION_ORDER:
              return Filter.in(COL_TRANSPORTATION_ORDER, VIEW_ORDER_CARGO, COL_ORDER,
                  Filter.compareId(parentId));
            case COL_TRIP:
              return Filter.in(COL_TRIP, VIEW_CARGO_TRIPS, COL_TRIP,
                  Filter.equals(COL_CARGO, parentId));
          }
          break;

        case COL_TRANSPORTATION_ORDER:
          switch (childColumn) {
            case COL_CARGO:
              return Filter.in(COL_CARGO, VIEW_ORDER_CARGO, COL_CARGO_ID,
                  Filter.equals(COL_ORDER, parentId));
            case COL_TRIP:
              return Filter.in(COL_TRIP, VIEW_CARGO_TRIPS, COL_TRIP,
                  Filter.in(COL_CARGO, VIEW_ORDER_CARGO, COL_CARGO_ID,
                      Filter.equals(COL_ORDER, parentId)));
          }
          break;

        case COL_TRIP:
          switch (childColumn) {
            case COL_CARGO:
              return Filter.in(COL_CARGO, VIEW_CARGO_TRIPS, COL_CARGO,
                  Filter.equals(COL_TRIP, parentId));
            case COL_TRANSPORTATION_ORDER:
              return Filter.in(COL_TRANSPORTATION_ORDER, VIEW_ORDER_CARGO, COL_ORDER,
                  Filter.in(COL_CARGO_ID, VIEW_CARGO_TRIPS, COL_CARGO,
                      Filter.equals(COL_TRIP, parentId)));
          }
          break;
      }

      logger.severe(NameUtils.getName(this), "relation", getParentColumn(), childColumn,
          "not supported");
    }

    return Filter.isFalse();
  }

  private String getParentColumn() {
    return parentColumn;
  }

  private Long getPendingId() {
    return pendingId;
  }

  private void maybeRefresh(GridPresenter presenter, Long parentId) {
    if (presenter != null) {
      Filter filter = getFilter(parentId);
      boolean changed = presenter.getDataProvider().setDefaultParentFilter(filter);

      if (changed) {
        presenter.handleAction(Action.REFRESH);
      }
    }
  }

  private void setParentColumn(String parentColumn) {
    this.parentColumn = parentColumn;
  }

  private void setPendingId(Long pendingId) {
    this.pendingId = pendingId;
  }
}
