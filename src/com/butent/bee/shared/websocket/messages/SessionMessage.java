package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.websocket.SessionUser;

public class SessionMessage extends Message {

  public static SessionMessage close(SessionUser su) {
    return new SessionMessage(su, State.CLOSED);
  }

  public static SessionMessage open(SessionUser su) {
    return new SessionMessage(su, State.OPEN);
  }

  private SessionUser sessionUser;
  private State state;

  private SessionMessage(SessionUser sessionUser, State state) {
    this();

    this.sessionUser = sessionUser;
    this.state = state;
  }

  SessionMessage() {
    super(Type.SESSION);
  }

  @Override
  public String brief() {
    return string(getState());
  }

  public SessionUser getSessionUser() {
    return sessionUser;
  }

  public State getState() {
    return state;
  }

  public boolean isClosed() {
    return getState() == State.CLOSED;
  }

  public boolean isOpen() {
    return getState() == State.OPEN;
  }

  @Override
  public boolean isValid() {
    return getSessionUser() != null && getState() != null;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "sessionUser",
        (getSessionUser() == null) ? null : BeeUtils.bracket(getSessionUser().toString()),
        "state", string(getState()));
  }

  @Override
  protected void deserialize(String s) {
    Pair<String, String> pair = Pair.restore(s);

    this.sessionUser = SessionUser.restore(pair.getA());
    this.state = Codec.unpack(State.class, pair.getB());
  }

  @Override
  protected String serialize() {
    return Pair.of(getSessionUser(), Codec.pack(getState())).serialize();
  }
}
