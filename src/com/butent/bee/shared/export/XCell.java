package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class XCell implements BeeSerializable {

  private enum Serial {
    INDEX, VALUE, FORMULA, STYLE, PICTURE_REF, PICTURE_LAYOUT, COL_SPAN, ROW_SPAN
  }
  
  public static XCell forPicture(int index, int pictureRef) {
    XCell cell = new XCell();
    
    cell.setIndex(index);
    cell.setPictureRef(pictureRef);

    return cell;
  }

  public static XCell forStyle(int index, int styleRef) {
    XCell cell = new XCell();
    
    cell.setIndex(index);
    cell.setStyleRef(styleRef);
    
    return cell;
  }

  public static XCell restore(String s) {
    Assert.notEmpty(s);
    XCell cell = new XCell();
    cell.deserialize(s);
    return cell;
  }

  private int index;

  private Value value;
  private String formula;

  private Integer styleRef;

  private Integer pictureRef;
  private XPicture.Layout pictureLayout;

  private int colSpan;
  private int rowSpan;

  public XCell(int index, Double v) {
    this(index, new NumberValue(v));
  }

  public XCell(int index, Double v, Integer styleRef) {
    this(index, new NumberValue(v), styleRef);
  }

  public XCell(int index, Integer v) {
    this(index, new IntegerValue(v));
  }

  public XCell(int index, Integer v, Integer styleRef) {
    this(index, new IntegerValue(v), styleRef);
  }

  public XCell(int index, Long v) {
    this(index, new LongValue(v));
  }

  public XCell(int index, Long v, Integer styleRef) {
    this(index, new LongValue(v), styleRef);
  }

  public XCell(int index, String v) {
    this(index, new TextValue(v));
  }

  public XCell(int index, String v, Integer styleRef) {
    this(index, new TextValue(v), styleRef);
  }

  public XCell(int index) {
    this.index = index;
  }
  
  public XCell(int index, Value value) {
    this.index = index;
    this.value = value;
  }

  public XCell(int index, Value value, Integer styleRef) {
    this(index, value);
    this.styleRef = styleRef;
  }

  private XCell() {
    super();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String v = arr[i];
      if (BeeUtils.isEmpty(v)) {
        continue;
      }

      switch (members[i]) {
        case COL_SPAN:
          setColSpan(BeeUtils.toInt(v));
          break;
        case FORMULA:
          setFormula(v);
          break;
        case INDEX:
          setIndex(BeeUtils.toInt(v));
          break;
        case PICTURE_REF:
          setPictureRef(BeeUtils.toIntOrNull(v));
          break;
        case PICTURE_LAYOUT:
          setPictureLayout(Codec.unpack(XPicture.Layout.class, v));
          break;
        case ROW_SPAN:
          setRowSpan(BeeUtils.toInt(v));
          break;
        case STYLE:
          setStyleRef(BeeUtils.toIntOrNull(v));
          break;
        case VALUE:
          setValue(Value.restore(Codec.decodeBase64(v)));
          break;
      }
    }
  }

  public int getColSpan() {
    return colSpan;
  }

  public String getFormula() {
    return formula;
  }

  public int getIndex() {
    return index;
  }

  public XPicture.Layout getPictureLayout() {
    return pictureLayout;
  }

  public Integer getPictureRef() {
    return pictureRef;
  }

  public int getRowSpan() {
    return rowSpan;
  }

  public Integer getStyleRef() {
    return styleRef;
  }

  public Value getValue() {
    return value;
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    for (Serial member : Serial.values()) {
      switch (member) {
        case COL_SPAN:
          values.add(BeeUtils.toString(getColSpan()));
          break;
        case FORMULA:
          values.add(getFormula());
          break;
        case INDEX:
          values.add(BeeUtils.toString(getIndex()));
          break;
        case PICTURE_REF:
          values.add((getPictureRef() == null) ? null : BeeUtils.toString(getPictureRef()));
          break;
        case PICTURE_LAYOUT:
          values.add(Codec.pack(getPictureLayout()));
          break;
        case ROW_SPAN:
          values.add(BeeUtils.toString(getRowSpan()));
          break;
        case STYLE:
          values.add((getStyleRef() == null) ? null : BeeUtils.toString(getStyleRef()));
          break;
        case VALUE:
          values.add((getValue() == null) ? null : Codec.encodeBase64(getValue().serialize()));
          break;
      }
    }

    return Codec.beeSerialize(values);
  }

  public void setColSpan(int colSpan) {
    this.colSpan = colSpan;
  }

  public void setFormula(String formula) {
    this.formula = formula;
  }

  public void setPictureLayout(XPicture.Layout pictureLayout) {
    this.pictureLayout = pictureLayout;
  }

  public void setPictureRef(Integer pictureRef) {
    this.pictureRef = pictureRef;
  }

  public void setRowSpan(int rowSpan) {
    this.rowSpan = rowSpan;
  }

  public void setStyleRef(Integer styleRef) {
    this.styleRef = styleRef;
  }

  public void setValue(Value value) {
    this.value = value;
  }

  private void setIndex(int index) {
    this.index = index;
  }
}
