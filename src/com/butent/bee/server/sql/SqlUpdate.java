package com.butent.bee.server.sql;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Generates UPDATE SQL statements for specified target, update values and WHERE condition.
 */

public class SqlUpdate extends SqlQuery<SqlUpdate> implements HasTarget {

  private final String target;
  private IsFrom fromSource;
  private IsCondition fromJoin;
  private final Map<String, IsSql> updates = Maps.newLinkedHashMap();
  private IsCondition whereClause;

  /**
   * Creates an SqlUpdate statement with a specified target {@code target}.
   *
   * @param target the String target
   */
  public SqlUpdate(String target) {
    Assert.notEmpty(target);
    this.target = target;
  }

  /**
   * Adds a constant value expression in a field for an SqlUpdate statement.
   *
   * @param field the field's name
   * @param value the field's value
   * @return object's SqlInsert instance.
   */
  public SqlUpdate addConstant(String field, Object value) {
    return addExpression(field, value instanceof IsSql ? (IsSql) value : SqlUtils.constant(value));
  }

  /**
   * Adds an expression for an SqlUpdate statement.
   *
   * @param field the field to add
   * @param value the expression to add
   * @return object's SqlInsert instance.
   */
  public SqlUpdate addExpression(String field, IsSql value) {
    Assert.notEmpty(field);
    Assert.notNull(value);
    Assert.state(!hasField(field), "Field " + field + " already exists");

    updates.put(field, value);
    return getReference();
  }

  public IsCondition getFromJoin() {
    return fromJoin;
  }

  public IsFrom getFromSource() {
    return fromSource;
  }

  /**
   * @return a list of sources found in the target and Where clause.
   */
  @Override
  public Collection<String> getSources() {
    Collection<String> sources = Sets.newHashSet(target);

    if (fromSource != null) {
      sources = SqlUtils.addCollection(sources, fromSource.getSources());
      sources = SqlUtils.addCollection(sources, fromJoin.getSources());
    }
    if (whereClause != null) {
      sources = SqlUtils.addCollection(sources, whereClause.getSources());
    }
    return sources;
  }

  /**
   * @return the current target {@code target}
   */
  @Override
  public String getTarget() {
    return target;
  }

  /**
   * @return the current update {@code updates} list.
   */
  public Map<String, IsSql> getUpdates() {
    return updates;
  }

  public IsSql getValue(String field) {
    Assert.state(hasField(field));
    return updates.get(field);
  }

  /**
   * @return a Where clause.
   */
  public IsCondition getWhere() {
    return whereClause;
  }

  public boolean hasField(String field) {
    Assert.notEmpty(field);
    return updates.containsKey(field);
  }

  /**
   * Checks if the current instance of SqlUpdate is empty. Checks if the target and {@code updates}
   * list are empty.
   *
   * @returns true if it is empty, otherwise false.
   */
  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(updates);
  }

  /**
   * Clears the update {@code updates} list and the Where clause.
   *
   * @return object's SqlUpdate instance
   */
  @Override
  public SqlUpdate reset() {
    updates.clear();
    fromSource = null;
    fromJoin = null;
    whereClause = null;
    return getReference();
  }

  public SqlUpdate setFrom(String source, IsCondition on) {
    return setFrom(source, null, on);
  }

  public SqlUpdate setFrom(String source, String alias, IsCondition on) {
    Assert.notNull(on);
    fromSource = FromJoin.fromSingle(source, alias);
    fromJoin = on;
    return getReference();
  }

  public SqlUpdate setFrom(SqlSelect source, String alias, IsCondition on) {
    Assert.notNull(on);
    fromSource = FromJoin.fromSingle(source, alias);
    fromJoin = on;
    return getReference();
  }

  /**
   * Sets the Where clause to the specified clause {@code clause}.
   *
   * @param clause a clause to set Where to.
   *
   * @return object's SqlUpdate instance
   */
  public SqlUpdate setWhere(IsCondition clause) {
    whereClause = clause;
    return getReference();
  }

  public SqlUpdate updExpression(String field, IsSql value) {
    Assert.notEmpty(field);
    Assert.notNull(value);
    Assert.state(hasField(field), "Field " + field + " does not exist");

    updates.put(field, value);
    return getReference();
  }
}
