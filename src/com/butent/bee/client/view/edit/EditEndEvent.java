package com.butent.bee.client.view.edit;

import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;

/**
 * Handles edit event ending, gets old and new values of edited data.
 */

public class EditEndEvent {

  /**
   * Requires implementing methods to have a method to handle edit end.
   */

  public interface Handler {
    void onEditEnd(EditEndEvent event, HasEditEndHandler source);
  }

  public interface HasEditEndHandler {
  }

  private final IsRow rowValue;
  private final IsColumn column;

  private final String oldValue;
  private final String newValue;

  private final boolean rowMode;
  private final boolean hasRelation;

  private final Integer keyCode;
  private final boolean hasModifiers;

  private final String widgetId;

  public EditEndEvent(Integer keyCode, boolean hasModifiers, String widgetId) {
    this(null, null, null, null, false, false, keyCode, hasModifiers, widgetId);
  }

  public EditEndEvent(IsRow rowValue, IsColumn column, String oldValue, String newValue,
      boolean rowMode, boolean hasRelation, Integer keyCode, boolean hasModifiers,
      String widgetId) {
    this.rowValue = rowValue;
    this.column = column;
    this.oldValue = oldValue;
    this.newValue = newValue;

    this.rowMode = rowMode;
    this.hasRelation = hasRelation;

    this.keyCode = keyCode;
    this.hasModifiers = hasModifiers;
    this.widgetId = widgetId;
  }

  public IsColumn getColumn() {
    return column;
  }

  public Integer getKeyCode() {
    return keyCode;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getOldValue() {
    return oldValue;
  }

  public IsRow getRowValue() {
    return rowValue;
  }

  public String getWidgetId() {
    return widgetId;
  }

  public boolean hasModifiers() {
    return hasModifiers;
  }

  public boolean hasRelation() {
    return hasRelation;
  }

  public boolean isRowMode() {
    return rowMode;
  }
}
