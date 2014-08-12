package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class XWorkbook implements BeeSerializable {

  public static XWorkbook restore(String s) {
    Assert.notEmpty(s);
    XWorkbook workbook = new XWorkbook();
    workbook.deserialize(s);
    return workbook;
  }

  private String name;

  private final List<XSheet> sheets = new ArrayList<>();

  public XWorkbook() {
    super();
  }

  public XWorkbook(String name) {
    this();
    this.name = name;
  }

  public void add(XSheet sheet) {
    Assert.notNull(sheet);
    sheets.add(sheet);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setName(arr[0]);

    if (!sheets.isEmpty()) {
      sheets.clear();
    }

    String[] sarr = Codec.beeDeserializeCollection(arr[1]);
    if (sarr != null) {
      for (String v : sarr) {
        add(XSheet.restore(v));
      }
    }
  }

  public String getName() {
    return name;
  }

  public List<XSheet> getSheets() {
    return sheets;
  }

  public boolean isEmpty() {
    return sheets.isEmpty();
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    values.add(getName());
    values.add(sheets.isEmpty() ? null : Codec.beeSerialize(sheets));

    return Codec.beeSerialize(values);
  }

  public void setName(String name) {
    this.name = name;
  }
}
