package com.butent.bee.client.render;

import com.google.common.base.Splitter;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapRenderer extends AbstractCellRenderer implements HasItems {

  private static final BeeLogger logger = LogUtils.getLogger(MapRenderer.class);

  public static final String DEFAULT_SEPARATOR = "=";

  private final Map<String, String> map = new HashMap<>();

  private final String separator;
  private final Splitter splitter;

  public MapRenderer(CellSource cellSource, String sep) {
    super(cellSource);

    this.separator = BeeUtils.notEmpty(sep, DEFAULT_SEPARATOR).trim();
    this.splitter = Splitter.on(this.separator).omitEmptyStrings().trimResults().limit(2);
  }

  @Override
  public void addItem(String item) {
    Assert.notNull(item);
    Value key = null;
    String value = null;
    int index = 0;

    for (String s : splitter.split(item)) {
      switch (index) {
        case 0:
          key = parse(s, true, DateOrdering.DEFAULT);
          break;
        case 1:
          value = Localized.maybeTranslate(s);
          break;
      }
      index++;
    }

    if (key == null || BeeUtils.isEmpty(value)) {
      logger.warning(NameUtils.getName(this), "cannot add item:", item);
    } else {
      map.put(key.getString(), value);
    }
  }

  @Override
  public void addItems(Collection<String> items) {
    Assert.notNull(items);
    for (String item : items) {
      addItem(item);
    }
  }

  @Override
  public int getItemCount() {
    return map.size();
  }

  @Override
  public List<String> getItems() {
    List<String> result = new ArrayList<>();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = Value.parseValue(getValueType(), entry.getKey(),
          false, DateOrdering.DEFAULT).toString();

      if (!BeeUtils.isEmpty(key)) {
        result.add(BeeUtils.joinWords(key, separator, entry.getValue()));
      }
    }
    return result;
  }

  @Override
  public boolean isEmpty() {
    return getItemCount() <= 0;
  }

  @Override
  public boolean isIndex(int index) {
    return index >= 0 && index < getItemCount();
  }

  @Override
  public String render(IsRow row) {
    String key = getString(row);

    if (key == null) {
      return null;

    } else if (map.containsKey(key)) {
      return map.get(key);

    } else {
      Value value = getValue(row);
      return (value == null)
          ? null : value.render(Format.getDateRenderer(), Format.getDateTimeRenderer());
    }
  }

  @Override
  public void setItems(Collection<String> items) {
    if (!map.isEmpty()) {
      map.clear();
    }
    addItems(items);
  }
}
