package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables using different styles for user interface components depending on applied conditions.
 */

public class ConditionalStyleDeclaration implements BeeSerializable, HasInfo {

  public static final String TAG_DYN_STYLE = "dynStyle";

  public static ConditionalStyleDeclaration restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    ConditionalStyleDeclaration cs = new ConditionalStyleDeclaration();
    cs.deserialize(s);
    return cs;
  }

  private StyleDeclaration style;
  private Calculation condition;

  public ConditionalStyleDeclaration(StyleDeclaration style, Calculation condition) {
    this.style = style;
    this.condition = condition;
  }

  private ConditionalStyleDeclaration() {
  }

  public ConditionalStyleDeclaration copy() {
    ConditionalStyleDeclaration copy = new ConditionalStyleDeclaration();

    if (getStyle() != null) {
      copy.setStyle(getStyle().copy());
    }
    if (getCondition() != null) {
      copy.setCondition(getCondition().copy());
    }

    return copy;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setStyle(StyleDeclaration.restore(arr[0]));
    setCondition(Calculation.restore(arr[1]));
  }

  public Calculation getCondition() {
    return condition;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = new ArrayList<>();

    if (getStyle() != null) {
      info.addAll(getStyle().getInfo());
    }
    if (getCondition() != null) {
      info.addAll(getCondition().getInfo());
    }

    if (info.isEmpty()) {
      PropertyUtils.addWhenEmpty(info, getClass());
    } else if (!validState()) {
      info.add(new Property("State", "illegal"));
    }
    return info;
  }

  public StyleDeclaration getStyle() {
    return style;
  }

  public void replaceColumn(String oldId, String newId) {
    if (getCondition() != null) {
      getCondition().replaceColumn(oldId, newId);
    }
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getStyle(), getCondition()});
  }

  public boolean validState() {
    return getStyle() != null && !getStyle().isEmpty();
  }

  private void setCondition(Calculation condition) {
    this.condition = condition;
  }

  private void setStyle(StyleDeclaration style) {
    this.style = style;
  }
}
