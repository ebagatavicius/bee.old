package com.butent.bee.client.output;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.ui.Relation;
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

  private boolean filterNegation;
  private MultiSelector filterWidget;
  private List<String> filter;

  public ReportTextItem(String name, String caption) {
    super(name, caption);
  }

  @Override
  public void clearFilter() {
    if (filterWidget != null) {
      filterWidget.clearValue();
    }
    filter = null;
  }

  @Override
  public void deserialize(String data) {
    if (data != null) {
      Map<String, String> map = Codec.deserializeMap(data);
      filterNegation = BeeUtils.toBoolean(map.get(NEGATION));
      filter = Arrays.asList(Codec.beeDeserializeCollection(map.get(Service.VAR_DATA)));
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
  public ReportValue evaluate(SimpleRow row) {
    return ReportValue.of(row.getValue(getName()));
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
    return Objects.hash(getName(), isNegationFilter());
  }

  public boolean isNegationFilter() {
    return filterNegation;
  }

  @Override
  public String serialize() {
    String data = null;

    if (!BeeUtils.isEmpty(filter)) {
      Map<String, Object> map = new HashMap<>();

      if (isNegationFilter()) {
        map.put(NEGATION, true);
      }
      map.put(Service.VAR_DATA, filter);
      data = Codec.beeSerialize(map);
    }
    return serialize(data);
  }

  @Override
  public ReportItem setFilter(String value) {
    filterNegation = false;

    if (!BeeUtils.isEmpty(value)) {
      filter = Arrays.asList(value);
    } else {
      filter = null;
    }
    return this;
  }

  @Override
  public boolean validate(SimpleRow row) {
    if (BeeUtils.isEmpty(filter) || !row.getRowSet().hasColumn(getName())) {
      return true;
    }
    String value = row.getValue(getName());

    for (String opt : filter) {
      if (BeeUtils.containsSame(value, opt)) {
        return !isNegationFilter();
      }
    }
    return isNegationFilter();
  }

  private void renderFilter(final Flow container) {
    final Toggle toggle = new Toggle(Localized.getConstants().is(),
        Localized.getConstants().isNot(), null, filterNegation);
    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        filterNegation = toggle.isChecked();
      }
    });
    container.add(toggle);

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

      filterWidget.addSelectorHandler(new SelectorEvent.Handler() {
        @Override
        public void onDataSelector(SelectorEvent event) {
          if (EnumUtils.in(event.getState(), State.INSERTED, State.REMOVED)) {
            filter = filterWidget.getValues();
          }
        }
      });
      filterWidget.setValues(filter);
    }
    container.add(filterWidget);
  }
}
