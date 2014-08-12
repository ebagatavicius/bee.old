package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.screen.Favorites;
import com.butent.bee.client.screen.Favorites.Group;
import com.butent.bee.shared.Assert;

public class BookmarkEvent extends Event<BookmarkEvent.Handler> {

  public interface Handler extends EventHandler {
    void onBookmark(BookmarkEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private final Favorites.Group group;
  private final long rowId;

  public BookmarkEvent(Group group, long rowId) {
    super();
    this.group = group;
    this.rowId = rowId;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Favorites.Group getGroup() {
    return group;
  }

  public long getRowId() {
    return rowId;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onBookmark(this);
  }
}
