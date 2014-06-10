package com.butent.bee.shared;

import com.butent.bee.shared.data.StringRow;

/**
 * Enables creating and cloning string row array data structures.
 */

public class StringRowArray extends ArraySequence<StringRow> {

  /**
   * Creates new string array with new values.
   * 
   * @param values array of string values
   */
  public StringRowArray(StringRow[] values) {
    super(values);
  }
}
