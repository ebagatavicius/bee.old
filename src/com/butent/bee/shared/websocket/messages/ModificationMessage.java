package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.event.ModificationEvent;

public class ModificationMessage extends Message {

  private ModificationEvent<?> event;

  public ModificationMessage(ModificationEvent<?> event) {
    this();
    this.event = event;
  }

  ModificationMessage() {
    super(Type.MODIFICATION);
  }

  @Override
  public String brief() {
    if (getEvent() == null) {
      return BeeConst.NULL;
    } else {
      return getEvent().brief();
    }
  }

  public ModificationEvent<?> getEvent() {
    return event;
  }

  @Override
  public boolean isValid() {
    return getEvent() != null;
  }

  @Override
  public String toString() {
    if (getEvent() == null) {
      return BeeConst.NULL;
    } else {
      return getEvent().toString();
    }
  }

  @Override
  protected void deserialize(String s) {
    setEvent(ModificationEvent.decode(s));
  }

  @Override
  protected String serialize() {
    return (getEvent() == null) ? null : getEvent().encode();
  }

  private void setEvent(ModificationEvent<?> event) {
    this.event = event;
  }
}
