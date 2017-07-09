package com.butent.bee.client.view.search;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public class BooleanFilterSupplier extends AbstractFilterSupplier {

  private Boolean value;

  public BooleanFilterSupplier(String viewName, BeeColumn column, String label, String options) {
    super(viewName, column, label, options);
  }

  @Override
  public String getComponentLabel(String ownerLabel) {
    return BeeUtils.isEmpty(getColumnLabel()) ? super.getComponentLabel(ownerLabel) : getLabel();
  }

  @Override
  public FilterValue getFilterValue() {
    return (value == null) ? null : FilterValue.of(null, !value);
  }

  @Override
  public String getLabel() {
    if (value == null) {
      return null;
    } else {
      return value ? getLabelForNotEmpty() : getLabelForEmpty();
    }
  }

  @Override
  public void onRequest(Element target, Scheduler.ScheduledCommand onChange) {
    openDialog(target, createWidget(), null, onChange);
  }

  @Override
  public Filter parse(FilterValue input) {
    Boolean b = getBoolean(input);

    if (b == null) {
      return null;
    } else {
      return b ? Filter.notNull(getColumnId()) : Filter.isNull(getColumnId());
    }
  }

  @Override
  public void setFilterValue(FilterValue filterValue) {
    value = getBoolean(filterValue);
  }

  @Override
  protected String getStylePrefix() {
    return DEFAULT_STYLE_PREFIX + "boolean-";
  }

  protected static Boolean getBoolean(FilterValue filterValue) {
    if (filterValue == null) {
      return null;
    } else if (filterValue.hasValue()) {
      return BooleanValue.unpack(filterValue.getValue());
    } else if (filterValue.hasEmptiness()) {
      return !filterValue.getEmptyValues();
    } else {
      return null;
    }
  }

  private Widget createWidget() {
    HtmlTable container = new HtmlTable();
    container.addStyleName(getStylePrefix() + "container");

    Button notEmpty = new Button(getLabelForNotEmpty());
    notEmpty.addStyleName(getStylePrefix() + "notEmpty");

    notEmpty.addClickHandler(event -> {
      boolean changed = !BeeUtils.isTrue(value);
      value = true;
      update(changed);
    });

    container.setWidget(0, 0, notEmpty);

    Button empty = new Button(getLabelForEmpty());
    empty.addStyleName(getStylePrefix() + "empty");

    empty.addClickHandler(event -> {
      boolean changed = !BeeUtils.isFalse(value);
      value = false;
      update(changed);
    });

    container.setWidget(0, 1, empty);

    Button all = new Button(Localized.dictionary().filterAll());
    all.addStyleName(getStylePrefix() + "all");

    all.addClickHandler(event -> {
      boolean changed = value != null;
      value = null;
      update(changed);
    });

    container.setWidget(1, 0, all);

    Button cancel = new Button(Localized.dictionary().cancel());
    cancel.addStyleName(getStylePrefix() + "cancel");

    cancel.addClickHandler(event -> closeDialog());

    container.setWidget(1, 1, cancel);

    return container;
  }

  private String getLabelForEmpty() {
    return BeeUtils.isEmpty(getColumnLabel())
        ? NULL_VALUE_LABEL : Localized.dictionary().not(getColumnLabel());
  }

  private String getLabelForNotEmpty() {
    return BeeUtils.isEmpty(getColumnLabel()) ? NOT_NULL_VALUE_LABEL : getColumnLabel();
  }
}
