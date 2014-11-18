package com.butent.bee.shared.communication;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class ChatRoom implements BeeSerializable, HasInfo {

  public enum Type implements HasCaption {
    PUBLIC(Localized.getConstants().roomTypePublic()),
    PRIVATE(Localized.getConstants().roomTypePrivate());

    public static final Type DEFAULT = PUBLIC;

    private final String caption;

    private Type(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  private enum Serial {
    ID, NAME, TYPE, OWNERS, DWELLERS, USERS, MESSAGES, MESSAGE_COUNT, MAX_TIME
  }

  public static final int DEFAULT_CAPACITY = 1024;

  public static final int MAX_NAME_LENGTH = 32;

  private static long idCounter = 1;

  public static ChatRoom restore(String s) {
    ChatRoom room = new ChatRoom();
    room.deserialize(s);
    return room;
  }

  private static String formatMillis(long millis) {
    return (millis > 0) ? TimeUtils.renderDateTime(millis, true) : null;
  }

  private long id;

  private String name;

  private Type type;
  private final Vector<Long> owners = new Vector<>();

  private final Vector<Long> dwellers = new Vector<>();

  private final Vector<Long> users = new Vector<>();

  private final Collection<TextMessage> messages;

  private int messageCount;
  private long maxTime;

  public ChatRoom(String name, Type type, Collection<TextMessage> messages) {
    this.id = idCounter++;
    this.name = name;
    this.type = type;
    this.messages = messages;
  }

  private ChatRoom() {
    this.messages = new ArrayList<>();
  }

  public boolean addOwner(Long ownerId) {
    if (DataUtils.isId(ownerId) && !getOwners().contains(ownerId)) {
      getOwners().add(ownerId);
      return true;
    } else {
      return false;
    }
  }

  public void clear() {
    messages.clear();

    setMessageCount(0);
    setMaxTime(0);
  }

  public ChatRoom copyWithoutMessages() {
    ChatRoom copy = new ChatRoom();

    copy.setId(getId());
    copy.setName(getName());

    copy.setType(getType());

    if (!BeeUtils.isEmpty(getOwners())) {
      copy.getOwners().addAll(getOwners());
    }
    if (!BeeUtils.isEmpty(getDwellers())) {
      copy.getDwellers().addAll(getDwellers());
    }

    if (!BeeUtils.isEmpty(getUsers())) {
      copy.getUsers().addAll(getUsers());
    }

    copy.setMessageCount(getMessageCount());
    copy.setMaxTime(getMaxTime());

    return copy;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, Serial.values().length);

    for (int i = 0; i < arr.length; i++) {
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (Serial.values()[i]) {
        case ID:
          setId(BeeUtils.toLong(value));
          break;

        case NAME:
          setName(value);
          break;

        case TYPE:
          setType(Codec.unpack(Type.class, value));
          break;

        case OWNERS:
          if (!owners.isEmpty()) {
            owners.clear();
          }

          String[] oArr = Codec.beeDeserializeCollection(value);
          if (oArr != null) {
            for (String os : oArr) {
              Long owner = BeeUtils.toLongOrNull(os);
              if (DataUtils.isId(owner)) {
                owners.add(owner);
              }
            }
          }
          break;

        case DWELLERS:
          if (!dwellers.isEmpty()) {
            dwellers.clear();
          }

          String[] dArr = Codec.beeDeserializeCollection(value);
          if (dArr != null) {
            for (String ds : dArr) {
              Long dweller = BeeUtils.toLongOrNull(ds);
              if (DataUtils.isId(dweller)) {
                dwellers.add(dweller);
              }
            }
          }
          break;

        case USERS:
          if (!users.isEmpty()) {
            users.clear();
          }

          String[] uArr = Codec.beeDeserializeCollection(value);
          if (uArr != null) {
            for (String us : uArr) {
              Long user = BeeUtils.toLongOrNull(us);
              if (DataUtils.isId(user)) {
                users.add(user);
              }
            }
          }
          break;

        case MESSAGES:
          if (!messages.isEmpty()) {
            messages.clear();
          }

          String[] mArr = Codec.beeDeserializeCollection(value);
          if (mArr != null) {
            for (String msg : mArr) {
              messages.add(TextMessage.restore(msg));
            }
          }
          break;

        case MESSAGE_COUNT:
          setMessageCount(BeeUtils.toInt(value));
          break;

        case MAX_TIME:
          setMaxTime(BeeUtils.toLong(value));
          break;
      }
    }
  }

  public Vector<Long> getDwellers() {
    return dwellers;
  }

  public long getId() {
    return id;
  }

  @Override
  public List<Property> getInfo() {
    return PropertyUtils.createProperties("Room Id", getId(), "Name", getName(), "Type", getType(),
        "Owners", getOwners(), "Dwellers", getDwellers(), "Users", getUsers(),
        "Messages", (getMessages() == null) ? null : BeeUtils.bracket(getMessages().size()),
        "Message Count", getMessageCount(), "Max Time", formatMillis(getMaxTime()));
  }

  public long getMaxTime() {
    return maxTime;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public Collection<TextMessage> getMessages() {
    return messages;
  }

  public String getName() {
    return name;
  }

  public Vector<Long> getOwners() {
    return owners;
  }

  public Type getType() {
    return type;
  }

  public Vector<Long> getUsers() {
    return users;
  }

  public void incrementMessageCount() {
    setMessageCount(getMessageCount() + 1);
  }

  public boolean invite(Long dwellerId) {
    if (DataUtils.isId(dwellerId) && !getDwellers().contains(dwellerId)) {
      getDwellers().add(dwellerId);
      return true;
    } else {
      return false;
    }
  }

  public boolean is(Long roomId) {
    return Objects.equals(roomId, getId());
  }

  public boolean isOwner(Long userId) {
    return userId != null && getOwners().contains(userId);
  }

  public boolean isVisible(Long userId) {
    if (userId == null) {
      return false;
    } else if (getType() == Type.PUBLIC) {
      return true;
    } else {
      return getOwners().contains(userId) || getDwellers().contains(userId);
    }
  }

  public boolean join(Long userId) {
    if (isVisible(userId)) {
      if (!getUsers().contains(userId)) {
        getUsers().add(userId);
      }
      return true;

    } else {
      return false;
    }
  }

  public boolean kick(Long dwellerId) {
    return getDwellers().remove(dwellerId);
  }

  public boolean quit(Long userId) {
    return getUsers().remove(userId);
  }

  public boolean removeOwner(Long ownerId) {
    return getOwners().remove(ownerId);
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[Serial.values().length];
    int i = 0;

    for (Serial member : Serial.values()) {
      switch (member) {
        case ID:
          arr[i++] = getId();
          break;

        case NAME:
          arr[i++] = getName();
          break;

        case TYPE:
          arr[i++] = Codec.pack(getType());
          break;

        case OWNERS:
          arr[i++] = getOwners();
          break;

        case DWELLERS:
          arr[i++] = getDwellers();
          break;

        case USERS:
          arr[i++] = getUsers();
          break;

        case MESSAGES:
          arr[i++] = getMessages();
          break;

        case MESSAGE_COUNT:
          arr[i++] = getMessageCount();
          break;

        case MAX_TIME:
          arr[i++] = getMaxTime();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setMaxTime(long maxTime) {
    this.maxTime = maxTime;
  }

  public void setMessageCount(int messageCount) {
    this.messageCount = messageCount;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("id", BeeUtils.toString(getId()),
        "name", getName(),
        "type", (getType() == null) ? null : getType().name().toLowerCase(),
        "owners", getOwners().isEmpty() ? null : getOwners().toString(),
        "dwellers", getDwellers().isEmpty() ? null : getDwellers().toString(),
        "users", getUsers().isEmpty() ? null : getUsers().toString(),
        "messages", (getMessages() == null) ? null : BeeUtils.toString(getMessages().size()),
        "message count", BeeUtils.toString(getMessageCount()),
        "max time", formatMillis(getMaxTime()));
  }

  public void updateMaxTime(long time) {
    if (time > 0) {
      setMaxTime(Math.max(getMaxTime(), time));
    }
  }

  private void setId(long id) {
    this.id = id;
  }
}
