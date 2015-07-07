package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.HasRowId;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Collections;

/**
 * Handles an event when a row value is updated in table based user interface components.
 */

public class RowUpdateEvent extends ModificationEvent<RowUpdateEvent.Handler> implements HasRowId,
    HasViewName {

  /**
   * Requires implementing classes to have a method to handle row update event.
   */

  public interface Handler {
    void onRowUpdate(RowUpdateEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents eventManager, String viewName, BeeRow row) {
    Assert.notNull(eventManager);
    Assert.notEmpty(viewName);
    Assert.notNull(row);

    eventManager.fireModificationEvent(new RowUpdateEvent(viewName, row), Locality.ENTANGLED);
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private String viewName;
  private BeeRow row;

  private RowUpdateEvent(String viewName, BeeRow row) {
    this.viewName = viewName;
    this.row = row;
  }

  RowUpdateEvent() {
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
    return Kind.UPDATE_ROW;
  }

  public BeeRow getRow() {
    return row;
  }

  @Override
  public long getRowId() {
    return getRow().getId();
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public Collection<String> getViewNames() {
    return Collections.singleton(viewName);
  }

  @Override
  public boolean hasView(String view) {
    return BeeUtils.same(view, getViewName());
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[] {getViewName(), getRow()};
    return Codec.beeSerialize(arr);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowUpdate(this);
  }
}
