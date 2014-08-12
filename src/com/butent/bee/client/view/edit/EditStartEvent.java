package com.butent.bee.client.view.edit;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.data.IsRow;

/**
 * Manages edit start event, gets column id, type, row value and other necessary parameters.
 */

public class EditStartEvent extends GwtEvent<EditStartEvent.Handler> implements Consumable {

  /**
   * Requires implementing methods to have a method to handle edit start.
   */

  public interface Handler extends EventHandler {
    void onEditStart(EditStartEvent event);
  }

  public static final int CLICK = 1;
  public static final int ENTER = 2;
  public static final int DELETE = 3;

  private static final Type<Handler> TYPE = new Type<>();

  public static int getStartKey(int keyCode) {
    int startKey;

    switch (keyCode) {
      case KeyCodes.KEY_ENTER:
        startKey = ENTER;
        break;
      case KeyCodes.KEY_DELETE:
        startKey = DELETE;
        break;
      default:
        startKey = BeeConst.UNDEF;
    }

    return startKey;
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  public static boolean isClickOrEnter(int code) {
    return code == CLICK || isEnter(code);
  }

  public static boolean isEnter(int code) {
    return code == ENTER || code == KeyCodes.KEY_ENTER;
  }

  private final IsRow rowValue;
  private final String columnId;

  private final Element sourceElement;
  private final int charCode;

  private final boolean readOnly;

  private boolean consumed;

  public EditStartEvent(IsRow rowValue, String columnId, Element sourceElement, int charCode,
      boolean readOnly) {
    this.rowValue = rowValue;
    this.columnId = columnId;
    this.sourceElement = sourceElement;
    this.charCode = charCode;
    this.readOnly = readOnly;
  }

  @Override
  public void consume() {
    setConsumed(true);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getCharCode() {
    return charCode;
  }

  public String getColumnId() {
    return columnId;
  }

  public IsRow getRowValue() {
    return rowValue;
  }

  public Element getSourceElement() {
    return sourceElement;
  }

  @Override
  public boolean isConsumed() {
    return consumed;
  }

  public boolean isDelete() {
    return charCode == DELETE;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public void setConsumed(boolean consumed) {
    this.consumed = consumed;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onEditStart(this);
  }
}
