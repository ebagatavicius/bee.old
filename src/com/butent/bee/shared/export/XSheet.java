package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XSheet implements BeeSerializable {

  private enum Serial {
    NAME, ROW_HEIGHT_FACTOR, ROWS, FONTS, STYLES, PICTURES, AUTO_SIZE, COLUMN_WIDTH_FACTORS
  }

  private static final double H1_FONT_FACTOR = 1.4;
  private static final double HX_FONT_FACTOR = 1.2;

  public static XSheet restore(String s) {
    Assert.notEmpty(s);
    XSheet sheet = new XSheet();
    sheet.deserialize(s);
    return sheet;
  }

  private String name;

  private Double rowHeightFactor;

  private final List<XRow> rows = new ArrayList<>();

  private final List<XFont> fonts = new ArrayList<>();
  private final List<XStyle> styles = new ArrayList<>();

  private final List<XPicture> pictures = new ArrayList<>();

  private final List<Integer> autoSize = new ArrayList<>();
  private final Map<Integer, Double> columnWidthFactors = new HashMap<>();

  public XSheet() {
    super();
  }

  public XSheet(String name) {
    this();
    this.name = name;
  }

  public void add(XRow row) {
    Assert.notNull(row);
    rows.add(row);
  }

  public void addHeaders(List<String> headers) {
    if (BeeUtils.isEmpty(headers)) {
      return;
    }

    XStyle s1 = XStyle.center();

    XFont f1 = XFont.bold();
    f1.setFactor(H1_FONT_FACTOR);
    s1.setFontRef(registerFont(f1));

    XStyle sx;
    if (headers.size() > 1) {
      sx = XStyle.center();

      XFont fx = XFont.bold();
      fx.setFactor(HX_FONT_FACTOR);
      sx.setFontRef(registerFont(fx));

    } else {
      sx = null;
    }

    addHeaders(headers, s1, sx, 0, getMaxColumn() + 1);
  }

  public void addHeaders(List<String> headers, XStyle s1, XStyle sx, int column, int colSpan) {
    if (BeeUtils.isEmpty(headers)) {
      return;
    }

    for (XRow row : rows) {
      row.shift(headers.size() + 2);
    }

    Integer cs1 = (s1 == null) ? null : registerStyle(s1);
    Integer csx = (sx == null) ? cs1 : registerStyle(sx);

    int rowIndex = 1;

    for (String header : headers) {
      if (!BeeUtils.isEmpty(header)) {
        XRow row = new XRow(rowIndex);
        XCell cell = new XCell(column, header);

        Integer styleRef = (rowIndex == 1) ? cs1 : csx;
        if (styleRef != null) {
          cell.setStyleRef(styleRef);
        }

        if (colSpan > 1) {
          cell.setColSpan(colSpan);
        }

        row.add(cell);
        add(row);
      }
      rowIndex++;
    }
  }

  public void autoSizeAll() {
    if (!autoSize.isEmpty()) {
      autoSize.clear();
    }

    int maxColumn = getMaxColumn();
    for (int i = 0; i <= maxColumn; i++) {
      autoSize.add(i);
    }
  }

  public void autoSizeColumn(int index) {
    if (!autoSize.contains(index)) {
      autoSize.add(index);
    }
  }

  public void clear() {
    rows.clear();

    fonts.clear();
    styles.clear();

    autoSize.clear();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    if (!rows.isEmpty()) {
      rows.clear();
    }
    if (!fonts.isEmpty()) {
      fonts.clear();
    }
    if (!styles.isEmpty()) {
      styles.clear();
    }
    if (!pictures.isEmpty()) {
      pictures.clear();
    }
    if (!autoSize.isEmpty()) {
      autoSize.clear();
    }
    if (!columnWidthFactors.isEmpty()) {
      columnWidthFactors.clear();
    }

    for (int i = 0; i < members.length; i++) {
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (members[i]) {
        case COLUMN_WIDTH_FACTORS:
          Map<String, String> map = Codec.deserializeMap(value);

          for (Map.Entry<String, String> entry : map.entrySet()) {
            if (BeeUtils.isDigit(entry.getKey()) && BeeUtils.isPositiveDouble(entry.getValue())) {
              columnWidthFactors.put(BeeUtils.toInt(entry.getKey()),
                  BeeUtils.toDouble(entry.getValue()));
            }
          }
          break;

        case NAME:
          setName(value);
          break;

        case ROW_HEIGHT_FACTOR:
          setRowHeightFactor(BeeUtils.toDoubleOrNull(value));
          break;

        default:
          String[] items = Codec.beeDeserializeCollection(value);

          for (String item : items) {
            if (BeeUtils.isEmpty(item)) {
              continue;
            }

            switch (members[i]) {
              case AUTO_SIZE:
                if (BeeUtils.isDigit(item)) {
                  autoSize.add(BeeUtils.toInt(item));
                }
                break;

              case FONTS:
                fonts.add(XFont.restore(item));
                break;

              case PICTURES:
                pictures.add(XPicture.restore(item));
                break;

              case ROWS:
                add(XRow.restore(item));
                break;

              case STYLES:
                styles.add(XStyle.restore(item));
                break;

              case COLUMN_WIDTH_FACTORS:
              case NAME:
              case ROW_HEIGHT_FACTOR:
                break;
            }
          }
      }
    }
  }

  public XRow ensureRow(int index) {
    for (XRow row : rows) {
      if (row.getIndex() == index) {
        return row;
      }
    }

    XRow row = new XRow(index);
    add(row);

    return row;
  }

  public List<Integer> getAutoSize() {
    return autoSize;
  }

  public Map<Integer, Double> getColumnWidthFactors() {
    return columnWidthFactors;
  }

  public XFont getFont(int index) {
    return fonts.get(index);
  }

  public List<XFont> getFonts() {
    return fonts;
  }

  public int getMaxColumn() {
    int result = BeeConst.UNDEF;
    for (XRow row : rows) {
      result = Math.max(result, row.getMaxColumn());
    }
    return result;
  }

  public String getName() {
    return name;
  }

  public List<XPicture> getPictures() {
    return pictures;
  }

  public Double getRowHeightFactor() {
    return rowHeightFactor;
  }

  public List<XRow> getRows() {
    return rows;
  }

  public XStyle getStyle(int index) {
    return styles.get(index);
  }

  public List<XStyle> getStyles() {
    return styles;
  }

  public boolean hasPictures(int column) {
    if (pictures.isEmpty()) {
      return false;
    }

    for (XRow row : rows) {
      if (row.hasPicture(column)) {
        return true;
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return rows.isEmpty() && fonts.isEmpty() && styles.isEmpty() && autoSize.isEmpty();
  }

  public int registerFont(XFont font) {
    Assert.notNull(font);

    int index = fonts.indexOf(font);
    if (index >= 0) {
      return index;
    } else {
      fonts.add(font);
      return fonts.size() - 1;
    }
  }

  public int registerPicture(XPicture picture) {
    Assert.notNull(picture);

    int index = pictures.indexOf(picture);
    if (index >= 0) {
      return index;
    } else {
      pictures.add(picture);
      return pictures.size() - 1;
    }
  }

  public int registerStyle(XStyle style) {
    Assert.notNull(style);

    int index = styles.indexOf(style);
    if (index >= 0) {
      return index;
    } else {
      styles.add(style);
      return styles.size() - 1;
    }
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    for (Serial member : Serial.values()) {
      switch (member) {
        case AUTO_SIZE:
          values.add(autoSize.isEmpty() ? null : Codec.beeSerialize(autoSize));
          break;
        case COLUMN_WIDTH_FACTORS:
          values.add(columnWidthFactors.isEmpty() ? null : Codec.beeSerialize(columnWidthFactors));
          break;
        case FONTS:
          values.add(fonts.isEmpty() ? null : Codec.beeSerialize(fonts));
          break;
        case NAME:
          values.add(getName());
          break;
        case PICTURES:
          values.add(pictures.isEmpty() ? null : Codec.beeSerialize(pictures));
          break;
        case ROW_HEIGHT_FACTOR:
          values.add((getRowHeightFactor() == null)
              ? null : BeeUtils.toString(getRowHeightFactor()));
          break;
        case ROWS:
          values.add(rows.isEmpty() ? null : Codec.beeSerialize(rows));
          break;
        case STYLES:
          values.add(styles.isEmpty() ? null : Codec.beeSerialize(styles));
          break;
      }
    }

    return Codec.beeSerialize(values);
  }

  public void setColumnWidthFactor(int columnIndex, Double widthFactor) {
    if (BeeUtils.isPositive(widthFactor)) {
      columnWidthFactors.put(columnIndex, widthFactor);
    } else {
      columnWidthFactors.remove(columnIndex);
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setRowHeightFactor(Double rowHeightFactor) {
    this.rowHeightFactor = rowHeightFactor;
  }
}
