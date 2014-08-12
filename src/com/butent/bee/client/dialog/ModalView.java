package com.butent.bee.client.dialog;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Window;

import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.shared.css.CssUnit;

public class ModalView extends Popup {

  private static final double MAX_WIDTH_FACTOR = 0.95;
  private static final double MAX_HEIGHT_FACTOR = 0.95;

  public ModalView(Presenter presenter, String styleName, HasDimensions dimensions) {
    super(OutsideClick.IGNORE, styleName);

    presenter.getMainView().addStyleName(styleName + "-content");
    setWidget(presenter.getMainView());

    if (dimensions != null) {
      setDimensions(dimensions);
    }
    if (presenter.getHeader() != null) {
      enableDragging();
    }
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

  private void setDimensions(HasDimensions dimensions) {
    double v;
    CssUnit u;

    if (dimensions.getWidthValue() != null) {
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
      if (u == CssUnit.PX) {
        v = Math.round(Math.min(v, Window.getClientWidth() * MAX_WIDTH_FACTOR));
      }
      StyleUtils.setWidth(getElement(), v, u);
    }

    if (dimensions.getHeightValue() != null) {
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
      if (u == CssUnit.PX) {
        v = Math.round(Math.min(v, Window.getClientHeight() * MAX_HEIGHT_FACTOR));
      }
      StyleUtils.setHeight(getElement(), v, u);
    }
  }
}
