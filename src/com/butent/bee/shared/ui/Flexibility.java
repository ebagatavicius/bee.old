package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;

public class Flexibility implements BeeSerializable, HasInfo {

  private enum Serial {
    GROW, SHRINK, BASIS_WIDTH, BASIS_UNIT, BASIS_AUTO
  }

  public static final String ATTR_GROW = "flexGrow";
  public static final String ATTR_SHRINK = "flexShrink";
  public static final String ATTR_BASIS = "flexBasis";
  public static final String ATTR_BASIS_UNIT = "flexBasisUnit";

  private static final String VALUE_AUTO = "auto";

  public static Flexibility createIfDefined(Map<String, String> attributes) {
    if (BeeUtils.isEmpty(attributes)) {
      return null;
    }

    boolean defined = false;
    Flexibility flexibility = new Flexibility();

    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      String value = attribute.getValue();
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (BeeUtils.same(key, ATTR_GROW)) {
        Integer grow = BeeUtils.toIntOrNull(value);
        if (BeeUtils.isNonNegative(grow)) {
          flexibility.setGrow(grow);
          defined = true;
        }

      } else if (BeeUtils.same(key, ATTR_SHRINK)) {
        Integer shrink = BeeUtils.toIntOrNull(value);
        if (BeeUtils.isNonNegative(shrink)) {
          flexibility.setShrink(shrink);
          defined = true;
        }

      } else if (BeeUtils.same(key, ATTR_BASIS)) {
        if (BeeUtils.same(value, VALUE_AUTO)) {
          flexibility.setBasisAuto(true);
          defined = true;

        } else {
          Integer width = BeeUtils.toIntOrNull(value);
          if (BeeUtils.isNonNegative(width)) {
            flexibility.setBasisWidth(width);
            defined = true;
          }
        }

      } else if (BeeUtils.same(key, ATTR_BASIS_UNIT)) {
        CssUnit unit = CssUnit.parse(value);
        if (unit != null) {
          flexibility.setBasisUnit(unit);
        }
      }
    }

    return defined ? flexibility : null;
  }

  public static boolean isAttributeRelevant(String name) {
    return BeeUtils.inListSame(name, ATTR_GROW, ATTR_SHRINK, ATTR_BASIS, ATTR_BASIS_UNIT);
  }

  public static Flexibility maybeCreate(Integer grow, Integer shrink, Integer basis,
      String basisUnit) {

    if (BeeUtils.isNonNegative(grow) || BeeUtils.isNonNegative(shrink)
        || BeeUtils.isNonNegative(basis)) {

      Flexibility flexibility = new Flexibility();

      if (BeeUtils.isNonNegative(grow)) {
        flexibility.setGrow(grow);
      }
      if (BeeUtils.isNonNegative(shrink)) {
        flexibility.setShrink(shrink);
      }

      if (BeeUtils.isNonNegative(basis)) {
        flexibility.setBasisWidth(basis);
      }
      if (!BeeUtils.isEmpty(basisUnit)) {
        flexibility.setBasisUnit(CssUnit.parse(basisUnit));
      }

      return flexibility;

    } else {
      return null;
    }
  }

  public static Flexibility restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    Flexibility flexibility = new Flexibility();
    flexibility.deserialize(s);

    return flexibility;
  }

  private int grow = BeeConst.UNDEF;
  private int shrink = BeeConst.UNDEF;

  private int basisWidth = BeeConst.UNDEF;
  private CssUnit basisUnit;

  private boolean basisAuto;

  public Flexibility(int grow, int shrink, boolean basisAuto) {
    this();

    this.grow = grow;
    this.shrink = shrink;
    this.basisAuto = basisAuto;
  }

  public Flexibility(int grow, int shrink, int basisWidth, CssUnit basisUnit) {
    this();

    this.grow = grow;
    this.shrink = shrink;
    this.basisWidth = basisWidth;
    this.basisUnit = basisUnit;
  }

  private Flexibility() {
    super();
  }

  public Flexibility copy() {
    Flexibility copy = new Flexibility(getGrow(), getShrink(), getBasisWidth(), getBasisUnit());
    copy.setBasisAuto(isBasisAuto());
    return copy;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, Serial.values().length);

    for (Serial member : Serial.values()) {
      int i = member.ordinal();
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (member) {
        case GROW:
          setGrow(BeeUtils.toInt(value));
          break;
        case SHRINK:
          setShrink(BeeUtils.toInt(value));
          break;
        case BASIS_WIDTH:
          setBasisWidth(BeeUtils.toInt(value));
          break;
        case BASIS_UNIT:
          setBasisUnit(EnumUtils.getEnumByName(CssUnit.class, value));
          break;
        case BASIS_AUTO:
          setBasisAuto(BeeUtils.toBoolean(value));
          break;
      }
    }
  }

  public CssUnit getBasisUnit() {
    return basisUnit;
  }

  public int getBasisWidth() {
    return basisWidth;
  }

  public int getGrow() {
    return grow;
  }

  @Override
  public List<Property> getInfo() {
    return PropertyUtils.createProperties(ATTR_GROW, getGrow(), ATTR_SHRINK, getShrink(),
        ATTR_BASIS, getBasisWidth(), ATTR_BASIS_UNIT, getBasisUnit(),
        ATTR_BASIS + VALUE_AUTO, isBasisAuto());
  }

  public int getShrink() {
    return shrink;
  }

  public boolean isBasisAuto() {
    return basisAuto;
  }

  public boolean isEmpty() {
    return getGrow() < 0 && getShrink() < 0 && getBasisWidth() < 0 && !isBasisAuto();
  }

  public void merge(Flexibility preferred) {
    Assert.notNull(preferred);

    if (preferred.getGrow() >= 0) {
      setGrow(preferred.getGrow());
    }
    if (preferred.getShrink() >= 0) {
      setShrink(preferred.getShrink());
    }

    if (preferred.getBasisWidth() >= 0 || preferred.isBasisAuto()) {
      setBasisWidth(preferred.getBasisWidth());
      setBasisAuto(preferred.isBasisAuto());
    }
    if (preferred.getBasisUnit() != null) {
      setBasisUnit(preferred.getBasisUnit());
    }
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[Serial.values().length];

    for (Serial member : Serial.values()) {
      int i = member.ordinal();

      switch (member) {
        case GROW:
          arr[i] = getGrow();
          break;
        case SHRINK:
          arr[i] = getShrink();
          break;
        case BASIS_WIDTH:
          arr[i] = getBasisWidth();
          break;
        case BASIS_UNIT:
          arr[i] = (getBasisUnit() == null) ? null : getBasisUnit().name();
          break;
        case BASIS_AUTO:
          arr[i] = isBasisAuto();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setBasisAuto(boolean basisAuto) {
    this.basisAuto = basisAuto;
  }

  public void setBasisUnit(CssUnit basisUnit) {
    this.basisUnit = basisUnit;
  }

  public void setBasisWidth(int basisWidth) {
    this.basisWidth = basisWidth;
  }

  public void setGrow(int grow) {
    this.grow = grow;
  }

  public void setShrink(int shrink) {
    this.shrink = shrink;
  }
}
