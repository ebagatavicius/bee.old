package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class ColumnInFilter extends Filter {

  private enum Serial {
    COLUMN, IN_VIEW, IN_COLUMN, IN_FILTER
  }

  private String column;

  private String inView;
  private String inColumn;

  private Filter inFilter;

  protected ColumnInFilter() {
    super();
  }

  protected ColumnInFilter(String column, String inView, String inColumn) {
    this(column, inView, inColumn, null);
  }

  protected ColumnInFilter(String column, String inView, String inColumn, Filter inFilter) {
    super();
    this.column = column;
    this.inView = inView;
    this.inColumn = inColumn;
    this.inFilter = inFilter;
  }

  @Override
  public void deserialize(String s) {
    setSafe();

    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case COLUMN:
          column = value;
          break;
        case IN_VIEW:
          inView = value;
          break;
        case IN_COLUMN:
          inColumn = value;
          break;
        case IN_FILTER:
          inFilter = Filter.restore(value);
          break;
      }
    }
  }

  public String getColumn() {
    return column;
  }

  public String getInColumn() {
    return inColumn;
  }

  public Filter getInFilter() {
    return inFilter;
  }

  public String getInView() {
    return inView;
  }

  @Override
  public boolean involvesColumn(String colName) {
    return BeeUtils.same(colName, column);
  }

  @Override
  public boolean isMatch(List<? extends IsColumn> columns, IsRow row) {
    Assert.unsupported();
    return false;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case COLUMN:
          arr[i++] = column;
          break;
        case IN_VIEW:
          arr[i++] = inView;
          break;
        case IN_COLUMN:
          arr[i++] = inColumn;
          break;
        case IN_FILTER:
          arr[i++] = inFilter;
          break;
      }
    }
    return super.serialize(arr);
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(column, "IN", inView, inColumn, inFilter);
  }
}
