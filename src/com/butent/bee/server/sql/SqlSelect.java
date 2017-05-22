package com.butent.bee.server.sql;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NullOrdering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generates SELECT SQL statements and their full range of elements (inc. formation of WHERE and
 * GROUP BY clauses, union, distinct, result limitations support etc).
 */

public class SqlSelect extends HasFrom<SqlSelect> implements IsCloneable<SqlSelect> {

  public static final int FIELD_EXPR = 0;
  public static final int FIELD_ALIAS = 1;

  static final int ORDER_SRC = 0;
  static final int ORDER_FLD = 1;
  static final int ORDER_DESC = 2;
  static final int ORDER_NULLS = 3;

  private List<IsExpression[]> fieldList;
  private IsCondition whereClause;
  private List<IsExpression> groupList;
  private List<String[]> orderList;
  private IsCondition havingClause;
  private List<SqlSelect> unionList;

  private boolean distinctMode;
  private boolean unionAllMode = true;
  private int limit;
  private int offset;

  /**
   * Adds all friend from {@code source} table.
   * <p>
   * E.g: source.*
   * </p>
   *
   * @param source the source table
   * @return object's SqlSelect instance.
   */
  public SqlSelect addAllFields(String source) {
    Assert.notEmpty(source);
    addField(SqlUtils.name(source + ".*"), null);
    return getReference();
  }

  /**
   * Adds an AVG function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addAvg(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.AVG, expr), alias);
    return getReference();
  }

  /**
   * Adds an AVG function for {@code source} table and field {@code field}.
   *
   * @param source the source's name
   * @param field the field's name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addAvg(String source, String field) {
    return addAvg(SqlUtils.field(source, field), field);
  }

  /**
   * Adds an AVG function for {@code source} table and field {@code field} using an alias
   * {@code alias}.
   *
   * @param source the source's name
   * @param field the field's name
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addAvg(String source, String field, String alias) {
    return addAvg(SqlUtils.field(source, field), alias);
  }

  /**
   * Adds an AVG DISTINCT function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addAvgDistinct(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.AVG_DISTINCT, expr), alias);
    return getReference();
  }

  /**
   * Adds an AVG DISTINCT function for {@code source} table and field {@code field} using an alias
   * {@code alias}.
   *
   * @param source the source's name
   * @param field the field's name
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addAvgDistinct(String source, String field, String alias) {
    return addAvgDistinct(SqlUtils.field(source, field), alias);
  }

  /**
   * Creates a constant expression.
   *
   * @param constant the constant value.
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addConstant(Object constant, String alias) {
    addExpr(SqlUtils.constant(constant), alias);
    return getReference();
  }

  /**
   * Adds a COUNT function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addCount(IsExpression expr, String alias) {
    addExpr(SqlUtils.aggregate(SqlFunction.COUNT, expr), alias);
    return getReference();
  }

  /**
   * Adds an COUNT function for {@code source} table and field {@code field}.
   *
   * @param source the source's name
   * @param field the field's name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addCount(String source, String field) {
    return addCount(SqlUtils.field(source, field), field);
  }

  /**
   * Adds an COUNT function for {@code source} table and field {@code field} using an alias
   * {@code alias}.
   *
   * @param source the source's name
   * @param field the field's name
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addCount(String source, String field, String alias) {
    return addCount(SqlUtils.field(source, field), alias);
  }

  /**
   * Adds a COUNT function without any defined expressions.
   *
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addCount(String alias) {
    return addCount((IsExpression) null, alias);
  }

  /**
   * Adds a COUNT DISTINCT function with a specified expression {@code expr} and alias {@code alias}
   * .
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addCountDistinct(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.COUNT_DISTINCT, expr), alias);
    return getReference();
  }

  /**
   * Adds an COUNT DISTINCT function for {@code source} table and field {@code field} using an alias
   * {@code alias}.
   *
   * @param source the source's name
   * @param field the field's name
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addCountDistinct(String source, String field, String alias) {
    return addCountDistinct(SqlUtils.field(source, field), alias);
  }

  /**
   * Adds an empty BOOLEAN field with the specified {@code alias} name.
   *
   * @param alias the alias name.
   * @return object's SqlSelect instance.
   */
  public SqlSelect addEmptyBoolean(String alias) {
    return addEmptyField(alias, SqlDataType.BOOLEAN, 0, 0, false);
  }

