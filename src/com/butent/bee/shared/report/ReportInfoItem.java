package com.butent.bee.shared.report;

import com.butent.bee.client.output.ReportItem;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashMap;
import java.util.Map;

public final class ReportInfoItem implements BeeSerializable {

  private enum Serial {
    ITEM, RELATION, FUNCTION, COL_SUMMARY, GROUP_SUMMARY, ROW_SUMMARY, DESCENDING
  }

  private ReportItem item;
  private String relation;

  ReportFunction function;
  boolean colSummary;
  boolean groupSummary;
  boolean rowSummary;
  Boolean descending;

  ReportInfoItem(ReportItem item) {
    this.item = Assert.notNull(item);
  }

  ReportInfoItem() {
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeMap(data);

    if (!BeeUtils.isEmpty(map)) {
      for (Serial key : Serial.values()) {
        String value = map.get(key.name());

        switch (key) {
          case COL_SUMMARY:
            colSummary = BeeUtils.toBoolean(value);
            break;
          case DESCENDING:
            descending = BeeUtils.toBooleanOrNull(value);
            break;
          case FUNCTION:
            function = EnumUtils.getEnumByName(ReportFunction.class, value);
            break;
          case GROUP_SUMMARY:
            groupSummary = BeeUtils.toBoolean(value);
            break;
          case ITEM:
            item = ReportItem.restore(value);
            break;
          case RELATION:
            setRelation(value);
            break;
          case ROW_SUMMARY:
            rowSummary = BeeUtils.toBoolean(value);
            break;
          default:
            break;
        }
      }
    }
  }

  public boolean getDescending() {
    return BeeUtils.unbox(descending);
  }

  public ReportFunction getFunction() {
    return function;
  }

  public ReportItem getItem() {
    return item;
  }

  public String getRelation() {
    return relation;
  }

  public String getStyle() {
    if (getFunction() != null) {
      switch (getFunction()) {
        case LIST:
          return ReportItem.STYLE_TEXT;

        case COUNT:
        case SUM:
          return ReportItem.STYLE_NUM;

        default:
          break;
      }
    }
    return getItem().getStyle();
  }

  public boolean isColSummary() {
    return colSummary;
  }

  public boolean isGroupSummary() {
    return groupSummary;
  }

  public boolean isRowSummary() {
    return rowSummary;
  }

  public boolean isSorted() {
    return descending != null;
  }

  @Override
  public String serialize() {
    Map<String, Object> map = new HashMap<>();

    for (Serial key : Serial.values()) {
      Object value = null;

      switch (key) {
        case COL_SUMMARY:
          value = isColSummary();
          break;
        case DESCENDING:
          value = getDescending();
          break;
        case FUNCTION:
          value = getFunction();
          break;
        case GROUP_SUMMARY:
          value = isGroupSummary();
          break;
        case ITEM:
          value = getItem();
          break;
        case RELATION:
          value = getRelation();
          break;
        case ROW_SUMMARY:
          value = isRowSummary();
          break;
      }
      map.put(key.name(), value);
    }
    return Codec.beeSerialize(map);
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }
}