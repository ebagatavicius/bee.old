package com.butent.bee.client.ui;

import com.butent.bee.shared.css.CssUnit;

public interface HasDimensions {

  String ATTR_WIDTH = "width";
  String ATTR_WIDTH_UNIT = "widthUnit";
  String ATTR_HEIGHT = "height";
  String ATTR_HEIGHT_UNIT = "heightUnit";

  CssUnit getHeightUnit();

  Double getHeightValue();

  CssUnit getWidthUnit();

  Double getWidthValue();

  void setHeightUnit(CssUnit heightUnit);

  void setHeightValue(Double heightValue);

  void setWidthUnit(CssUnit widthUnit);

  void setWidthValue(Double widthValue);
}
