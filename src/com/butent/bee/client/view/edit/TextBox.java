package com.butent.bee.client.view.edit;

public interface TextBox {

  int getCursorPos();

  String getSelectedText();

  int getSelectionLength();

  String getText();

  void selectAll();

  void setCursorPos(int pos);

  void setText(String text);
}
