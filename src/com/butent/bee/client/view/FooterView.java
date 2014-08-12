package com.butent.bee.client.view;

import com.butent.bee.client.event.logical.SelectionCountChangeEvent;
import com.butent.bee.client.output.Printable;

public interface FooterView extends View, SelectionCountChangeEvent.Handler, Printable {

  void create(int maxRowCount, boolean addPaging, boolean showPageSize, boolean addSearch);

  int getHeight();
}
