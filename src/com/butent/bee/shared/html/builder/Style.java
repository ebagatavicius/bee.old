package com.butent.bee.shared.html.builder;

import com.google.common.collect.Lists;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.HasCssName;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Style {

  public static String join(String separator, HasCssName first, HasCssName second) {
    return join(separator, Lists.newArrayList(first, second));
  }

  public static String join(String separator, Collection<? extends HasCssName> values) {
    if (BeeUtils.isEmpty(values)) {
      return null;
    }

    List<String> names = new ArrayList<>();
    for (HasCssName value : values) {
      if (value != null && !names.contains(value.getCssName())) {
        names.add(value.getCssName());
      }
    }

    return names.isEmpty() ? null : BeeUtils.join(separator, names);
  }

  private final String name;
  private String value;

  public Style(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String build() {
    if (getValue() == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return getName() + ": " + getValue() + ";";
    }
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return build();
  }
}
