package com.butent.bee.client.render;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.utils.BeeUtils;

public class ColumnToken implements HasDateTimeFormat, HasNumberFormat, HasScale, HasValueType {

  public static ColumnToken create(int dataIndex, ValueType valueType,
      RenderableToken renderableToken) {
    ColumnToken columnToken = new ColumnToken(dataIndex, valueType);

    if (renderableToken != null) {
      columnToken.setPrefix(Localized.maybeTranslate(renderableToken.getPrefix()));
      columnToken.setSuffix(Localized.maybeTranslate(renderableToken.getSuffix()));

      if (BeeUtils.isTrue(renderableToken.getAddPrefixWhenEmpty())) {
        columnToken.setAddPrefixWhenEmpty(true);
      }
      if (BeeUtils.isTrue(renderableToken.getAddSuffixWhenEmpty())) {
        columnToken.setAddSuffixWhenEmpty(true);
      }

      if (!BeeUtils.isEmpty(renderableToken.getFormat())) {
        Format.setFormat(columnToken, valueType, renderableToken.getFormat());
      }
      if (BeeUtils.isNonNegative(renderableToken.getScale())) {
        columnToken.setScale(renderableToken.getScale());
      }
    }

    return columnToken;
  }

  private final int dataIndex;
  private final ValueType valueType;

  private String prefix;
  private String suffix;

  private boolean addPrefixWhenEmpty;
  private boolean addSuffixWhenEmpty;

  private DateTimeFormat dateTimeFormat;
  private NumberFormat numberFormat;

  private int scale = BeeConst.UNDEF;

  public ColumnToken(int dataIndex, ValueType valueType) {
    super();
    this.dataIndex = dataIndex;
    this.valueType = valueType;
  }

  public boolean addPrefixWhenEmpty() {
    return addPrefixWhenEmpty;
  }

  public boolean addSuffixWhenEmpty() {
    return addSuffixWhenEmpty;
  }

  public int getDataIndex() {
    return dataIndex;
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return dateTimeFormat;
  }

  @Override
  public NumberFormat getNumberFormat() {
    return numberFormat;
  }

  public String getPrefix() {
    return prefix;
  }

  @Override
  public int getScale() {
    return scale;
  }

  public String getSuffix() {
    return suffix;
  }

  @Override
  public ValueType getValueType() {
    return valueType;
  }

  public boolean hasPrefix() {
    return BeeUtils.hasLength(getPrefix(), 1);
  }

  public boolean hasSuffix() {
    return BeeUtils.hasLength(getSuffix(), 1);
  }

  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    String value = DataUtils.getStringQuietly(row, getDataIndex());
    String formatted = Format.render(value, getValueType(), getDateTimeFormat(), getNumberFormat(),
        getScale());

    return addPrefixAndSuffix(formatted);
  }

  public void setAddPrefixWhenEmpty(boolean addPrefixWhenEmpty) {
    this.addPrefixWhenEmpty = addPrefixWhenEmpty;
  }

  public void setAddSuffixWhenEmpty(boolean addSuffixWhenEmpty) {
    this.addSuffixWhenEmpty = addSuffixWhenEmpty;
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat format) {
    this.dateTimeFormat = format;
  }

  @Override
  public void setNumberFormat(NumberFormat format) {
    this.numberFormat = format;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public void setScale(int scale) {
    this.scale = scale;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  private String addPrefixAndSuffix(String input) {
    if (!hasPrefix() && !hasSuffix()) {
      return input;
    }

    StringBuilder result = new StringBuilder();
    if (hasPrefix() && (!BeeUtils.isEmpty(input) || addPrefixWhenEmpty())) {
      result.append(getPrefix());
    }
    if (input != null) {
      result.append(input);
    }
    if (hasSuffix() && (!BeeUtils.isEmpty(input) || addSuffixWhenEmpty())) {
      result.append(getSuffix());
    }

    return result.toString();
  }
}
