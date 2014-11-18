package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage extends Message {

  private long roomId;
  private TextMessage textMessage;

  public ChatMessage(long roomId, TextMessage textMessage) {
    this();

    this.roomId = roomId;
    this.textMessage = textMessage;
  }

  ChatMessage() {
    super(Type.CHAT);
  }

  @Override
  public String brief() {
    return (getTextMessage() == null) ? null : getTextMessage().getText();
  }

  public long getRoomId() {
    return roomId;
  }

  public TextMessage getTextMessage() {
    return textMessage;
  }

  @Override
  public boolean isValid() {
    return getTextMessage() != null && getTextMessage().isValid();
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("roomId", BeeUtils.toString(getRoomId()),
        "message", (getTextMessage() == null) ? null : getTextMessage().toString());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setRoomId(BeeUtils.toLong(arr[0]));
    setTextMessage(TextMessage.restore(arr[1]));
  }

  @Override
  protected String serialize() {
    List<Object> values = new ArrayList<>();

    values.add(getRoomId());
    values.add(getTextMessage());

    return Codec.beeSerialize(values);
  }

  private void setRoomId(long roomId) {
    this.roomId = roomId;
  }

  private void setTextMessage(TextMessage textMessage) {
    this.textMessage = textMessage;
  }
}