  /**
   * Adds an empty CHAR field with the specified {@code alias} name and precition.
   *
   * @param alias the alias name
   * @param precision the fields precision
   * @return object's SqlSelect instance.
   */
  public SqlSelect addEmptyChar(String alias, int precision) {
    return addEmptyField(alias, SqlDataType.CHAR, precision, 0, false);
  }

  /**
   * Adds an empty DATE field with the specified {@code alias} name.
   *
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addEmptyDate(String alias) {
    return addEmptyField(alias, SqlDataType.DATE, 0, 0, false);
  }

  /**
   * Adds an empty DATETIME field with the specified {@code alias} name.
   *
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addEmptyDateTime(String alias) {
    return addEmptyField(alias, SqlDataType.DATETIME, 0, 0, false);
  }

  /**
   * Adds an empty DOUBLE field with the specified {@code alias} name.
   *
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addEmptyDouble(String alias) {
    return addEmptyField(alias, SqlDataType.DOUBLE, 0, 0, false);
  }

  /**
   * Adds a specified type {@code type} field with a specified precision and scale.
   *
   * @param alias the alias name
   * @param type the field's type to add
   * @param precision the field's name
   * @param scale the field's scale
   * @param notNull field's default value is not null
   * @return object's SqlSelect instance.
   */
  public SqlSelect addEmptyField(String alias, SqlDataType type, int precision, int scale,
      boolean notNull) {
    Assert.notEmpty(alias);
    Assert.notNull(type);
    Object emptyValue = null;

    if (notNull) {
      emptyValue = type.getEmptyValue();
    }
    addField(SqlUtils.cast(SqlUtils.constant(emptyValue), type, precision, scale), alias);
    return getReference();
  }

  /**
   * Creates an empty INTEGER type field and adds it.
   *
   * @param alias the alias to use
   * @return object's SqlSelect instance
   */
  public SqlSelect addEmptyInt(String alias) {
    return addEmptyField(alias, SqlDataType.INTEGER, 0, 0, false);
  }

  /**
   * Creates an empty LONG type field and adds it.
   *
   * @param alias the alias to use
   * @return object's SqlSelect instance
   */
  public SqlSelect addEmptyLong(String alias) {
    return addEmptyField(alias, SqlDataType.LONG, 0, 0, false);
  }

  /**
   * Creates an empty NUMERIC type field with specified precision {@code precision} and scale
   * {@code scale} and adds it.
   *
   * @param alias the alias to use
   * @param precision the precision
   * @param scale the scale
   * @return object's SqlSelect instance
   */
  public SqlSelect addEmptyNumeric(String alias, int precision, int scale) {
    return addEmptyField(alias, SqlDataType.DECIMAL, precision, scale, false);
  }

  /**
   * Creates an empty STRING type field with specified precision {@code precision} and adds it.
   *
   * @param alias the alias to use
   * @param precision the precision
   * @return object's SqlSelect instance
   */
  public SqlSelect addEmptyString(String alias, int precision) {
    return addEmptyField(alias, SqlDataType.STRING, precision, 0, false);
  }

  /**
   * Creates an empty TEXT type field and adds it.
   *
   * @param alias the alias to use
   * @return object's SqlSelect instance
   */
  public SqlSelect addEmptyText(String alias) {
    return addEmptyField(alias, SqlDataType.TEXT, 0, 0, false);
  }

  /**
   * Adds an expression {@code expr}.
   *
   * @param expr the expression
   * @param alias the alias
   * @return object's SqlSelect instance
   */
  public SqlSelect addExpr(IsExpression expr, String alias) {
    Assert.notNull(expr);
    Assert.notEmpty(alias);

    addField(expr, alias);
    return getReference();
  }

  /**
   * Adds a field to a specified source destination {@code source}. Fields name is {@code field} and
   * alias {@code alias}.
   *
   * @param source the source table to add to
   * @param field the field to add
   * @param alias alias to use
   * @return object's SqlSelect instance
   */
  public SqlSelect addField(String source, String field, String alias) {
    addExpr(SqlUtils.field(source, field), alias);
    return getReference();
  }

  /**
   * Adds multiple fields {@code fields} to a specified source table {@code  source}.
   *
   * @param source the source table to add to
   * @param fields the fields to add
   * @return object's SqlSelect instance
   */
  public SqlSelect addFields(String source, String... fields) {
    Assert.minLength(ArrayUtils.length(fields), 1);

    for (String fld : fields) {
      addField(SqlUtils.field(source, fld), null);
    }
    return getReference();
  }

