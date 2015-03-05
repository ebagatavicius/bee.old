package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles deletion of multiple rows event.
 */

public class MultiDeleteEvent extends ModificationEvent<MultiDeleteEvent.Handler> {

  /**
   * Requires implementing classes to have a method to handle multiple row deletion event.
   */

  public interface Handler {
    void onMultiDelete(MultiDeleteEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents em, String viewName, Collection<RowInfo> rows) {
    Assert.notNull(em);
    Assert.notEmpty(viewName);
    Assert.notEmpty(rows);

    em.fireModificationEvent(new MultiDeleteEvent(viewName, rows), Locality.ENTANGLED);
  }

  public static void forward(Handler handler, String viewName, Collection<RowInfo> rows) {
    Assert.notNull(handler);
    handler.onMultiDelete(new MultiDeleteEvent(viewName, rows));
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private String viewName;
  private Set<RowInfo> rows;

  private MultiDeleteEvent(String viewName, Collection<RowInfo> rows) {
    this.viewName = viewName;
    this.rows = new HashSet<>(rows);
  }

  MultiDeleteEvent() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    this.viewName = arr[0];
    this.rows = new HashSet<>();

    String[] rowInfos = Codec.beeDeserializeCollection(arr[1]);
    if (!ArrayUtils.isEmpty(rowInfos)) {
      for (String ri : rowInfos) {
        rows.add(RowInfo.restore(ri));
      }
    }
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public Kind getKind() {
    return Kind.DELETE_MULTI;
  }

  public Set<Long> getRowIds() {
    Set<Long> ids = new HashSet<>();

    if (rows != null) {
      for (RowInfo rowInfo : rows) {
        ids.add(rowInfo.getId());
      }
    }

    return ids;
  }

  public Set<RowInfo> getRows() {
    return rows;
  }

  public int getSize() {
    return rows.size();
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
  public String serialize() {
    Object[] arr = new Object[] {getViewName(), getRows()};
    return Codec.beeSerialize(arr);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMultiDelete(this);
  }
}
