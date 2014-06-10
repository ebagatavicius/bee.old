package com.butent.bee.client.data;

import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.event.logical.DataRequestEvent;
import com.butent.bee.client.event.logical.HasActiveRowChangeHandlers;
import com.butent.bee.client.event.logical.HasSelectionCountChangeHandlers;
import com.butent.bee.client.event.logical.HasSortHandlers;
import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.client.view.edit.HasEditState;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.ui.NavigationOrigin;

import java.util.List;

/**
 * Requires that implementing classes would have data rows and sort handlers.
 */

public interface HasDataTable extends HasSortHandlers, HandlesDeleteEvents, HandlesUpdateEvents,
    HasSelectionCountChangeHandlers, HasEditState, HasActiveRowChangeHandlers, HasDataRows {

  HandlerRegistration addDataRequestHandler(DataRequestEvent.Handler handler);
  
  HandlerRegistration addScopeChangeHandler(ScopeChangeEvent.Handler handler);
  
  int getPageSize();
  
  int getPageStart();

  int getRowCount();
  
  void preserveActiveRow(List<? extends IsRow> rows);

  void refresh();
  
  boolean removeRowById(long rowId);

  void reset();
  
  void setPageSize(int pageSize, boolean fireScopeChange);

  void setPageStart(int pageStart, boolean fireScopeChange, boolean fireDataRequest,
      NavigationOrigin origin);

  void setRowCount(int count, boolean fireScopeChange);
  
  void setRowData(List<? extends IsRow> rows, boolean refresh);
}