  public SqlSelect addFields(String source, Collection<String> fields) {
    Assert.notEmpty(fields);

    for (String fld : fields) {
      addField(SqlUtils.field(source, fld), null);
    }
    return getReference();
  }

  /**
   * Adds specified fields to a group list.
   *
   * @param source the source table
   * @param fields the fields to add to the group
   * @return object's SqlSelect instance.
   */
  public SqlSelect addGroup(String source, String... fields) {
    addGroup(SqlUtils.fields(source, fields));
    return getReference();
  }

  public SqlSelect addGroup(IsExpression... group) {
    if (BeeUtils.isEmpty(groupList)) {
      groupList = new ArrayList<>();
    }
    for (IsExpression grp : group) {
      groupList.add(grp);
    }
    return getReference();
  }

  public SqlSelect addGroup(String source, Collection<String> fields) {
    fields.forEach(field -> addGroup(source, field));
    return getReference();
  }

  /**
   * Adds a MAX function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addMax(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.MAX, expr), alias);
    return getReference();
  }

  /**
   * Adds a MAX function with a specified table {@code source} and field {@code field}.
   *
   * @param source the source table name
   * @param field the field's name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addMax(String source, String field) {
    return addMax(SqlUtils.field(source, field), field);
  }

  /**
   * Adds a MAX function with a specified table {@code source} and field {@code field} using an
   * alias.
   *
   * @param source the source table name
   * @param field the field's name
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addMax(String source, String field, String alias) {
    return addMax(SqlUtils.field(source, field), alias);
  }

  /**
   * Adds a MIN function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addMin(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.MIN, expr), alias);
    return getReference();
  }

  /**
   * Adds a MIN function with a specified table {@code source} and field {@code field}.
   *
   * @param source the source table name
   * @param field the field's name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addMin(String source, String field) {
    return addMin(SqlUtils.field(source, field), field);
  }

  /**
   * Adds a MIN function with a specified table {@code source} and field {@code field} using an
   * alias.
   *
   * @param source the source table name
   * @param field the field's name
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addMin(String source, String field, String alias) {
    return addMin(SqlUtils.field(source, field), alias);
  }

  /**
   * Adds {@code order} to an order list.
   *
   * @param source the source table.
   * @param order the fields to add to the order list
   * @return object's SqlSelect instance.
   */
  public SqlSelect addOrder(String source, String... order) {
    addOrder(false, null, source, order);
    return getReference();
  }

  public SqlSelect addOrder(String source, String field, NullOrdering nullOrdering) {
    addOrder(false, nullOrdering, source, field);
    return getReference();
  }

  public SqlSelect addOrderBy(Order.Column column, String source, String field) {
    Assert.notNull(column);
    Assert.notEmpty(field);

    addOrder(!column.isAscending(), column.getNullOrdering(), source, field);
    return getReference();
  }

  /**
   * Adds {@code order} to an order list. Uses descending ordering.
   *
   * @param source the source table.
   * @param order the fields to add to the order list
   * @return object's SqlSelect instance.
   */
  public SqlSelect addOrderDesc(String source, String... order) {
    addOrder(true, null, source, order);
    return getReference();
  }

  /**
   * Adds a SUM function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addSum(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.SUM, expr), alias);
    return getReference();
  }

  /**
   * Adds a SUM function with a specified table {@code source} and field {@code field}.
   *
   * @param source the source table name
   * @param field the field's name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addSum(String source, String field) {
    return addSum(SqlUtils.field(source, field), field);
  }

  /**
   * Adds a SUM function with a specified table {@code source} and field {@code field} using an
   * alias.
   *
   * @param source the source table name
   * @param field the field's name
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addSum(String source, String field, String alias) {
    return addSum(SqlUtils.field(source, field), alias);
  }

  public SqlSelect addSum(String source, Collection<String> fields) {
    fields.forEach(field -> addSum(source, field));
    return getReference();
  }

  /**
   * Adds a SUM DISTINCT function with a specified expression {@code expr} and alias {@code alias}.
   *
   * @param expr the expression
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addSumDistinct(IsExpression expr, String alias) {
    Assert.notNull(expr);
    addExpr(SqlUtils.aggregate(SqlFunction.SUM_DISTINCT, expr), alias);
    return getReference();
  }

  /**
   * Adds a SUM DISTINCT function with a specified table {@code source} and field {@code field}
   * using an alias.
   *
   * @param source the source table name
   * @param field the field's name
   * @param alias the alias name
   * @return object's SqlSelect instance.
   */
  public SqlSelect addSumDistinct(String source, String field, String alias) {
    return addSumDistinct(SqlUtils.field(source, field), alias);
  }

