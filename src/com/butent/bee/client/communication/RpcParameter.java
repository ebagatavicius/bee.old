package com.butent.bee.client.communication;

import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

/**
 * Contains necessary properties of a particular RPC parameter and methods to get and set them.
 */
public class RpcParameter {
  /**
   * Contains available sections of RPC parameters.
   */
  public enum Section {
    QUERY, HEADER, DATA
  }

  private final Section section;
  private String name;
  private final String value;

  public RpcParameter(Section section, String name, String value) {
    super();
    this.section = section;
    this.name = name;
    this.value = value;
  }

  public RpcParameter copy() {
    return new RpcParameter(getSection(), getName(), getValue());
  }

  public String getName() {
    return name;
  }

  public Section getSection() {
    return section;
  }

  public String getValue() {
    return value;
  }

  public boolean isNamed() {
    return !BeeUtils.isEmpty(getName());
  }

  public boolean isReady() {
    return isValid() && isNamed();
  }

  public boolean isValid() {
    return getSection() != null && !BeeUtils.isEmpty(getValue());
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return NameUtils.addName(BeeUtils.joinWords(getSection(), getName()), getValue());
  }
}
