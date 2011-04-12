package com.butent.bee.shared.utils;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Transformable;

public class Property implements Comparable<Property>, Transformable {
  public static String[] HEADERS = new String[]{"Property", "Value"};
  public static int HEADER_COUNT = HEADERS.length;

  private String name;
  private String value;

  /**
   * Creates a Property with specified {@code name} and {@code value} values.
   * 
   * @param name the {@code name} to set for the Property
   * @param value the {@code value} to set for the Property
   */
  public Property(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Compares {@code oth} with the current Property. Only names are compared.
   * 
   * @return 0 if values are equal, -1 if {@code oth} name value is greater, 1 if the current
   *         Property name value is greater than {@code oth} name value.
   */
  public int compareTo(Property oth) {
    if (oth == null) {
      return BeeConst.COMPARE_MORE;
    }
    return BeeUtils.compare(getName(), oth.getName());
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

  /**
   * Sets the name.
   * 
   * @param name the value to set name to.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the value.
   * 
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

  /**
   * @return a String representation of the Object. See {@link #toString()}.
   */
  public String transform() {
    return toString();
  }
}
