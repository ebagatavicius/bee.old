package com.butent.bee.shared.imports;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

public final class ImportProperty {
  private final String name;
  private final String caption;
  private final boolean isDataProperty;
  private String relTable;
  private String relField;

  public ImportProperty(String name, String caption, boolean isDataProperty) {
    this.name = Assert.notEmpty(name);
    this.caption = BeeUtils.notEmpty(caption, name);
    this.isDataProperty = isDataProperty;
  }

  public String getCaption() {
    return caption;
  }

  public String getName() {
    return name;
  }

  public String getRelField() {
    return relField;
  }

  public String getRelTable() {
    return relTable;
  }

  public boolean isDataProperty() {
    return isDataProperty;
  }

  public void setRelField(String relField) {
    this.relField = relField;
  }

  public void setRelTable(String relTable) {
    this.relTable = relTable;
  }
}