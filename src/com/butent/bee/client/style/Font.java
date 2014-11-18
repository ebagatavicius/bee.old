package com.butent.bee.client.style;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.RangeMap;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.css.values.FontStyle;
import com.butent.bee.shared.css.values.FontVariant;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextTransform;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enables to operate with various parameters of fonts used by the system.
 */

public final class Font implements HasInfo {

  public static class Builder {

    private FontStyle style;
    private FontVariant variant;
    private FontWeight weight;
    private FontSize absoluteSize;
    private double sizeValue = UNKNOWN;
    private CssUnit sizeCssUnit;
    private String family;

    private String lineHeight;
    private TextTransform textTransform;
    private String letterSpacing;

    public Font build() {
      Font result = new Font();

      result.setStyle(style);
      result.setVariant(variant);
      result.setWeight(weight);

      result.setAbsoluteSize(absoluteSize);
      result.setSizeValue(sizeValue);
      result.setSizeCssUnit(sizeCssUnit);

      result.setFamily(family);

      result.setLineHeight(lineHeight);
      result.setTextTransform(textTransform);
      result.setLetterSpacing(letterSpacing);

      return result;
    }

    public Builder family(String value) {
      this.family = value;
      return this;
    }

    public Builder letterSpacing(String value) {
      this.letterSpacing = value;
      return this;
    }

    public Builder lineHeight(String value) {
      this.lineHeight = value;
      return this;
    }

    public Builder size(double value, CssUnit unit) {
      this.sizeValue = value;
      this.sizeCssUnit = unit;
      return this;
    }

    public Builder size(FontSize size) {
      this.absoluteSize = size;
      return this;
    }

    public Builder style(FontStyle value) {
      this.style = value;
      return this;
    }

    public Builder textTransform(TextTransform value) {
      this.textTransform = value;
      return this;
    }

    public Builder variant(FontVariant value) {
      this.variant = value;
      return this;
    }

    public Builder weight(FontWeight value) {
      this.weight = value;
      return this;
    }
  }

  public static final String PREFIX_STYLE = "st:";
  public static final String PREFIX_VARIANT = "v:";
  public static final String PREFIX_WEIGHT = "w:";
  public static final String PREFIX_SIZE = "x:";
  public static final String PREFIX_FAMILY = "f:";

  public static final String PREFIX_LINE_HEIGHT = "lh:";
  public static final String PREFIX_TEXT_TRANSFORM = "tt:";
  public static final String PREFIX_LETTER_SPACING = "ls:";

  private static final double UNKNOWN = -1.0;

  private static final RangeMap<Double, CssUnit> DEFAULT_UNITS =
      RangeMap.create(Range.lessThan(4.0), CssUnit.EM, Range.atLeast(4.0), CssUnit.PX);

  public static Font bold() {
    Font font = new Font();
    font.setWeight(FontWeight.BOLD);
    return font;
  }

