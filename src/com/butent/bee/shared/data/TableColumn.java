package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

/**
 * Implements {@code IsColumn} interface, gets and sets table column's properties.
 */

public class TableColumn implements HasInfo, IsColumn {

  private ValueType type;
  
  private String label;
  
  private String id;
  
  private String pattern;
  
  private CustomProperties properties;
  
  private int precision = BeeConst.UNDEF;
  private int scale = BeeConst.UNDEF;

  private String enumKey;

  public TableColumn(ValueType type) {
    this(type, null, null);
  }

  public TableColumn(ValueType type, String label, String id) {
    this.type = type;
    this.label = label;
    this.id = id;
    this.pattern = null;
  }

  @Override
  public void clearProperty(String key) {
    Assert.notEmpty(key);
    if (properties != null) {
      properties.remove(key);
    }
  }
  
  @Override
  public TableColumn copy() {
    TableColumn result = new TableColumn(type, label, id);
    result.setPattern(pattern);
    if (properties != null) {
      result.properties = properties.copy();
    }
    return result;
  }

  @Override
  public String getEnumKey() {
    return enumKey;
  }
  
  @Override
  public String getId() {
    return id;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> lst = PropertyUtils.createProperties("Id", getId(), "Type", getType(),
        "Label", getLabel(), "Pattern", getPattern());
    if (getProperties() != null) {
      lst.addAll(getProperties().getInfo());
    }
    return lst;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public String getPattern() {
    return pattern;
  }

  @Override
  public int getPrecision() {
    return precision;
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
  public int getScale() {
    return scale;
  }

  @Override
  public ValueType getType() {
    return type;
  }

  @Override
  public boolean isCharacter() {
    return ValueType.TEXT.equals(getType()) && getPrecision() > 0;
  }
  
  @Override
  public boolean isText() {
    return ValueType.TEXT.equals(getType()) && getPrecision() <= 0;
  }

  @Override
  public void setEnumKey(String enumKey) {
    this.enumKey = enumKey;
  }
  
  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public void setPrecision(int precision) {
    this.precision = precision;
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
  public void setScale(int scale) {
    this.scale = scale;
  }
  
  @Override
  public void setType(ValueType type) {
    this.type = type;
  }
}
