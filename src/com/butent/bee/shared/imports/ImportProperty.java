package com.butent.bee.shared.imports;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

public final class ImportProperty {
  private final String name;
  private final String caption;
  private final boolean isDataProperty;
  private String relation;

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

  public String getRelation() {
    return relation;
  }

  public boolean isDataProperty() {
    return isDataProperty;
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }
}