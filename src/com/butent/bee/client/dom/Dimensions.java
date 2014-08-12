package com.butent.bee.client.dom;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

/**
 * Contains information about application height and width, both current and max values.
 */

public class Dimensions implements HasInfo, HasDimensions {

  private static final CssUnit DEFAULT_UNIT = CssUnit.PX;

  public static CssUnit normalizeUnit(CssUnit unit) {
    return (unit == null) ? DEFAULT_UNIT : unit;
  }

  private CssUnit widthUnit;
  private Double widthValue;
  private CssUnit heightUnit;
  private Double heightValue;

  private CssUnit minHeightUnit;
  private Double minHeightValue;
  private CssUnit minWidthUnit;
  private Double minWidthValue;

  private CssUnit maxHeightUnit;
  private Double maxHeightValue;
  private CssUnit maxWidthUnit;
  private Double maxWidthValue;

  public Dimensions() {
  }

  public Dimensions(double width, double height) {
    this(width, DEFAULT_UNIT, height, DEFAULT_UNIT);
  }

  public Dimensions(double width, double height, double minWidth, double minHeight,
      double maxWidth, double maxHeight) {
    this(width, DEFAULT_UNIT, height, DEFAULT_UNIT,
        minWidth, DEFAULT_UNIT, minHeight, DEFAULT_UNIT,
        maxWidth, DEFAULT_UNIT, maxHeight, DEFAULT_UNIT);
  }

  public Dimensions(Double widthValue, CssUnit widthUnit, Double heightValue, CssUnit heightUnit) {
    this.widthValue = widthValue;
    this.widthUnit = widthUnit;
    this.heightValue = heightValue;
    this.heightUnit = heightUnit;
  }

  public Dimensions(Double widthValue, CssUnit widthUnit, Double heightValue, CssUnit heightUnit,
      Double minWidthValue, CssUnit minWidthUnit, Double minHeightValue, CssUnit minHeightUnit,
      Double maxWidthValue, CssUnit maxWidthUnit, Double maxHeightValue, CssUnit maxHeightUnit) {
    this.widthValue = widthValue;
    this.widthUnit = widthUnit;
    this.heightValue = heightValue;
    this.heightUnit = heightUnit;
    this.minWidthValue = minWidthValue;
    this.minWidthUnit = minWidthUnit;
    this.minHeightValue = minHeightValue;
    this.minHeightUnit = minHeightUnit;
    this.maxWidthValue = maxWidthValue;
    this.maxWidthUnit = maxWidthUnit;
    this.maxHeightValue = maxHeightValue;
    this.maxHeightUnit = maxHeightUnit;
  }

  public Dimensions(Element element) {
    this();
    Assert.notNull(element);
    setFromStyle(element.getStyle());
  }

  public Dimensions(Style style) {
    this();
    setFromStyle(style);
  }

  public Dimensions(UIObject uiObject) {
    this();
    Assert.notNull(uiObject);
    setFromStyle(uiObject.getElement().getStyle());
  }

  public void applyTo(Element el) {
    Assert.notNull(el);
    applyTo(el.getStyle());
  }

  public void applyTo(Style st) {
    Assert.notNull(st);

    if (getWidthValue() != null) {
      setStyleProperty(st, StyleUtils.STYLE_WIDTH, getWidthValue(), getWidthUnit());
    }
    if (getHeightValue() != null) {
      setStyleProperty(st, StyleUtils.STYLE_HEIGHT, getHeightValue(), getHeightUnit());
    }

    if (getMinWidthValue() != null) {
      setStyleProperty(st, StyleUtils.STYLE_MIN_WIDTH, getMinWidthValue(), getMinWidthUnit());
    }
    if (getMinHeightValue() != null) {
      setStyleProperty(st, StyleUtils.STYLE_MIN_HEIGHT, getMinHeightValue(), getMinHeightUnit());
    }

    if (getMaxWidthValue() != null) {
      setStyleProperty(st, StyleUtils.STYLE_MAX_WIDTH, getMaxWidthValue(), getMaxWidthUnit());
    }
    if (getMaxHeightValue() != null) {
      setStyleProperty(st, StyleUtils.STYLE_MAX_HEIGHT, getMaxHeightValue(), getMaxHeightUnit());
    }
  }

  public void applyTo(UIObject obj) {
    Assert.notNull(obj);
    applyTo(obj.getElement());
  }

