package com.butent.bee.client.ui;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Set;

public interface WidgetInterceptor extends HasCaption, HasWidgetSupplier {

  void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback);

  boolean beforeCreateWidget(String name, Element description);

  void configureRelation(String name, Relation relation);

  IdentifiableWidget createCustomWidget(String name, Element description);

  Boolean getBooleanValue(String source);

  List<BeeColumn> getDataColumns();

  int getDataIndex(String source);

  DateTime getDateTimeValue(String source);

  JustDate getDateValue(String source);

  Set<Action> getDisabledActions(Set<Action> defaultActions);

  Set<Action> getEnabledActions(Set<Action> defaultActions);

  default <E extends Enum<?>> E getEnumValue(String source, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getIntegerValue(source));
  }

  HeaderView getHeaderView();

  Integer getIntegerValue(String source);

  Long getLongValue(String source);

  String getStringValue(String source);

  default boolean startCommand(String styleName, int duration) {
    HeaderView headerView = getHeaderView();

    if (headerView == null) {
      return false;
    } else {
      return headerView.startCommandByStyleName(styleName, duration);
    }
  }

  default boolean endCommand(String styleName) {
    return endCommand(styleName, false);
  }

  default boolean endCommand(String styleName, boolean disableAnimation) {
    HeaderView headerView = getHeaderView();

    if (headerView == null) {
      return false;
    } else {
      return headerView.stopCommandByStyleName(styleName, disableAnimation);
    }
  }
}
