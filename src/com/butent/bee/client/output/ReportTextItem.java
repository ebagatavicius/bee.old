package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportTextItem extends ReportItem {

  private static final String NEGATION = "NEGATION";
  private static final String EMPTY = "EMPTY";

  private boolean filterNegation;
  private MultiSelector filterWidget;
  private List<String> filter;
  private boolean filterEmpty;

  public ReportTextItem(String expression, String caption) {
    super(expression, caption);
  }

  @Override
  public void clearFilter() {
    filter = null;
    filterEmpty = false;

    if (filterWidget != null) {
      filterWidget.clearValue();
      renderFilter((Flow) filterWidget.getParent());
    }
  }

  @Override
  public void deserialize(String data) {
    if (data != null) {
      Map<String, String> map = Codec.deserializeLinkedHashMap(data);

      filterNegation = BeeUtils.toBoolean(map.get(NEGATION));

      String[] values = Codec.beeDeserializeCollection(map.get(Service.VAR_DATA));

      if (!ArrayUtils.isEmpty(values)) {
        filter = Arrays.asList(values);
      }
      filterEmpty = BeeUtils.toBoolean(map.get(EMPTY));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ReportTextItem)) {
      return false;
    }
    return super.equals(obj)
        && Objects.equals(isNegationFilter(), ((ReportTextItem) obj).isNegationFilter());
  }

  @Override
  public ResultValue evaluate(SimpleRow row, Dictionary dictionary) {
    return ResultValue.of(row.getValue(getExpression()));
  }

  @Override
  public List<String> getFilter() {
    return filter;
  }

  @Override
  public Widget getFilterWidget() {
    Flow container = new Flow(getStyle() + "-filter");
    renderFilter(container);
    return container;
  }

  @Override
  public String getStyle() {
    return STYLE_TEXT;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getExpression(), isNegationFilter());
  }

  public boolean isEmptyFilter() {
    return filterEmpty;
  }

  public boolean isNegationFilter() {
    return filterNegation;
  }

  @Override
  public String serialize() {
    String data = null;

    if (!BeeUtils.isEmpty(filter) || isEmptyFilter()) {
      Map<String, Object> map = new HashMap<>();

      map.put(NEGATION, isNegationFilter());
      map.put(Service.VAR_DATA, filter);
      map.put(EMPTY, isEmptyFilter());

      data = Codec.beeSerialize(map);
    }
    return serialize(data);
  }

  @Override
  public ReportItem setFilter(String value) {
    filterNegation = false;
    filterEmpty = BeeUtils.isEmpty(value);

    if (!isEmptyFilter()) {
      filter = Collections.singletonList(BeeConst.STRING_EQ + value);
    } else {
      filter = null;
    }
    return this;
  }

  @Override
  public boolean validate(SimpleRow row) {
    if (BeeUtils.isEmpty(filter) && !isEmptyFilter()
        || !row.getRowSet().hasColumn(getExpression())) {
      return true;
    }
    String value = row.getValue(getExpression());

    if (isEmptyFilter() && BeeUtils.isEmpty(value)) {
      return !isNegationFilter();
    }
    if (!BeeUtils.isEmpty(filter)) {
      for (String opt : filter) {
        if (BeeUtils.isPrefix(opt, BeeConst.STRING_EQ)
            ? Objects.equals(value, BeeUtils.removePrefix(opt, BeeConst.STRING_EQ))
            : BeeUtils.containsSame(value, opt)) {
          return !isNegationFilter();
        }
      }
    }
    return isNegationFilter();
  }

  private void renderFilter(Flow container) {
    container.clear();

    Toggle toggle = new Toggle(Localized.dictionary().is(),
        Localized.dictionary().isNot(), null, isNegationFilter());
    toggle.addClickHandler(clickEvent -> filterNegation = toggle.isChecked());
    container.add(toggle);

    InputBoolean empty = new InputBoolean(Localized.dictionary().empty());
    empty.setChecked(isEmptyFilter());
    empty.addValueChangeHandler(ev -> filterEmpty = empty.isChecked());
    container.add(empty);

    if (filterWidget == null) {
      Relation relation = Relation.create(ClassifierConstants.TBL_COUNTRIES,
          Collections.singletonList(ClassifierConstants.COL_COUNTRY_NAME));
      relation.setStrict(false);
      relation.setFilter(Filter.isFalse());
      relation.disableNewRow();
      relation.disableEdit();
      relation.setValueSource(ClassifierConstants.COL_COUNTRY_NAME);

      filterWidget = MultiSelector.autonomous(relation, (AbstractCellRenderer) null);
      filterWidget.addStyleName(StyleUtils.NAME_FLEX_BOX_CENTER);

      filterWidget.addSelectorHandler(event -> {
        if (EnumUtils.in(event.getState(), State.INSERTED, State.REMOVED)) {
          filter = filterWidget.getValues();
        }
      });
      filterWidget.setValues(filter);
    }
    container.add(filterWidget);
  }
}
