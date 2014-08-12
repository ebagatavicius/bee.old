package com.butent.bee.shared.utils;

import javax.xml.bind.annotation.XmlEnumValue;

public enum NullOrdering {
  @XmlEnumValue("first")
  NULLS_FIRST,
  @XmlEnumValue("last")
  NULLS_LAST;

  public static final NullOrdering DEFAULT = NULLS_LAST;
}
