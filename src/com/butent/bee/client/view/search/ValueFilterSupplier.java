package com.butent.bee.client.view.search;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.ColumnValueFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterParser;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ValueFilterSupplier extends AbstractFilterSupplier {

  private static final String STYLE_PREFIX = DEFAULT_STYLE_PREFIX + "value-";

  private static final Splitter valueSplitter =
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults();

  private final List<BeeColumn> columns = new ArrayList<>();
  private final String idColumnName;
  private final String versionColumnName;

  private final List<BeeColumn> searchBy = new ArrayList<>();

  private final InputText editor;
  private final Label errorMessage;

  private Boolean emptiness;

  private String oldValue;

  public ValueFilterSupplier(String viewName, List<BeeColumn> columns,
      String idColumnName, String versionColumnName, BeeColumn column, String label,
      List<BeeColumn> searchColumns, String options) {

    super(viewName, column, label, options);

    this.columns.addAll(columns);
    this.idColumnName = idColumnName;
    this.versionColumnName = versionColumnName;

    if (BeeUtils.isEmpty(searchColumns) && column != null) {
      searchBy.add(column);
    } else {
      searchBy.addAll(searchColumns);
    }

    for (BeeColumn by : searchBy) {
      if (!DataUtils.contains(columns, by.getId())) {
        this.columns.add(by);
      }
    }

    this.editor = new InputText();
    editor.addStyleName(STYLE_PREFIX + "editor");

    if (!BeeUtils.isEmpty(viewName)) {
      AutocompleteProvider.enableAutocomplete(editor,
          BeeUtils.join(BeeConst.STRING_MINUS, viewName,
              BeeUtils.join(BeeConst.STRING_MINUS, searchBy), "filter"));
    }

    editor.addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
        ValueFilterSupplier.this.onSave();
      }
    });

    this.errorMessage = new Label();
    errorMessage.addStyleName(STYLE_PREFIX + "error");
  }

  @Override
  protected List<? extends IdentifiableWidget> getAutocompletableWidgets() {
    return Lists.newArrayList(editor);
  }

  @Override
  public FilterValue getFilterValue() {
    if (!BeeUtils.isEmpty(getEditorValue())) {
      return FilterValue.of(getEditorValue());
    } else if (getEmptiness() != null) {
      return FilterValue.of(null, getEmptiness());
    } else {
      return null;
    }
  }

  @Override
  public String getLabel() {
    if (!BeeUtils.isEmpty(getEditorValue())) {
      return getEditorValue();
    } else if (getEmptiness() != null) {
      return getEmptinessLabel(getEmptiness());
    } else {
      return null;
    }
  }

  @Override
  public void onRequest(Element target, Scheduler.ScheduledCommand onChange) {
    setOldValue(getEditorValue());

    Flow panel = new Flow(STYLE_PREFIX + "panel");

    CustomDiv caption = new CustomDiv(STYLE_PREFIX + "caption");
    caption.setHtml(getColumnLabel());
    panel.add(caption);

    panel.add(editor);

    errorMessage.clear();
    panel.add(errorMessage);

    if (hasEmptiness()) {
      CustomDiv separator = new CustomDiv(STYLE_PREFIX + "separator");
      panel.add(separator);

      Button notEmpty = new Button(NOT_NULL_VALUE_LABEL, event -> onEmptiness(false));
      notEmpty.addStyleName(STYLE_PREFIX + "notEmpty");
      panel.add(notEmpty);

      Button empty = new Button(NULL_VALUE_LABEL, event -> onEmptiness(true));
      empty.addStyleName(STYLE_PREFIX + "empty");
      panel.add(empty);
    }

    OpenEvent.Handler onOpen = event -> {
      editor.setFocus(true);
      if (!BeeUtils.isEmpty(getOldValue())) {
        editor.selectAll();
      }
    };

    openDialog(target, panel, onOpen, onChange);
  }

  @Override
  public Filter parse(FilterValue input) {
    if (input == null) {
      return null;

    } else if (input.hasValue()) {
      return buildFilter(BeeUtils.trim(input.getValue()));

    } else if (input.hasEmptiness()) {
      return input.getEmptyValues() ? buildIsEmpty() : buildNotEmpty();

    } else {
      return null;
    }
  }

  @Override
  public void setFilterValue(FilterValue filterValue) {
    if (filterValue == null) {
      setValue(null, null);
    } else {
      setValue(filterValue.getValue(), filterValue.getEmptyValues());
    }
  }

  @Override
  protected void onDialogCancel() {
    editor.setValue(getOldValue());
  }

  private Filter buildComparison(Operator operator, String value) {
    DateOrdering dateOrdering = Format.getDefaultDateOrdering();

    if (searchBy.size() <= 1) {
      return ColumnValueFilter.compareWithValue(getColumn(), operator, value, dateOrdering);

    } else {
      CompoundFilter filter = Filter.or();
      for (BeeColumn by : searchBy) {
        filter.add(ColumnValueFilter.compareWithValue(by, operator, value, dateOrdering));
      }
      return filter;
    }
  }

  private Filter buildFilter(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;

    } else if (value.contains(BeeConst.STRING_COMMA)) {
      CompoundFilter filter = Filter.or();
      for (String s : valueSplitter.split(value)) {
        filter.add(parseInput(s));
      }

      return filter;

    } else {
      return parseInput(value);
    }
  }

  private Filter buildIsEmpty() {
    if (searchBy.size() <= 1) {
      return Filter.isNull(getColumnId());

    } else {
      CompoundFilter filter = Filter.and();
      for (BeeColumn by : searchBy) {
        filter.add(Filter.isNull(by.getId()));
      }
      return filter;
    }
  }

  private Filter buildNotEmpty() {
    if (searchBy.size() <= 1) {
      return Filter.notNull(getColumnId());

    } else {
      CompoundFilter filter = Filter.or();
      for (BeeColumn by : searchBy) {
        filter.add(Filter.notNull(by.getId()));
      }
      return filter;
    }
  }

  private boolean containsColumnName(String input) {
    for (BeeColumn by : searchBy) {
      if (BeeUtils.containsSame(input, by.getId())) {
        return true;
      }
    }
    return false;
  }

  private String getEditorValue() {
    return BeeUtils.trim(editor.getValue());
  }

  private Boolean getEmptiness() {
    return emptiness;
  }

  private String getOldValue() {
    return oldValue;
  }

  private void onEmptiness(Boolean value) {
    boolean changed = !BeeUtils.isEmpty(getOldValue()) || !Objects.equals(getEmptiness(), value);
    setValue(null, value);
    update(changed);
  }

  private void onSave() {
    String input = getEditorValue();

    if (validate(input)) {
      boolean changed = !BeeUtils.equalsTrim(getOldValue(), input) || getEmptiness() != null;
      setEmptiness(null);
      update(changed);

    } else {
      errorMessage.setHtml(Localized.dictionary().error());
    }
  }

  private Filter parseInput(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    Operator operator = Operator.detectOperator(input);

    if (operator != null) {
      if (input.equals(operator.toTextString())) {
        if (operator == Operator.EQ || operator == Operator.LT || operator == Operator.LE) {
          return buildIsEmpty();
        } else {
          return buildNotEmpty();
        }

      } else {
        return buildComparison(operator,
            BeeUtils.removePrefix(input, operator.toTextString()).trim());
      }

    } else if (containsColumnName(input)) {
      return FilterParser.parse(input, columns, idColumnName, versionColumnName,
          BeeKeeper.getUser().getUserId());

    } else {
      operator = ValueType.isString(getColumnType()) ? Operator.CONTAINS : Operator.EQ;
      return buildComparison(operator, input);
    }
  }

  private void setEmptiness(Boolean emptiness) {
    this.emptiness = emptiness;
  }

  private void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  private void setValue(String value, Boolean empt) {
    editor.setValue(value);
    this.emptiness = empt;
  }

  protected boolean validate(String input) {
    if (BeeUtils.isEmpty(input)) {
      return true;

    } else if (input.contains(BeeConst.STRING_COMMA)) {
      for (String s : valueSplitter.split(input)) {
        Filter filter = parseInput(s);
        if (filter == null) {
          return false;
        }
      }
      return true;

    } else {
      return parseInput(input) != null;
    }
  }
}
