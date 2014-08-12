package com.butent.bee.shared.data.filter;

import com.google.common.base.Objects;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public final class FilterValue implements BeeSerializable {

  public static FilterValue of(String value) {
    return new FilterValue(value, null);
  }

  public static FilterValue of(String value, Boolean emptyValues) {
    return new FilterValue(value, emptyValues);
  }

  public static FilterValue restore(String s) {
    Assert.notEmpty(s);

    FilterValue filterValue = new FilterValue();
    filterValue.deserialize(s);

    return filterValue;
  }

  private String value;
  private Boolean emptyValues;

  private FilterValue() {
  }

  private FilterValue(String value, Boolean emptyValues) {
    this.value = value;
    this.emptyValues = emptyValues;
  }

  public FilterValue copy() {
    return new FilterValue(getValue(), getEmptyValues());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    int i = 0;
    setValue(arr[i++]);
    setEmptyValues(BeeUtils.toBooleanOrNull(arr[i++]));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof FilterValue) {
      FilterValue other = (FilterValue) obj;
      return Objects.equal(value, other.value) && Objects.equal(emptyValues, other.emptyValues);
    } else {
      return false;
    }
  }

  public Boolean getEmptyValues() {
    return emptyValues;
  }

  public String getValue() {
    return value;
  }

  public boolean hasEmptiness() {
    return getEmptyValues() != null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    result = prime * result + ((emptyValues == null) ? 0 : emptyValues.hashCode());
    return result;
  }

  public boolean hasValue() {
    return !BeeUtils.isEmpty(getValue());
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getValue(), getEmptyValues()});
  }

  @Override
  public String toString() {
    return "FilterValue [value=" + value + ", emptyValues=" + emptyValues + "]";
  }

  private void setEmptyValues(Boolean emptyValues) {
    this.emptyValues = emptyValues;
  }

  private void setValue(String value) {
    this.value = value;
  }
}
