package com.butent.bee.client.modules.service;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;

abstract class MaintenanceExpanderForm extends AbstractFormInterceptor {

  private static final String NAME_SOUTH_EXPANDER = "SouthExpander";
  private static final String NAME_SPLIT = "Split";
  private static final double DEFAULT_SOUTH_PERCENT = 50d;
  private int southExpandedFrom;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {
    if (BeeUtils.same(name, NAME_SOUTH_EXPANDER) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(event -> {
        if (event.getSource() instanceof HasCheckedness) {
          boolean expand = ((HasCheckedness) event.getSource()).isChecked();

          int top = BeeConst.UNDEF;
          if (event.getSource() instanceof HasOptions) {
            top = BeeUtils.toInt(((HasOptions) event.getSource()).getOptions());
          }

          expandSouth(expand, top);
        }
      });
    }
  }

  private void expandSouth(boolean expand, int top) {
    Split split = getSplit(getFormView());

    if (split != null) {
      int splitHeight = split.getOffsetHeight();

      int oldSize = split.getDirectionSize(Direction.SOUTH);
      int newSize;

      if (expand) {
        newSize = splitHeight - top;
        setSouthExpandedFrom(oldSize);

      } else {
        newSize = southExpandedFrom;
        setSouthExpandedFrom(BeeConst.UNDEF);
      }

      if (splitHeight <= 2) {
        newSize = oldSize;

      } else if (newSize <= 0 || newSize >= splitHeight) {
        if (expand) {
          newSize = BeeUtils.percent(splitHeight, 90d);
        } else {
          newSize = BeeUtils.percent(splitHeight, DEFAULT_SOUTH_PERCENT);
        }

        newSize = BeeUtils.clamp(newSize, 1, splitHeight - 1);
      }

      if (oldSize != newSize) {
        split.setDirectionSize(Direction.SOUTH, newSize, true);
      }
    }
  }

  private static Split getSplit(FormView form) {
    Widget widget = (form == null) ? null : form.getWidgetByName(NAME_SPLIT);
    return (widget instanceof Split) ? (Split) widget : null;
  }

  private void setSouthExpandedFrom(int southExpandedFrom) {
    this.southExpandedFrom = southExpandedFrom;
  }
}
