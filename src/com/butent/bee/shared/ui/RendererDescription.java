package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;

public class RendererDescription implements BeeSerializable, HasInfo, HasOptions {

  private enum Serial {
    TYPE, VALUE_START_INDEX, SEPARATOR, OPTIONS, ITEMS
  }

  public static final String TAG_RENDERER = "renderer";
  public static final String TAG_RENDER = "render";

  public static final String ATTR_RENDER_COLUMNS = "renderColumns";

  public static final String ATTR_TYPE = "type";

  private static final String ATTR_SEPARATOR = "separator";

  public static boolean canRestore(String[] arr) {
    if (arr == null) {
      return false;
    } else {
      return arr.length == Serial.values().length;
    }
  }

  public static RendererDescription restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    RendererDescription rd = new RendererDescription();
    rd.deserialize(s);
    if (rd.isEmpty()) {
      return null;
    }
    return rd;
  }

  private RendererType type;

  private Integer valueStartIndex;

  private String separator;

  private String options;

  private List<String> items;

  public RendererDescription(RendererType type) {
    this.type = type;
  }

  protected RendererDescription() {
  }

  @Override
  public void deserialize(String s) {
    deserializeMembers(Codec.beeDeserializeCollection(s));
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(
        "Type", getType(),
        "Value Start Index", getValueStartIndex(),
        "Separator", getSeparator(),
        "Options", getOptions());

    if (getItems() != null) {
      info.add(new Property("Items", BeeUtils.bracket(getItems().size())));
      for (int i = 0; i < getItems().size(); i++) {
        info.add(new Property(BeeUtils.joinWords("Item", i + 1), getItems().get(i)));
      }
    }

    if (isEmpty()) {
      PropertyUtils.addWhenEmpty(info, getClass());
      return info;
    }
    return info;
  }

  public List<String> getItems() {
    return items;
  }

  @Override
  public String getOptions() {
    return options;
  }

  public String getSeparator() {
    return separator;
  }

  public RendererType getType() {
    return type;
  }

  public Integer getValueStartIndex() {
    return valueStartIndex;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];

    for (int i = 0; i < members.length; i++) {
      switch (members[i]) {
        case TYPE:
          arr[i] = (getType() == null) ? null : getType().getTypeCode();
          break;
        case VALUE_START_INDEX:
          arr[i] = getValueStartIndex();
          break;
        case SEPARATOR:
          arr[i] = getSeparator();
          break;
        case OPTIONS:
          arr[i] = getOptions();
          break;
        case ITEMS:
          arr[i] = getItems();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      String value = attribute.getValue();
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (BeeUtils.same(key, HasValueStartIndex.ATTR_VALUE_START_INDEX)) {
        setValueStartIndex(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_SEPARATOR)) {
        setSeparator(value);
      } else if (BeeUtils.same(key, ATTR_OPTIONS)) {
        setOptions(value.trim());
      }
    }
  }

  public void setItems(List<String> items) {
    this.items = items;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

  public void setValueStartIndex(Integer valueStartIndex) {
    this.valueStartIndex = valueStartIndex;
  }

  protected void deserializeMembers(String[] arr) {
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (member) {
        case TYPE:
          setType(RendererType.getByTypeCode(value));
          break;
        case VALUE_START_INDEX:
          setValueStartIndex(BeeUtils.toIntOrNull(value));
          break;
        case SEPARATOR:
          setSeparator(value);
          break;
        case OPTIONS:
          setOptions(value.trim());
          break;
        case ITEMS:
          String[] data = Codec.beeDeserializeCollection(value);

          if (ArrayUtils.isEmpty(data)) {
            setItems(null);
          } else {
            setItems(Lists.newArrayList(data));
          }
          break;
      }
    }
  }

  private boolean isEmpty() {
    return getType() == null;
  }

  private void setType(RendererType type) {
    this.type = type;
  }
}
