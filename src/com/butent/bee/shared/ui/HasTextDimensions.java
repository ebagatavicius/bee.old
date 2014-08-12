package com.butent.bee.shared.ui;

/**
 * Requires implementing classes to have methods to get and set parameters for character width and
 * visible lines.
 */

public interface HasTextDimensions extends HasVisibleLines {

  String ATTR_CHARACTER_WIDTH = "characterWidth";

  int getCharacterWidth();

  void setCharacterWidth(int width);
}