  public static Font getComputed(Element el) {
    Map<String, String> styles = ComputedStyles.getNormalized(el);
    Font font = new Font();

    String value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_FONT_STYLE));
    if (!BeeUtils.isEmpty(value)) {
      font.setStyle(StyleUtils.parseFontStyle(value));
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_FONT_WEIGHT));
    if (!BeeUtils.isEmpty(value)) {
      font.setWeight(StyleUtils.parseFontWeight(value));
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_FONT_VARIANT));
    if (!BeeUtils.isEmpty(value)) {
      font.setVariant(StyleUtils.parseFontVariant(value));
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_FONT_FAMILY));
    if (!BeeUtils.isEmpty(value)) {
      font.setFamily(value);
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_LINE_HEIGHT));
    if (!BeeUtils.isEmpty(value)) {
      font.setLineHeight(value);
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_TEXT_TRANSFORM));
    if (!BeeUtils.isEmpty(value)) {
      font.setTextTransform(StyleUtils.parseTextTransform(value));
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_LETTER_SPACING));
    if (!BeeUtils.isEmpty(value)) {
      font.setLetterSpacing(value);
    }

    value = styles.get(ComputedStyles.normalize(StyleUtils.STYLE_FONT_SIZE));
    if (!BeeUtils.isEmpty(value)) {
      font.setSize(value);
    }

    return font;
  }

  public static Font merge(Font... fonts) {
    if (fonts == null || fonts.length <= 0) {
      return null;
    }
    Font result = null;

    for (Font font : fonts) {
      if (font == null) {
        continue;
      }
      if (result == null) {
        result = font.copy();
        continue;
      }

      if (font.getStyle() != null) {
        result.setStyle(font.getStyle());
      }
      if (font.getVariant() != null) {
        result.setVariant(font.getVariant());
      }
      if (font.getWeight() != null) {
        result.setWeight(font.getWeight());
      }
      if (font.getAbsoluteSize() != null) {
        result.setAbsoluteSize(font.getAbsoluteSize());
      }
      if (font.getSizeValue() > 0) {
        result.setSizeValue(font.getSizeValue());
        result.setSizeCssUnit(font.getSizeCssUnit());
      }
      if (!BeeUtils.isEmpty(font.getFamily())) {
        result.setFamily(font.getFamily());
      }

      if (!BeeUtils.isEmpty(font.getLineHeight())) {
        result.setLineHeight(font.getLineHeight());
      }
      if (font.getTextTransform() != null) {
        result.setTextTransform(font.getTextTransform());
      }
      if (!BeeUtils.isEmpty(font.getLetterSpacing())) {
        result.setLetterSpacing(font.getLetterSpacing());
      }
    }
    return result;
  }

  public static Font parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }
    return parse(BeeUtils.split(input, BeeConst.CHAR_SPACE));
  }

  public static Font parse(String[] input) {
    if (input == null || input.length <= 0) {
      return null;
    }
    Font font = new Font();

    for (String s : input) {
      if (BeeUtils.isEmpty(s)) {
        continue;
      }

      if (BeeUtils.isPrefix(s, PREFIX_STYLE)) {
        font.setStyle(StyleUtils.parseFontStyle(BeeUtils.removePrefix(s, PREFIX_STYLE)));
        continue;
      }
      if (BeeUtils.isPrefix(s, PREFIX_WEIGHT)) {
        font.setWeight(StyleUtils.parseFontWeight(BeeUtils.removePrefix(s, PREFIX_WEIGHT)));
        continue;
      }
      if (BeeUtils.isPrefix(s, PREFIX_VARIANT)) {
        font.setVariant(StyleUtils.parseFontVariant(BeeUtils.removePrefix(s, PREFIX_VARIANT)));
        continue;
      }
      if (BeeUtils.isPrefix(s, PREFIX_FAMILY)) {
        font.setFamily(BeeUtils.removePrefix(s, PREFIX_FAMILY));
        continue;
      }

      if (BeeUtils.isPrefix(s, PREFIX_LINE_HEIGHT)) {
        font.setLineHeight(BeeUtils.removePrefix(s, PREFIX_LINE_HEIGHT));
        continue;
      }
      if (BeeUtils.isPrefix(s, PREFIX_TEXT_TRANSFORM)) {
        font.setTextTransform(StyleUtils.parseTextTransform(BeeUtils.removePrefix(s,
            PREFIX_TEXT_TRANSFORM)));
        continue;
      }
      if (BeeUtils.isPrefix(s, PREFIX_LETTER_SPACING)) {
        font.setLetterSpacing(BeeUtils.removePrefix(s, PREFIX_LETTER_SPACING));
        continue;
      }

      if (BeeUtils.isPrefix(s, PREFIX_SIZE)) {
        font.setSize(BeeUtils.removePrefix(s, PREFIX_SIZE));
        continue;
      }

      FontStyle fontStyle = StyleUtils.parseFontStyle(s);
      if (fontStyle != null) {
        font.setStyle(fontStyle);
        continue;
      }

      FontWeight fontWeight = StyleUtils.parseFontWeight(s);
      if (fontWeight != null) {
        font.setWeight(fontWeight);
        continue;
      }

      FontVariant fontVariant = StyleUtils.parseFontVariant(s);
      if (fontVariant != null) {
        font.setVariant(fontVariant);
        continue;
      }

      if (!font.setSize(s)) {
        font.setFamily(s);
      }
    }
    return font;
  }

  private FontStyle style;
  private FontVariant variant;
  private FontWeight weight;
  private FontSize absoluteSize;
  private double sizeValue;
  private CssUnit sizeCssUnit;

  private String family;
  private String lineHeight;
  private TextTransform textTransform;

  private String letterSpacing;

  private Font() {
    super();
  }

  public void applyTo(Element el) {
    Assert.notNull(el);
    applyTo(el.getStyle());
  }

  public void applyTo(Style st) {
    Assert.notNull(st);

    if (getStyle() != null) {
      StyleUtils.setProperty(st, CssProperties.FONT_STYLE, getStyle());
    }
    if (getVariant() != null) {
      StyleUtils.setFontVariant(st, getVariant());
    }
    if (getWeight() != null) {
      StyleUtils.setProperty(st, CssProperties.FONT_WEIGHT, getWeight());
    }

    if (getSizeValue() > 0 && getSizeCssUnit() != null) {
      StyleUtils.setFontSize(st, getSizeValue(), getSizeCssUnit());
    } else if (getAbsoluteSize() != null) {
      StyleUtils.setFontSize(st, getAbsoluteSize());
    } else if (getSizeValue() > 0) {
      StyleUtils.setFontSize(st, getSizeValue(), DEFAULT_UNITS.get(getSizeValue()));
    }

    if (!BeeUtils.isEmpty(getFamily())) {
      StyleUtils.setFontFamily(st, getFamily());
    }

    if (!BeeUtils.isEmpty(getLineHeight())) {
      StyleUtils.setLineHeight(st, getLineHeight());
    }
    if (getTextTransform() != null) {
      StyleUtils.setProperty(st, CssProperties.TEXT_TRANSFORM, getTextTransform());
    }
    if (!BeeUtils.isEmpty(getLetterSpacing())) {
      StyleUtils.setLetterSpacing(st, getLetterSpacing());
    }
  }

  public void applyTo(UIObject obj) {
    Assert.notNull(obj);
    applyTo(obj.getElement());
  }

  public SafeStyles buildCss() {
    if (isEmpty()) {
      return null;
    }

    SafeStylesBuilder builder = new SafeStylesBuilder();

    if (getStyle() != null) {
      builder.append(StyleUtils.buildFontStyle(getStyle()));
    }
    if (getVariant() != null) {
      builder.append(StyleUtils.buildFontVariant(getVariant()));
    }
    if (getWeight() != null) {
      builder.append(StyleUtils.buildFontWeight(getWeight()));
    }

    if (getSizeValue() > 0 && getSizeCssUnit() != null) {
      builder.append(StyleUtils.buildFontSize(getSizeValue(), getSizeCssUnit()));
    } else if (getAbsoluteSize() != null) {
      builder.append(StyleUtils.buildFontSize(getAbsoluteSize()));
    } else if (getSizeValue() > 0) {
      builder.append(StyleUtils.buildFontSize(getSizeValue(), DEFAULT_UNITS.get(getSizeValue())));
    }

    if (!BeeUtils.isEmpty(getFamily())) {
      builder.append(StyleUtils.buildFontFamily(getFamily()));
    }

    if (!BeeUtils.isEmpty(getLineHeight())) {
      builder.append(StyleUtils.buildLineHeight(getLineHeight()));
    }
    if (getTextTransform() != null) {
      builder.append(StyleUtils.buildTextTransform(getTextTransform()));
    }
    if (!BeeUtils.isEmpty(getLetterSpacing())) {
      builder.append(StyleUtils.buildLetterSpacing(getLetterSpacing()));
    }

    return builder.toSafeStyles();
  }

  public Font copy() {
    Font result = new Font();

    result.setStyle(getStyle());
    result.setVariant(getVariant());
    result.setWeight(getWeight());

    result.setAbsoluteSize(getAbsoluteSize());
    result.setSizeValue(getSizeValue());
    result.setSizeCssUnit(getSizeCssUnit());

    result.setFamily(getFamily());

    result.setLineHeight(getLineHeight());
    result.setTextTransform(getTextTransform());
    result.setLetterSpacing(getLetterSpacing());

    return result;
  }

  public FontSize getAbsoluteSize() {
    return absoluteSize;
  }

  public String getFamily() {
    return family;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = new ArrayList<>();

    if (getStyle() != null) {
      info.add(new Property("Font Style", getStyle().getCssName()));
    }
    if (getVariant() != null) {
      info.add(new Property("Font Variant", getVariant().getCssName()));
    }
    if (getWeight() != null) {
      info.add(new Property("Font Weight", getWeight().getCssName()));
    }

    if (getAbsoluteSize() != null) {
      info.add(new Property("Font Absolute Size", getAbsoluteSize().getCssName()));
    }
    if (getSizeValue() > 0) {
      info.add(new Property("Font Size Value", BeeUtils.toString(getSizeValue())));
    }
    if (getSizeCssUnit() != null) {
      info.add(new Property("Font Size CssUnit", getSizeCssUnit().getCaption()));
    }

    if (!BeeUtils.isEmpty(getFamily())) {
      info.add(new Property("Font Family", getFamily()));
    }

    if (!BeeUtils.isEmpty(getLineHeight())) {
      info.add(new Property("Line Height", getLineHeight()));
    }
    if (getTextTransform() != null) {
      info.add(new Property("Text Transform", getTextTransform().getCssName()));
    }
    if (!BeeUtils.isEmpty(getLetterSpacing())) {
      info.add(new Property("Letter Spacing", getLetterSpacing()));
    }

    PropertyUtils.addWhenEmpty(info, getClass());
    return info;
  }

  public String getLetterSpacing() {
    return letterSpacing;
  }

  public String getLineHeight() {
    return lineHeight;
  }

  public CssUnit getSizeCssUnit() {
    return sizeCssUnit;
  }

  public double getSizeValue() {
    return sizeValue;
  }

  public FontStyle getStyle() {
    return style;
  }

  public TextTransform getTextTransform() {
    return textTransform;
  }

  public FontVariant getVariant() {
    return variant;
  }

  public FontWeight getWeight() {
    return weight;
  }

  public boolean isEmpty() {
    return getStyle() == null && getVariant() == null && getWeight() == null
        && getAbsoluteSize() == null && getSizeValue() <= 0
        && BeeUtils.isEmpty(getFamily()) && BeeUtils.isEmpty(getLineHeight())
        && getTextTransform() == null && BeeUtils.isEmpty(getLetterSpacing());
  }

  public void removeFrom(Element el) {
    Assert.notNull(el);
    removeFrom(el.getStyle());
  }

  public void removeFrom(Style st) {
    Assert.notNull(st);

    if (getStyle() != null) {
      st.clearFontStyle();
    }
    if (getVariant() != null) {
      st.clearProperty(StyleUtils.STYLE_FONT_VARIANT);
    }
    if (getWeight() != null) {
      st.clearFontWeight();
    }

    if (getAbsoluteSize() != null || getSizeValue() > 0) {
      st.clearFontSize();
    }

    if (!BeeUtils.isEmpty(getFamily())) {
      st.clearProperty(StyleUtils.STYLE_FONT_FAMILY);
    }

    if (!BeeUtils.isEmpty(getLineHeight())) {
      st.clearLineHeight();
    }
    if (getTextTransform() != null) {
      st.clearTextTransform();
    }
    if (!BeeUtils.isEmpty(getLetterSpacing())) {
      st.clearProperty(StyleUtils.STYLE_LETTER_SPACING);
    }
  }

  public void removeFrom(UIObject obj) {
    Assert.notNull(obj);
    removeFrom(obj.getElement());
  }

  public void setAbsoluteSize(FontSize absoluteSize) {
    this.absoluteSize = absoluteSize;
  }

  public void setFamily(String family) {
    this.family = family;
  }

  public void setLetterSpacing(String letterSpacing) {
    this.letterSpacing = letterSpacing;
  }

  public void setLineHeight(String lineHeight) {
    this.lineHeight = lineHeight;
  }

  public void setSizeCssUnit(CssUnit sizeCssUnit) {
    this.sizeCssUnit = sizeCssUnit;
  }

  public void setSizeValue(double sizeValue) {
    this.sizeValue = sizeValue;
  }

  public void setStyle(FontStyle style) {
    this.style = style;
  }

  public void setTextTransform(TextTransform textTransform) {
    this.textTransform = textTransform;
  }

  public void setVariant(FontVariant variant) {
    this.variant = variant;
  }

  public void setWeight(FontWeight weight) {
    this.weight = weight;
  }

  @Override
  public String toString() {
    SafeStyles css = buildCss();
    if (css == null) {
      return "Font instance is empty";
    }
    return css.asString();
  }

  private boolean setSize(String input) {
    if (BeeUtils.isEmpty(input)) {
      return false;
    }

    if (BeeUtils.isDigit(input.trim().charAt(0))) {
      Pair<Double, CssUnit> cssLength = StyleUtils.parseCssLength(input);
      if (cssLength != null && cssLength.getA() != null) {
        setSizeValue(cssLength.getA());
        setSizeCssUnit(cssLength.getB());
      } else if (BeeUtils.isDouble(input)) {
        setSizeValue(BeeUtils.toDouble(input));
      }
      return true;

    } else {
      FontSize fontSize = StyleUtils.parseFontSize(input);
      if (fontSize != null) {
        setAbsoluteSize(fontSize);
        return true;
      }
    }
    return false;
  }
}