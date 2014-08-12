package com.butent.bee.client.grid;

import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.ReadyEvent.HasReadyHandlers;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;

public class GridPanel extends Simple implements EnablableWidget, HasFosterParent,
    ParentRowEvent.Handler, HasGridView, ReadyEvent.HasReadyHandlers {

  private final String gridName;
  private GridFactory.GridOptions gridOptions;
  private final boolean child;

  private Presenter presenter;
  private GridInterceptor gridInterceptor;

  private String parentId;
  private HandlerRegistration parentRowReg;

  public GridPanel(String gridName, GridFactory.GridOptions gridOptions, boolean child) {
    super();

    this.gridName = gridName;
    this.gridOptions = gridOptions;
    this.child = child;

    this.gridInterceptor = GridFactory.getGridInterceptor(gridName);

    addStyleName("bee-GridPanel");
  }

  public GridFactory.GridOptions getGridOptions() {
    return gridOptions;
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {

    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public GridView getGridView() {
    if (getPresenter() instanceof HasGridView) {
      return ((HasGridView) getPresenter()).getGridView();
    } else {
      return null;
    }
  }

  @Override
  public String getIdPrefix() {
    return "grid-panel";
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  public Presenter getPresenter() {
    return presenter;
  }

  @Override
  public boolean isEnabled() {
    if (getPresenter() == null) {
      return false;
    }
    return getPresenter().getMainView().isEnabled();
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (getGridInterceptor() != null) {
      getGridInterceptor().onParentRow(event);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (getPresenter() != null) {
      getPresenter().getMainView().setEnabled(enabled);
    }
  }

  public void setGridInterceptor(GridInterceptor gridInterceptor) {
    this.gridInterceptor = gridInterceptor;
  }

  public void setGridOptions(GridFactory.GridOptions gridOptions) {
    this.gridOptions = gridOptions;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
    if (isAttached()) {
      register();
    }
  }

  @Override
  public void setWidget(Widget w) {
    if (w != null) {
      StyleUtils.makeAbsolute(w);

      if (w instanceof HasReadyHandlers) {
        ReadyEvent.maybeDelegate(this, (HasReadyHandlers) w);
      }
    }

    super.setWidget(w);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    register();

    if (getPresenter() == null) {
      GridInterceptor gic = getGridInterceptor();

      UiOption uiOption = child ? UiOption.CHILD : UiOption.EMBEDDED;

      GridFactory.createGrid(getGridName(), GridFactory.getSupplierKey(getGridName(), gic), gic,
          EnumSet.of(uiOption), getGridOptions(), new PresenterCallback() {
            @Override
            public void onCreate(Presenter gp) {
              if (gp != null) {
                setPresenter(gp);
                setWidget(gp.getMainView());
                gp.setEventSource(getId());
              }
            }
          });
    }
  }

  private GridInterceptor getGridInterceptor() {
    return gridInterceptor;
  }

  private String getGridName() {
    return gridName;
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

  private void setPresenter(Presenter presenter) {
    this.presenter = presenter;
  }

  private void unregister() {
    if (getParentRowReg() != null) {
      getParentRowReg().removeHandler();
      setParentRowReg(null);
    }
  }
}
