package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class ShowMessage extends Message {

  public enum Subject implements HasCaption {
    SESSION("Session"),
    OPEN_SESSIONS("Open Sessions"),
    ENDPOINT("Server Endpoint"),
    ROOMS("Server Rooms");

    private final String caption;

    private Subject(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static ShowMessage showEndpoint() {
    return new ShowMessage(Subject.ENDPOINT);
  }

  public static ShowMessage showOpenSessions() {
    return new ShowMessage(Subject.OPEN_SESSIONS);
  }

  public static ShowMessage showRooms() {
    return new ShowMessage(Subject.ROOMS);
  }

  public static ShowMessage showSessionInfo() {
    return new ShowMessage(Subject.SESSION);
  }

  private Subject subject;

  ShowMessage() {
    super(Type.SHOW);
  }

  private ShowMessage(Subject subject) {
    this();
    this.subject = subject;
  }

  @Override
  public String brief() {
    return string(getSubject());
  }

  public Subject getSubject() {
    return subject;
  }

  @Override
  public boolean isValid() {
    return getSubject() != null;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "subject", string(getSubject()));
  }

  @Override
  protected void deserialize(String s) {
    this.subject = Codec.unpack(Subject.class, s);
  }

  @Override
  protected String serialize() {
    return Codec.pack(getSubject());
  }
}