  /**
   * Adds other SqlSelect {@code union} sentences to the union list.
   *
   * @param union specified SqlSelect sentences
   * @return object's SqlSelect instance.
   */
  public SqlSelect addUnion(SqlSelect... union) {
    Assert.noNulls((Object[]) union);

    if (BeeUtils.isEmpty(unionList)) {
      this.unionList = new ArrayList<>();
    }
    for (SqlSelect un : union) {
      if (!un.isEmpty()) {
        this.unionList.add(un);
      }
    }
    return getReference();
  }

  @Override
  public SqlSelect copyOf() {
    return copyOf(false);
  }

  public SqlSelect copyOf(boolean deep) {
    SqlSelect query = new SqlSelect();

    if (fieldList != null) {
      query.fieldList = new ArrayList<>(fieldList);
    }
    List<IsFrom> fromList = getFrom();

    if (fromList != null) {
      if (deep) {
        List<IsFrom> froms = new ArrayList<>();

        for (IsFrom from : fromList) {
          froms.add(from.copyOf());
        }
        query.setFrom(froms);
      } else {
        query.setFrom(new ArrayList<>(fromList));
      }
    }
    if (whereClause != null) {
      query.setWhere(deep ? whereClause.copyOf() : whereClause);
    }
    if (groupList != null) {
      query.groupList = new ArrayList<>(groupList);
    }
    if (orderList != null) {
      query.orderList = new ArrayList<>(orderList);
    }
    if (havingClause != null) {
      query.setHaving(deep ? havingClause.copyOf() : havingClause);
    }
    if (unionList != null) {
      if (deep) {
        List<SqlSelect> unions = new ArrayList<>();

        for (SqlSelect union : unionList) {
          unions.add(union.copyOf(deep));
        }
        query.unionList = unions;
      } else {
        query.unionList = new ArrayList<>(unionList);
      }
    }
    query.setDistinctMode(distinctMode);
    query.setUnionAllMode(unionAllMode);
    query.setLimit(limit);
    query.setOffset(offset);

    return query;
  }

  // Getters ----------------------------------------------------------------
  /**
   * @return current field list.
   */
  public List<IsExpression[]> getFields() {
    return fieldList;
  }

  /**
   * @return the current group by list {@code groupList}.
   */
  public List<IsExpression> getGroupBy() {
    return groupList;
  }

  /**
   * @return the current having clause
   */
  public IsCondition getHaving() {
    return havingClause;
  }

  /**
   * @return the current limit {@code limit}
   */
  public int getLimit() {
    return limit;
  }

  /**
   * @return the current offset {@code offset}
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @return the current order by {@code orgetList} list
   */
  public List<String[]> getOrderBy() {
    return orderList;
  }

  /**
   * Returns a list of sources found in the where clause {@code whereClause} , having clause
   * {@code havingClause}, union list {@code unionList} .
   *
   * @return the list of sources
   */
  @Override
  public Collection<String> getSources() {
    Assert.state(!isEmpty());

    Collection<String> sources = super.getSources();

    if (whereClause != null) {
      sources = SqlUtils.addCollection(sources, whereClause.getSources());
    }
    if (havingClause != null) {
      sources = SqlUtils.addCollection(sources, havingClause.getSources());
    }
    if (!BeeUtils.isEmpty(unionList)) {
      for (SqlSelect union : unionList) {
        sources = SqlUtils.addCollection(sources, union.getSources());
      }
    }
    return sources;
  }

  /**
   * @return the current SqlSelect union list.
   */
  public List<SqlSelect> getUnion() {
    return unionList;
  }

  /**
   * @return the current Where clause.
   */
  public IsCondition getWhere() {
    return whereClause;
  }

  /**
   * @return the current distinct mode.
   */
  public boolean isDistinctMode() {
    return distinctMode;
  }

