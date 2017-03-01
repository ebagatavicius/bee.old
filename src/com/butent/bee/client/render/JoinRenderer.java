package com.butent.bee.client.render;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class JoinRenderer extends AbstractCellRenderer implements HasItems {

  public static final String DEFAULT_SEPARATOR = BeeConst.STRING_SPACE;

  private final List<BeeColumn> dataColumns;

  private final List<CellSource> sources = new ArrayList<>();

  private final String separator;

  private final Function<HasDateValue, String> dateRenderer;
  private final Function<HasDateValue, String> dateTimeRenderer;

  public JoinRenderer(List<BeeColumn> dataColumns, String sep, List<String> items) {
    super(null);
    this.dataColumns = dataColumns;

    if (BeeUtils.isDigit(sep)) {
      this.separator = BeeUtils.space(BeeUtils.toInt(sep));
    } else if (BeeUtils.hasLength(sep, 1)) {
      this.separator = sep;
    } else {
      this.separator = DEFAULT_SEPARATOR;
    }

    this.dateRenderer = Format.getDateRenderer();
    this.dateTimeRenderer = Format.getDateTimeRenderer();

    if (!BeeUtils.isEmpty(items)) {
      addItems(items);
    }
  }

  @Override
  public void addItem(String item) {
    Assert.notEmpty(item);

    CellSource source;

    int index = DataUtils.getColumnIndex(item, dataColumns);
    if (BeeConst.isUndef(index)) {
      source = CellSource.forProperty(item, null, ValueType.TEXT);
    } else {
      source = CellSource.forColumn(dataColumns.get(index), index);
    }

    sources.add(source);
  }

  @Override
  public void addItems(Collection<String> items) {
    Assert.notNull(items);
    for (String item : items) {
      addItem(item);
    }
  }

  @Override
  public ValueType getExportType() {
    return ValueType.TEXT;
  }

  @Override
  public int getItemCount() {
    return sources.size();
  }

  @Override
  public List<String> getItems() {
    List<String> result = new ArrayList<>();
    for (CellSource source : sources) {
      result.add(source.getName());
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
    if (row == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    for (CellSource source : sources) {
      String value = source.render(row, dateRenderer, dateTimeRenderer);
      if (!BeeUtils.isEmpty(value)) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(value.trim());
      }
    }
    return sb.toString();
  }

  @Override
  public void setItems(Collection<String> items) {
    if (!sources.isEmpty()) {
      sources.clear();
    }
    addItems(items);
  }
}
