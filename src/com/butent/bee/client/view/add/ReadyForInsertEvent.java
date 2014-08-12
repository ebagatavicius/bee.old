package com.butent.bee.client.view.add;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.client.data.RowCallback;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.RowChildren;

import java.util.Collection;
import java.util.List;

/**
 * Gets handler type for ready for insertion event and registers the handler.
 */

public class ReadyForInsertEvent extends GwtEvent<ReadyForInsertEvent.Handler> implements
    Consumable {

  /**
   * Requires implementing classes to have a method for ready for insertion event.
   */

  public interface Handler extends EventHandler {
    void onReadyForInsert(ReadyForInsertEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final List<BeeColumn> columns;
  private final List<String> values;
  private final Collection<RowChildren> children;
  private final RowCallback callback;

  private final String sourceId;

  private boolean consumed;

  public ReadyForInsertEvent(List<BeeColumn> columns, List<String> values,
      Collection<RowChildren> children, RowCallback callback, String sourceId) {
    super();
    this.columns = columns;
    this.values = values;
    this.children = children;
    this.callback = callback;
    this.sourceId = sourceId;
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

  public Collection<RowChildren> getChildren() {
    return children;
  }

  public List<BeeColumn> getColumns() {
    return columns;
  }

  public String getSourceId() {
    return sourceId;
  }

  public List<String> getValues() {
    return values;
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
    handler.onReadyForInsert(this);
  }
}
