package com.butent.bee.client.view.edit;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.client.data.RowCallback;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

/**
 * Remembers old and new values of the field being updated to make further validations with them.
 */

public class ReadyForUpdateEvent extends GwtEvent<ReadyForUpdateEvent.Handler> implements
    Consumable {

  /**
   * Requires to have a method to handle read for update event.
   */

  public interface Handler extends EventHandler {
    boolean onReadyForUpdate(ReadyForUpdateEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final IsRow rowValue;
  private final IsColumn column;

  private final String oldValue;
  private final String newValue;

  private final boolean rowMode;

  private final RowCallback callback;

  private boolean consumed;

  public ReadyForUpdateEvent(IsRow rowValue, IsColumn column, String oldValue, String newValue,
      boolean rowMode, RowCallback callback) {
    this.rowValue = rowValue;
    this.column = column;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.rowMode = rowMode;
    this.callback = callback;
  }

  @Override
  public void consume() {
    setConsumed(true);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public RowCallback getCallback() {
    return callback;
  }

  public IsColumn getColumn() {
    return column;
  }

  public String getNewValue() {
    return newValue;
  }

  public String getOldValue() {
    return oldValue;
  }

  public BeeRowSet getRowSet(String viewName, List<BeeColumn> columns) {
    List<BeeColumn> updatedColumns = Lists.newArrayList();
    List<String> oldValues = Lists.newArrayList();
    List<String> newValues = Lists.newArrayList();

    BeeColumn firstColumn = DataUtils.getColumn(getColumn().getId(), columns);
    if (firstColumn == null) {
      firstColumn = new BeeColumn(getColumn().getType(), getColumn().getId());
    }

    updatedColumns.add(firstColumn);
    oldValues.add(getOldValue());
    newValues.add(getNewValue());

    Map<Integer, String> shadow = getRowValue().getShadow();

    if (!BeeUtils.isEmpty(shadow) && !BeeUtils.isEmpty(columns)) {
      for (Map.Entry<Integer, String> entry : shadow.entrySet()) {
        int index = entry.getKey();

        BeeColumn shadowColumn = columns.get(index);
        String shadowOld = entry.getValue();
        String shadowNew = getRowValue().getString(index);

        if (!DataUtils.contains(updatedColumns, shadowColumn.getId())
            && !BeeUtils.equalsTrimRight(shadowOld, shadowNew)) {
          updatedColumns.add(shadowColumn);
          oldValues.add(shadowOld);
          newValues.add(shadowNew);
        }
      }
    }

    BeeRow row = new BeeRow(getRowValue().getId(), getRowValue().getVersion(), oldValues);
    for (int i = 0; i < newValues.size(); i++) {
      row.preliminaryUpdate(i, newValues.get(i));
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, updatedColumns);
    rowSet.addRow(row);

    return rowSet;
  }

  public IsRow getRowValue() {
    return rowValue;
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

  @Override
  protected void dispatch(Handler handler) {
    handler.onReadyForUpdate(this);
  }
}
