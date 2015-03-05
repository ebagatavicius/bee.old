package com.butent.bee.client.view.edit;

import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;

/**
 * Handles edit event ending, gets old and new values of edited data.
 */

public class EditEndEvent implements Consumable {

  /**
   * Requires implementing methods to have a method to handle edit end.
   */

  public interface Handler {
    void onEditEnd(EditEndEvent event, Object source);
  }

  private final IsRow rowValue;
  private final IsColumn column;

  private String oldValue;
  private String newValue;

  private final boolean rowMode;
  private final boolean hasRelation;

  private final Integer keyCode;
  private final boolean hasModifiers;

  private final String widgetId;

  private boolean consumed;

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

  @Override
  public void consume() {
    setConsumed(true);
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

  @Override
  public boolean isConsumed() {
    return consumed;
  }

  public boolean isRowMode() {
    return rowMode;
  }

  @Override
  public void setConsumed(boolean consumed) {
    this.consumed = consumed;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }
}
