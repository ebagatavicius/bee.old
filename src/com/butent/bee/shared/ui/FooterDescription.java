package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;

public class FooterDescription implements BeeSerializable, HasInfo, HasOptions {

  private enum Serial {
    AGGREGATE, EXPRESSION, EVALUATOR, TYPE, TEXT, HTML,
    FORMAT, HOR_ALIGN, WHITE_SPACE, SCALE, OPTIONS
  }

  private static final String ATTR_AGGREGATE = "aggregate";
  private static final String ATTR_EXPRESSION = "expression";
  private static final String ATTR_EVALUATOR = "evaluator";
  private static final String ATTR_TYPE = "type";

  public static FooterDescription restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    FooterDescription footerDescription = new FooterDescription();
    footerDescription.deserialize(s);
    return footerDescription;
  }

  private String aggregate;
  private String expression;
  private String evaluator;
  private String type;

  private String text;
  private String html;

  private String format;
  private String horAlign;
  private String whiteSpace;

  private Integer scale;

  private String options;

  public FooterDescription(Map<String, String> attributes) {
    setAttributes(attributes);
  }

  private FooterDescription() {
  }

  public FooterDescription copy() {
    return restore(serialize());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (member) {
        case AGGREGATE:
          setAggregate(value);
          break;
        case EXPRESSION:
          setExpression(value);
          break;
        case EVALUATOR:
          setEvaluator(value);
          break;
        case TYPE:
          setType(value);
          break;
        case TEXT:
          setText(value);
          break;
        case HTML:
          setHtml(value);
          break;
        case FORMAT:
          setFormat(value);
          break;
        case HOR_ALIGN:
          setHorAlign(value);
          break;
        case WHITE_SPACE:
          setWhiteSpace(value);
          break;
        case SCALE:
          setScale(BeeUtils.toIntOrNull(value));
          break;
        case OPTIONS:
          setOptions(value);
          break;
      }
    }
  }

  public String getAggregate() {
    return aggregate;
  }

  public String getEvaluator() {
    return evaluator;
  }

  public String getExpression() {
    return expression;
  }

  public String getFormat() {
    return format;
  }

  public String getHorAlign() {
    return horAlign;
  }

  public String getHtml() {
    return html;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(
        "Aggregate", getAggregate(),
        "Expression", getExpression(),
        "Evaluator", getEvaluator(),
        "Type", getType(),
        "Text", getText(),
        "Html", getHtml(),
        "Format", getFormat(),
        "Horizontal Alignment", getHorAlign(),
        "White Space", getWhiteSpace(),
        "Scale", getScale(),
        "Options", getOptions());

    PropertyUtils.addWhenEmpty(info, getClass());
    return info;
  }

  @Override
  public String getOptions() {
    return options;
  }

  public Integer getScale() {
    return scale;
  }

  public String getText() {
    return text;
  }

  public String getType() {
    return type;
  }

  public String getWhiteSpace() {
    return whiteSpace;
  }

  public void replaceColumn(String oldId, String newId) {
    if (BeeUtils.containsSame(getExpression(), oldId)) {
      setExpression(Calculation.renameColumn(getExpression(), oldId, newId));
    }
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case AGGREGATE:
          arr[i++] = getAggregate();
          break;
        case EXPRESSION:
          arr[i++] = getExpression();
          break;
        case EVALUATOR:
          arr[i++] = getEvaluator();
          break;
        case TYPE:
          arr[i++] = getType();
          break;
        case TEXT:
          arr[i++] = getText();
          break;
        case HTML:
          arr[i++] = getHtml();
          break;
        case FORMAT:
          arr[i++] = getFormat();
          break;
        case HOR_ALIGN:
          arr[i++] = getHorAlign();
          break;
        case WHITE_SPACE:
          arr[i++] = getWhiteSpace();
          break;
        case SCALE:
          arr[i++] = getScale();
          break;
        case OPTIONS:
          arr[i++] = getOptions();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAggregate(String aggregate) {
    this.aggregate = aggregate;
  }

  public void setAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      String value = BeeUtils.trim(attribute.getValue());
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (BeeUtils.same(key, ATTR_AGGREGATE)) {
        setAggregate(value);
      } else if (BeeUtils.same(key, ATTR_EXPRESSION)) {
        setExpression(value);
      } else if (BeeUtils.same(key, ATTR_EVALUATOR)) {
        setEvaluator(value);
      } else if (BeeUtils.same(key, ATTR_TYPE)) {
        setType(value);

      } else if (BeeUtils.same(key, UiConstants.ATTR_TEXT)) {
        setText(value);
      } else if (BeeUtils.same(key, UiConstants.ATTR_HTML)) {
        setHtml(value);

      } else if (BeeUtils.same(key, UiConstants.ATTR_FORMAT)) {
        setFormat(value);
      } else if (BeeUtils.same(key, UiConstants.ATTR_HORIZONTAL_ALIGNMENT)) {
        setHorAlign(value);
      } else if (BeeUtils.same(key, UiConstants.ATTR_SCALE)) {
        setScale(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_OPTIONS)) {
        setOptions(value);
      }
    }
  }

  public void setEvaluator(String evaluator) {
    this.evaluator = evaluator;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setHorAlign(String horAlign) {
    this.horAlign = horAlign;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setWhiteSpace(String whiteSpace) {
    this.whiteSpace = whiteSpace;
  }
}