  /**
   * Checks if the current SqlSelect object is empty.
   *
   * @return true - if the object is empty, otherwise false.
   */
  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(fieldList);
  }

  /**
   * @return the currently set union all mode.
   */
  public boolean isUnionAllMode() {
    return unionAllMode;
  }

  /**
   * Clears the SqlSelect object.
   *
   * @return a cleared SqlSelect object.
   */
  @Override
  public SqlSelect reset() {
    resetFields();
    resetGroup();
    resetOrder();
    resetUnion();
    whereClause = null;
    havingClause = null;
    setDistinctMode(false);
    setUnionAllMode(true);
    setOffset(0);
    setLimit(0);
    return getReference();
  }

  /**
   * Clears all fields from the field list.
   *
   * @return object's SqlSelect instance
   */
  public SqlSelect resetFields() {
    if (!BeeUtils.isEmpty(fieldList)) {
      fieldList.clear();
    }
    return getReference();
  }

  /**
   * Resets the group list.
   *
   * @return object's SqlSelect instance.
   */
  public SqlSelect resetGroup() {
    if (!BeeUtils.isEmpty(groupList)) {
      groupList.clear();
    }
    return getReference();
  }

  /**
   * Resets the order list.
   *
   * @return object's SqlSelect instance.
   */
  public SqlSelect resetOrder() {
    if (!BeeUtils.isEmpty(orderList)) {
      orderList.clear();
    }
    return getReference();
  }

  /**
   * Resets the union list.
   *
   * @return object's SqlSelect instance.
   */
  public SqlSelect resetUnion() {
    if (!BeeUtils.isEmpty(unionList)) {
      unionList.clear();
    }
    return getReference();
  }

  /**
   * Resets the distinct mode to the specified {@code distinct} value.
   *
   * @param distinct the value to change to
   * @return object's SqlSelect instance.
   */
  public SqlSelect setDistinctMode(boolean distinct) {
    this.distinctMode = distinct;
    return getReference();
  }

  /**
   * Sets the having clause.
   *
   * @param having the clause's condition to set
   * @return object's SqlSelect instance.
   */
  public SqlSelect setHaving(IsCondition having) {
    havingClause = having;
    return getReference();
  }

  /**
   * Sets the limit parameter to {@code limit}.
   *
   * @param lim the value to set to
   * @return object's SqlSelect instance.
   */
  public SqlSelect setLimit(int lim) {
    Assert.nonNegative(lim);
    this.limit = lim;
    return getReference();
  }

  /**
   * Sets the offset parameter to {@code offset}.
   *
   * @param off the value to set to.
   * @return object's SqlSelect instance.
   */
  public SqlSelect setOffset(int off) {
    Assert.nonNegative(off);
    this.offset = off;
    return getReference();
  }

/**
   * Sets the  union all mode to the specified argument {@code unionAll).
   *
   * @param unionAll the argument to use for setting the mode
   * @return object's SqlSelect instance
   */
  public SqlSelect setUnionAllMode(boolean unionAll) {
    unionAllMode = unionAll;
    return getReference();
  }

  /**
   * Sets the where condition.
   *
   * @param clause the condition to set.
   * @return object's SqlSelect instance.
   */
  public SqlSelect setWhere(IsCondition clause) {
    whereClause = clause;
    return getReference();
  }

  private void addField(IsExpression expr, String alias) {
    IsExpression[] fieldEntry = new IsExpression[2];
    fieldEntry[FIELD_EXPR] = expr;
    fieldEntry[FIELD_ALIAS] = BeeUtils.isEmpty(alias) ? null : SqlUtils.name(alias);

    if (BeeUtils.isEmpty(fieldList)) {
      fieldList = new ArrayList<>();
    }
    fieldList.add(fieldEntry);
  }

  private void addOrder(Boolean desc, NullOrdering nullOrdering, String source, String... fields) {
    for (String ord : fields) {
      String[] orderEntry = new String[4];
      orderEntry[ORDER_SRC] = source;
      orderEntry[ORDER_FLD] = ord;
      orderEntry[ORDER_DESC] = BeeUtils.isTrue(desc) ? " DESC" : "";

      orderEntry[ORDER_NULLS] = (nullOrdering == null) ? ""
          : (" NULLS " + ((nullOrdering == NullOrdering.NULLS_FIRST) ? "FIRST" : "LAST"));

      if (BeeUtils.isEmpty(orderList)) {
        orderList = new ArrayList<>();
      }
      orderList.add(orderEntry);
    }
  }
}
