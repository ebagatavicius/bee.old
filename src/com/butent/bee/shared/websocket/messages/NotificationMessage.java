package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class NotificationMessage extends Message implements HasRecipient {

  public enum DisplayMode {
    POPUP, DIALOG, EXTERNAL_NOTIFICATION, INTERNAL_NOTIFICATION
  }

  public static final int MAX_LENGTH = 280;

  public static NotificationMessage create(DisplayMode displayMode, String from, String to,
      TextMessage textMessage) {
    return new NotificationMessage(from, to, displayMode, textMessage);
  }

  public static NotificationMessage dialog(String from, String to, TextMessage textMessage) {
    return new NotificationMessage(from, to, DisplayMode.DIALOG, textMessage);
  }

  public static NotificationMessage externalNotification(String from, String to,
      TextMessage textMessage) {
    return new NotificationMessage(from, to, DisplayMode.EXTERNAL_NOTIFICATION, textMessage);
  }

  public static NotificationMessage internalNotification(String from, String to,
      TextMessage textMessage) {
    return new NotificationMessage(from, to, DisplayMode.INTERNAL_NOTIFICATION, textMessage);
  }

  public static NotificationMessage popup(String from, String to, TextMessage textMessage) {
    return new NotificationMessage(from, to, DisplayMode.POPUP, textMessage);
  }

  private String from;
  private String to;

  private DisplayMode displayMode;
  private String icon;

  private final List<TextMessage> messages = new ArrayList<>();

  NotificationMessage() {
    super(Type.NOTIFICATION);
  }

  private NotificationMessage(String from, String to, DisplayMode displayMode,
      TextMessage textMessage) {
    this();

    this.from = from;
    this.to = to;
    this.displayMode = displayMode;

    if (textMessage != null && textMessage.isValid()) {
      this.messages.add(textMessage);
    }
  }

  @Override
  public String brief() {
    if (getMessages().isEmpty()) {
      return null;
    } else {
      TextMessage lastMessage = getMessages().get(getMessages().size() - 1);
      return (lastMessage == null) ? null : lastMessage.getText();
    }
  }

  public DisplayMode getDisplayMode() {
    return displayMode;
  }

  public String getFrom() {
    return from;
  }

  public String getIcon() {
    return icon;
  }

  public List<TextMessage> getMessages() {
    return messages;
  }

  @Override
  public String getTo() {
    return to;
  }

  public Long getUserId() {
    if (getMessages().isEmpty()) {
      return null;
    } else {
      TextMessage lastMessage = getMessages().get(getMessages().size() - 1);
      return (lastMessage == null) ? null : lastMessage.getUserId();
    }
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.anyEmpty(getFrom(), getTo()) && getDisplayMode() != null
        && !getMessages().isEmpty();
  }

  public NotificationMessage reply(TextMessage textMessage) {
    NotificationMessage copy = new NotificationMessage();

    copy.setFrom(getTo());
    copy.setTo(getFrom());

    copy.setDisplayMode(getDisplayMode());
    copy.setIcon(getIcon());

    copy.getMessages().addAll(getMessages());
    if (textMessage != null && textMessage.isValid()) {
      copy.getMessages().add(textMessage);
    }

    return copy;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("from", getFrom(), "to", getTo(),
        "display mode", string(getDisplayMode()), "icon", getIcon(),
        "messages", getMessages().toString());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 5);

    int i = 0;
    setFrom(arr[i++]);
    setTo(arr[i++]);
    setDisplayMode(Codec.unpack(DisplayMode.class, arr[i++]));
    setIcon(arr[i++]);

    String[] mArr = Codec.beeDeserializeCollection(arr[i++]);
    if (!messages.isEmpty()) {
      messages.clear();
    }

    if (mArr != null) {
      for (String msg : mArr) {
        messages.add(TextMessage.restore(msg));
      }
    }
  }

  @Override
  protected String serialize() {
    List<Object> values = new ArrayList<>();

    values.add(getFrom());
    values.add(getTo());
    values.add(Codec.pack(getDisplayMode()));
    values.add(getIcon());
    values.add(getMessages());

    return Codec.beeSerialize(values);
  }

  private void setDisplayMode(DisplayMode displayMode) {
    this.displayMode = displayMode;
  }
}
