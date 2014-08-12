package com.butent.bee.server.communication;

import com.butent.bee.server.Config;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ChatRoom;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Rooms {

  private static BeeLogger logger = LogUtils.getLogger(Rooms.class);

  private static Queue<ChatRoom> chatRooms = new ConcurrentLinkedQueue<>();

  static {
    String defRoom = Config.getProperty("DefaultChatRoom");

    if (!BeeUtils.isEmpty(defRoom) && !BeeConst.STRING_MINUS.equals(defRoom)) {
      chatRooms.add(new ChatRoom(defRoom.trim(), ChatRoom.Type.PUBLIC,
          emptyTextMessages(ChatRoom.DEFAULT_CAPACITY)));
    }
  }

  public static boolean addMessage(ChatRoom room, TextMessage message) {
    if (room == null || message == null || !message.isValid()) {
      return false;
    }
    sanitizeIncomingMessage(message);

    synchronized (room) {
      if (room.getMessages() instanceof Queue) {
        while (!((Queue<TextMessage>) room.getMessages()).offer(message)) {
          ((Queue<TextMessage>) room.getMessages()).poll();
        }

      } else {
        room.getMessages().add(message);
      }

      room.setMessageCount(room.getMessages().size());
      room.updateMaxTime(message.getMillis());
    }

    return true;
  }

  public static ChatRoom addRoom(ChatRoom source) {
    if (source == null) {
      logger.severe("cannot add room without source");
      return null;
    }
    if (BeeUtils.isEmpty(source.getName())) {
      logger.severe("room name is required");
      return null;
    }
    if (source.getType() == null) {
      logger.severe("room type is required");
      return null;
    }

    ChatRoom room = new ChatRoom(source.getName(), source.getType(),
        emptyTextMessages(ChatRoom.DEFAULT_CAPACITY));

    if (!BeeUtils.isEmpty(source.getOwners())) {
      room.getOwners().addAll(source.getOwners());
    }
    if (!BeeUtils.isEmpty(source.getDwellers())) {
      room.getDwellers().addAll(source.getDwellers());
    }

    if (!BeeUtils.isEmpty(source.getUsers())) {
      room.getUsers().addAll(source.getUsers());
    }

    if (!BeeUtils.isEmpty(source.getMessages())) {
      room.getMessages().addAll(source.getMessages());

      room.setMessageCount(source.getMessageCount());
      room.setMaxTime(source.getMaxTime());
    }

    if (chatRooms.add(room)) {
      return room;
    } else {
      return null;
    }
  }

  public static List<Property> getInfo() {
    List<Property> info = new ArrayList<>();
    info.add(new Property("Server Rooms", BeeUtils.bracket(chatRooms.size())));

    for (ChatRoom room : chatRooms) {
      info.addAll(room.getInfo());
    }
    return info;
  }

  public static ChatRoom getRoom(Long roomId) {
    for (ChatRoom room : chatRooms) {
      if (room.is(roomId)) {
        return room;
      }
    }

    logger.warning("room not found:", roomId);
    return null;
  }

  public static ResponseObject getRoom(RequestInfo reqInfo) {
    Long id = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_ID));
    if (id == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_ID);
    }

    ChatRoom room = getRoom(id);

    if (room == null) {
      return ResponseObject.error("room", id, "not found");
    } else {
      return ResponseObject.response(room);
    }
  }

  public static List<ChatRoom> getRoomDataWithoutMessagess(Long userId) {
    List<ChatRoom> result = new ArrayList<>();

    for (ChatRoom room : chatRooms) {
      if (room.isVisible(userId)) {
        result.add(room.copyWithoutMessages());
      }
    }

    return result;
  }

  public static ChatRoom removeRoom(Long roomId) {
    ChatRoom room = getRoom(roomId);

    if (room != null && chatRooms.remove(room)) {
      return room;
    } else {
      return null;
    }
  }

  private static void sanitizeIncomingMessage(TextMessage textMessage) {
    if (textMessage != null) {
      textMessage.setMillis(System.currentTimeMillis());
    }
  }

  public static ChatRoom updateRoom(ChatRoom source) {
    if (source == null) {
      logger.severe("cannot update room without source");
      return null;
    }
    if (BeeUtils.isEmpty(source.getName())) {
      logger.severe("room name is required");
      return null;
    }
    if (source.getType() == null) {
      logger.severe("room type is required");
      return null;
    }

    ChatRoom target = getRoom(source.getId());
    if (target == null) {
      return null;
    }

    target.setName(source.getName());
    target.setType(source.getType());

    BeeUtils.overwrite(target.getOwners(), source.getOwners());
    BeeUtils.overwrite(target.getDwellers(), source.getDwellers());

    return target;
  }

  private static Collection<TextMessage> emptyTextMessages(int capacity) {
    if (capacity > 0) {
      return new ArrayBlockingQueue<>(capacity);
    } else {
      return new ArrayList<>();
    }
  }

  private Rooms() {
  }
}
