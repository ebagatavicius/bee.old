package com.butent.bee.client.output;

import com.butent.bee.client.composite.TabBar;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Collections;

public class ReportBooleanItem extends ReportItem {

  private TabBar filterWidget;
  private Boolean filter;

  public ReportBooleanItem(String expression, String caption) {
    super(expression, caption);
  }

  @Override
  public void clearFilter() {
    if (filterWidget != null) {
      filterWidget.selectTab(0);
    } else {
      filter = null;
    }
  }

  @Override
  public void deserialize(String data) {
    if (data != null) {
      setFilter(Codec.deserializeLinkedHashMap(data).get(Service.VAR_DATA));
    }
  }

  @Override
  public ResultValue evaluate(SimpleRow row, Dictionary dictionary) {
    String display;
    boolean on = BeeUtils.unbox(row.getBoolean(getExpression()));

    if (on) {
      display = dictionary.yes();
    } else {
      display = dictionary.no();
    }
    return ResultValue.of(Boolean.toString(on)).setDisplay(display);
  }

  @Override
  public Boolean getFilter() {
    return filter;
  }

  @Override
  public TabBar getFilterWidget() {
    if (filterWidget == null) {
      Dictionary loc = Localized.dictionary();
      filterWidget = new TabBar(Orientation.HORIZONTAL);
      filterWidget.addStyleName(getStyle() + "-filter");

      filterWidget.addItems(Arrays.asList(loc.noMatter(), loc.yes(), loc.no()));

      filterWidget.addSelectionHandler(event -> filter = event.getSelectedItem() == 0
          ? null : BeeUtils.toBoolean(event.getSelectedItem()));
      filterWidget.selectTab(filter == null ? 0 : (filter ? 1 : 2));
    }
    return filterWidget;
  }

  @Override
  public String getStyle() {
    return STYLE_BOOLEAN;
  }

  @Override
  public String serialize() {
    String data = null;

    if (filter != null) {
      data = Codec.beeSerialize(Collections.singletonMap(Service.VAR_DATA, filter));
    }
    return serialize(data);
  }

  @Override
  public ReportItem setFilter(String value) {
    filter = BeeUtils.toBooleanOrNull(value);
    return this;
  }

  @Override
  public boolean validate(SimpleRow row) {
    if (filter == null || !row.getRowSet().hasColumn(getExpression())) {
      return true;
    }
    return BeeUtils.unbox(row.getBoolean(getExpression())) == filter;
  }
}
