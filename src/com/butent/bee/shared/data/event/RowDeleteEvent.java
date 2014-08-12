package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowId;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

/**
 * Handles single row deletion event.
 */

public class RowDeleteEvent extends ModificationEvent<RowDeleteEvent.Handler> implements HasRowId {

  /**
   * Requires implementing classes to have a method to handle single row deletion event.
   */

  public interface Handler {
    void onRowDelete(RowDeleteEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents eventManager, String viewName, long rowId) {
    Assert.notNull(eventManager);
    Assert.notEmpty(viewName);
    Assert.isTrue(DataUtils.isId(rowId));

    eventManager.fireModificationEvent(new RowDeleteEvent(viewName, rowId), Locality.ENTANGLED);
  }

  public static void forward(Handler handler, String viewName, long rowId) {
    Assert.notNull(handler);
    handler.onRowDelete(new RowDeleteEvent(viewName, rowId));
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private String viewName;
  private long rowId;

  private RowDeleteEvent(String viewName, long rowId) {
    this.viewName = viewName;
    this.rowId = rowId;
  }

  RowDeleteEvent() {
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public long getRowId() {
    return rowId;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public boolean hasView(String view) {
    return BeeUtils.same(view, getViewName());
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowDelete(this);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    this.viewName = arr[0];
    this.rowId = BeeUtils.toLong(arr[1]);
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[] {getViewName(), getRowId()};
    return Codec.beeSerialize(arr);
  }

  @Override
  public Kind getKind() {
    return Kind.DELETE_ROW;
  }
}
