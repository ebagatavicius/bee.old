package com.butent.bee.shared.data;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enables using custom properties in data objects.
 */

@SuppressWarnings("serial")
public class CustomProperties extends HashMap<String, String> implements HasInfo, BeeSerializable {

  public static final String TAG_PROPERTIES = "properties";

  public static CustomProperties create() {
    return new CustomProperties();
  }

  public static CustomProperties restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    CustomProperties customProperties = new CustomProperties();
    customProperties.deserialize(s);

    return customProperties;
  }

  public CustomProperties copy() {
    CustomProperties properties = create();
    for (Map.Entry<String, String> entry : entrySet()) {
      properties.put(entry.getKey(), entry.getValue());
    }
    return properties;
  }

  @Override
  public void deserialize(String s) {
    clear();
    if (BeeUtils.isEmpty(s)) {
      return;
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    int c = ArrayUtils.length(arr);
    Assert.isEven(c);

    for (int i = 0; i < c; i += 2) {
      put(arr[i], arr[i + 1]);
    }
  }

  @Override
  public List<Property> getInfo() {
    List<Property> lst = Lists.newArrayList();
    if (isEmpty()) {
      return lst;
    }

    PropertyUtils.addProperty(lst, "Custom Properties", size());
    for (Map.Entry<String, String> entry : entrySet()) {
      PropertyUtils.addProperty(lst, entry.getKey(), entry.getValue());
    }
    return lst;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(this);
  }
}
