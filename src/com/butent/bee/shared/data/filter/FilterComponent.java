package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FilterComponent implements BeeSerializable, HasInfo {

  private static final String ATTR_EMPTY_VALUES = "emptyValues";

  public static FilterComponent create(Map<String, String> attributes) {
    if (BeeUtils.isEmpty(attributes)) {
      return null;
    }

    String name = attributes.get(UiConstants.ATTR_NAME);
    if (BeeUtils.isEmpty(name)) {
      return null;
    }

    String value = attributes.get(UiConstants.ATTR_VALUE);
    Boolean emptyValues = BeeUtils.toBooleanOrNull(attributes.get(ATTR_EMPTY_VALUES));

    if (BeeUtils.isEmpty(value) && emptyValues == null) {
      return null;
    } else {
      return new FilterComponent(name, FilterValue.of(value, emptyValues));
    }
  }

  public static FilterComponent restore(String s) {
    Assert.notEmpty(s);

    FilterComponent filterComponent = new FilterComponent();
    filterComponent.deserialize(s);

    return filterComponent;
  }

  private String name;
  private FilterValue filterValue;

  private FilterComponent() {
  }

  public FilterComponent(String name, FilterValue filterValue) {
    this.name = name;
    this.filterValue = filterValue;
  }

  public FilterComponent copy() {
    return new FilterComponent(getName(),
        (getFilterValue() == null) ? null : getFilterValue().copy());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    int i = 0;
    setName(arr[i++]);
    setFilterValue(FilterValue.restore(arr[i++]));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof FilterComponent) {
      FilterComponent other = (FilterComponent) obj;
      return Objects.equals(name, other.name) && Objects.equals(filterValue, other.filterValue);
    } else {
      return false;
    }
  }

  public FilterValue getFilterValue() {
    return filterValue;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties("Name", getName());

    if (getFilterValue() != null) {
      PropertyUtils.addProperties(info, "Value", getFilterValue().getValue(),
          "Empty Values", getFilterValue().getEmptyValues());
    }

    return info;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((filterValue == null) ? 0 : filterValue.hashCode());
    return result;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getName(), getFilterValue()});
  }

  @Override
  public String toString() {
    return "FilterComponent [name=" + name + ", filterValue=" + filterValue + "]";
  }

  private void setFilterValue(FilterValue filterValue) {
    this.filterValue = filterValue;
  }

  private void setName(String name) {
    this.name = name;
  }
}
