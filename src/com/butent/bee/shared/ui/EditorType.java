package com.butent.bee.shared.ui;

import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains a list of possible data editing user interface components.
 */

public enum EditorType {
  LIST("list"),
  DATE("date"),
  DATE_TIME("dateTime"),
  STRING("string"),
  AREA("area", 300, 100),
  TEXT("text", 300, 120),
  RICH("rich", 550, 200, 550, 150),
  NUMBER("number"),
  INTEGER("integer"),
  SLIDER("slider"),
  SPINNER("spinner"),
  LONG("long"),
  SELECTOR("selector", null, null, 120, null),
  TOGGLE("toggle"),
  TIME("time"),
  TIME_OF_DAY("timeOfDay"),
  COLOR("color", null, null, 100, null);

  public static EditorType getByTypeCode(String code) {
    if (code == null || code.isEmpty()) {
      return null;
    }
    for (EditorType type : EditorType.values()) {
      if (BeeUtils.same(type.getTypeCode(), code)) {
        return type;
      }
    }
    return null;
  }

  private final String typeCode;

  private final Integer defaultWidth;
  private final Integer defaultHeight;
  private final Integer minWidth;
  private final Integer minHeight;

  private EditorType(String typeCode) {
    this(typeCode, null, null, null, null);
  }

  private EditorType(String typeCode, Integer defaultWidth, Integer defaultHeight) {
    this(typeCode, defaultWidth, defaultHeight, null, null);
  }

  private EditorType(String typeCode, Integer defaultWidth, Integer defaultHeight,
      Integer minWidth, Integer minHeight) {
    this.typeCode = typeCode;
    this.defaultWidth = defaultWidth;
    this.defaultHeight = defaultHeight;
    this.minWidth = minWidth;
    this.minHeight = minHeight;
  }

  public Integer getDefaultHeight() {
    return defaultHeight;
  }

  public Integer getDefaultWidth() {
    return defaultWidth;
  }

  public Integer getMinHeight() {
    return minHeight;
  }

  public Integer getMinWidth() {
    return minWidth;
  }

  public String getTypeCode() {
    return typeCode;
  }
}