  public String getCssHeight() {
    if (hasHeight()) {
      return toCssLength(getHeightValue(), normalizeUnit(getHeightUnit()));
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public String getCssWidth() {
    if (hasWidth()) {
      return toCssLength(getWidthValue(), normalizeUnit(getWidthUnit()));
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  @Override
  public CssUnit getHeightUnit() {
    return heightUnit;
  }

  @Override
  public Double getHeightValue() {
    return heightValue;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = Lists.newArrayList();

    if (getWidthValue() != null) {
      info.add(new Property(StyleUtils.STYLE_WIDTH,
          toCssLength(getWidthValue(), getWidthUnit())));
    }
    if (getHeightValue() != null) {
      info.add(new Property(StyleUtils.STYLE_HEIGHT,
          toCssLength(getHeightValue(), getHeightUnit())));
    }

    if (getMinWidthValue() != null) {
      info.add(new Property(StyleUtils.STYLE_MIN_WIDTH,
          toCssLength(getMinWidthValue(), getMinWidthUnit())));
    }
    if (getMinHeightValue() != null) {
      info.add(new Property(StyleUtils.STYLE_MIN_HEIGHT,
          toCssLength(getMinHeightValue(), getMinHeightUnit())));
    }

    if (getMaxWidthValue() != null) {
      info.add(new Property(StyleUtils.STYLE_MAX_WIDTH,
          toCssLength(getMaxWidthValue(), getMaxWidthUnit())));
    }
    if (getMaxHeightValue() != null) {
      info.add(new Property(StyleUtils.STYLE_MAX_HEIGHT,
          toCssLength(getMaxHeightValue(), getMaxHeightUnit())));
    }

    PropertyUtils.addWhenEmpty(info, getClass());
    return info;
  }

  public int getIntHeight() {
    return BeeUtils.toInt(getHeightValue());
  }

  public int getIntMaxHeight() {
    return BeeUtils.toInt(getMaxHeightValue());
  }

  public int getIntMaxWidth() {
    return BeeUtils.toInt(getMaxWidthValue());
  }

  public int getIntMinHeight() {
    return BeeUtils.toInt(getMinHeightValue());
  }

  public int getIntMinWidth() {
    return BeeUtils.toInt(getMinWidthValue());
  }

  public int getIntWidth() {
    return BeeUtils.toInt(getWidthValue());
  }

  public CssUnit getMaxHeightUnit() {
    return maxHeightUnit;
  }

  public Double getMaxHeightValue() {
    return maxHeightValue;
  }

  public CssUnit getMaxWidthUnit() {
    return maxWidthUnit;
  }

  public Double getMaxWidthValue() {
    return maxWidthValue;
  }

  public CssUnit getMinHeightUnit() {
    return minHeightUnit;
  }

  public Double getMinHeightValue() {
    return minHeightValue;
  }

  public CssUnit getMinWidthUnit() {
    return minWidthUnit;
  }

  public Double getMinWidthValue() {
    return minWidthValue;
  }

  @Override
  public CssUnit getWidthUnit() {
    return widthUnit;
  }

  @Override
  public Double getWidthValue() {
    return widthValue;
  }

  public boolean hasHeight() {
    return BeeUtils.isPositive(getHeightValue());
  }

  public boolean hasMaxHeight() {
    return BeeUtils.isPositive(getMaxHeightValue());
  }

  public boolean hasMaxWidth() {
    return BeeUtils.isPositive(getMaxWidthValue());
  }

  public boolean hasMinHeight() {
    return BeeUtils.isPositive(getMinHeightValue());
  }

  public boolean hasMinWidth() {
    return BeeUtils.isPositive(getMinWidthValue());
  }

  public boolean hasWidth() {
    return BeeUtils.isPositive(getWidthValue());
  }

  public boolean isEmpty() {
    return getWidthValue() == null && getHeightValue() == null
        && getMinWidthValue() == null && getMinHeightValue() == null
        && getMaxWidthValue() == null && getMaxHeightValue() == null;
  }

  public void removeFrom(Element el) {
    Assert.notNull(el);
    removeFrom(el.getStyle());
  }

  public void removeFrom(Style st) {
    Assert.notNull(st);

    if (getWidthValue() != null) {
      st.clearProperty(StyleUtils.STYLE_WIDTH);
    }
    if (getHeightValue() != null) {
      st.clearProperty(StyleUtils.STYLE_HEIGHT);
    }

    if (getMinWidthValue() != null) {
      st.clearProperty(StyleUtils.STYLE_MIN_WIDTH);
    }
    if (getMinHeightValue() != null) {
      st.clearProperty(StyleUtils.STYLE_MIN_HEIGHT);
    }

    if (getMaxWidthValue() != null) {
      st.clearProperty(StyleUtils.STYLE_MAX_WIDTH);
    }
    if (getMaxHeightValue() != null) {
      st.clearProperty(StyleUtils.STYLE_MAX_HEIGHT);
    }
  }

  public void removeFrom(UIObject obj) {
    Assert.notNull(obj);
    removeFrom(obj.getElement());
  }

  public void setHeight(Double value, CssUnit unit) {
    setHeightValue(value);
    setHeightUnit(unit);
  }

  public void setHeight(int value) {
    setHeight((double) value, DEFAULT_UNIT);
  }

  @Override
  public void setHeightUnit(CssUnit heightUnit) {
    this.heightUnit = heightUnit;
  }

  @Override
  public void setHeightValue(Double heightValue) {
    this.heightValue = heightValue;
  }

  public void setMaxHeight(Double value, CssUnit unit) {
    setMaxHeightValue(value);
    setMaxHeightUnit(unit);
  }

  public void setMaxHeight(int value) {
    setMaxHeight((double) value, DEFAULT_UNIT);
  }

  public void setMaxHeightUnit(CssUnit maxHeightUnit) {
    this.maxHeightUnit = maxHeightUnit;
  }

  public void setMaxHeightValue(Double maxHeightValue) {
    this.maxHeightValue = maxHeightValue;
  }

  public void setMaxWidth(Double value, CssUnit unit) {
    setMaxWidthValue(value);
    setMaxWidthUnit(unit);
  }

  public void setMaxWidth(int value) {
    setMaxWidth((double) value, DEFAULT_UNIT);
  }

  public void setMaxWidthUnit(CssUnit maxWidthUnit) {
    this.maxWidthUnit = maxWidthUnit;
  }

  public void setMaxWidthValue(Double maxWidthValue) {
    this.maxWidthValue = maxWidthValue;
  }

  public void setMinHeight(Double value, CssUnit unit) {
    setMinHeightValue(value);
    setMinHeightUnit(unit);
  }

  public void setMinHeight(int value) {
    setMinHeight((double) value, DEFAULT_UNIT);
  }

  public void setMinHeightUnit(CssUnit minHeightUnit) {
    this.minHeightUnit = minHeightUnit;
  }

  public void setMinHeightValue(Double minHeightValue) {
    this.minHeightValue = minHeightValue;
  }

  public void setMinWidth(Double value, CssUnit unit) {
    setMinWidthValue(value);
    setMinWidthUnit(unit);
  }

  public void setMinWidth(int value) {
    setMinWidth((double) value, DEFAULT_UNIT);
  }

  public void setMinWidthUnit(CssUnit minWidthUnit) {
    this.minWidthUnit = minWidthUnit;
  }

  public void setMinWidthValue(Double minWidthValue) {
    this.minWidthValue = minWidthValue;
  }

  public void setWidth(Double value, CssUnit unit) {
    setWidthValue(value);
    setWidthUnit(unit);
  }

  public void setWidth(int value) {
    setWidth((double) value, DEFAULT_UNIT);
  }

  @Override
  public void setWidthUnit(CssUnit widthUnit) {
    this.widthUnit = widthUnit;
  }

  @Override
  public void setWidthValue(Double widthValue) {
    this.widthValue = widthValue;
  }

  private void setFromStyle(Style style) {
    Assert.notNull(style);

    setFromStyleProperty(style, StyleUtils.STYLE_WIDTH);
    setFromStyleProperty(style, StyleUtils.STYLE_HEIGHT);
    setFromStyleProperty(style, StyleUtils.STYLE_MIN_WIDTH);
    setFromStyleProperty(style, StyleUtils.STYLE_MIN_HEIGHT);
    setFromStyleProperty(style, StyleUtils.STYLE_MAX_WIDTH);
    setFromStyleProperty(style, StyleUtils.STYLE_MAX_HEIGHT);
  }

  private void setFromStyleProperty(Style style, String name) {
    String length = style.getProperty(name);
    if (!BeeUtils.isEmpty(length)) {
      return;
    }
    Pair<Double, CssUnit> pair = StyleUtils.parseCssLength(length);
    if (pair == null) {
      return;
    }

    Double value = pair.getA();
    if (value == null) {
      return;
    }

    if (BeeUtils.same(name, StyleUtils.STYLE_WIDTH)) {
      setWidthValue(value);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_HEIGHT)) {
      setHeightValue(value);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MIN_WIDTH)) {
      setMinWidthValue(value);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MIN_HEIGHT)) {
      setMinHeightValue(value);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MAX_WIDTH)) {
      setMaxWidthValue(value);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MAX_HEIGHT)) {
      setMaxHeightValue(value);
    } else {
      Assert.untouchable();
    }

    CssUnit unit = pair.getB();
    if (unit == null) {
      return;
    }

    if (BeeUtils.same(name, StyleUtils.STYLE_WIDTH)) {
      setWidthUnit(unit);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_HEIGHT)) {
      setHeightUnit(unit);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MIN_WIDTH)) {
      setMinWidthUnit(unit);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MIN_HEIGHT)) {
      setMinHeightUnit(unit);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MAX_WIDTH)) {
      setMaxWidthUnit(unit);
    } else if (BeeUtils.same(name, StyleUtils.STYLE_MAX_HEIGHT)) {
      setMaxHeightUnit(unit);
    }
  }

  private static void setStyleProperty(Style style, String name, Double value, CssUnit unit) {
    if (value == null) {
      return;
    }
    if (unit == null) {
      StyleUtils.setProperty(style, name, value, DEFAULT_UNIT);
    } else {
      StyleUtils.setProperty(style, name, value, unit);
    }
  }

  private static String toCssLength(Double value, CssUnit unit) {
    if (value == null) {
      return BeeConst.STRING_EMPTY;
    }
    if (unit == null) {
      return BeeUtils.toString(value);
    }
    return BeeUtils.toString(value) + unit.getCaption();
  }
}
