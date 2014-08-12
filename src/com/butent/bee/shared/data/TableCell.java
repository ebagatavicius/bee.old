package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Comparator;

/**
 * Implements {@code IsCell} interface, handles data cell functionality like changing value and
 * format of it.
 */

public class TableCell implements IsCell {

  public static Comparator<TableCell> getComparator() {
    return new Comparator<TableCell>() {
      @Override
      public int compare(TableCell cell1, TableCell cell2) {
        if (cell1 == cell2) {
          return 0;
        }
        if (cell1 == null) {
          return -1;
        }
        if (cell2 == null) {
          return 1;
        }
        return cell1.getValue().compareTo(cell2.getValue());
      }
    };
  }

  private Value value;
  private String formattedValue;
  private CustomProperties properties;

  public TableCell(TableCell other) {
    this(other.value, other.formattedValue);
  }

  public TableCell(Value value) {
    this.value = value;
  }

  public TableCell(Value value, String formattedValue) {
    this.value = value;
    this.formattedValue = formattedValue;
  }

  public TableCell(Value value, String formattedValue, CustomProperties properties) {
    this.value = value;
    this.formattedValue = formattedValue;
    this.properties = properties;
  }

  @Override
  public void clearFormattedValue() {
    setFormattedValue(null);
  }

  @Override
  public void clearProperties() {
    setProperties(null);
  }

  @Override
  public void clearProperty(String key) {
    Assert.notEmpty(key);
    if (properties != null) {
      properties.remove(key);
    }
  }

  @Override
  public void clearValue() {
    setValue(Value.getNullValueFromValueType(getType()));
  }

  @Override
  public TableCell copy() {
    TableCell result = new TableCell(value, formattedValue);
    if (properties != null) {
      result.properties = properties.copy();
    }
    return result;
  }

  @Override
  public String getFormattedValue() {
    return formattedValue;
  }

  @Override
  public CustomProperties getProperties() {
    return properties;
  }

  @Override
  public String getProperty(String key) {
    Assert.notEmpty(key);
    if (properties == null) {
      return null;
    }
    return properties.get(key);
  }

  @Override
  public ValueType getType() {
    return value.getType();
  }

  @Override
  public Value getValue() {
    return value;
  }

  @Override
  public boolean isNull() {
    return value.isNull();
  }

  @Override
  public void setFormattedValue(String formattedValue) {
    this.formattedValue = formattedValue;
  }

  @Override
  public void setProperties(CustomProperties properties) {
    this.properties = properties;
  }

  @Override
  public void setProperty(String propertyKey, String propertyValue) {
    Assert.notEmpty(propertyKey);

    if (BeeUtils.isEmpty(propertyValue)) {
      clearProperty(propertyKey);
    } else {
      if (properties == null) {
        properties = CustomProperties.create();
      }
      properties.put(propertyKey, propertyValue);
    }
  }

  @Override
  public void setValue(Value value) {
    this.value = value;
  }

  @Override
  public void setValue(Value v, String formatted) {
    this.value = v;
    this.formattedValue = formatted;
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
