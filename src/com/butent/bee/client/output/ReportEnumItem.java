package com.butent.bee.client.output;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.Global;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportEnumItem extends ReportItem implements ClickHandler {

  private static final String ENUM = "ENUM";

  private String enumKey;
  private Label filterWidget;
  private Set<Integer> filter;

  public ReportEnumItem(String name, String caption,
      Class<? extends Enum<? extends HasCaption>> en) {
    this(name, caption);
    Assert.notNull(en);
    enumKey = NameUtils.getClassName(en);
  }

  protected ReportEnumItem(String expression, String caption) {
    super(expression, caption);
  }

  @Override
  public void clearFilter() {
    filter = null;
    renderFilter();
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeLinkedHashMap(data);

    if (!BeeUtils.isEmpty(map)) {
      enumKey = map.get(ENUM);

      String[] arr = Codec.beeDeserializeCollection(map.get(Service.VAR_DATA));

      if (!ArrayUtils.isEmpty(arr)) {
        filter = new HashSet<>();

        for (String idx : arr) {
          filter.add(BeeUtils.toInt(idx));
        }
      }
    }
  }

  @Override
  public ResultValue evaluate(SimpleRow row, Dictionary dictionary) {
    Integer value = row.getInt(getExpression());
    return value == null ? ResultValue.empty() : ResultValue.of(TimeUtils.padTwo(value))
        .setDisplay(EnumUtils.getLocalizedCaption(enumKey, value, dictionary));
  }

  @Override
  public Set<Integer> getFilter() {
    return filter;
  }

  @Override
  public Label getFilterWidget() {
    if (filterWidget == null) {
      filterWidget = new Label();
      filterWidget.addStyleName(getStyle() + "-filter");
      filterWidget.addClickHandler(this);
      renderFilter();
    }
    return filterWidget;
  }

  @Override
  public String getStyle() {
    return STYLE_ENUM;
  }

  @Override
  public void onClick(ClickEvent event) {
    final ListBox list = new ListBox(true);
    int x = 0;

    for (String caption : EnumUtils.getCaptions(enumKey)) {
      list.addItem(caption);
      list.getOptionElement(x).setSelected(filter != null && filter.contains(x));
      x++;
    }
    list.setAllVisible();

    Global.inputWidget(Localized.dictionary().values(), list, () -> {
      filter = new HashSet<>();

      for (int i = 0; i < list.getItemCount(); i++) {
        if (list.getOptionElement(i).isSelected()) {
          filter.add(i);
        }
      }
      renderFilter();
    });
  }

  @Override
  public String serialize() {
    Map<String, Object> map = new HashMap<>();
    map.put(ENUM, enumKey);

    if (!BeeUtils.isEmpty(filter)) {
      map.put(Service.VAR_DATA, filter);
    }
    return serialize(Codec.beeSerialize(map));
  }

  @Override
  public ReportItem setFilter(String value) {
    if (!BeeUtils.isEmpty(value)) {
      filter = Collections.singleton(BeeUtils.toInt(value));
    } else {
      filter = null;
    }
    return this;
  }

  @Override
  public boolean validate(SimpleRow row) {
    if (BeeUtils.isEmpty(filter) || !row.getRowSet().hasColumn(getExpression())) {
      return true;
    }
    return filter.contains(row.getInt(getExpression()));
  }

  private void renderFilter() {
    if (filterWidget != null) {
      List<String> caps = new ArrayList<>();

      if (!BeeUtils.isEmpty(filter)) {
        for (Integer idx : filter) {
          caps.add(EnumUtils.getCaption(enumKey, idx));
        }
      }
      filterWidget.setText(BeeUtils.joinItems(caps));
      filterWidget.setStyleName(getStyle() + "-filter-empty", caps.isEmpty());
    }
  }
}
