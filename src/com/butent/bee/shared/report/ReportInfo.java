package com.butent.bee.shared.report;

import com.google.gwt.core.shared.GwtIncompatible;

import com.butent.bee.client.output.ReportBooleanItem;
import com.butent.bee.client.output.ReportDateItem;
import com.butent.bee.client.output.ReportEnumItem;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.client.output.ReportNumericItem;
import com.butent.bee.client.output.ReportTextItem;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class ReportInfo implements BeeSerializable {

  private enum Serial {
    CAPTION, ROW_ITEMS, COL_ITEMS, FILTER_ITEMS, ROW_GROUPING, COL_GROUPING
  }

  private String caption;
  private Long id;
  private boolean global;

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

  public int addColItem(ReportItem colItem) {
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
    return idx;
  }

  public int addRowItem(ReportItem rowItem) {
    rowItems.add(new ReportInfoItem(rowItem));
    return rowItems.size() - 1;
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeLinkedHashMap(data);

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
          boolean emptyValues = ((ReportTextItem) filterItem).isEmptyFilter();

          if (!BeeUtils.isEmpty(options) || emptyValues) {
            boolean isNegation = ((ReportTextItem) filterItem).isNegationFilter();
            HasConditions conditions = isNegation ? SqlUtils.and() : SqlUtils.or();

            if (!BeeUtils.isEmpty(options)) {
              for (String opt : options) {
                IsCondition condition = BeeUtils.isPrefix(opt, BeeConst.STRING_EQ)
                    ? SqlUtils.equals(expr, BeeUtils.removePrefix(opt, BeeConst.STRING_EQ))
                    : SqlUtils.contains(expr, opt);

                if (isNegation) {
                  condition = SqlUtils.not(condition);
                }
                conditions.add(condition);
              }
            }
            if (emptyValues) {
              conditions.add(isNegation ? SqlUtils.notNull(expr) : SqlUtils.isNull(expr));
            }
            and.add(conditions);
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
        } else if (filterItem instanceof ReportDateItem
            && Objects.nonNull(((ReportDateItem) filterItem).getFilterOperator())) {

          Operator op = ((ReportDateItem) filterItem).getFilterOperator();

          switch (op) {
            case IS_NULL:
              and.add(SqlUtils.isNull(expr));
              break;
            case NOT_NULL:
              and.add(SqlUtils.notNull(expr));
              break;
            default:
              Long value = ((ReportDateItem) filterItem).getFilter();

              if (value != null) {
                Long dt;
                DateTimeFunction format = ((ReportDateItem) filterItem).getFormat();

                switch (format) {
                  case DATETIME:
                    dt = value;
                    break;
                  case DATE:
                  case YEAR:
                    Function<Integer, JustDate> dateSupplier = format == DateTimeFunction.DATE
                        ? JustDate::new : TimeUtils::startOfYear;

                    switch (op) {
                      case EQ:
                        and.add(SqlUtils.compare(expr, Operator.GE,
                            SqlUtils.constant(dateSupplier.apply(value.intValue()).getTime())));

                        dt = dateSupplier.apply(value.intValue() + 1).getTime();
                        op = Operator.LT;
                        break;
                      case GE:
                        dt = dateSupplier.apply(value.intValue()).getTime();
                        break;
                      case GT:
                        dt = dateSupplier.apply(value.intValue() + 1).getTime();
                        op = Operator.GE;
                        break;
                      case LE:
                        dt = dateSupplier.apply(value.intValue() + 1).getTime();
                        op = Operator.LT;
                        break;
                      case LT:
                        dt = dateSupplier.apply(value.intValue()).getTime();
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
              break;
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

  public ResultHolder getResult(SimpleRowSet rowSet, Dictionary dictionary) {
    ResultHolder result = new ResultHolder();

    List<ReportInfoItem> rowInfoItems = getRowItems();
    List<ReportInfoItem> colInfoItems = getColItems();

    rowSet.forEach(row -> {
      if (getFilterItems().stream().allMatch(filterItem -> filterItem.validate(row))) {
        ResultValue rowGroup;
        ResultValue colGroup;

        if (getRowGrouping() != null) {
          rowGroup = getRowGrouping().getItem().evaluate(row, dictionary);
        } else {
          rowGroup = ResultValue.empty();
        }
        if (getColGrouping() != null) {
          colGroup = getColGrouping().getItem().evaluate(row, dictionary);
        } else {
          colGroup = ResultValue.empty();
        }
        ResultValue[] details = rowInfoItems.stream()
            .map(infoItem -> infoItem.getItem().evaluate(row, dictionary))
            .toArray(ResultValue[]::new);

        colInfoItems.stream().filter(colItem -> !colItem.getItem().isResultItem())
            .forEach(colItem -> result.addValues(rowGroup, details, colGroup, colItem,
                colItem.getItem().evaluate(row, dictionary)));
      }
    });
    // CALC RESULTS
    colInfoItems.stream().filter(colItem -> colItem.getItem().isResultItem())
        .forEach(colItem -> result.getRowGroups(null)
            .forEach(rowGroup -> result.getRows(rowGroup, null)
                .forEach(rowValues -> result.getColGroups()
                    .forEach(colGroup -> result.addValues(rowGroup, rowValues, colGroup, colItem,
                        colItem.getItem().evaluate(rowGroup, rowValues, colGroup, result))))));
    return result;
  }

  @GwtIncompatible
  public ResponseObject getResultResponse(QueryServiceBean qs, String tmp, Dictionary dictionary,
      IsCondition... clauses) {
    SqlSelect resultQuery = new SqlSelect()
        .addFrom(tmp)
        .setWhere(SqlUtils.and(clauses));

    Arrays.stream(qs.getData(new SqlSelect()
        .addAllFields(tmp)
        .addFrom(tmp)
        .setWhere(SqlUtils.sqlFalse())).getColumnNames())
        .filter(this::requiresField)
        .forEach(s -> resultQuery.addFields(tmp, s));

    ResultHolder result = getResult(qs.getData(resultQuery), dictionary);
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(result);
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
    return BeeUtils.isEmpty(getColItems());
  }

  public boolean isGlobal() {
    return global;
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
        if (BeeUtils.same(item.getExpression(), field) && item.getFilter() != null) {
          return true;
        }
      }
    }
    return false;
  }

  public static ReportInfo restore(String data) {
    return BeeSerializable.restore(data, ReportInfo::new);
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

  public void setDescending(int colIndex, Boolean descending) {
    ReportInfoItem item = BeeUtils.getQuietly(getColItems(), colIndex);

    if (item != null) {
      item.descending = descending;
    }
  }

  public void setFunction(int colIndex, ReportFunction function) {
    ReportInfoItem item = BeeUtils.getQuietly(getColItems(), colIndex);

    if (item != null) {
      item.function = Assert.notNull(function);
    }
  }

  public void setGlobal(boolean global) {
    this.global = global;
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
