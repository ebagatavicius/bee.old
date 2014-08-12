package com.butent.bee.client;

import com.butent.bee.client.ui.HandlesHistory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;

public abstract class Place implements HandlesHistory {

  private final String id;

  public Place(IdentifiableWidget widget) {
    this(Assert.notNull(widget).getId());
  }

  public Place(String id) {
    super();
    this.id = Assert.notEmpty(id);
  }

  public abstract boolean activate();

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Place) ? id.equals(((Place) obj).getId()) : false;
  }

  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean onHistory(Place place, boolean forward) {
    return false;
  }
}