package com.butent.bee.client.render;

import com.google.common.base.Splitter;
import com.google.common.collect.Range;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
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
import com.butent.bee.shared.utils.RangeOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RangeRenderer extends AbstractCellRenderer implements HasItems {

  private static final BeeLogger logger = LogUtils.getLogger(RangeRenderer.class);

  public static final String DEFAULT_SEPARATOR = ",";

  public static final boolean DEFAULT_LOWER_OPEN = false;
  public static final boolean DEFAULT_UPPER_OPEN = true;

  private final Map<Range<Value>, String> map = new HashMap<>();

  private final String separator;
  private final Splitter splitter;

  private final RangeOptions rangeOptions;

  public RangeRenderer(CellSource cellSource, String sep, String opt) {
    super(cellSource);

    this.separator = BeeUtils.notEmpty(sep, DEFAULT_SEPARATOR).trim();
    this.splitter = Splitter.on(this.separator).trimResults().limit(3);

    this.rangeOptions = new RangeOptions(RangeOptions.hasLowerOpen(opt, DEFAULT_LOWER_OPEN),
        RangeOptions.hasUpperOpen(opt, DEFAULT_UPPER_OPEN), false, false);
  }

  @Override
  public void addItem(String item) {
    Assert.notNull(item);

    Value low = null;
    Value upp = null;
    String value = null;
    int index = 0;

    for (String s : splitter.split(item)) {
      switch (index) {
        case 0:
          low = parse(s, true, DateOrdering.DEFAULT);
          break;
        case 1:
          upp = parse(s, true, DateOrdering.DEFAULT);
          break;
        case 2:
          value = Localized.maybeTranslate(s);
          break;
      }
      index++;
    }

    if (low == null && upp == null) {
      logger.warning(NameUtils.getName(this), "cannot parse item:", item);
      return;
    }
    if (low != null && upp != null) {
      if (BeeUtils.isMore(low, upp)) {
        logger.warning(NameUtils.getName(this), "invalid range:", item);
        return;
      }
      if (low.equals(upp) && (rangeOptions.isLowerOpen() || rangeOptions.isUpperOpen())) {
        logger.warning(NameUtils.getName(this), "invalid range:", item,
            rangeOptions.isLowerOpen(), rangeOptions.isUpperOpen());
        return;
      }
    }

    Range<Value> range = rangeOptions.getRange(low, upp);
    if (range == null || range.isEmpty()) {
      logger.warning(NameUtils.getName(this), "range is empty:", item);
    } else {
      map.put(range, value);
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
    String low;
    String upp;

    for (Map.Entry<Range<Value>, String> entry : map.entrySet()) {
      Range<Value> range = entry.getKey();
      if (range == null || range.isEmpty()) {
        continue;
      }

      low = range.hasLowerBound() ? range.lowerEndpoint().toString() : BeeConst.STRING_EMPTY;
      upp = range.hasUpperBound() ? range.upperEndpoint().toString() : BeeConst.STRING_EMPTY;

      result.add(BeeUtils.join(separator, low + separator + upp, entry.getValue()));
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
    Value v = getValue(row);
    if (v == null) {
      return null;
    }

    for (Map.Entry<Range<Value>, String> entry : map.entrySet()) {
      if (entry.getKey().contains(v)) {
        return entry.getValue();
      }
    }
    return v.render(Format.getDateRenderer(), Format.getDateTimeRenderer());
  }

  @Override
  public void setItems(Collection<String> items) {
    if (!map.isEmpty()) {
      map.clear();
    }
    addItems(items);
  }
}
