package com.butent.bee.client.view.search;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorFactory;
import com.butent.bee.client.view.edit.SimpleEditorHandler;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Objects;

public class RangeFilterSupplier extends AbstractFilterSupplier {

  private static final String STYLE_PREFIX = DEFAULT_STYLE_PREFIX + "range-";

  private static final String STYLE_FROM = STYLE_PREFIX + "from";
  private static final String STYLE_TO = STYLE_PREFIX + "to";

  private static final String STYLE_SUFFIX_WRAPPER = "-wrapper";
  private static final String STYLE_SUFFIX_LABEL = "-label";
  private static final String STYLE_SUFFIX_INPUT = "-input";

  private static final String STYLE_EMPTINESS = STYLE_PREFIX + "emptiness";
  private static final String STYLE_NOT_NULL = STYLE_PREFIX + "notNull";
  private static final String STYLE_NULL = STYLE_PREFIX + "null";

  private static final Operator LOWER_OPERATOR = Operator.GE;
  private static final Operator UPPER_OPERATOR = Operator.LT;

  private static Pair<String, String> parseRange(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    }

    String lower = null;
    String upper = null;

    int i = 0;
    for (String s : Splitter.on(BeeConst.CHAR_COMMA).trimResults().split(value)) {
      if (i == 0) {
        lower = s;
      } else if (i == 1) {
        upper = s;
      }

      i++;
    }

