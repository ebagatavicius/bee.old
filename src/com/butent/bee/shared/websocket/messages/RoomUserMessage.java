package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class RoomUserMessage extends Message {

  public static RoomUserMessage enter(Long roomId, Long userId) {
    if (DataUtils.isId(roomId) && DataUtils.isId(userId)) {
      return new RoomUserMessage(roomId, userId, true);
    } else {
      return null;
    }
  }

  public static RoomUserMessage leave(Long roomId, Long userId) {
    if (DataUtils.isId(roomId) && DataUtils.isId(userId)) {
      return new RoomUserMessage(roomId, userId, false);
    } else {
      return null;
    }
  }

  private long roomId;
  private long userId;

  private boolean join;

  private RoomUserMessage(long roomId, long userId, boolean join) {
    this();

    this.roomId = roomId;
    this.userId = userId;
    this.join = join;
  }

  RoomUserMessage() {
    super(Type.ROOM_USER);
  }

  @Override
  public String brief() {
    return BeeUtils.joinWords(getRoomId(), getUserId(), join());
  }

  public long getRoomId() {
    return roomId;
  }

  public long getUserId() {
    return userId;
  }

  @Override
  public boolean isValid() {
    return getRoomId() > 0 && DataUtils.isId(getUserId());
  }

  public boolean join() {
    return join;
  }

  public boolean quit() {
    return !join;
  }

  public void setJoin(boolean join) {
    this.join = join;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("room", Long.toString(getRoomId()),
        "user", Long.toString(getUserId()), "join", Boolean.toString(join()));
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 3);

    setRoomId(BeeUtils.toLong(arr[0]));
    setUserId(BeeUtils.toLong(arr[1]));
    setJoin(Codec.unpack(arr[2]));
  }

  @Override
  protected String serialize() {
    List<Object> values = new ArrayList<>();

    values.add(getRoomId());
    values.add(getUserId());
    values.add(Codec.pack(join()));

    return Codec.beeSerialize(values);
  }

  private void setRoomId(long roomId) {
    this.roomId = roomId;
  }

  private void setUserId(long userId) {
    this.userId = userId;
  }
}
