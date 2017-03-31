package com.butent.bee.client.data;

import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.event.logical.DataRequestEvent;
import com.butent.bee.client.event.logical.HasActiveRowChangeHandlers;
import com.butent.bee.client.event.logical.HasSelectionCountChangeHandlers;
import com.butent.bee.client.event.logical.HasSortHandlers;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.view.edit.HasEditState;
import com.butent.bee.client.view.navigation.HasPaging;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;

import java.util.List;

/**
 * Requires that implementing classes would have data rows and sort handlers.
 */

public interface HasDataTable extends HasSortHandlers, HandlesDeleteEvents, HandlesUpdateEvents,
    HasSelectionCountChangeHandlers, HasEditState, HasActiveRowChangeHandlers, HasDataRows,
    HasPaging {

  HandlerRegistration addDataReceivedHandler(DataReceivedEvent.Handler handler);

  HandlerRegistration addDataRequestHandler(DataRequestEvent.Handler handler);

  HandlerRegistration addRowCountChangeHandler(RowCountChangeEvent.Handler handler);

  void preserveActiveRow(List<? extends IsRow> rows);

  void refresh();

  boolean removeRowById(long rowId);

  void reset();

  void setRowData(List<? extends IsRow> rows, boolean refresh);
}
