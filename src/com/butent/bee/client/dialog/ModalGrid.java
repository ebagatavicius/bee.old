package com.butent.bee.client.dialog;

import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;

public class ModalGrid extends ModalView implements ReadyEvent.Handler {

  public static PresenterCallback opener(int width, int height, boolean storeSize) {
    return opener(new Dimensions(width, height), storeSize);
  }

  public static PresenterCallback opener(double width, CssUnit widthUnit,
      double height, CssUnit heightUnit) {
    return opener(width, widthUnit, height, heightUnit, true);
  }

  public static PresenterCallback opener(double width, CssUnit widthUnit,
      double height, CssUnit heightUnit, boolean storeSize) {
    return opener(new Dimensions(width, widthUnit, height, heightUnit), storeSize);
  }

  public static PresenterCallback opener(final HasDimensions dimensions, final boolean storeSize) {
    return presenter -> {
      ModalGrid modalGrid = new ModalGrid(presenter, dimensions, storeSize);

      modalGrid.setAnimationEnabled(true);
      modalGrid.setHideOnEscape(true);

      modalGrid.cascade();
    };
  }

  public ModalGrid(Presenter presenter, HasDimensions dimensions, boolean storeSize) {
    super(presenter, BeeConst.CSS_CLASS_PREFIX + "ModalGrid", dimensions, storeSize);

    presenter.getMainView().addReadyHandler(this);
  }

  @Override
  public String getIdPrefix() {
    return "modal-grid";
  }

  @Override
  public void onReady(ReadyEvent event) {
    if (getWidget() instanceof HasGridView) {
      CellGrid grid = ((HasGridView) getWidget()).getGridView().getGrid();

      int dataSize = grid.getDataSize();
      int rowCount = grid.getRowCount();

      if (dataSize > 0 && dataSize == rowCount) {
        int childrenHeight = grid.getChildrenHeight();
        int offsetHeight = grid.getOffsetHeight();

        int reserve = DomUtils.getScrollBarHeight() * 2;
        int shrink = offsetHeight - childrenHeight - reserve;

        if (shrink > 0) {
          int height = StyleUtils.getHeight(this);
          if (height > shrink) {
            StyleUtils.setHeight(this, height - shrink);
          }
        }
      }
    }
  }
}
