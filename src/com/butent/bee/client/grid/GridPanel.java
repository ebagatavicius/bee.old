package com.butent.bee.client.grid;

import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;

import java.util.EnumSet;

public class GridPanel extends EmbeddedGrid {

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "GridPanel";

  private final boolean child;

  public GridPanel(String gridName, GridFactory.GridOptions gridOptions, boolean child) {
    super(gridName, gridOptions);

    this.child = child;

    addStyleName(STYLE_NAME);
  }

  @Override
  public String getIdPrefix() {
    return "grid-panel";
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (getPresenter() != null) {
      getPresenter().getMainView().setEnabled(enabled);
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (getPresenter() == null) {
      GridInterceptor gic = getGridInterceptor();

      UiOption uiOption = child ? UiOption.CHILD : UiOption.EMBEDDED;

      GridFactory.createGrid(getGridName(), GridFactory.getSupplierKey(getGridName(), gic), gic,
          EnumSet.of(uiOption), getGridOptions(), gp -> {
            if (gp instanceof GridPresenter) {
              setPresenter((GridPresenter) gp);
              setWidget(gp.getMainView());
              gp.setEventSource(getId());

              afterCreateGrid(getGridView());
            }
          });
    }
  }
}
