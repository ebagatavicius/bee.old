package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowId;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Collections;

/**
 * Handles an event when a cell value is updated in table based user interface components.
 */

public class CellUpdateEvent extends ModificationEvent<CellUpdateEvent.Handler>
    implements HasRowId, HasViewName {

  @FunctionalInterface
  public interface Handler {
    void onCellUpdate(CellUpdateEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents em, String viewName, long rowId, long version,
      CellSource source, String value) {

    Assert.notNull(em);
    Assert.notEmpty(viewName);
    Assert.isTrue(DataUtils.isId(rowId));
    Assert.notNull(source);

    em.fireModificationEvent(new CellUpdateEvent(viewName, rowId, version, source, value),
        Locality.ENTANGLED);
  }

  public static void forward(Handler handler, String viewName, long rowId, long version,
      CellSource source, String value) {

    Assert.notNull(handler);
    handler.onCellUpdate(new CellUpdateEvent(viewName, rowId, version, source, value));
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private String viewName;

  private long rowId;
  private long version;

  private CellSource source;

  private String value;

  private CellUpdateEvent(String viewName, long rowId, long version, CellSource source,
      String value) {

    this.viewName = viewName;

    this.rowId = rowId;
    this.version = version;

    this.source = Assert.notNull(source);

    this.value = value;
  }

  CellUpdateEvent() {
  }

  public boolean applyTo(BeeRowSet rowSet) {
    Assert.notNull(rowSet);

    IsRow row = rowSet.getRowById(getRowId());
    if (row == null) {
      return false;
    } else {
      return applyTo(row);
    }
  }

  public boolean applyTo(IsRow row) {
    Assert.notNull(row);

    if (source.set(row, value)) {
      row.setVersion(getVersion());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String brief() {
    return BeeUtils.joinWords(getViewName(), getRowId(), getSourceName(), getValue());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 5);

    int i = 0;

    this.viewName = arr[i++];
    this.rowId = BeeUtils.toLong(arr[i++]);
    this.version = BeeUtils.toLong(arr[i++]);
    this.source = CellSource.restore(arr[i++]);
    this.value = arr[i];
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public CellSource getCellSource() {
    return source;
  }

  @Override
  public Kind getKind() {
    return Kind.UPDATE_CELL;
  }

  @Override
  public long getRowId() {
    return rowId;
  }

  public String getSourceName() {
    return source.getName();
  }

  public String getValue() {
    return value;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public Collection<String> getViewNames() {
    return Collections.singleton(viewName);
  }

  public boolean hasColumn() {
    return source.hasColumn();
  }

  public boolean hasSource(String name) {
    return BeeUtils.same(name, getSourceName());
  }

  @Override
  public boolean hasView(String view) {
    return BeeUtils.same(view, getViewName());
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[] {getViewName(), getRowId(), getVersion(), getCellSource(), value};
    return Codec.beeSerialize(arr);
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(getKind(), getViewName(), getRowId(), getVersion(), getSourceName(),
        getValue());
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onCellUpdate(this);
  }
}
