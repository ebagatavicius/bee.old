package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class ColumnInFilter extends Filter implements HasOptions {

  private enum Serial {
    COLUMN, IN_VIEW, IN_COLUMN, IN_FILTER, OPTIONS
  }

  public static final String ID_TAG = "_ID_";
  public static final String OPTION_FROM = "_FROM_";

  private String column;

  private String inView;
  private String inColumn;

  private Filter inFilter;

  private String options;

  protected ColumnInFilter() {
    super();
  }

  protected ColumnInFilter(String column, String inView, String inColumn, Filter inFilter) {
    this(column, inView, inColumn, inFilter, null);
  }

  protected ColumnInFilter(String column, String inView, String inColumn, Filter inFilter,
      String options) {

    super();

    this.column = column;
    this.inView = inView;
    this.inColumn = inColumn;
    this.inFilter = inFilter;
    this.options = options;
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
        case OPTIONS:
          options = value;
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
  public String getOptions() {
    return options;
  }

  @Override
  public boolean involvesColumn(String colName) {
    return BeeUtils.same(colName, column);
  }

  public boolean needsFrom() {
    return BeeUtils.containsSame(getOptions(), OPTION_FROM);
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
        case OPTIONS:
          arr[i++] = options;
          break;
      }
    }
    return super.serialize(arr);
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(column, "IN", inView, inColumn, inFilter, options);
  }
}
