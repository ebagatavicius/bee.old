package com.butent.bee.client.view.navigation;

import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.shared.ui.NavigationOrigin;

public interface HasPaging {

  HandlerRegistration addScopeChangeHandler(ScopeChangeEvent.Handler handler);

  int getPageSize();

  int getPageStart();

  int getRowCount();

  void setPageSize(int pageSize, boolean fireScopeChange);

  void setPageStart(int pageStart, boolean fireScopeChange, boolean fireDataRequest,
      NavigationOrigin origin);

  void setRowCount(int count, boolean fireScopeChange);
}