    return Pair.of(lower, upper);
  }

  private final Editor inputFrom;
  private final Editor inputTo;

  private Boolean emptiness;

  private final Pair<String, String> oldValue = Pair.of(null, null);

  private Widget widget;

  public RangeFilterSupplier(String viewName, BeeColumn column, String label, String options) {
    super(viewName, column, label, options);

    this.inputFrom = EditorFactory.createEditor(column, false);
    inputFrom.addStyleName(STYLE_FROM + STYLE_SUFFIX_INPUT);

    this.inputTo = EditorFactory.createEditor(column, false);
    inputTo.addStyleName(STYLE_TO + STYLE_SUFFIX_INPUT);

    if (!BeeUtils.isEmpty(viewName) && AutocompleteProvider.isAutocompleteCandidate(inputFrom)) {
      AutocompleteProvider.enableAutocomplete(inputFrom,
          BeeUtils.join(BeeConst.STRING_MINUS, viewName, column.getId(), "filter-from"));

      AutocompleteProvider.enableAutocomplete(inputTo,
          BeeUtils.join(BeeConst.STRING_MINUS, viewName, column.getId(), "filter-to"));
    }
  }

  @Override
  protected List<? extends IdentifiableWidget> getAutocompletableWidgets() {
    if (AutocompleteProvider.isAutocompleteCandidate(inputFrom)) {
      return Lists.newArrayList(inputFrom, inputTo);
    } else {
      return super.getAutocompletableWidgets();
    }
  }

  @Override
  public FilterValue getFilterValue() {
    String lower = getLowerValue();
    String upper = getUpperValue();

    if (!BeeUtils.allEmpty(lower, upper)) {
      StringBuilder sb = new StringBuilder();
      if (!BeeUtils.isEmpty(lower)) {
        sb.append(lower);
      }
      if (!BeeUtils.isEmpty(upper)) {
        sb.append(BeeConst.CHAR_COMMA).append(upper);
      }

      return FilterValue.of(sb.toString());

    } else if (getEmptiness() != null) {
      return FilterValue.of(null, getEmptiness());

    } else {
      return null;
    }
  }

  @Override
  public String getLabel() {
    String lower = getLowerValue();
    String upper = getUpperValue();

    if (!BeeUtils.allEmpty(lower, upper)) {
      if (BeeUtils.isEmpty(lower)) {
        return BeeUtils.joinWords(UPPER_OPERATOR.toTextString(), upper);
      } else if (BeeUtils.isEmpty(upper)) {
        return BeeUtils.joinWords(LOWER_OPERATOR.toTextString(), lower);
      } else if (lower.equals(upper)) {
        return lower;
      } else {
        return BeeUtils.join(" - ", lower, upper);
      }

    } else if (getEmptiness() != null) {
      return getEmptinessLabel(getEmptiness());

    } else {
      return null;
    }
  }

  @Override
  public void onRequest(Element target, Scheduler.ScheduledCommand onChange) {
    oldValue.setA(getLowerValue());
    oldValue.setB(getUpperValue());

    if (getWidget() == null) {
      setWidget(createWidget());

      SimpleEditorHandler.observe(null, inputFrom, getWidget());
      SimpleEditorHandler.observe(null, inputTo, getWidget());
    }

    openDialog(target, getWidget(), OpenEvent.focus(inputFrom.asWidget()), onChange);
  }

  @Override
  public Filter parse(FilterValue input) {
    if (input == null) {
      return null;

    } else if (input.hasValue()) {
      Pair<String, String> r = parseRange(input.getValue());
      return (r == null) ? null : buildFilter(r.getA(), r.getB());

    } else {
      return getEmptinessFilter(getColumnId(), input.getEmptyValues());
    }
  }

  @Override
  public void setFilterValue(FilterValue filterValue) {
    if (filterValue == null) {
      clearValue();
    } else {
      setValue(filterValue.getValue(), filterValue.getEmptyValues());
    }
  }

  @Override
  protected void doClear() {
    super.doClear();

    inputFrom.clearValue();
    inputTo.clearValue();
  }

  @Override
  protected void doCommit() {
    String lower = getLowerValue();
    String upper = getUpperValue();

    boolean changed = !Objects.equals(lower, oldValue.getA())
        || !Objects.equals(upper, oldValue.getB()) || getEmptiness() != null;
    update(changed);
  }

  @Override
  protected List<SupplierAction> getActions() {
    return Lists.newArrayList(SupplierAction.COMMIT, SupplierAction.CLEAR, SupplierAction.CANCEL);
  }

  @Override
  protected void onDialogCancel() {
    inputFrom.setValue(oldValue.getA());
    inputTo.setValue(oldValue.getB());
  }

  private Filter buildFilter(String lower, String upper) {
    if (BeeUtils.allEmpty(lower, upper)) {
      return null;
    }

    DateOrdering dateOrdering = Format.getDefaultDateOrdering();

    if (BeeUtils.isEmpty(lower)) {
      return Filter.compareWithValue(getColumn(), UPPER_OPERATOR, upper, dateOrdering);

    } else if (BeeUtils.isEmpty(upper)) {
      return Filter.compareWithValue(getColumn(), LOWER_OPERATOR, lower, dateOrdering);

    } else if (lower.equals(upper)) {
      return Filter.compareWithValue(getColumn(), Operator.EQ, lower, dateOrdering);

    } else {
      return Filter.and(Filter.compareWithValue(getColumn(), LOWER_OPERATOR, lower, dateOrdering),
          Filter.compareWithValue(getColumn(), UPPER_OPERATOR, upper, dateOrdering));
    }
  }

  private void clearValue() {
    setValue(null, null);
  }

  private Widget createWidget() {
    Flow panel = new Flow(STYLE_PREFIX + "panel");

    CustomDiv caption = new CustomDiv(STYLE_PREFIX + "caption");
    caption.setHtml(getColumnLabel());
    panel.add(caption);

    Flow fromWrapper = new Flow(STYLE_FROM + STYLE_SUFFIX_WRAPPER);

    Label labelFrom = new Label();
    labelFrom.getElement().setInnerText(LOWER_OPERATOR.toTextString());
    labelFrom.addStyleName(STYLE_FROM + STYLE_SUFFIX_LABEL);

    fromWrapper.add(labelFrom);
    fromWrapper.add(inputFrom);

    panel.add(fromWrapper);

    Flow toWrapper = new Flow(STYLE_TO + STYLE_SUFFIX_WRAPPER);

    Label labelTo = new Label();
    labelTo.getElement().setInnerText(UPPER_OPERATOR.toTextString());
    labelTo.addStyleName(STYLE_TO + STYLE_SUFFIX_LABEL);

    toWrapper.add(labelTo);
    toWrapper.add(inputTo);

    panel.add(toWrapper);

    if (hasEmptiness()) {
      Flow emptinessWrapper = new Flow(STYLE_EMPTINESS);

      Button notEmpty = new Button(NOT_NULL_VALUE_LABEL, event -> onEmptiness(false));
      notEmpty.addStyleName(STYLE_NOT_NULL);
      emptinessWrapper.add(notEmpty);

      Button empty = new Button(NULL_VALUE_LABEL, event -> onEmptiness(true));
      empty.addStyleName(STYLE_NULL);
      emptinessWrapper.add(empty);

      panel.add(emptinessWrapper);
    }

    panel.add(getCommandWidgets(false));

    return panel;
  }

  private Boolean getEmptiness() {
    return emptiness;
  }

  private String getLowerValue() {
    return BeeUtils.trim(inputFrom.getValue());
  }

  private String getUpperValue() {
    return BeeUtils.trim(inputTo.getValue());
  }

  private Widget getWidget() {
    return widget;
  }

  private void onEmptiness(Boolean value) {
    boolean changed = !BeeUtils.isEmpty(oldValue.getA()) || !BeeUtils.isEmpty(oldValue.getB())
        || !Objects.equals(getEmptiness(), value);
    setValue(null, value);
    update(changed);
  }

  private void setValue(String value, Boolean empt) {
    Pair<String, String> r = parseRange(value);
    if (r == null) {
      inputFrom.clearValue();
      inputTo.clearValue();
    } else {
      inputFrom.setValue(r.getA());
      inputTo.setValue(r.getB());
    }

    this.emptiness = empt;
  }

  private void setWidget(Widget widget) {
    this.widget = widget;
  }
}
