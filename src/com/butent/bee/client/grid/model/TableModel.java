package com.butent.bee.client.grid.model;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;

import com.butent.bee.client.grid.event.HasRowCountChangeHandlers;
import com.butent.bee.client.grid.event.RowCountChangeEvent;
import com.butent.bee.client.grid.event.RowCountChangeHandler;
import com.butent.bee.client.grid.model.TableModelHelper.Request;
import com.butent.bee.client.grid.model.TableModelHelper.Response;

public abstract class TableModel implements HasRowCountChangeHandlers {

  public static interface Callback {
    void onFailure(Throwable caught);
    void onRowsReady(Request request, Response response);
  }

  public static final int ALL_ROWS = -1;
  public static final int UNKNOWN_ROW_COUNT = -1;

  private SimpleEventBus handlers = new SimpleEventBus();

  private int rowCount = UNKNOWN_ROW_COUNT;

  public HandlerRegistration addRowCountChangeHandler(RowCountChangeHandler handler) {
    return addHandler(RowCountChangeEvent.getType(), handler);
  }

  public void fireEvent(GwtEvent<?> event) {
    handlers.fireEvent(event);
  }

  public int getRowCount() {
    return rowCount;
  }

  public abstract void requestRows(Request request, Callback callback);

  public void setRowCount(int rowCount) {
    if (this.rowCount != rowCount) {
      int oldRowCount = this.rowCount;
      this.rowCount = rowCount;
      fireEvent(new RowCountChangeEvent(oldRowCount, rowCount));
    }
  }

  protected <H extends EventHandler> HandlerRegistration addHandler(
      GwtEvent.Type<H> key, final H handler) {
    return handlers.addHandler(key, handler);
  }

  protected final SimpleEventBus getHandlerManager() {
    return handlers;
  }
}
