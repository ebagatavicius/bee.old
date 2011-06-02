package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

public class ConditionalStyle implements BeeSerializable, HasInfo {

  public static final String TAG_STYLE = "style";
  public static final String TAG_CONDITION = "condition";
  
  public static ConditionalStyle restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    ConditionalStyle cs = new ConditionalStyle();
    cs.deserialize(s);
    return cs;
  }

  private StyleDeclaration style;
  private Calculation condition;

  public ConditionalStyle(StyleDeclaration style, Calculation condition) {
    this.style = style;
    this.condition = condition;
  }

  private ConditionalStyle() {
  }

  public void deserialize(String s) {
    String[] arr = Codec.beeDeserialize(s);
    Assert.lengthEquals(arr, 2);

    setStyle(StyleDeclaration.restore(arr[0]));
    setCondition(Calculation.restore(arr[1]));
  }

  public List<Property> getInfo() {
    List<Property> info = Lists.newArrayList();
    
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

  public String serialize() {
    return Codec.beeSerializeAll(getStyle(), getCondition());
  }

  public boolean validState() {
    return getStyle() != null && !getStyle().isEmpty()
        && getCondition() != null && !getCondition().isEmpty();
  }

  private Calculation getCondition() {
    return condition;
  }

  private StyleDeclaration getStyle() {
    return style;
  }

  private void setCondition(Calculation condition) {
    this.condition = condition;
  }

  private void setStyle(StyleDeclaration style) {
    this.style = style;
  }
}
