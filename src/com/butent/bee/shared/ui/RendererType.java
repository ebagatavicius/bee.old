package com.butent.bee.shared.ui;

import com.butent.bee.shared.utils.BeeUtils;

public enum RendererType {
  LIST("list", true),
  MAP("map", true),
  RANGE("range", true),
  ENUM("enum", true),
  JOIN("join", false),
  TOKEN("token", false),
  FLAG("flag", true),
  STAR("star", true),
  ATTACHMENT("attachment", true),
  FILE_ICON("fileIcon", true),
  FILE_SIZE("fileSize", true),
  PHOTO("photo", true),
  MAIL("mail", false),
  URL("url", true),
  IMAGE("image", true),
  TOTAL("total", false),
  VAT("vat", false),
  DISCOUNT("discount", false);

  public static RendererType getByTypeCode(String code) {
    if (code == null || code.isEmpty()) {
      return null;
    }
    for (RendererType type : RendererType.values()) {
      if (BeeUtils.same(type.getTypeCode(), code)) {
        return type;
      }
    }
    return null;
  }

  private final String typeCode;
  private final boolean requiresSource;

  private RendererType(String typeCode, boolean requiresSource) {
    this.typeCode = typeCode;
    this.requiresSource = requiresSource;
  }

  public String getTypeCode() {
    return typeCode;
  }

  public boolean requiresSource() {
    return requiresSource;
  }
}
