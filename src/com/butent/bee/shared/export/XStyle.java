package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XStyle implements BeeSerializable {

  private enum Serial {
    COLOR, FONT, FORMAT, TEXT_ALIGN, VERTICAL_ALIGN,
    BORDER_LEFT, BORDER_LEFT_COLOR, BORDER_RIGHT, BORDER_RIGHT_COLOR,
    BORDER_TOP, BORDER_TOP_COLOR, BORDER_BOTTOM, BORDER_BOTTOM_COLOR
  }

  public static XStyle background(String bg) {
    XStyle style = new XStyle();
    style.setColor(bg);
    return style;
  }

  public static XStyle center() {
    XStyle style = new XStyle();
    style.setTextAlign(TextAlign.CENTER);
    return style;
  }

  public static XStyle middle() {
    XStyle style = new XStyle();
    style.setVerticalAlign(VerticalAlign.MIDDLE);
    return style;
  }

  public static XStyle restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else {
      XStyle style = new XStyle();
      style.deserialize(s);
      return style;
    }
  }

  public static XStyle right() {
    XStyle style = new XStyle();
    style.setTextAlign(TextAlign.RIGHT);
    return style;
  }

  private String color;
  private Integer fontRef;

  private String format;

  private TextAlign textAlign;
  private VerticalAlign verticalAlign;

  private BorderStyle borderLeft;
  private String borderLeftColor;

  private BorderStyle borderRight;
  private String borderRightColor;

  private BorderStyle borderTop;
  private String borderTopColor;

  private BorderStyle borderBottom;
  private String borderBottomColor;

  public XStyle() {
    super();
  }

  public XStyle copy() {
    return restore(serialize());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (members[i]) {
        case COLOR:
          setColor(value);
          break;
        case FONT:
          setFontRef(BeeUtils.toIntOrNull(value));
          break;

        case FORMAT:
          setFormat(value);
          break;

        case TEXT_ALIGN:
          setTextAlign(Codec.unpack(TextAlign.class, value));
          break;
        case VERTICAL_ALIGN:
          setVerticalAlign(Codec.unpack(VerticalAlign.class, value));
          break;

        case BORDER_BOTTOM:
          setBorderBottom(Codec.unpack(BorderStyle.class, value));
          break;
        case BORDER_BOTTOM_COLOR:
          setBorderBottomColor(value);
          break;

        case BORDER_LEFT:
          setBorderLeft(Codec.unpack(BorderStyle.class, value));
          break;
        case BORDER_LEFT_COLOR:
          setBorderLeftColor(value);
          break;

        case BORDER_RIGHT:
          setBorderRight(Codec.unpack(BorderStyle.class, value));
          break;
        case BORDER_RIGHT_COLOR:
          setBorderRightColor(value);
          break;

        case BORDER_TOP:
          setBorderTop(Codec.unpack(BorderStyle.class, value));
          break;
        case BORDER_TOP_COLOR:
          setBorderTopColor(value);
          break;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (getClass() != obj.getClass()) {
      return false;
    }

    XStyle other = (XStyle) obj;

    return Objects.equals(color, other.color)
        && Objects.equals(fontRef, other.fontRef)
        && Objects.equals(format, other.format)
        && textAlign == other.textAlign
        && verticalAlign == other.verticalAlign
        && borderLeft == other.borderLeft
        && Objects.equals(borderLeftColor, other.borderLeftColor)
        && borderRight == other.borderRight
        && Objects.equals(borderRightColor, other.borderRightColor)
        && borderTop == other.borderTop
        && Objects.equals(borderTopColor, other.borderTopColor)
        && borderBottom == other.borderBottom
        && Objects.equals(borderBottomColor, other.borderBottomColor);
  }

  public BorderStyle getBorderBottom() {
    return borderBottom;
  }

  public String getBorderBottomColor() {
    return borderBottomColor;
  }

  public BorderStyle getBorderLeft() {
    return borderLeft;
  }

  public String getBorderLeftColor() {
    return borderLeftColor;
  }

  public BorderStyle getBorderRight() {
    return borderRight;
  }

  public String getBorderRightColor() {
    return borderRightColor;
  }

  public BorderStyle getBorderTop() {
    return borderTop;
  }

  public String getBorderTopColor() {
    return borderTopColor;
  }

  public String getColor() {
    return color;
  }

  public String getFormat() {
    return format;
  }

  public TextAlign getTextAlign() {
    return textAlign;
  }

  public VerticalAlign getVerticalAlign() {
    return verticalAlign;
  }

  @Override
  public int hashCode() {
    return Objects.hash(color, fontRef, format, textAlign, verticalAlign,
        borderLeft, borderLeftColor, borderRight, borderRightColor,
        borderTop, borderTopColor, borderBottom, borderBottomColor);
  }

  public XStyle merge(XStyle other) {
    XStyle result = copy();
    if (other == null) {
      return result;
    }

    if (BeeUtils.isEmpty(getColor()) && !BeeUtils.isEmpty(other.getColor())) {
      result.setColor(other.getColor());
    }
    if (getFontRef() == null && other.getFontRef() != null) {
      result.setFontRef(other.getFontRef());
    }

    if (BeeUtils.isEmpty(getFormat()) && !BeeUtils.isEmpty(other.getFormat())) {
      result.setFormat(other.getFormat());
    }

    if (getTextAlign() == null && other.getTextAlign() != null) {
      result.setTextAlign(other.getTextAlign());
    }
    if (getVerticalAlign() == null && other.getVerticalAlign() != null) {
      result.setVerticalAlign(other.getVerticalAlign());
    }

    if (getBorderLeft() == null && other.getBorderLeft() != null) {
      result.setBorderLeft(other.getBorderLeft());
    }
    if (BeeUtils.isEmpty(getBorderLeftColor()) && !BeeUtils.isEmpty(other.getBorderLeftColor())) {
      result.setBorderLeftColor(other.getBorderLeftColor());
    }

    if (getBorderRight() == null && other.getBorderRight() != null) {
      result.setBorderRight(other.getBorderRight());
    }
    if (BeeUtils.isEmpty(getBorderRightColor()) && !BeeUtils.isEmpty(other.getBorderRightColor())) {
      result.setBorderRightColor(other.getBorderRightColor());
    }

    if (getBorderTop() == null && other.getBorderTop() != null) {
      result.setBorderTop(other.getBorderTop());
    }
    if (BeeUtils.isEmpty(getBorderTopColor()) && !BeeUtils.isEmpty(other.getBorderTopColor())) {
      result.setBorderTopColor(other.getBorderTopColor());
    }

    if (getBorderBottom() == null && other.getBorderBottom() != null) {
      result.setBorderBottom(other.getBorderBottom());
    }
    if (BeeUtils.isEmpty(getBorderBottomColor())
        && !BeeUtils.isEmpty(other.getBorderBottomColor())) {
      result.setBorderBottomColor(other.getBorderBottomColor());
    }

    return result;
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    for (Serial member : Serial.values()) {
      switch (member) {
        case COLOR:
          values.add(getColor());
          break;
        case FONT:
          values.add((getFontRef() == null) ? null : BeeUtils.toString(getFontRef()));
          break;

        case FORMAT:
          values.add(getFormat());
          break;

        case TEXT_ALIGN:
          values.add(Codec.pack(getTextAlign()));
          break;
        case VERTICAL_ALIGN:
          values.add(Codec.pack(getVerticalAlign()));
          break;

        case BORDER_BOTTOM:
          values.add(Codec.pack(getBorderBottom()));
          break;
        case BORDER_BOTTOM_COLOR:
          values.add(getBorderBottomColor());
          break;

        case BORDER_LEFT:
          values.add(Codec.pack(getBorderLeft()));
          break;
        case BORDER_LEFT_COLOR:
          values.add(getBorderLeftColor());
          break;

        case BORDER_RIGHT:
          values.add(Codec.pack(getBorderRight()));
          break;
        case BORDER_RIGHT_COLOR:
          values.add(getBorderRightColor());
          break;

        case BORDER_TOP:
          values.add(Codec.pack(getBorderTop()));
          break;
        case BORDER_TOP_COLOR:
          values.add(getBorderTopColor());
          break;
      }
    }

    return Codec.beeSerialize(values);
  }

  public void setBorderBottom(BorderStyle borderBottom) {
    this.borderBottom = borderBottom;
  }

  public void setBorderBottomColor(String borderBottomColor) {
    this.borderBottomColor = borderBottomColor;
  }

  public void setBorderLeft(BorderStyle borderLeft) {
    this.borderLeft = borderLeft;
  }

  public void setBorderLeftColor(String borderLeftColor) {
    this.borderLeftColor = borderLeftColor;
  }

  public void setBorderRight(BorderStyle borderRight) {
    this.borderRight = borderRight;
  }

  public void setBorderRightColor(String borderRightColor) {
    this.borderRightColor = borderRightColor;
  }

  public void setBorderTop(BorderStyle borderTop) {
    this.borderTop = borderTop;
  }

  public void setBorderTopColor(String borderTopColor) {
    this.borderTopColor = borderTopColor;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setTextAlign(TextAlign textAlign) {
    this.textAlign = textAlign;
  }

  public void setVerticalAlign(VerticalAlign verticalAlign) {
    this.verticalAlign = verticalAlign;
  }

  public Integer getFontRef() {
    return fontRef;
  }

  public void setFontRef(Integer fontRef) {
    this.fontRef = fontRef;
  }
}
