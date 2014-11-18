package com.butent.bee.shared.utils;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;

import java.util.Objects;

/**
 * Used for creating Properties.
 */
public class Property implements Comparable<Property>, BeeSerializable {

  public static final String[] HEADERS = new String[] {"Property", "Value"};
  public static final int HEADER_COUNT = HEADERS.length;

  public static Property restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Property property = new Property();
    property.deserialize(s);
    return property;
  }

  private String name;
  private String value;

  /**
   * Creates a Property with specified {@code name} and {@code value} values.
   * @param name the {@code name} to set for the Property
   * @param value the {@code value} to set for the Property
   */
  public Property(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public Property(String name, int value) {
    this(name, String.valueOf(value));
  }

  protected Property() {
  }

  /**
   * Compares {@code oth} with the current Property. Only names are compared.
   * @return 0 if values are equal, -1 if {@code oth} name value is greater, 1 if the current
   *         Property name value is greater than {@code oth} name value.
   */
  @Override
  public int compareTo(Property oth) {
    if (oth == null) {
      return BeeConst.COMPARE_MORE;
    }
    return BeeUtils.compareNullsFirst(getName(), oth.getName());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, HEADER_COUNT);

    setName(arr[0]);
    setValue(arr[1]);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Property)) {
      return false;
    } else if (this == obj) {
      return true;
    } else {
      return Objects.equals(getName(), ((Property) obj).getName())
          && Objects.equals(getValue(), ((Property) obj).getValue());
    }
  }

  /**
   * @return the stored name in the Property.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the stored value in the Property.
   */
  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getValue());
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getName(), getValue()});
  }

  /**
   * Sets the name.
   * @param name the value to set name to.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the value.
   * @param value the value to set.
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * @return a String representation of the current Property. Name and value are separated with a
   *         default value separator "=".
   *         <p>
   *         E.g A Property with {@code Name} and {@code Value} set would be represented as:
   *         {@code Name=Value}
   *         </p>
   */
  @Override
  public String toString() {
    return name + BeeConst.DEFAULT_VALUE_SEPARATOR + value;
  }
}
