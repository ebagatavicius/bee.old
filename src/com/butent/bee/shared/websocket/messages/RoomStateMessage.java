package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ChatRoom;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class RoomStateMessage extends Message {

  public static RoomStateMessage add(ChatRoom room) {
    return (room == null) ? null : new RoomStateMessage(room, State.NEW);
  }

  public static RoomStateMessage load(ChatRoom room) {
    return (room == null) ? null : new RoomStateMessage(room, State.LOADING);
  }

  public static RoomStateMessage remove(ChatRoom room) {
    return (room == null) ? null : new RoomStateMessage(room, State.REMOVED);
  }

  public static RoomStateMessage update(ChatRoom room) {
    return (room == null) ? null : new RoomStateMessage(room, State.UPDATING);
  }

  private ChatRoom room;
  private State state;

  private RoomStateMessage(ChatRoom room, State state) {
    this();

    this.room = room;
    this.state = state;
  }

  RoomStateMessage() {
    super(Type.ROOM_STATE);
  }

  @Override
  public String brief() {
    return string(getState());
  }

  public ChatRoom getRoom() {
    return room;
  }

  public State getState() {
    return state;
  }

  public boolean isLoading() {
    return getState() == State.LOADING;
  }

  public boolean isNew() {
    return getState() == State.NEW;
  }

  public boolean isRemoved() {
    return getState() == State.REMOVED;
  }

  public boolean isUpdated() {
    return getState() == State.UPDATING;
  }

  @Override
  public boolean isValid() {
    return getRoom() != null && getState() != null;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("state", string(getState()),
        "room", (getRoom() == null) ? null : room.toString());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setRoom(ChatRoom.restore(arr[0]));
    setState(Codec.unpack(State.class, arr[1]));
  }

  @Override
  protected String serialize() {
    List<Object> values = Lists.newArrayList();

    values.add(getRoom());
    values.add(Codec.pack(getState()));

    return Codec.beeSerialize(values);
  }

  private void setRoom(ChatRoom room) {
    this.room = room;
  }

  private void setState(State state) {
    this.state = state;
  }
}
