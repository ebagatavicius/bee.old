package com.butent.bee.server.sql;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an INSERT SQL statement for a given target using specified field and value lists.
 */

public class SqlInsert extends SqlQuery<SqlInsert> implements HasTarget {

  private final String target;
  private final LinkedHashMap<String, Integer> fieldList = new LinkedHashMap<>();
  private List<IsExpression[]> data;
  private SqlSelect dataSource;

  /**
   * Creates an SqlInsert statement with a specified target {@code target}. Target type is
   * FromSingle.
   * 
   * @param target the FromSingle target
   */
  public SqlInsert(String target) {
    Assert.notEmpty(target);
    this.target = target;
  }

  public SqlInsert addAll(Map<String, ?> map) {
    if (map != null) {
      map.forEach(this::addConstant);
    }
    return getReference();
  }

  /**
   * Adds a constant value expression in a field for an SqlInsert statement.
   * 
   * @param field the field's name
   * @param value the field's value
   * @return object's SqlInsert instance.
   */
  public SqlInsert addConstant(String field, Object value) {
    addExpression(field, value instanceof IsExpression
        ? (IsExpression) value : SqlUtils.constant(value));
    return getReference();
  }

  /**
   * Adds an expression for an SqlInsert statement.
   * 
   * @param field the field to add
   * @param value the expression to add
   * @return object's SqlInsert instance.
   */
  public SqlInsert addExpression(String field, IsExpression value) {
    Assert.notNull(value);
    Assert.state(getFieldCount() == 0 || !BeeUtils.isEmpty(data));
    Assert.state(!isMultipleInsert());

    addField(field);

    if (BeeUtils.isEmpty(data)) {
      data = Lists.newArrayListWithExpectedSize(1);
      data.add(new IsExpression[0]);
    }
    ArrayList<IsExpression> values = Lists.newArrayList(data.get(0));
    values.add(value);

    data.set(0, values.toArray(new IsExpression[0]));

    return getReference();
  }

  /**
   * Adds the specified fields {@code fields} to the field list.
   * 
   * @param fields the fields to add
   * @return object's SqlInsert instance.
   */
  public SqlInsert addFields(String... fields) {
    Assert.state(BeeUtils.isEmpty(data));

    for (String fld : fields) {
      addField(fld);
    }
    return getReference();
  }

  public SqlInsert addFields(Collection<String> fields) {
    Assert.state(BeeUtils.isEmpty(data));

    for (String fld : fields) {
      addField(fld);
    }
    return getReference();
  }

  public SqlInsert addNotEmpty(String field, String value) {
    if (BeeUtils.isEmpty(value)) {
      return getReference();
    } else {
      return addConstant(field, value);
    }
  }

  public SqlInsert addNotNull(String field, Object value) {
    if (value == null) {
      return getReference();
    } else {
      return addConstant(field, value);
    }
  }

  public SqlInsert addValues(Object... values) {
    Assert.notNull(values);
    Assert.state(getFieldCount() == values.length);

    IsExpression[] row = new IsExpression[values.length];

    for (int i = 0; i < values.length; i++) {
      row[i] = values[i] instanceof IsExpression
          ? (IsExpression) values[i]
          : SqlUtils.constant(values[i]);
    }
    if (data == null) {
      data = new ArrayList<>();
    }
    data.add(row);

    return getReference();
  }

  /**
   * @return the current value list.
   */
  public List<IsExpression[]> getData() {
    return data;
  }

  /**
   * @return the current {@code dataSource}.
   */
  public SqlSelect getDataSource() {
    return dataSource;
  }

  /**
   * Counts how many fields are in the field list {@code fieldList} and returns the amount.
   * 
   * @return the amount of fields
   */
  public int getFieldCount() {
    return fieldList.size();
  }

  public Collection<String> getFields() {
    return fieldList.keySet();
  }

  public int getRowCount() {
    return data == null ? 0 : data.size();
  }

  /**
   * Returns a list of sources found in the {@code dataSource}.
   * 
   * @return a list of sources found in the {@code dataSource}.
   */
  @Override
  public Collection<String> getSources() {
    Collection<String> sources = Sets.newHashSet(getTarget());

    if (dataSource != null) {
      sources = SqlUtils.addCollection(sources, dataSource.getSources());
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
   * @param field the field, which value must be returned
   * @return value of the given field.
   */
  public IsExpression getValue(String field) {
    Assert.state(!isMultipleInsert());
    Assert.state(hasField(field));
    Assert.state(!BeeUtils.isEmpty(data));

    return data.get(0)[fieldList.get(field)];
  }

  /**
   * Checks if a field name {@code field} is already in a field list.
   * 
   * @param field the field to check
   * @return true if the field exist in the list, otherwise false.
   */
  public boolean hasField(String field) {
    return fieldList.containsKey(field);
  }

  /**
   * Checks if the current instance of SqlInsert is empty.
   * 
   * @return true if it is empty, otherwise false.
   */
  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(fieldList)
        || (BeeUtils.isEmpty(data) && dataSource == null);
  }

  public boolean isMultipleInsert() {
    return dataSource != null || (getRowCount() > 1);
  }

  /**
   * Clears {@code fieldList}, {@code valueList} and {@code dataSource}.
   * 
   * @return object's SqlInsert instance.
   */
  @Override
  public SqlInsert reset() {
    fieldList.clear();
    return resetValues();
  }

  public SqlInsert resetValues() {
    data = null;
    dataSource = null;
    return getReference();
  }

  /**
   * If there are no values in the {@code valueList} created sets the {@code dataSource} from an
   * SqlSelect query {@code query}.
   * 
   * @param query the query to use for setting the dataSource
   * @return object's SqlInsert instance
   */
  public SqlInsert setDataSource(SqlSelect query) {
    Assert.notNull(query);
    Assert.state(!query.isEmpty());
    Assert.state(BeeUtils.isEmpty(data));

    dataSource = query;

    return getReference();
  }

  public SqlInsert updExpression(String field, IsExpression value) {
    Assert.notEmpty(field);
    Assert.state(hasField(field), "Field " + field + " does not exist");
    Assert.notEmpty(data);
    Assert.state(!isMultipleInsert());

    data.get(0)[fieldList.get(field)] = value;

    return getReference();
  }

  private void addField(String field) {
    Assert.notEmpty(field);
    Assert.state(!hasField(field), "Field " + field + " already exists");
    fieldList.put(field, getFieldCount());
  }
}
