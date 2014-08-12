package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class XRow implements BeeSerializable {

  private enum Serial {
    INDEX, STYLE, HEIGHT_FACTOR, CELLS
  }

  public static XRow restore(String s) {
    Assert.notEmpty(s);
    XRow row = new XRow();
    row.deserialize(s);
    return row;
  }

  private int index;

  private Integer styleRef;

  private Double heightFactor;

  private final List<XCell> cells = new ArrayList<>();

  public XRow(int index) {
    this.index = index;
  }

  private XRow() {
    super();
  }

  public void add(XCell cell) {
    Assert.notNull(cell);
    cells.add(cell);
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
        case CELLS:
          if (!cells.isEmpty()) {
            cells.clear();
          }

          String[] carr = Codec.beeDeserializeCollection(value);
          if (carr != null) {
            for (String cv : carr) {
              add(XCell.restore(cv));
            }
          }
          break;

        case HEIGHT_FACTOR:
          setHeightFactor(BeeUtils.toDoubleOrNull(value));
          break;
        case INDEX:
          setIndex(BeeUtils.toInt(value));
          break;
        case STYLE:
          setStyleRef(BeeUtils.toIntOrNull(value));
          break;
      }
    }
  }

  public List<XCell> getCells() {
    return cells;
  }

  public Double getHeightFactor() {
    return heightFactor;
  }

  public int getIndex() {
    return index;
  }

  public int getMaxColumn() {
    int result = BeeConst.UNDEF;
    for (XCell cell : cells) {
      result = Math.max(result, cell.getIndex());
    }
    return result;
  }

  public Integer getStyleRef() {
    return styleRef;
  }

  public boolean hasPicture(int column) {
    for (XCell cell : cells) {
      if (cell.getIndex() == column) {
        return cell.getPictureRef() != null;
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return cells.isEmpty();
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    for (Serial member : Serial.values()) {
      switch (member) {
        case CELLS:
          values.add(cells.isEmpty() ? null : Codec.beeSerialize(cells));
          break;
        case HEIGHT_FACTOR:
          values.add(BeeUtils.isDouble(getHeightFactor())
              ? BeeUtils.toString(getHeightFactor()) : null);
          break;
        case INDEX:
          values.add(BeeUtils.toString(getIndex()));
          break;
        case STYLE:
          values.add((getStyleRef() == null) ? null : BeeUtils.toString(getStyleRef()));
          break;
      }
    }

    return Codec.beeSerialize(values);
  }

  public void setHeightFactor(Double heightFactor) {
    this.heightFactor = heightFactor;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public void setStyleRef(Integer styleRef) {
    this.styleRef = styleRef;
  }

  public void shift(int by) {
    setIndex(getIndex() + by);
  }
}
