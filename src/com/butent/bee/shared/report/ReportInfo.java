package com.butent.bee.shared.report;

import com.google.gwt.core.shared.GwtIncompatible;

import com.butent.bee.client.output.ReportBooleanItem;
import com.butent.bee.client.output.ReportDateItem;
import com.butent.bee.client.output.ReportEnumItem;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.client.output.ReportNumericItem;
import com.butent.bee.client.output.ReportTextItem;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ReportInfo implements BeeSerializable {

  private enum Serial {
    CAPTION, ROW_ITEMS, COL_ITEMS, FILTER_ITEMS, ROW_GROUPING, COL_GROUPING
  }

  private String caption;
  private Long id;

  private final List<ReportInfoItem> colItems = new ArrayList<>();
  private final List<ReportItem> filterItems = new ArrayList<>();
  private final List<ReportInfoItem> rowItems = new ArrayList<>();
  private ReportInfoItem colGrouping;
  private ReportInfoItem rowGrouping;

  public ReportInfo(String caption) {
    setCaption(caption);
  }

  private ReportInfo() {
  }

  public void addColItem(ReportItem colItem) {
    colItems.add(new ReportInfoItem(colItem));
    int idx = colItems.size() - 1;

    if (colItem instanceof ReportNumericItem) {
      setFunction(idx, ReportFunction.SUM);
      setColSummary(idx, true);
      setGroupSummary(idx, true);
      setRowSummary(idx, true);
    } else {
      setFunction(idx, ReportFunction.LIST);
    }
  }

  public void addRowItem(ReportItem rowItem) {
    rowItems.add(new ReportInfoItem(rowItem));
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeMap(data);

    if (!BeeUtils.isEmpty(map)) {
      for (Serial key : Serial.values()) {
        String value = map.get(key.name());

        switch (key) {
          case CAPTION:
            if (BeeUtils.isEmpty(getCaption())) {
              setCaption(value);
            }
            break;
          case COL_GROUPING:
            ReportInfoItem groupItem = null;

            if (!BeeUtils.isEmpty(value)) {
              groupItem = new ReportInfoItem();
              groupItem.deserialize(value);
            }
            colGrouping = groupItem;
            break;
          case COL_ITEMS:
            colItems.clear();
            String[] items = Codec.beeDeserializeCollection(value);

            if (!ArrayUtils.isEmpty(items)) {
              for (String item : items) {
                ReportInfoItem infoItem = new ReportInfoItem();
                infoItem.deserialize(item);
                colItems.add(infoItem);
              }
            }
            break;
          case FILTER_ITEMS:
            filterItems.clear();
            items = Codec.beeDeserializeCollection(value);

            if (!ArrayUtils.isEmpty(items)) {
              for (String item : items) {
                filterItems.add(ReportItem.restore(item));
              }
            }
            break;
          case ROW_GROUPING:
            groupItem = null;

            if (!BeeUtils.isEmpty(value)) {
              groupItem = new ReportInfoItem();
              groupItem.deserialize(value);
            }
            rowGrouping = groupItem;
            break;
          case ROW_ITEMS:
            rowItems.clear();
            items = Codec.beeDeserializeCollection(value);

            if (!ArrayUtils.isEmpty(items)) {
              for (String item : items) {
                ReportInfoItem infoItem = new ReportInfoItem();
                infoItem.deserialize(item);
                rowItems.add(infoItem);
              }
            }
            break;
        }
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ReportInfo)) {
      return false;
    }
    return Objects.equals(caption, ((ReportInfo) obj).caption);
  }

  public String getCaption() {
    return caption;
  }

  public ReportInfoItem getColGrouping() {
    return colGrouping;
  }

  public List<ReportInfoItem> getColItems() {
    return colItems;
  }

  @GwtIncompatible
  public IsCondition getCondition(String table, String field) {
    return getCondition(SqlUtils.field(table, field), field);
  }

  @GwtIncompatible
  public IsCondition getCondition(IsExpression expr, String field) {
    HasConditions and = SqlUtils.and();

    for (ReportItem filterItem : getFilterItems()) {
      if (BeeUtils.same(filterItem.getExpression(), field)) {
        if (filterItem instanceof ReportTextItem) {
          List<String> options = ((ReportTextItem) filterItem).getFilter();

          if (!BeeUtils.isEmpty(options)) {
            HasConditions or = SqlUtils.or();

            for (String opt : ((ReportTextItem) filterItem).getFilter()) {
              or.add(SqlUtils.contains(expr, opt));
            }
            and.add(or);
          }
        } else if (filterItem instanceof ReportEnumItem) {
          Set<Integer> options = ((ReportEnumItem) filterItem).getFilter();

          if (!BeeUtils.isEmpty(options)) {
            and.add(SqlUtils.inList(expr, options));
          }
        } else if (filterItem instanceof ReportBooleanItem) {
          Boolean ok = ((ReportBooleanItem) filterItem).getFilter();

          if (ok != null) {
            and.add(ok ? SqlUtils.notNull(expr) : SqlUtils.isNull(expr));
          }
        } else if (filterItem instanceof ReportDateItem) {
          Long value = ((ReportDateItem) filterItem).getFilter();

          if (value != null) {
            Long dt = null;
            Operator op = ((ReportDateItem) filterItem).getFilterOperator();

            switch (((ReportDateItem) filterItem).getFormat()) {
              case DATE:
                dt = new JustDate(value.intValue()).getTime();
                break;
              case DATETIME:
                dt = value;
                break;
              case YEAR:
                switch (op) {
                  case EQ:
                    and.add(SqlUtils.compare(expr, Operator.GE,
                        SqlUtils.constant(TimeUtils.startOfYear(value.intValue()).getTime())));

                    dt = TimeUtils.startOfYear(value.intValue() + 1).getTime();
                    op = Operator.LT;
                    break;
                  case GE:
                    dt = TimeUtils.startOfYear(value.intValue()).getTime();
                    break;
                  case GT:
                    dt = TimeUtils.startOfYear(value.intValue() + 1).getTime();
                    op = Operator.GE;
                    break;
                  case LE:
                    dt = TimeUtils.startOfYear(value.intValue() + 1).getTime();
                    op = Operator.LT;
                    break;
                  case LT:
                    dt = TimeUtils.startOfYear(value.intValue()).getTime();
                    break;
                  default:
                    continue;
                }
                break;
              default:
                continue;
            }
            and.add(SqlUtils.compare(expr, op, SqlUtils.constant(dt)));
          }
        }
      }
    }
    return and.isEmpty() ? null : and;
  }

  public List<ReportItem> getFilterItems() {
    return filterItems;
  }

  public Long getId() {
    return id;
  }

  public ReportInfoItem getRowGrouping() {
    return rowGrouping;
  }

  public List<ReportInfoItem> getRowItems() {
    return rowItems;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(caption);
  }

  public boolean isEmpty() {
    return BeeUtils.isEmpty(getRowItems()) && BeeUtils.isEmpty(getColItems());
  }

  public boolean requiresField(String field) {
    for (ReportInfoItem infoItem : getRowItems()) {
      for (ReportItem item : infoItem.getItem().getMembers()) {
        if (BeeUtils.same(item.getExpression(), field)) {
          return true;
        }
      }
    }
    for (ReportInfoItem infoItem : getColItems()) {
      for (ReportItem item : infoItem.getItem().getMembers()) {
        if (BeeUtils.same(item.getExpression(), field)) {
          return true;
        }
      }
    }
    if (getRowGrouping() != null) {
      for (ReportItem item : getRowGrouping().getItem().getMembers()) {
        if (BeeUtils.same(item.getExpression(), field)) {
          return true;
        }
      }
    }
    if (getColGrouping() != null) {
      for (ReportItem item : getColGrouping().getItem().getMembers()) {
        if (BeeUtils.same(item.getExpression(), field)) {
          return true;
        }
      }
    }
    for (ReportItem filterItem : getFilterItems()) {
      for (ReportItem item : filterItem.getMembers()) {
        if (BeeUtils.same(item.getExpression(), field)
            && item.getFilter() != null
            && item instanceof ReportDateItem
            && !EnumUtils.in(((ReportDateItem) item).getFormat(), DateTimeFunction.YEAR,
                DateTimeFunction.DATE, DateTimeFunction.DATETIME)) {
          return true;
        }
      }
    }
    return false;
  }

  public static ReportInfo restore(String data) {
    ReportInfo reportInfo = new ReportInfo();
    reportInfo.deserialize(Assert.notEmpty(data));
    return reportInfo;
  }

  @Override
  public String serialize() {
    Map<String, Object> map = new HashMap<>();

    for (Serial key : Serial.values()) {
      Object value = null;

      switch (key) {
        case CAPTION:
          value = getCaption();
          break;
        case COL_GROUPING:
          value = getColGrouping();
          break;
        case COL_ITEMS:
          value = getColItems();
          break;
        case FILTER_ITEMS:
          value = getFilterItems();
          break;
        case ROW_GROUPING:
          value = getRowGrouping();
          break;
        case ROW_ITEMS:
          value = getRowItems();
          break;
      }
      map.put(key.name(), value);
    }
    return Codec.beeSerialize(map);
  }

  public void setColGrouping(ReportItem groupItem) {
    ReportInfoItem infoItem = null;

    if (groupItem != null) {
      infoItem = new ReportInfoItem(groupItem);

    }
    colGrouping = infoItem;
  }

  public void setColSummary(int colIndex, boolean summary) {
    ReportInfoItem item = BeeUtils.getQuietly(getColItems(), colIndex);

    if (item != null) {
      item.colSummary = summary;
    }
  }

  public void setFunction(int colIndex, ReportFunction function) {
    ReportInfoItem item = BeeUtils.getQuietly(getColItems(), colIndex);

    if (item != null) {
      item.function = Assert.notNull(function);
    }
  }

  public void setGroupSummary(int colIndex, boolean summary) {
    ReportInfoItem item = BeeUtils.getQuietly(getColItems(), colIndex);

    if (item != null) {
      item.groupSummary = summary;
    }
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setRowGrouping(ReportItem groupItem) {
    ReportInfoItem infoItem = null;

    if (groupItem != null) {
      infoItem = new ReportInfoItem(groupItem);
    }
    rowGrouping = infoItem;
  }

  public void setRowSummary(int colIndex, boolean summary) {
    ReportInfoItem item = BeeUtils.getQuietly(getColItems(), colIndex);

    if (item != null) {
      item.rowSummary = summary;
    }
  }

  private void setCaption(String caption) {
    this.caption = Assert.notEmpty(caption);
  }
}
