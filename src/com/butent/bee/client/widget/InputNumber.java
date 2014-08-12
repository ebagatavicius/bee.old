package com.butent.bee.client.widget;

import com.google.common.base.CharMatcher;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.HasIntStep;
import com.butent.bee.shared.HasPrecision;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.data.HasRelatedCurrency;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a user interface component for inserting number type values.
 */

public class InputNumber extends InputText implements HasBounds, HasIntStep,
    HasNumberFormat, HasPrecision, HasScale, HasRelatedCurrency {

  public static final CharMatcher INT_CHAR_MATCHER =
      CharMatcher.inRange(BeeConst.CHAR_ZERO, BeeConst.CHAR_NINE)
          .or(CharMatcher.is(BeeConst.CHAR_SPACE))
          .or(CharMatcher.is(BeeConst.CHAR_MINUS));

  public static final CharMatcher NUM_CHAR_MATCHER = INT_CHAR_MATCHER
      .or(CharMatcher.is(BeeConst.CHAR_POINT))
      .or(CharMatcher.is(BeeConst.CHAR_COMMA));

  private int precision = BeeConst.UNDEF;
  private int scale = BeeConst.UNDEF;

  private String minValue;
  private String maxValue;

  private int stepValue = BeeConst.UNDEF;

  private NumberFormat format;

  private String currencySource;

  public InputNumber() {
    super();
  }

  public InputNumber(Element element) {
    super(element);
  }

  @Override
  public String getCurrencySource() {
    return currencySource;
  }

  @Override
  public String getIdPrefix() {
    return "number";
  }

  @Override
  public String getMaxValue() {
    return maxValue;
  }

  @Override
  public String getMinValue() {
    return minValue;
  }

  @Override
  public String getNormalizedValue() {
    String v = BeeUtils.trim(getValue());
    if (BeeUtils.isEmpty(v) && isNullable()) {
      return null;
    }

    Double d = parse(v);
    if (d == null) {
      return isNullable() ? null : BeeConst.STRING_ZERO;
    } else {
      return BeeUtils.toString(d, getDecimals(sanitize(v)));
    }
  }

  public Double getNumber() {
    return parse(BeeUtils.trim(getValue()));
  }

  @Override
  public NumberFormat getNumberFormat() {
    return format;
  }

  @Override
  public int getPrecision() {
    return precision;
  }

  @Override
  public int getScale() {
    return scale;
  }

  @Override
  public int getStepValue() {
    return stepValue;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_DECIMAL;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    if (getNumberFormat() != null || getScale() >= 0) {
      render(normalizedValue);
    }
  }

  @Override
  public void render(String value) {
    setValue(parse(BeeUtils.trim(value)));
  }

  @Override
  public void setCurrencySource(String currencySource) {
    this.currencySource = currencySource;
  }

  @Override
  public void setMaxValue(String maxValue) {
    this.maxValue = maxValue;
  }

  @Override
  public void setMinValue(String minValue) {
    this.minValue = minValue;
  }

  @Override
  public void setNumberFormat(NumberFormat numberFormat) {
    this.format = numberFormat;
  }

  @Override
  public void setPrecision(int precision) {
    this.precision = precision;
  }

  @Override
  public void setScale(int scale) {
    this.scale = scale;
  }

  @Override
  public void setStepValue(int stepValue) {
    this.stepValue = stepValue;
  }

  public void setValue(Double v) {
    setValue(format(v));
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {

    if (BeeUtils.isEmpty(oldValue) || acceptChar(charCode) || getNumberFormat() == null) {
      super.startEdit(oldValue, charCode, onEntry, sourceElement);
    } else {
      setValue(BeeUtils.toDouble(oldValue));
      if (onEntry != null) {
        super.startEdit(getValue(), charCode, onEntry, sourceElement);
      }
    }
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    List<String> messages = new ArrayList<>();
    messages.addAll(super.validate(checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    String v = BeeUtils.trim(getValue());
    if (BeeUtils.isEmpty(v)) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.getConstants().valueRequired());
      }
      return messages;
    }

    if (getNumberFormat() != null) {
      String s = sanitize(v);
      Double d = Format.parseQuietly(getNumberFormat(), s);
      if (d == null) {
        messages.add(Localized.getConstants().invalidNumberFormat());
        messages.add(BeeUtils.joinWords(v, BeeUtils.bracket(getNumberFormat().getPattern())));
        return messages;
      }

      v = BeeUtils.toString(d, getDecimals(s));
    }

    if (!checkType(sanitize(v))) {
      messages.add(BeeUtils.joinWords(Localized.getConstants().invalidNumber(), v));
      return messages;
    }

    if (!checkBounds(getNumber())) {
      messages.addAll(ValidationHelper.getBounds(this));
    }
    return messages;
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    List<String> messages = new ArrayList<>();
    messages.addAll(super.validate(normalizedValue, checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (BeeUtils.isEmpty(normalizedValue)) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.getConstants().valueRequired());
      }
      return messages;
    }

    if (!checkBounds(BeeUtils.toDoubleOrNull(normalizedValue))) {
      messages.addAll(ValidationHelper.getBounds(this));
    }
    return messages;
  }

  protected boolean checkType(String v) {
    return BeeUtils.isDouble(v);
  }

  @Override
  protected CharMatcher getDefaultCharMatcher() {
    return NUM_CHAR_MATCHER;
  }

  @Override
  protected String getDefaultStyleName() {
    return "bee-InputNumber";
  }

  protected String sanitize(String v) {
    if (BeeUtils.contains(v, BeeConst.CHAR_COMMA) && !BeeUtils.contains(v, BeeConst.CHAR_POINT)) {
      return v.replace(BeeConst.CHAR_COMMA, BeeConst.CHAR_POINT);
    } else {
      return v;
    }
  }

  private boolean checkBounds(Double v) {
    if (v == null) {
      return isNullable();
    }

    Double min = BeeUtils.toDoubleOrNull(getMinValue());
    if (min != null && v < min) {
      return false;
    }

    Double max = BeeUtils.toDoubleOrNull(getMaxValue());
    if (max != null && v > max) {
      return false;
    }
    return true;
  }

  private String format(Double v) {
    if (v == null) {
      return BeeConst.STRING_EMPTY;
    } else if (getNumberFormat() != null) {
      return getNumberFormat().format(v);
    } else {
      return BeeUtils.toString(v, getScale());
    }
  }

  private int getDecimals(String s) {
    int decimals = BeeUtils.getDecimals(s);
    if (decimals < 0 || getScale() < 0) {
      return decimals;
    } else {
      return Math.min(decimals, getScale());
    }
  }

  private Double parse(String input) {
    String s = sanitize(input);

    if (BeeUtils.isEmpty(s)) {
      return null;

    } else if (getNumberFormat() != null) {
      Double d = Format.parseQuietly(getNumberFormat(), s);
      if (d != null) {
        return d;
      }
    }

    return BeeUtils.toDoubleOrNull(BeeUtils.removeWhiteSpace(s));
  }
}
