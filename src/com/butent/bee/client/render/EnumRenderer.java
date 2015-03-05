package com.butent.bee.client.render;

import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.HasValueStartIndex;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;

public class EnumRenderer extends AbstractCellRenderer implements HasValueStartIndex {

  private static final BeeLogger logger = LogUtils.getLogger(EnumRenderer.class);

  public static final int DEFAULT_VALUE_START_INDEX = 0;

  private final List<String> captions = new ArrayList<>();
  private int valueStartIndex;

  public EnumRenderer(CellSource cellSource, String key) {
    this(cellSource, key, DEFAULT_VALUE_START_INDEX);
  }

  public EnumRenderer(CellSource cellSource, String key, int valueStartIndex) {
    this(cellSource, EnumUtils.getCaptions(key), valueStartIndex);
  }

  public EnumRenderer(CellSource cellSource, Class<? extends Enum<?>> clazz) {
    this(cellSource, clazz, DEFAULT_VALUE_START_INDEX);
  }

  public EnumRenderer(CellSource cellSource, Class<? extends Enum<?>> clazz, int valueStartIndex) {
    this(cellSource, EnumUtils.getCaptions(clazz), valueStartIndex);
  }

  private EnumRenderer(CellSource cellSource, List<String> captions, int valueStartIndex) {
    super(cellSource);

    if (BeeUtils.isEmpty(captions)) {
      logger.severe(NameUtils.getName(this), ": no captions available");
    } else {
      this.captions.addAll(captions);
    }

    this.valueStartIndex = valueStartIndex;
  }

  public List<String> getCaptions() {
    return captions;
  }

  @Override
  public int getValueStartIndex() {
    return valueStartIndex;
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    Integer value = getInteger(row);
    if (value == null) {
      return null;
    }

    int index = value - getValueStartIndex();
    if (index >= 0 && index < captions.size()) {
      return captions.get(index);
    } else {
      return value.toString();
    }
  }

  @Override
  public void setValueStartIndex(int valueStartIndex) {
    this.valueStartIndex = valueStartIndex;
  }
}
