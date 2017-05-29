package com.butent.bee.client.grid;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class EmbeddedGrid extends Simple implements EnablableWidget, HasFosterParent,
    ParentRowEvent.Handler, HasGridView, ReadyEvent.HasReadyHandlers, HasSummaryChangeHandlers {

  private final String gridName;
  private final GridFactory.GridOptions gridOptions;

  private GridPresenter presenter;
  private GridInterceptor gridInterceptor;

  private String parentId;
  private HandlerRegistration parentRowReg;

  private boolean summarize = true;

  private final List<SummaryChangeEvent.Handler> pendingSummaryChangeHandlers = new ArrayList<>();
  private final Map<EventHandler, HandlerRegistration> gridHandlerRegistry = new HashMap<>();

  EmbeddedGrid(String gridName, GridFactory.GridOptions gridOptions) {
    super();

    this.gridName = gridName;
    this.gridOptions = gridOptions;

    this.gridInterceptor = GridFactory.getGridInterceptor(gridName);
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {

    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addSummaryChangeHandler(
      final SummaryChangeEvent.Handler handler) {

    GridView gridView = getGridView();
    if (gridView == null) {
      pendingSummaryChangeHandlers.add(handler);

      return () -> {
        HandlerRegistration registration = gridHandlerRegistry.get(handler);
        if (registration != null) {
          registration.removeHandler();
        }
      };

    } else {
      return gridView.addSummaryChangeHandler(handler);
    }
  }

  public GridInterceptor getGridInterceptor() {
    return gridInterceptor;
  }

  @Override
  public GridView getGridView() {
    return (getPresenter() == null) ? null : getPresenter().getGridView();
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  public GridPresenter getPresenter() {
    return presenter;
  }

  @Override
  public Value getSummary() {
    GridView gridView = getGridView();
    return (gridView == null) ? BooleanValue.NULL : gridView.getSummary();
  }

  @Override
  public boolean isEnabled() {
    if (getPresenter() == null) {
      return false;
    } else {
      return getPresenter().getMainView().isEnabled();
    }
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (getGridInterceptor() != null) {
      getGridInterceptor().onParentRow(event);
    }
  }

  public void setGridInterceptor(GridInterceptor gridInterceptor) {
    this.gridInterceptor = gridInterceptor;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
    if (isAttached()) {
      register();
    }
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;

    GridView gridView = getGridView();
    if (gridView != null) {
      gridView.setSummarize(summarize);
    }
  }

  @Override
  public void setWidget(Widget w) {
    if (w != null) {
      StyleUtils.makeAbsolute(w);

      if (w instanceof ReadyEvent.HasReadyHandlers) {
        ReadyEvent.maybeDelegate(this, (ReadyEvent.HasReadyHandlers) w);
      }
    }

    super.setWidget(w);
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  protected void afterCreateGrid(GridView gridView) {
    if (gridView != null) {
      gridView.setSummarize(summarize());

      if (!pendingSummaryChangeHandlers.isEmpty()) {
        for (SummaryChangeEvent.Handler handler : pendingSummaryChangeHandlers) {
          gridHandlerRegistry.put(handler, gridView.addSummaryChangeHandler(handler));
        }

        pendingSummaryChangeHandlers.clear();

        if (summarize() && !BeeConst.isUndef(gridView.getGrid().getRowCount())) {
          SummaryChangeEvent.fire(gridView);
        }
      }
    }
  }

  protected String getGridKey(String suffix) {
    return GridFactory.getSupplierKey(BeeUtils.join(BeeConst.STRING_UNDER, gridName, suffix),
        getGridInterceptor());
  }

  protected String getGridName() {
    return gridName;
  }

  protected GridFactory.GridOptions getGridOptions() {
    return gridOptions;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    register();
  }

  @Override
  protected void onUnload() {
    unregister();
    super.onUnload();
  }

  protected void setPresenter(GridPresenter presenter) {
    this.presenter = presenter;
  }

  private HandlerRegistration getParentRowReg() {
    return parentRowReg;
  }

  private void register() {
    unregister();
    if (!BeeUtils.isEmpty(getParentId())) {
      setParentRowReg(BeeKeeper.getBus().registerParentRowHandler(getParentId(), this, false));
    }
  }

  private void setParentRowReg(HandlerRegistration parentRowReg) {
    this.parentRowReg = parentRowReg;
  }

  private void unregister() {
    if (getParentRowReg() != null) {
      getParentRowReg().removeHandler();
      setParentRowReg(null);
    }
  }
}
