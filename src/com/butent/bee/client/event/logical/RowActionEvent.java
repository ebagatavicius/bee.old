package com.butent.bee.client.event.logical;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.utils.BeeUtils;

public final class RowActionEvent extends Event<RowActionEvent.Handler> implements DataEvent,
    Consumable {

  public interface Handler {
    void onRowAction(RowActionEvent event);
  }

  private enum Kind {
    CELL_CLICK, CREATE_ROW, EDIT_ROW, OPEN_FAVORITE;
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static boolean fireCellClick(String viewName, IsRow row, String options) {
    RowActionEvent event = new RowActionEvent(Kind.CELL_CLICK, viewName, row, options);
    BeeKeeper.getBus().fireEvent(event);
    return !event.isConsumed();
  }

  public static boolean fireCreateRow(String viewName, IsRow row, String options) {
    RowActionEvent event = new RowActionEvent(Kind.CREATE_ROW, viewName, row, options);
    BeeKeeper.getBus().fireEvent(event);
    return !event.isConsumed();
  }

  public static boolean fireEditRow(String viewName, IsRow row) {
    RowActionEvent event = new RowActionEvent(Kind.EDIT_ROW, viewName, row, null);
    BeeKeeper.getBus().fireEvent(event);
    return !event.isConsumed();
  }

  public static boolean fireOpenFavorite(String viewName, Long rowId, String options) {
    RowActionEvent event = new RowActionEvent(Kind.OPEN_FAVORITE, viewName, rowId, options);
    BeeKeeper.getBus().fireEvent(event);
    return !event.isConsumed();
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private final Kind kind;

  private final String viewName;
  private final IsRow row;
  private final Long rowId;

  private final String options;

  private boolean consumed;

  private RowActionEvent(Kind kind, String viewName, IsRow row, String options) {
    this(kind, viewName, row, (row == null) ? null : row.getId(), options);
  }

  private RowActionEvent(Kind kind, String viewName, Long rowId, String options) {
    this(kind, viewName, null, rowId, options);
  }

  private RowActionEvent(Kind kind, String viewName, IsRow row, Long rowId, String options) {
    this.viewName = viewName;
    this.row = row;
    this.rowId = rowId;
    this.kind = kind;
    this.options = options;
  }

  @Override
  public void consume() {
    setConsumed(true);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getOptions() {
    return options;
  }

  public IsRow getRow() {
    return row;
  }

  public Long getRowId() {
    return rowId;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public boolean hasAnyView(String... views) {
    if (views != null) {
      for (String view : views) {
        if (hasView(view)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasRow() {
    return row != null;
  }

  @Override
  public boolean hasView(String view) {
    return BeeUtils.same(view, getViewName());
  }

  public boolean isCellClick() {
    return kind == Kind.CELL_CLICK;
  }

  public boolean isCreateRow() {
    return kind == Kind.CREATE_ROW;
  }

  public boolean isEditRow() {
    return kind == Kind.EDIT_ROW;
  }

  public boolean isOpenFavorite() {
    return kind == Kind.OPEN_FAVORITE;
  }

  @Override
  public boolean isConsumed() {
    return consumed;
  }

  @Override
  public void setConsumed(boolean consumed) {
    this.consumed = consumed;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowAction(this);
  }
}
