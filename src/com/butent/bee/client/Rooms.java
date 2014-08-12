package com.butent.bee.client;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.communication.Chat;
import com.butent.bee.client.communication.ChatUtils;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Badge;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.communication.ChatRoom;
import com.butent.bee.shared.communication.ChatRoom.Type;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.websocket.messages.ChatMessage;
import com.butent.bee.shared.websocket.messages.RoomStateMessage;
import com.butent.bee.shared.websocket.messages.RoomUserMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Rooms implements HasInfo {

  private final class RoomSettings {
    private String name;
    private Type type;

    private final List<Long> owners = new ArrayList<>();
    private final List<Long> dwellers = new ArrayList<>();

    private RoomSettings() {
      this.type = Type.DEFAULT;
      this.owners.add(BeeKeeper.getUser().getUserId());
    }

    private RoomSettings(ChatRoom room) {
      this.name = room.getName();
      this.type = room.getType();

      if (!BeeUtils.isEmpty(room.getOwners())) {
        this.owners.addAll(room.getOwners());
      }
      if (!BeeUtils.isEmpty(room.getDwellers())) {
        this.dwellers.addAll(room.getDwellers());
      }
    }

    private RoomSettings(RoomSettings other) {
      this.name = other.getName();
      this.type = other.getType();

      if (!BeeUtils.isEmpty(other.getOwners())) {
        this.owners.addAll(other.getOwners());
      }
      if (!BeeUtils.isEmpty(other.getDwellers())) {
        this.dwellers.addAll(other.getDwellers());
      }
    }

    private List<Long> getDwellers() {
      return dwellers;
    }

    private String getName() {
      return name;
    }

    private List<Long> getOwners() {
      return owners;
    }

    private Type getType() {
      return type;
    }

    private boolean isValid() {
      return !BeeUtils.isEmpty(getName()) && getType() != null && !getOwners().isEmpty();
    }

    private void setName(String name) {
      this.name = name;
    }

    private void setType(Type type) {
      this.type = type;
    }
  }

  private final class RoomsPanel extends Flow {

    private RoomsPanel() {
      super(STYLE_ROOMS_PREFIX + "panel");

      FaLabel plusWidget = new FaLabel(FontAwesome.PLUS_SQUARE_O);
      plusWidget.setTitle(Localized.getConstants().roomNew());
      plusWidget.addStyleName(STYLE_ROOMS_PREFIX + "plus");

      plusWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          createRoom();
        }
      });

      add(plusWidget);
    }

    @Override
    protected void onUnload() {
      timer.cancel();
      super.onUnload();
    }

    private void addRoomWidget(RoomWidget roomWidget) {
      int count = getWidgetCount();
      insert(roomWidget, count - 1);
    }

    private RoomWidget findRoomWidget(long roomId) {
      for (Widget widget : this) {
        if (widget instanceof RoomWidget
            && Objects.equals(((RoomWidget) widget).getRoomId(), roomId)) {
          return (RoomWidget) widget;
        }
      }

      logger.warning("widget not found for room", roomId);
      return null;
    }
  }

  private final class RoomWidget extends Flow {

    private final long roomId;

    private final CustomDiv nameWidget;

    private final FaLabel settingsWidget;
    private final FaLabel deleteWidget;

    private final Badge usersBadge;
    private final Flow usersPanel;
    private final CustomDiv timeLabel;

    private RoomWidget(ChatRoom chatRoom) {
      super(STYLE_ROOM_PREFIX + "container");
      this.roomId = chatRoom.getId();

      Flow headerPanel = new Flow(STYLE_ROOM_PREFIX + "headerPanel");

      this.nameWidget = new CustomDiv(STYLE_ROOM_PREFIX + "nameLabel");
      nameWidget.setText(chatRoom.getName());

      nameWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          enterRoom(roomId);
          removeStyleName(STYLE_ROOM_UPDATED);
        }
      });

      headerPanel.add(nameWidget);

      FaLabel infoWidget = new FaLabel(FontAwesome.INFO_CIRCLE);
      infoWidget.addStyleName(STYLE_ROOM_PREFIX + "infoCommand");

      infoWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showInfo(roomId);
        }
      });

      headerPanel.add(infoWidget);

      this.settingsWidget = new FaLabel(FontAwesome.GEAR);
      settingsWidget.addStyleName(STYLE_ROOM_PREFIX + "settingsCommand");

      settingsWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          editSetting(roomId);
        }
      });

      headerPanel.add(settingsWidget);

      this.deleteWidget = new FaLabel(FontAwesome.TRASH_O);
      deleteWidget.addStyleName(STYLE_ROOM_PREFIX + "deleteCommand");

      deleteWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          ChatRoom room = findRoom(roomId);
          if (room != null) {
            onDelete(room);
          }
        }
      });

      headerPanel.add(deleteWidget);

      if (!chatRoom.isOwner(BeeKeeper.getUser().getUserId())) {
        settingsWidget.addStyleName(STYLE_GUEST);
        deleteWidget.addStyleName(STYLE_GUEST);
      }

      add(headerPanel);

      Flow infoPanel = new Flow(STYLE_ROOM_PREFIX + "infoPanel");

      int userCount = ChatUtils.countOtherUsers(chatRoom.getUsers());

      this.usersBadge = new Badge(userCount, STYLE_ROOM_PREFIX + "usersBadge");
      infoPanel.add(usersBadge);

      this.usersPanel = new Flow(STYLE_ROOM_PREFIX + "usersPanel");
      if (userCount > 0) {
        ChatUtils.renderOtherUsers(usersPanel, chatRoom.getUsers(), STYLE_USER);
      }

      infoPanel.add(usersPanel);

      this.timeLabel = new CustomDiv(STYLE_ROOM_PREFIX + "maxTime");
      if (chatRoom.getMaxTime() > 0) {
        ChatUtils.updateTime(timeLabel, chatRoom.getMaxTime());
      }

      infoPanel.add(timeLabel);
      add(infoPanel);
    }

    private long getRoomId() {
      return roomId;
    }

    private void update(ChatRoom chatRoom) {
      if (chatRoom != null) {
        nameWidget.getElement().setInnerText(chatRoom.getName());

        boolean isOwner = chatRoom.isOwner(BeeKeeper.getUser().getUserId());
        settingsWidget.setStyleName(STYLE_GUEST, !isOwner);
        deleteWidget.setStyleName(STYLE_GUEST, !isOwner);

        updateUsers(chatRoom.getUsers());
        updateTime(chatRoom.getMaxTime());
      }
    }

    private void updateTime(long maxTime) {
      ChatUtils.updateTime(timeLabel, maxTime);
    }

    private void updateUsers(Collection<Long> users) {
      int count = ChatUtils.countOtherUsers(users);
      usersBadge.update(count);

      if (count > 0) {
        ChatUtils.renderOtherUsers(usersPanel, users, STYLE_USER);
      } else {
        usersPanel.clear();
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Rooms.class);

  private static final String STYLE_ROOMS_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "Rooms-";

  private static final String STYLE_ROOM_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "Room-";
  private static final String STYLE_GUEST = STYLE_ROOM_PREFIX + "guest";
  private static final String STYLE_USER = STYLE_ROOM_PREFIX + "user";
  private static final String STYLE_ROOM_UPDATED = STYLE_ROOM_PREFIX + "updated";

  private static final int TIMER_PERIOD = 10000;

  private static void enterRoom(long roomId) {
    Chat chat = findChat(roomId);

    if (chat == null) {
      Endpoint.send(RoomUserMessage.enter(roomId, BeeKeeper.getUser().getUserId()));
    } else {
      BeeKeeper.getScreen().activateWidget(chat);
    }
  }

  private static Chat findChat(long roomId) {
    List<IdentifiableWidget> openWidgets = BeeKeeper.getScreen().getOpenWidgets();
    for (IdentifiableWidget widget : openWidgets) {
      if (widget instanceof Chat && Objects.equals(((Chat) widget).getRoomId(), roomId)) {
        return (Chat) widget;
      }
    }
    return null;
  }

  private static void onDelete(final ChatRoom room) {
    List<String> messages = Lists.newArrayList(Localized.getConstants().roomDeleteQuestion());

    Global.confirmDelete(room.getName(), Icon.WARNING, messages, new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        Endpoint.send(RoomStateMessage.remove(room));
      }
    });
  }

  private final List<ChatRoom> chatRooms = new ArrayList<>();

  private final RoomsPanel roomsPanel = new RoomsPanel();

  private final Timer timer;

  private Badge sizeBadge;

  private Map<Long, ViewCallback> viewCallbacks = new HashMap<>();

  Rooms() {
    this.timer = new Timer() {
      @Override
      public void run() {
        if (!chatRooms.isEmpty()) {
          onTimer();
        }
      }
    };
    timer.scheduleRepeating(TIMER_PERIOD);
  }

  public void addMessage(ChatMessage chatMessage) {
    Assert.notNull(chatMessage);

    ChatRoom room = findRoom(chatMessage.getRoomId());
    if (room != null && chatMessage.isValid()) {
      room.incrementMassageCount();
      room.updateMaxTime(chatMessage.getTextMessage().getMillis());

      RoomWidget roomWidget = roomsPanel.findRoomWidget(room.getId());
      if (roomWidget != null) {
        roomWidget.updateTime(room.getMaxTime());

        if (!BeeKeeper.getUser().is(chatMessage.getTextMessage().getUserId())) {
          roomWidget.addStyleName(STYLE_ROOM_UPDATED);
        }
      }

      Chat chat = findChat(room.getId());
      if (chat != null) {
        chat.addMessage(chatMessage.getTextMessage(), true);
      }
    }
  }

  public void configure(long roomId) {
    ChatRoom room = findRoom(roomId);
    if (room == null) {
      return;
    }

    if (room.isOwner(BeeKeeper.getUser().getUserId())) {
      editSetting(roomId);
    } else {
      showInfo(roomId);
    }
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = new ArrayList<>();
    info.add(new Property("Client Rooms", BeeUtils.bracket(chatRooms.size())));

    for (ChatRoom room : chatRooms) {
      info.addAll(room.getInfo());

      Chat chat = findChat(room.getId());
      if (chat != null) {
        info.add(new Property("Chat View", chat.getId()));
      }
    }
    return info;
  }

  public Long getMaxTime(long roomId) {
    ChatRoom room = findRoom(roomId);
    return (room == null) ? null : room.getMaxTime();
  }

  public IdentifiableWidget getRoomsPanel() {
    return roomsPanel;
  }

  public void leaveRoom(long roomId) {
    Endpoint.send(RoomUserMessage.leave(roomId, BeeKeeper.getUser().getUserId()));

    ChatRoom room = findRoom(roomId);
    if (room != null) {
      room.quit(BeeKeeper.getUser().getUserId());
    }
  }

  public void onRoomState(RoomStateMessage roomStateMessage) {
    Assert.notNull(roomStateMessage);
    Assert.isTrue(roomStateMessage.isValid());

    ChatRoom room = roomStateMessage.getRoom();
    Chat chat;

    if (roomStateMessage.isLoading()) {
      chat = new Chat(room);

      ViewCallback callback = viewCallbacks.remove(room.getId());
      if (callback == null) {
        BeeKeeper.getScreen().show(chat);
      } else {
        callback.onSuccess(chat);
      }

      updateRoom(room);

    } else if (roomStateMessage.isNew()) {
      if (contains(room.getId())) {
        logger.warning("attempt to add existing room:", room.getId());
      } else if (room.isVisible(BeeKeeper.getUser().getUserId())) {
        addRoom(room);
      }

    } else if (roomStateMessage.isUpdated()) {
      boolean visible = room.isVisible(BeeKeeper.getUser().getUserId());

      if (contains(room.getId())) {
        if (visible) {
          updateRoom(room);

          chat = findChat(room.getId());
          if (chat != null) {
            chat.onRoomUpdate(room);
          }

        } else {
          removeRoom(room.getId());
        }

      } else if (visible) {
        addRoom(room);
      }

    } else if (roomStateMessage.isRemoved()) {
      removeRoom(room.getId());

    } else {
      logger.warning("unrecongnized room state:", roomStateMessage.getState());
    }
  }

  public void onRoomUser(RoomUserMessage roomUserMessage) {
    Assert.notNull(roomUserMessage);

    ChatRoom room = findRoom(roomUserMessage.getRoomId());

    if (room != null) {
      boolean ok;
      if (roomUserMessage.join()) {
        ok = room.join(roomUserMessage.getUserId());
      } else if (roomUserMessage.quit()) {
        ok = room.quit(roomUserMessage.getUserId());
      } else {
        ok = false;
      }

      if (ok) {
        RoomWidget widget = roomsPanel.findRoomWidget(room.getId());
        if (widget != null) {
          widget.updateUsers(room.getUsers());
          widget.updateTime(room.getMaxTime());
        }

        Chat chat = findChat(room.getId());
        if (chat != null) {
          chat.onRoomUpdate(room);
        }
      }
    }
  }

  public void open(long roomId, ViewCallback callback) {
    Assert.notNull(callback);

    ChatRoom room = findRoom(roomId);
    if (room != null) {
      viewCallbacks.put(roomId, callback);
      Endpoint.send(RoomUserMessage.enter(roomId, BeeKeeper.getUser().getUserId()));
    }
  }

  public void setRoomData(List<ChatRoom> data) {
    Assert.notNull(data);

    if (!chatRooms.isEmpty()) {
      for (ChatRoom room : chatRooms) {
        RoomWidget widget = roomsPanel.findRoomWidget(room.getId());
        if (widget != null) {
          roomsPanel.remove(widget);
        }
      }

      chatRooms.clear();
    }

    chatRooms.addAll(data);

    for (ChatRoom room : data) {
      RoomWidget widget = new RoomWidget(room);
      roomsPanel.addRoomWidget(widget);
    }

    updateHeader();
    logger.info("rooms", chatRooms.size());
  }

  private void addRoom(ChatRoom room) {
    chatRooms.add(room);

    RoomWidget widget = new RoomWidget(room);
    roomsPanel.addRoomWidget(widget);

    updateHeader();
    logger.info("added room", room.getName());
  }

  private boolean contains(long roomId) {
    for (ChatRoom room : chatRooms) {
      if (room.is(roomId)) {
        return true;
      }
    }
    return false;
  }

  private void createRoom() {
    RoomSettings roomSettings = new RoomSettings();
    openSettings(roomSettings, true, new Consumer<RoomSettings>() {
      @Override
      public void accept(RoomSettings input) {
        if (input.isValid()) {
          List<TextMessage> messages = new ArrayList<>();
          ChatRoom room = new ChatRoom(input.getName(), input.getType(), messages);

          room.getOwners().addAll(input.getOwners());
          if (!BeeUtils.isEmpty(input.getDwellers())) {
            room.getDwellers().addAll(input.getDwellers());
          }

          Endpoint.send(RoomStateMessage.add(room));
        }
      }
    });
  }

  private void editSetting(final long roomId) {
    ChatRoom original = findRoom(roomId);
    if (original == null) {
      return;
    }

    RoomSettings roomSettings = new RoomSettings(original);
    openSettings(roomSettings, false, new Consumer<RoomSettings>() {
      @Override
      public void accept(RoomSettings input) {
        if (!input.isValid()) {
          return;
        }

        ChatRoom room = findRoom(roomId);
        if (room == null) {
          return;
        }

        boolean changed = false;

        if (!BeeUtils.equalsTrimRight(room.getName(), input.getName())) {
          room.setName(input.getName());
          changed = true;
        }
        if (room.getType() != input.getType()) {
          room.setType(input.getType());
          changed = true;
        }

        if (!BeeUtils.sameElements(room.getOwners(), input.getOwners())) {
          BeeUtils.overwrite(room.getOwners(), input.getOwners());
          changed = true;
        }
        if (!BeeUtils.sameElements(room.getDwellers(), input.getDwellers())) {
          BeeUtils.overwrite(room.getDwellers(), input.getDwellers());
          changed = true;
        }

        if (changed) {
          Endpoint.send(RoomStateMessage.update(room));
        } else {
          logger.debug("settings not changed");
        }
      }
    });
  }

  private ChatRoom findRoom(long roomId) {
    for (ChatRoom room : chatRooms) {
      if (room.is(roomId)) {
        return room;
      }
    }

    logger.warning("room not found:", roomId);
    return null;
  }

  private Badge getSizeBadge() {
    return sizeBadge;
  }

  private void onTimer() {
    for (ChatRoom room : chatRooms) {
      RoomWidget widget = roomsPanel.findRoomWidget(room.getId());
      if (widget != null) {
        widget.updateTime(room.getMaxTime());
      }
    }
  }

  private void openSettings(RoomSettings roomSettings, boolean isNew,
      final Consumer<RoomSettings> consumer) {

    final RoomSettings result = new RoomSettings(roomSettings);

    String stylePrefix = STYLE_ROOM_PREFIX + "editor-";

    String caption = isNew ? Localized.getConstants().roomNew()
        : Localized.getConstants().roomSettings();
    final DialogBox dialog = DialogBox.create(caption, stylePrefix + "dialog");

    HtmlTable table = new HtmlTable(stylePrefix + "table");
    int row = 0;

    Label nameLabel = new Label(Localized.getConstants().roomName());
    nameLabel.addStyleName(StyleUtils.NAME_REQUIRED);
    table.setWidgetAndStyle(row, 0, nameLabel, stylePrefix + "nameLabel");

    final InputText nameInput = new InputText();
    nameInput.setMaxLength(ChatRoom.MAX_NAME_LENGTH);
    if (!BeeUtils.isEmpty(roomSettings.getName())) {
      nameInput.setValue(roomSettings.getName());
    }
    table.setWidgetAndStyle(row, 1, nameInput, stylePrefix + "nameInput");

    row++;
    Label typeLabel = new Label(Localized.getConstants().type());
    typeLabel.addStyleName(StyleUtils.NAME_REQUIRED);
    table.setWidgetAndStyle(row, 0, typeLabel, stylePrefix + "typeLabel");

    final RadioGroup typeWidget = new RadioGroup(Orientation.HORIZONTAL, roomSettings.getType(),
        Type.class);
    table.setWidgetAndStyle(row, 1, typeWidget, stylePrefix + "typeInput");

    row++;
    Label ownersLabel = new Label(Localized.getConstants().roomOwners());
    ownersLabel.addStyleName(StyleUtils.NAME_REQUIRED);
    table.setWidgetAndStyle(row, 0, ownersLabel, stylePrefix + "ownersLabel");

    final MultiSelector ownersWidget = MultiSelector.autonomous(AdministrationConstants.TBL_USERS,
        Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME));
    if (!BeeUtils.isEmpty(roomSettings.getOwners())) {
      ownersWidget.setIds(roomSettings.getOwners());
    }
    table.setWidgetAndStyle(row, 1, ownersWidget, stylePrefix + "ownersInput");

    row++;
    Label dwellersLabel = new Label(Localized.getConstants().roomDwellers());
    table.setWidgetAndStyle(row, 0, dwellersLabel, stylePrefix + "dwellersLabel");

    final MultiSelector dwellersWidget =
        MultiSelector.autonomous(AdministrationConstants.TBL_USERS,
            Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME));
    if (!BeeUtils.isEmpty(roomSettings.getDwellers())) {
      dwellersWidget.setIds(roomSettings.getDwellers());
    }
    table.setWidgetAndStyle(row, 1, dwellersWidget, stylePrefix + "dwellersInput");

    row++;
    Flow commands = new Flow();

    Button save = new Button(Localized.getConstants().actionSave());
    save.addStyleName(stylePrefix + "save");

    save.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (nameInput.isEmpty()) {
          nameInput.setFocus(true);
          return;
        }

        Type type = EnumUtils.getEnumByIndex(Type.class, typeWidget.getSelectedIndex());
        if (type == null) {
          return;
        }

        List<Long> owners = DataUtils.parseIdList(ownersWidget.getValue());
        if (BeeUtils.isEmpty(owners)) {
          ownersWidget.setFocus(true);
          return;
        }

        result.setName(BeeUtils.trim(nameInput.getValue()));
        result.setType(type);

        BeeUtils.overwrite(result.getOwners(), owners);
        BeeUtils.overwrite(result.getDwellers(),
            DataUtils.parseIdList(dwellersWidget.getValue()));

        dialog.close();
        consumer.accept(result);
      }
    });

    commands.add(save);

    Button cancel = new Button(Localized.getConstants().actionCancel());
    cancel.addStyleName(stylePrefix + "cancel");

    cancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialog.close();
      }
    });

    commands.add(cancel);

    table.setWidgetAndStyle(row, 1, commands, stylePrefix + "commands");

    dialog.setWidget(table);

    dialog.setAnimationEnabled(true);
    dialog.center();

    if (isNew) {
      nameInput.setFocus(true);
    }
  }

  private void removeRoom(long roomId) {
    ChatRoom room = findRoom(roomId);

    if (room != null) {
      chatRooms.remove(room);

      RoomWidget widget = roomsPanel.findRoomWidget(roomId);
      if (widget != null) {
        roomsPanel.remove(widget);
      }

      Chat chat = findChat(roomId);
      if (chat != null) {
        BeeKeeper.getScreen().closeWidget(chat);
      }

      updateHeader();
      logger.info("removed room", room.getName());
    }
  }

  private void setSizeBadge(Badge sizeBadge) {
    this.sizeBadge = sizeBadge;
  }

  private void showInfo(long roomId) {
    ChatRoom room = findRoom(roomId);
    if (room == null) {
      return;
    }

    HtmlTable table = new HtmlTable(STYLE_ROOM_PREFIX + "details");
    int row = 0;

    table.setText(row, 0, Localized.getConstants().type());
    if (room.getType() != null) {
      table.setText(row, 2, room.getType().getCaption());
    }

    row++;
    table.setText(row, 0, Localized.getConstants().roomOwners());
    table.setText(row, 1, BeeUtils.bracket(BeeUtils.size(room.getOwners())));
    if (!BeeUtils.isEmpty(room.getOwners())) {
      table.setText(row, 2, BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
          Global.getUsers().getSignatures(room.getOwners())));
    }

    row++;
    table.setText(row, 0, Localized.getConstants().roomDwellers());
    table.setText(row, 1, BeeUtils.bracket(BeeUtils.size(room.getDwellers())));
    if (!BeeUtils.isEmpty(room.getDwellers())) {
      table.setText(row, 2, BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
          Global.getUsers().getSignatures(room.getDwellers())));
    }

    row++;
    table.setText(row, 0, Localized.getConstants().roomUsers());
    table.setText(row, 1, BeeUtils.bracket(BeeUtils.size(room.getUsers())));
    if (!BeeUtils.isEmpty(room.getUsers())) {
      table.setText(row, 2, BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
          Global.getUsers().getSignatures(room.getUsers())));
    }

    row++;
    table.setText(row, 0, Localized.getConstants().roomUpdateTime());
    table.setText(row, 1, BeeUtils.bracket(room.getMessageCount()));
    if (room.getMaxTime() > 0) {
      table.setText(row, 2, BeeUtils.joinWords(ChatUtils.elapsed(room.getMaxTime()),
          TimeUtils.renderDateTime(room.getMaxTime(), true)));
    }

    Global.showModalWidget(room.getName(), table);
  }

  private void updateHeader() {
    Flow header = BeeKeeper.getScreen().getDomainHeader(Domain.ROOMS, null);
    if (header == null) {
      return;
    }

    if (getSizeBadge() == null) {
      Badge badge = new Badge(chatRooms.size(), STYLE_ROOMS_PREFIX + "size");

      header.add(badge);
      setSizeBadge(badge);

    } else {
      getSizeBadge().update(chatRooms.size());
    }
  }

  private void updateRoom(ChatRoom source) {
    ChatRoom target = findRoom(source.getId());

    if (target != null) {
      target.setName(source.getName());
      target.setType(source.getType());

      BeeUtils.overwrite(target.getOwners(), source.getOwners());
      BeeUtils.overwrite(target.getDwellers(), source.getDwellers());

      BeeUtils.overwrite(target.getUsers(), source.getUsers());

      target.setMessageCount(source.getMessageCount());
      target.setMaxTime(source.getMaxTime());

      RoomWidget widget = roomsPanel.findRoomWidget(source.getId());
      if (widget != null) {
        widget.update(source);
      }
    }
  }
}
