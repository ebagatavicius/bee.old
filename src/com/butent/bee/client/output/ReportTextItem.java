package com.butent.bee.client.output;

import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReportTextItem extends ReportItem {

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
      filter = Arrays.asList(Codec
          .beeDeserializeCollection(Codec.deserializeMap(data).get(Service.VAR_DATA)));
    }
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
  public MultiSelector getFilterWidget() {
    if (filterWidget == null) {
      Relation relation = Relation.create(ClassifierConstants.TBL_COUNTRIES,
          Collections.singletonList(ClassifierConstants.COL_COUNTRY_NAME));
      relation.setStrict(false);
      relation.setFilter(Filter.isFalse());
      relation.disableNewRow();
      relation.disableEdit();
      relation.setValueSource(ClassifierConstants.COL_COUNTRY_NAME);

      filterWidget = MultiSelector.autonomous(relation, (AbstractCellRenderer) null);
      filterWidget.addStyleName(getStyle() + "-filter");

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
    return filterWidget;
  }

  @Override
  public String getStyle() {
    return STYLE_TEXT;
  }

  @Override
  public String serialize() {
    String data = null;

    if (!BeeUtils.isEmpty(filter)) {
      data = Codec.beeSerialize(Collections.singletonMap(Service.VAR_DATA, filter));
    }
    return serialize(data);
  }

  @Override
  public ReportItem setFilter(String value) {
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
        return true;
      }
    }
    return false;
  }
}
