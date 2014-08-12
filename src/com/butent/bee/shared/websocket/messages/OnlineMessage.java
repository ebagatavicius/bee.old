package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ChatRoom;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.websocket.SessionUser;

import java.util.ArrayList;
import java.util.List;

public class OnlineMessage extends Message {

  private final List<SessionUser> sessionUsers = new ArrayList<>();
  private final List<ChatRoom> chatRooms = new ArrayList<>();

  public OnlineMessage(List<SessionUser> sessionUsers, List<ChatRoom> chatRooms) {
    this();

    if (!BeeUtils.isEmpty(sessionUsers)) {
      this.sessionUsers.addAll(sessionUsers);
    }
    if (!BeeUtils.isEmpty(chatRooms)) {
      this.chatRooms.addAll(chatRooms);
    }
  }

  OnlineMessage() {
    super(Type.ONLINE);
  }

  @Override
  public String brief() {
    return BeeUtils.joinItems(getSessionUsers().size(), getChatRooms().size());
  }

  public List<ChatRoom> getChatRooms() {
    return chatRooms;
  }

  public List<SessionUser> getSessionUsers() {
    return sessionUsers;
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.isEmpty(getSessionUsers());
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()),
        "session users", getSessionUsers().isEmpty() ? null : getSessionUsers().toString(),
        "chat rooms", getChatRooms().isEmpty() ? null : getChatRooms().toString());
  }

  @Override
  protected void deserialize(String s) {
    if (!sessionUsers.isEmpty()) {
      sessionUsers.clear();
    }
    if (!chatRooms.isEmpty()) {
      chatRooms.clear();
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    String[] suArr = Codec.beeDeserializeCollection(arr[0]);
    if (suArr != null) {
      for (String su : suArr) {
        sessionUsers.add(SessionUser.restore(su));
      }
    }

    String[] crArr = Codec.beeDeserializeCollection(arr[1]);
    if (crArr != null) {
      for (String cr : crArr) {
        chatRooms.add(ChatRoom.restore(cr));
      }
    }
  }

  @Override
  protected String serialize() {
    List<Object> values = new ArrayList<>();

    values.add(getSessionUsers());
    values.add(getChatRooms());

    return Codec.beeSerialize(values);
  }
}
