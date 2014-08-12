package com.butent.bee.client.layout;

import com.google.gwt.event.dom.client.HasClickHandlers;

public interface IsHtmlTable extends HasClickHandlers {

  void setBorderSpacing(int spacing);

  void setDefaultCellClasses(String classes);

  void setDefaultCellStyles(String styles);
}
