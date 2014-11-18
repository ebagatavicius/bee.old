package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables using calculation expressions and functions in user interface components.
 */

public class Calculation implements BeeSerializable, HasInfo {

  private enum Serial {
    EXPRESSION, FUNCTION
  }

  public static final String TAG_EXPRESSION = "expression";
  public static final String TAG_FUNCTION = "function";

  public static final String CELL_OBJECT = "cell";
  public static final String ROW_OBJECT = "row";
  public static final String PROPERTY_VALUE = "value";

  public static final String PROPERTY_OLD_VALUE = "oldValue";
  public static final String PROPERTY_NEW_VALUE = "newValue";
  public static final String VAR_COL_ID = "colName";

  public static final String VAR_ROW_ID = "rowId";
  public static final String VAR_ROW_VERSION = "rowVersion";

  public static final String VAR_ROW_INDEX = "rowIndex";

  public static final String VAR_COL_INDEX = "colIndex";

  public static final String DEFAULT_REPLACE_PREFIX = "[";
  public static final String DEFAULT_REPLACE_SUFFIX = "]";
  public static final String PROPERTY_SEPARATOR = ".";

  public static boolean canRestore(String[] arr) {
    if (arr == null) {
      return false;
    } else {
      return arr.length == Serial.values().length;
    }
  }

  public static String renameColumn(String input, String oldId, String newId) {
    if (BeeUtils.containsSame(input, oldId) && !BeeUtils.isEmpty(newId)
        && !BeeUtils.equalsTrim(oldId, newId)) {

      String oldRef = ROW_OBJECT + PROPERTY_SEPARATOR + oldId.trim();
      String newRef = ROW_OBJECT + PROPERTY_SEPARATOR + newId.trim();

      return NameUtils.replaceName(input, oldRef, newRef);
    } else {
      return input;
    }
  }

  public static Calculation restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Calculation calculation = new Calculation();
    calculation.deserialize(s);
    return calculation;
  }

  private String expression;
  private String function;

  public Calculation(String expression, String function) {
    this.expression = expression;
    this.function = function;
  }

  protected Calculation() {
  }

  public Calculation copy() {
    return new Calculation(getExpression(), getFunction());
  }

  @Override
  public void deserialize(String s) {
    deserializeMembers(Codec.beeDeserializeCollection(s));
  }

  public String getExpression() {
    return expression;
  }

  public String getFunction() {
    return function;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = new ArrayList<>();

    if (isEmpty()) {
      PropertyUtils.addWhenEmpty(info, getClass());
      return info;
    }

    if (!BeeUtils.isEmpty(getExpression())) {
      info.add(new Property(TAG_EXPRESSION, getExpression()));
    }
    if (!BeeUtils.isEmpty(getFunction())) {
      info.add(new Property(TAG_FUNCTION, getFunction()));
    }
    return info;
  }

  public boolean hasExpressionOrFunction() {
    return !BeeUtils.isEmpty(getExpression()) || !BeeUtils.isEmpty(getFunction());
  }

  public void replaceColumn(String oldId, String newId) {
    if (BeeUtils.containsSame(getExpression(), oldId)) {
      setExpression(renameColumn(getExpression(), oldId, newId));
    }
    if (BeeUtils.containsSame(getFunction(), oldId)) {
      setFunction(renameColumn(getFunction(), oldId, newId));
    }
  }

  @Override
  public String serialize() {
    String expr = BeeUtils.isEmpty(getExpression()) ? null : Codec.encodeBase64(getExpression());
    String func = BeeUtils.isEmpty(getFunction()) ? null : Codec.encodeBase64(getFunction());

    return Codec.beeSerialize(new Object[] {expr, func});
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions(TAG_EXPRESSION, getExpression(), TAG_FUNCTION, getFunction());
  }

  protected void deserializeMembers(String[] arr) {
    Assert.lengthEquals(arr, Serial.values().length);

    setExpression(BeeUtils.isEmpty(arr[0]) ? null : Codec.decodeBase64(arr[0]));
    setFunction(BeeUtils.isEmpty(arr[1]) ? null : Codec.decodeBase64(arr[1]));
  }

  private boolean isEmpty() {
    return BeeUtils.allEmpty(getExpression(), getFunction());
  }

  private void setExpression(String expression) {
    this.expression = expression;
  }

  private void setFunction(String function) {
    this.function = function;
  }
}
