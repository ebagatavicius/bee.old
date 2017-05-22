package com.butent.bee.client.dialog;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Window;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class ModalView extends Popup {

  private static final double MAX_WIDTH_FACTOR = 0.95;
  private static final double MAX_HEIGHT_FACTOR = 0.95;

  private final String storageKey;

  public ModalView(Presenter presenter, String styleName, HasDimensions dimensions,
      boolean storeSize) {

    super(OutsideClick.IGNORE, styleName);

    presenter.getMainView().addStyleName(styleName + "-content");
    setWidget(presenter.getMainView());

    int width = BeeConst.UNDEF;
    int height = BeeConst.UNDEF;

    String key = storeSize ? presenter.getViewKey() : null;

    if (BeeUtils.isEmpty(key)) {
      this.storageKey = null;

    } else {
      this.storageKey = BeeUtils.join(BeeConst.STRING_MINUS,
          key, BeeKeeper.getUser().getUserId(), "size");

      List<Integer> values = BeeUtils.toInts(BeeKeeper.getStorage().get(storageKey));
      if (values.size() == 2) {
        width = values.get(0);
        height = values.get(1);
      }
    }

    setDimensions(width, height, dimensions);
    setResizable(true);
  }

  @Override
  protected void afterResize() {
    super.afterResize();

    if (!BeeUtils.isEmpty(storageKey)) {
      int width = getElement().getClientWidth();
      int height = getElement().getClientHeight();

      BeeKeeper.getStorage().set(storageKey, BeeUtils.join(BeeConst.STRING_COMMA, width, height));
    }
  }

  @Override
  protected int getHeaderHeight() {
    HeaderView header = ViewHelper.getHeader(getWidget());
    return (header == null) ? super.getHeaderHeight() : header.getHeight();
  }

  @Override
  protected boolean isCaptionEvent(NativeEvent event) {
    HeaderView header = ViewHelper.getHeader(getWidget());
    if (header == null) {
      return false;
    }

    EventTarget target = event.getEventTarget();
    if (Element.is(target)) {
      Element el = Element.as(target);
      return header.asWidget().getElement().isOrHasChild(el) && !header.isActionOrCommand(el);
    } else {
      return false;
    }
  }

  private void setDimensions(int width, int height, HasDimensions dimensions) {
    double v;
    CssUnit u;

    if (width > 0) {
      v = Math.min(width, Window.getClientWidth() - 2);
      u = CssUnit.PX;

    } else if (dimensions != null && dimensions.getWidthValue() != null) {
      v = dimensions.getWidthValue();
      u = Dimensions.normalizeUnit(dimensions.getWidthUnit());
      if (u.isPercentage()) {
        v = Window.getClientWidth() * v / 100;
        u = CssUnit.PX;
      }

    } else {
      v = Window.getClientWidth() / 2;
      u = CssUnit.PX;
    }

    if (v > 0) {
      if (u == CssUnit.PX && width <= 0) {
        v = Math.round(Math.min(v, Window.getClientWidth() * MAX_WIDTH_FACTOR));
      }
      StyleUtils.setWidth(getElement(), v, u);
    }

    if (height > 0) {
      v = Math.min(height, Window.getClientHeight() - 2);
      u = CssUnit.PX;

    } else if (dimensions != null && dimensions.getHeightValue() != null) {
      v = dimensions.getHeightValue();
      u = Dimensions.normalizeUnit(dimensions.getHeightUnit());
      if (u.isPercentage()) {
        v = Window.getClientHeight() * v / 100;
        u = CssUnit.PX;
      }

    } else {
      v = Window.getClientHeight() / 2;
      u = CssUnit.PX;
    }

    if (v > 0) {
      if (u == CssUnit.PX && height <= 0) {
        v = Math.round(Math.min(v, Window.getClientHeight() * MAX_HEIGHT_FACTOR));
      }
      StyleUtils.setHeight(getElement(), v, u);
    }
  }
}
