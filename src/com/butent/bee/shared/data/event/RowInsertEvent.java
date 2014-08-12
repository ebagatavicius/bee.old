package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.HasRowId;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

/**
 * Handles an event when a row value is inserted in table based user interface components.
 */

public class RowInsertEvent extends ModificationEvent<RowInsertEvent.Handler> implements HasRowId {

  /**
   * Requires implementing classes to have a method to handle row insert event.
   */

  public interface Handler {
    void onRowInsert(RowInsertEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents eventManager, String viewName, BeeRow row,
      String sourceId) {
    Assert.notNull(eventManager);
    Assert.notEmpty(viewName);
    Assert.notNull(row);

    eventManager.fireModificationEvent(new RowInsertEvent(viewName, row, sourceId),
        Locality.ENTANGLED);
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);

    return eventBus.addHandler(TYPE, handler);
  }

  private String viewName;
  private BeeRow row;

  private transient String sourceId;

  private RowInsertEvent(String viewName, BeeRow row, String sourceId) {
    this.viewName = viewName;
    this.row = row;
    this.sourceId = sourceId;
  }

  RowInsertEvent() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    this.viewName = arr[0];
    this.row = BeeRow.restore(arr[1]);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public Kind getKind() {
    return Kind.INSERT;
  }

  public BeeRow getRow() {
    return row;
  }

  @Override
  public long getRowId() {
    return getRow().getId();
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public boolean hasSourceId(String id) {
    return !BeeUtils.isEmpty(id) && BeeUtils.same(id, getSourceId());
  }

  @Override
  public boolean hasView(String view) {
    return !BeeUtils.isEmpty(getViewName()) && BeeUtils.same(view, getViewName());
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[] {getViewName(), getRow()};
    return Codec.beeSerialize(arr);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowInsert(this);
  }
}
