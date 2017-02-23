package com.butent.bee.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.communication.ChatConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.communication.ChatGrid;
import com.butent.bee.client.communication.ChatPopup;
import com.butent.bee.client.communication.ChatUtils;
import com.butent.bee.client.communication.ChatView;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.Animation;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.Chat;
import com.butent.bee.shared.communication.ChatItem;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.websocket.messages.ChatMessage;
import com.butent.bee.shared.websocket.messages.ChatStateMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ChatManager implements HasInfo, HasEnabled {

  private final class ChatSettings {

    private String name;

    private final List<Long> users = new ArrayList<>();

    private ChatSettings() {
    }

    private ChatSettings(Chat chat) {
      this.name = chat.getName();

      if (!BeeUtils.isEmpty(chat.getUsers())) {
        this.users.addAll(chat.getUsers());
      }
    }

    private ChatSettings(ChatSettings other) {
      this.name = other.getName();

      if (!BeeUtils.isEmpty(other.getUsers())) {
        this.users.addAll(other.getUsers());
      }
    }

    private String getName() {
      return name;
    }

    private List<Long> getUsers() {
      return users;
    }

    private boolean isValid() {
      return !getUsers().isEmpty();
    }

    private void setName(String name) {
      this.name = name;
    }
  }

  private final class ChatsPanel extends Flow {

    private ChatsPanel(List<Chat> list) {
      super(STYLE_CHATS_PANEL);

      Flow wrapper = new Flow(STYLE_CHATS_WRAPPER);
      if (!list.isEmpty()) {
        for (Chat chat : list) {
          wrapper.add(new ChatWidget(chat));
        }
      }
      add(wrapper);

      Flow actions = new Flow(STYLE_CHATS_ACTIONS);

      CustomDiv plus = new CustomDiv(STYLE_CHATS_PLUS);
      plus.setText(Localized.dictionary().chatStartNew());

      plus.addClickHandler(event -> createChat());
      actions.add(plus);

      if (!list.isEmpty()) {
        CustomDiv showAll = new CustomDiv(STYLE_CHATS_SHOW);
        showAll.setText(Localized.dictionary().chatsShowAll());

        showAll.addClickHandler(event -> showAll());
        actions.add(showAll);
      }

      add(actions);
    }

    private void addChatWidget(ChatWidget chatWidget) {
      getWrapper().add(chatWidget);
    }

    private ChatWidget findChatWidget(long chatId) {
      for (Widget widget : getWrapper()) {
        if (widget instanceof ChatWidget
            && Objects.equals(((ChatWidget) widget).getChatId(), chatId)) {
          return (ChatWidget) widget;
        }
      }

      logger.warning("widget not found for chat", chatId);
      return null;
    }

    private Flow getWrapper() {
      return (Flow) getWidget(0);
    }
  }

  private final class ChatWidget extends Flow {

    private final long chatId;

    private final CustomDiv timeLabel;

    private CustomDiv lastMessageWidget;

    private ChatWidget(Chat chat) {
      this(chat, true, true, null);
    }

    private ChatWidget(Chat chat, boolean showFromNotifier,
        boolean showTime, String lastMessageStyle) {

      super(STYLE_CHATS_ITEM_PREFIX + "container");

      this.chatId = chat.getId();
      this.timeLabel = new CustomDiv(STYLE_CHATS_ITEM_PREFIX + "maxTime");

      addClickHandler(event -> {
        closeChatsPopup();
        enterChat(chatId, showFromNotifier);
      });

      render(chat, showFromNotifier, showTime, lastMessageStyle);
    }

    private Widget createPicture(List<Long> users) {
      switch (users.size()) {
        case 0:
          Image photo = new Image(DEFAULT_PHOTO_IMAGE);
          photo.addStyleName(STYLE_CHATS_ITEM_PREFIX + "photo");
          return photo;

        case 1:
          if (Global.getUsers().hasPhoto(users.get(0))) {
            photo = Global.getUsers().getPhoto(users.get(0));
          } else {
            photo = new Image(DEFAULT_PHOTO_IMAGE);
          }

          photo.addStyleName(STYLE_CHATS_ITEM_PREFIX + "photo");
          return photo;

        default:
          CustomDiv badge = new CustomDiv(STYLE_CHATS_ITEM_PREFIX + "usersBadge");
          badge.setText(BeeUtils.toString(users.size()));

          String title = ChatUtils.buildUserTitle(users, true);
          if (!BeeUtils.isEmpty(title)) {
            badge.setTitle(title);
          }

          return badge;
      }
    }

    private long getChatId() {
      return chatId;
    }

    private void render(Chat chat, boolean showFromNotifier,
        boolean showTime, String lastMessageStyle) {
      if (!isEmpty()) {
        clear();
      }

      setStyleName(STYLE_CHATS_ITEM_PREFIX + "hasUnread", chat.getUnreadCount() > 0
          && showFromNotifier);

      List<Long> users = ChatUtils.getOtherUsers(chat.getUsers());
      Multimap<Presence, Long> presence = ChatUtils.getUserPresence(users);

      Flow picturePanel = new Flow(STYLE_CHATS_ITEM_PREFIX + "picturePanel");
      Widget picture = createPicture(users);
      if (picture != null) {
        picturePanel.add(picture);
      }
      add(picturePanel);

      Flow presencePanel = new Flow(STYLE_CHATS_ITEM_PREFIX + "presencePanel");
      if (!presence.isEmpty()) {
        List<Presence> keys = new ArrayList<>(presence.keySet());
        Collections.sort(keys);

        for (Presence p : keys) {
          FaLabel icon = new FaLabel(p.getIcon(), p.getStyleName());
          icon.addStyleName(STYLE_CHATS_ITEM_PREFIX + "presenceIcon");

          if (users.size() > 1) {
            icon.setTitle(ChatUtils.buildUserTitle(presence.get(p), true));
          } else {
            icon.setTitle(p.getCaption());
          }

          presencePanel.add(icon);
        }
      }
      add(presencePanel);

      Flow mainPanel = new Flow(STYLE_CHATS_ITEM_PREFIX + "mainPanel");

      Flow headerPanel = new Flow(STYLE_CHATS_ITEM_PREFIX + "headerPanel");
      if (BeeUtils.isEmpty(chat.getName())) {
        if (users.size() == 1) {
          CustomDiv userWidget = new CustomDiv(STYLE_CHATS_ITEM_PREFIX + "userName");
          userWidget.setText(Global.getUsers().getSignature(users.get(0)));
          headerPanel.add(userWidget);

        } else {
          Flow usersPanel = new Flow(STYLE_CHATS_ITEM_PREFIX + "usersPanel");
          ChatUtils.renderUsers(usersPanel, users, STYLE_CHATS_ITEM_USER);
          headerPanel.add(usersPanel);
        }

      } else {
        CustomDiv nameWidget = new CustomDiv(STYLE_CHATS_ITEM_PREFIX + "nameLabel");
        nameWidget.setText(chat.getName());

        headerPanel.add(nameWidget);
      }
      if (showTime) {
        if (chat.getMaxTime() > 0) {
          ChatUtils.updateTime(timeLabel, chat.getMaxTime());
        } else {
          timeLabel.setText(BeeConst.STRING_EMPTY);
          timeLabel.setTitle(BeeConst.STRING_EMPTY);
        }

        headerPanel.add(timeLabel);
      }
      mainPanel.add(headerPanel);

      Flow infoPanel = new Flow(STYLE_CHATS_ITEM_PREFIX + "infoPanel");
      if (chat.getLastMessage() != null) {
        String text = chat.getLastMessagePlainText();
        if (BeeUtils.isEmpty(text) && chat.getLastMessage().hasFiles()) {
          text = FileInfo.getCaptions(chat.getLastMessage().getFiles());
        }

        if (!BeeUtils.isEmpty(text)) {
          lastMessageWidget =
              new CustomDiv(lastMessageStyle != null ? lastMessageStyle
                  : STYLE_CHATS_ITEM_LAST_MESSAGE);
          lastMessageWidget.setText(text);

          infoPanel.add(lastMessageWidget);
        }
      }
      mainPanel.add(infoPanel);

      add(mainPanel);
    }

    public void render(Chat chat) {
      render(chat, true, true, null);

    }

    private void updateTime(long maxTime) {
      ChatUtils.updateTime(timeLabel, maxTime);
    }

    public void updateLastMessage(String text) {
      if (lastMessageWidget != null) {
        lastMessageWidget.setText(text);
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(ChatManager.class);

  private static final String STYLE_CHATS_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Chats-";

  private static final String STYLE_CHATS_COMMAND = STYLE_CHATS_PREFIX + "command";
  private static final String STYLE_CHATS_UNREAD = STYLE_CHATS_PREFIX + "unread";
  private static final String STYLE_CHATS_ICON = STYLE_CHATS_PREFIX + "icon";
  private static final String STYLE_CHATS_DISABLED = STYLE_CHATS_PREFIX + "disabled";

  private static final String STYLE_CHATS_POPUP = STYLE_CHATS_PREFIX + "popup";
  private static final String STYLE_CHATS_PANEL = STYLE_CHATS_PREFIX + "panel";
  private static final String STYLE_CHATS_WRAPPER = STYLE_CHATS_PREFIX + "wrapper";
  private static final String STYLE_CHATS_ACTIONS = STYLE_CHATS_PREFIX + "actions";
  private static final String STYLE_CHATS_PLUS = STYLE_CHATS_PREFIX + "plus";
  private static final String STYLE_CHATS_SHOW = STYLE_CHATS_PREFIX + "show";

  private static final String STYLE_CHATS_ITEM_PREFIX = BeeConst.CSS_CLASS_PREFIX + "ChatsItem-";
  private static final String STYLE_CHATS_ITEM_USER = STYLE_CHATS_ITEM_PREFIX + "user";
  private static final String STYLE_CHATS_ITEM_LAST_MESSAGE = STYLE_CHATS_ITEM_PREFIX
      + "lastMessage";

  private static final String STYLE_CHAT_INFO_PREFIX = BeeConst.CSS_CLASS_PREFIX + "ChatInfo-";
  private static final String STYLE_CHAT_EDITOR_PREFIX = BeeConst.CSS_CLASS_PREFIX + "ChatEditor-";

  private static final String STYLE_CHATS_NOTIFIER = STYLE_CHATS_PREFIX + "notifier";
  private static final String STYLE_CHATS_NOTIFIER_CLOSE_BTN = STYLE_CHATS_NOTIFIER + "-close-btn";
  private static final String STYLE_CHATS_NOTIFIER_LAST_MESSAGE = STYLE_CHATS_NOTIFIER
      + "-last-message";
  private static final String STYLE_CHATS_NOTIFIER_LAST_MESSAGE_ACTION =
      STYLE_CHATS_NOTIFIER_LAST_MESSAGE + "-action";
  private static final String STYLE_CHATS_NOTIFIER_BORDER = STYLE_CHATS_NOTIFIER + "-border";
  private static final Integer MAX_CHAT_NOTIFIER_COUNT = 5;

  private static final int TIMER_PERIOD = 10_000;

  private static final String DEFAULT_PHOTO_IMAGE = "images/defaultUser.png";

  public static final long ASSISTANT_ID = 0;

  private static ChatView findChatView(long chatId) {
    for (IdentifiableWidget widget : BeeKeeper.getScreen().getOpenWidgets()) {
      if (isChatView(widget.asWidget(), chatId)) {
        return (ChatView) widget;
      }
    }

    for (Popup popup : Popup.getVisiblePopups()) {
      if (isChatView(popup.getWidget(), chatId)) {
        return (ChatView) popup.getWidget();
      }
    }

    return null;
  }

  private static boolean isChatView(Widget widget, long chatId) {
    return widget instanceof ChatView && Objects.equals(((ChatView) widget).getChatId(), chatId);
  }

  private final List<Chat> chats = new ArrayList<>();

  private List<Popup> chatNotifiersMap = new ArrayList<>();

  private Widget chatsCommand;
  private Widget unreadBadge;

  private AudioElement incomingSound;

  private ChatsPanel chatsPanel;

  private Timer timer;

  private boolean enabled;

  private Map<String, ChatView> chatViewInFlowPanelMap = new HashMap<>();

  private String notifierWidth =
      BeeUtils.resize(DomUtils.getClientWidth(), 1000, 2000, 240, 320) + "px";

  ChatManager() {
  }

  public void addMessage(ChatMessage chatMessage) {
    Assert.notNull(chatMessage);

    Chat chat = findChat(chatMessage.getChatId());

    if (chat != null && (chatMessage.isValid() || isAssistant(chat.getId()))) {
      if (chat.hasMessages() || chat.getMessageCount() == 0) {
        chat.addMessage(chatMessage.getChatItem());
      }
      chat.incrementMessageCount();
      chat.setLastMessage(chatMessage.getChatItem());

      boolean incoming = !BeeKeeper.getUser().is(chatMessage.getChatItem().getUserId());
      if (!incoming) {
        chat.setLastAccess(System.currentTimeMillis());
      }

      maybeRefreshChatWidget(chat);

      ChatView chatView = findChatView(chat.getId());
      boolean isChatInFlowPanel = isActiveChatInFlowPanel(chat.getId());

      if (incoming) {
        if ((chatView != null && chatView.isInteractive()) || isChatInFlowPanel) {
          chat.setUnreadCount(0);
        } else {
          chat.incrementUnreadCount();
        }
        updateUnreadBadge();
        createOrUpdateChatWidget(chat, false);
      }

      Presence presence = BeeKeeper.getUser().getPresence();
      boolean showNewMessagesNotifier = BeeKeeper.getUser().showNewMessagesNotifier();

      if (chatView != null) {
        chatView.addMessage(chatMessage.getChatItem(), true);
        chatView.updateUnreadCount(chat.getUnreadCount());

      } else if (EnumUtils.in(presence, Presence.IDLE, Presence.AWAY) && !showNewMessagesNotifier) {
        open(chat.getId(), ChatPopup::openMinimized, false);
      }

      if (chatViewInFlowPanelMap != null && chatViewInFlowPanelMap.size() > 0) {
        for (ChatView widget : chatViewInFlowPanelMap.values()) {
          if (isChatView(widget, chat.getId())) {
            widget.addMessage(chatMessage.getChatItem(), true);
            widget.updateUnreadCount(chat.getUnreadCount());
          }
        }
      }

      if (incoming && chatView == null && showNewMessagesNotifier && !isChatInFlowPanel
          && EnumUtils.in(presence, Presence.IDLE, Presence.ONLINE)) {
        openMessageNotifier(chat);
      }

      if (incoming && incomingSound != null
          && EnumUtils.in(presence, Presence.ONLINE, Presence.IDLE)) {
        incomingSound.play();
      }
    }
  }

  public static boolean isAssistant(long id) {
    return Objects.equals(id, ASSISTANT_ID);
  }

  private boolean isActiveChatInFlowPanel(long chatId) {
    for (IdentifiableWidget widget : BeeKeeper.getScreen().getOpenWidgets()) {
      if (chatViewInFlowPanelMap.containsKey(widget.getId())) {
        if (isChatView(chatViewInFlowPanelMap.get(widget.getId()), chatId)) {
          return true;
        }
      }
    }
    return false;
  }

  public void openMessageNotifier(Chat chat) {
    ChatWidget chatWidget = createOrUpdateChatWidget(chat, true);

    if (chatNotifiersMap.size() < MAX_CHAT_NOTIFIER_COUNT && chatWidget != null) {
      Popup unreadMessageNotifierPopup = new Popup(OutsideClick.IGNORE, STYLE_CHATS_NOTIFIER);
      unreadMessageNotifierPopup.setPreviewEnabled(false);

      CustomDiv closeBtn = new CustomDiv(STYLE_CHATS_NOTIFIER_CLOSE_BTN);
      closeBtn.setText("\u00D7");
      closeBtn.addClickHandler(event -> closeMessageNotifier(chat.getId(),
          unreadMessageNotifierPopup));
      chatWidget.add(closeBtn);

      chatWidget.addClickHandler(event -> closeMessageNotifier(chat.getId(),
          unreadMessageNotifierPopup));
      chatWidget.addStyleName(STYLE_CHATS_NOTIFIER_BORDER);
      unreadMessageNotifierPopup.setWidget(chatWidget);

      unreadMessageNotifierPopup.setAnimationEnabled(true);
      unreadMessageNotifierPopup.setAnimation(new Animation(300) {

        @Override
        protected void onComplete() {
          if (getPopup().isShowing()) {
            getPopup().removeStyleName(STYLE_CHATS_NOTIFIER_LAST_MESSAGE_ACTION);
          }
          super.onComplete();
        }

        @Override
        protected boolean run(double elapsed) {
          if (getPopup().isShowing()) {
            getPopup().addStyleName(STYLE_CHATS_NOTIFIER_LAST_MESSAGE_ACTION);
          }
          return true;
        }
      });
      unreadMessageNotifierPopup.setPopupPositionAndShow((width, height) -> {
        int top = Window.getClientHeight() - height * chatNotifiersMap.size();
        unreadMessageNotifierPopup.setPopupPosition(0, BeeUtils.nonNegative(top));
      });

      unreadMessageNotifierPopup.setWidth(notifierWidth);
      chatNotifiersMap.add(unreadMessageNotifierPopup);
    }

  }

  private ChatWidget createOrUpdateChatWidget(Chat chat, boolean createNewChat) {
    for (Popup widget : chatNotifiersMap) {
      if (isChatWidget(widget.getWidget(), chat.getId())) {
        ((ChatWidget) widget.getWidget()).updateLastMessage(chat.getLastMessagePlainText());
        widget.getAnimation().start();
        return null;
      }
    }
    if (createNewChat) {
      return new ChatWidget(chat, false, false, STYLE_CHATS_NOTIFIER_LAST_MESSAGE);
    }
    return null;
  }

  private Object closeMessageNotifier(long chatId, Popup unreadMessageNotifierPopup) {
    unreadMessageNotifierPopup.close();
    Popup delete = null;
    for (Popup chatNotifier : chatNotifiersMap) {
      if (delete != null) {
        chatNotifier.setPopupPosition(0, chatNotifier.getAbsoluteTop()
            + chatNotifier.getOffsetHeight());
      }
      if (isChatWidget(chatNotifier.getWidget(), chatId)) {
        delete = chatNotifier;
      }
    }
    if (delete != null) {
      chatNotifiersMap.remove(delete);
    }
    return null;
  }

  private static boolean isChatWidget(Widget widget, long chatId) {
    return widget instanceof ChatWidget
        && Objects.equals(((ChatWidget) widget).getChatId(), chatId);
  }

  public boolean chatWithUser(Long userId) {
    if (!DataUtils.isId(userId) || BeeKeeper.getUser().is(userId)) {
      return false;
    }

    List<Long> users = new ArrayList<>();
    users.add(BeeKeeper.getUser().getUserId());
    users.add(userId);

    for (Chat chat : getSortedChats()) {
      if (BeeUtils.sameElements(chat.getUsers(), users)) {
        enterChat(chat.getId());
        return true;
      }
    }

    ChatSettings settings = new ChatSettings();
    settings.getUsers().addAll(users);

    createChat(settings);

    return true;
  }

  public void configure(long chatId) {
    Chat chat = findChat(chatId);

    if (chat != null) {
      if (chat.isCreator(BeeKeeper.getUser().getUserId())) {
        editSettings(chat);
      } else {
        showInfo(chat);
      }
    }
  }

  public boolean contains(long chatId) {
    for (Chat chat : chats) {
      if (chat.is(chatId)) {
        return true;
      }
    }
    return false;
  }

  public void createChat() {
    ChatSettings settings = new ChatSettings();

    openSettings(settings, true, input -> {
      if (input.isValid()) {
        closeChatsPopup();
        createChat(input);
      }
    });
  }

  public Widget createCommand() {
    Flow command = new Flow(STYLE_CHATS_COMMAND);
    command.setTitle(Localized.dictionary().chats());

    CustomDiv unread = new CustomDiv(STYLE_CHATS_UNREAD);
    StyleUtils.setEmptiness(unread, true);
    command.add(unread);

    FaLabel icon = new FaLabel(FontAwesome.COMMENTS, STYLE_CHATS_ICON);
    command.add(icon);

    command.addClickHandler(event -> {
      if (isEnabled()) {
        showChats();
      }
    });

    setChatsCommand(command);
    setUnreadBadge(unread);

    return command;
  }

  public boolean enterChat(long chatId) {
    return enterChat(chatId, true);
  }

  public boolean enterChat(long chatId, boolean openFromLeftCorner) {
    if (contains(chatId)) {
      ChatView chatView = findChatView(chatId);

      if (chatView != null) {

        ChatPopup popup = chatView.getPopup();
        if (popup != null) {
          popup.setMinimized(false);

          popup.maybeBringToFront();
          chatView.getInputArea().setFocus(true);

        } else {
          if (chatView.getPopup() != null) {
            BeeKeeper.getScreen().activateWidget(chatView);
          } else {
            open(chatId, view -> ChatPopup.openNormal(view, openFromLeftCorner), true);
          }
        }

      } else {
        open(chatId, view -> ChatPopup.openNormal(view, openFromLeftCorner), true);
      }
      return true;

    } else {
      return false;
    }
  }

  public void enterChat(long chatId, FlowPanel chatsFlowWidget) {
    if (contains(chatId)) {
      if (chatsFlowWidget != null) {
        ViewCallback view = result -> {
          ChatView chatViewInFlowPanel =
              new ChatView(findChat(chatId), Action.NO_ACTIONS, EnumSet.of(Action.CLOSE), false);
          chatsFlowWidget.add(chatViewInFlowPanel);
          chatViewInFlowPanelMap
              .put(BeeKeeper.getScreen().getActiveWidget().getId(), chatViewInFlowPanel);
        };
        open(chatId, view);
      } else {
        open(chatId, ChatPopup::openNormal, true);
      }
    }
  }

  public Chat findChat(long chatId) {
    for (Chat chat : chats) {
      if (chat.is(chatId)) {
        return chat;
      }
    }

    logger.warning("chat not found:", chatId);
    return null;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = new ArrayList<>();
    info.add(new Property("Chats", BeeUtils.bracket(chats.size())));

    for (Chat chat : chats) {
      info.addAll(chat.getInfo());

      ChatView chatView = findChatView(chat.getId());
      if (chatView != null) {
        info.add(new Property("Chat View", chatView.getId()));
      }
    }
    return info;
  }

  public int getUnreadCount(long chatId) {
    Chat chat = findChat(chatId);
    return (chat == null) ? 0 : chat.getUnreadCount();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void load(String serialized) {
    if (!chats.isEmpty()) {
      chats.clear();
    }

    String[] arr = Codec.beeDeserializeCollection(serialized);

    if (!ArrayUtils.isEmpty(arr)) {
      for (String s : arr) {
        Chat chat = Chat.restore(s);
        chats.add(chat);
      }

      logger.info("loaded", chats.size(), "chats");
    }

    if (BeeKeeper.getUser().assistant()) {
      addAssistantChat();
    }

    updateUnreadBadge();
  }

  private void addAssistantChat() {
    Chat chat = new Chat(ASSISTANT_ID, Localized.dictionary().bAssistant());
    chat.addUser(BeeKeeper.getUser().getUserId());
    addChat(chat);
  }

  public void markAsRead(long chatId) {
    Chat chat = findChat(chatId);

    if (chat != null && chat.getUnreadCount() != 0) {
      chat.setUnreadCount(0);

      maybeRefreshChatWidget(chat);

      ChatView chatView = findChatView(chatId);
      if (chatView != null) {
        chatView.updateUnreadCount(0);
      }

      updateUnreadBadge();

      ParameterList params = BeeKeeper.getRpc().createParameters(Service.ACCESS_CHAT);
      params.addQueryItem(COL_CHAT, chat.getId());

      BeeKeeper.getRpc().makeRequest(params);
    }
  }

  public void onChatState(ChatStateMessage stateMessage) {
    Assert.notNull(stateMessage);
    Assert.isTrue(stateMessage.isValid());

    Chat chat = stateMessage.getChat();

    if (stateMessage.isNew()) {
      if (contains(chat.getId())) {
        logger.warning("attempt to add existing chat:", chat.getId());
      } else if (chat.hasUser(BeeKeeper.getUser().getUserId())) {
        addChat(chat);
      }

    } else if (stateMessage.isUpdated()) {
      boolean visible = chat.hasUser(BeeKeeper.getUser().getUserId());

      if (contains(chat.getId())) {
        if (visible) {
          updateChat(chat);

          ChatView chatView = findChatView(chat.getId());
          if (chatView != null) {
            chatView.onChatUpdate(chat);
          }

        } else {
          removeChat(chat.getId());
        }

      } else if (visible) {
        addChat(chat);
      }

    } else if (stateMessage.isRemoved()) {
      removeChat(chat.getId());

    } else {
      logger.warning("unrecognized chat state:", stateMessage.getState());
    }
  }

  public void onUserPresenceChange(Long userId) {
    if (DataUtils.isId(userId) && !BeeKeeper.getUser().is(userId)) {
      for (Chat chat : chats) {
        if (chat.hasUser(userId)) {
          maybeRefreshChatWidget(chat);

          ChatView chatView = findChatView(chat.getId());
          if (chatView != null) {
            chatView.onUserPresenceChange();
          }
        }
      }

    }
  }

  public void open(long chatId, final ViewCallback callback) {
    open(chatId, callback, true);
  }

  private void open(long chatId, final ViewCallback callback, final boolean markAccess) {
    Assert.notNull(callback);

    final Chat chat = findChat(chatId);

    if (chat != null) {
      if (markAccess) {
        chat.setLastAccess(System.currentTimeMillis());
      }

      if (ChatManager.isAssistant(chat.getId())) {
        if (markAccess) {
          chat.setUnreadCount(0);
        }
        maybeRefreshChatWidget(chat);
        updateUnreadBadge();
        callback.onSuccess(new ChatView(chat));
        return;
      }

      ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_CHAT_MESSAGES);
      params.addQueryItem(COL_CHAT, chat.getId());
      if (markAccess) {
        params.addQueryItem(COL_CHAT_USER_LAST_ACCESS, System.currentTimeMillis());
      }

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasResponse()) {
            chat.clearMessages();
            if (markAccess) {
              chat.setUnreadCount(0);
            }

            String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());
            for (String s : arr) {
              chat.addMessage(ChatItem.restore(s));
            }

            chat.setMessageCount(chat.getMessages().size());
            chat.setLastMessage(BeeUtils.getLast(chat.getMessages()));

            maybeRefreshChatWidget(chat);
          }

          updateUnreadBadge();
          callback.onSuccess(new ChatView(chat));
        }
      });
    }
  }

  public boolean removeChat(long chatId) {
    if (contains(chatId)) {
      Chat chat = findChat(chatId);
      chats.remove(chat);

      ChatWidget widget = findChatWidget(chatId);
      if (widget != null) {
        widget.removeFromParent();
      }

      ChatView chatView = findChatView(chatId);
      if (chatView != null) {
        BeeKeeper.getScreen().closeWidget(chatView);
      }

      updateUnreadBadge();

      BeeKeeper.getStorage().remove(ChatUtils.getSizeStorageKey(chatId));
      BeeKeeper.getStorage().remove(ChatUtils.getStyleStorageKey(chatId));

      logger.info("removed chat", chatId, chat.getName());
      return true;

    } else {
      return false;
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void start() {
    setEnabled(getChatsCommand() != null && Endpoint.isOpen());

    if (isEnabled()) {
      updateUnreadBadge();
      initSound();

      if (timer == null) {
        this.timer = new Timer() {
          @Override
          public void run() {
            if (!chats.isEmpty()) {
              onTimer();
            }
          }
        };

        timer.scheduleRepeating(TIMER_PERIOD);
      }

    } else if (getChatsCommand() != null) {
      getChatsCommand().addStyleName(STYLE_CHATS_DISABLED);
    }
  }

  public void updateAssistantChat(boolean assistant) {
    if (assistant) {
      addAssistantChat();

    } else {
      removeChat(ASSISTANT_ID);
    }
  }

  private void addChat(Chat chat) {
    chats.add(chat);

    if (getChatsPanel() != null) {
      ChatWidget widget = new ChatWidget(chat);
      getChatsPanel().addChatWidget(widget);
    }

    updateUnreadBadge();
    logger.info("added chat", chat.getId(), chat.getName());
  }

  private void closeChatsPopup() {
    if (getChatsPanel() != null) {
      UiHelper.closeDialog(getChatsPanel());
    }
  }

  private void createChat(ChatSettings settings) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.CREATE_CHAT);
    if (!BeeUtils.isEmpty(settings.getName())) {
      params.addDataItem(COL_CHAT_NAME, settings.getName());
    }
    params.addDataItem(TBL_CHAT_USERS, DataUtils.buildIdList(settings.getUsers()));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(Chat.class)) {
          Chat chat = Chat.restore(response.getResponseAsString());
          addChat(chat);

          Endpoint.send(ChatStateMessage.add(chat));
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_CHATS);

          enterChat(chat.getId());
        }
      }
    });
  }

  private void editSettings(final Chat chat) {
    ChatSettings settings = new ChatSettings(chat);

    openSettings(settings, false, input -> {
      if (input.isValid() && contains(chat.getId())) {

        boolean nameChanged = !BeeUtils.equalsTrimRight(chat.getName(), input.getName());
        boolean usersChanged = !BeeUtils.sameElements(chat.getUsers(), input.getUsers());

        if (nameChanged || usersChanged) {
          ParameterList params = BeeKeeper.getRpc().createParameters(Service.UPDATE_CHAT);
          params.addQueryItem(COL_CHAT, chat.getId());

          if (nameChanged) {
            if (BeeUtils.isEmpty(input.getName())) {
              params.addDataItem(Service.VAR_CLEAR, COL_CHAT_NAME);
            } else {
              params.addDataItem(COL_CHAT_NAME, BeeUtils.trim(input.getName()));
            }
          }

          if (usersChanged) {
            params.addDataItem(TBL_CHAT_USERS, DataUtils.buildIdList(input.getUsers()));
          }

          BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              if (!response.hasErrors()) {
                if (nameChanged) {
                  chat.setName(input.getName());
                }
                if (usersChanged) {
                  BeeUtils.overwrite(chat.getUsers(), input.getUsers());
                }

                maybeRefreshChatWidget(chat);
                if (chatViewInFlowPanelMap != null && chatViewInFlowPanelMap.size() > 0) {
                  for (ChatView widget : chatViewInFlowPanelMap.values()) {
                    widget.onChatUpdate(chat);
                  }
                }

                ChatView chatView = findChatView(chat.getId());
                if (chatView != null) {
                  chatView.onChatUpdate(chat);
                }

                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_CHATS);
              }
            }
          });

        } else {
          logger.debug("settings not changed");
        }
      }
    });
  }

  private ChatWidget findChatWidget(long chatId) {
    if (getChatsPanel() == null) {
      return null;
    } else {
      return getChatsPanel().findChatWidget(chatId);
    }
  }

  private Widget getChatsCommand() {
    return chatsCommand;
  }

  private ChatsPanel getChatsPanel() {
    return chatsPanel;
  }

  private List<Chat> getSortedChats() {
    List<Chat> list = new ArrayList<>();

    if (!chats.isEmpty()) {
      list.addAll(chats);

      if (list.size() > 1) {
        Collections.sort(list);
      }
    }

    return list;
  }

  private Widget getUnreadBadge() {
    return unreadBadge;
  }

  private void initSound() {
    String fileName = Settings.getIncomingChatMessageSound();

    if (!BeeUtils.isEmpty(fileName) && incomingSound == null) {
      this.incomingSound = Document.get().createAudioElement();
      DomUtils.createId(incomingSound, "icm");

      incomingSound.setAutoplay(false);
      incomingSound.setControls(false);

      Double volume = Settings.getIncomingChatMessageVolume();
      if (volume != null) {
        incomingSound.setVolume(volume);
      }

      incomingSound.setSrc(Paths.getSoundPath(fileName));
      BodyPanel.conceal(incomingSound);
    }
  }

  private void maybeRefreshChatWidget(Chat chat) {
    ChatWidget chatWidget = findChatWidget(chat.getId());
    if (chatWidget != null) {
      chatWidget.render(chat);
    }
  }

  private void onTimer() {
    if (getChatsPanel() != null) {
      if (Previewer.getIdleMillis() > TIMER_PERIOD * 10
          && BeeKeeper.getUser().getPresence() != Presence.ONLINE) {

        closeChatsPopup();

      } else {
        for (Chat chat : chats) {
          ChatWidget widget = findChatWidget(chat.getId());
          if (widget != null) {
            widget.updateTime(chat.getMaxTime());
          }
        }
      }
    }
  }

  private void openSettings(ChatSettings settings, final boolean isNew,
      final Consumer<ChatSettings> consumer) {

    final ChatSettings result = new ChatSettings(settings);

    String caption = isNew ? Localized.dictionary().chatNew()
        : Localized.dictionary().chatSettings();
    final DialogBox dialog = DialogBox.create(caption, STYLE_CHAT_EDITOR_PREFIX + "dialog");

    HtmlTable table = new HtmlTable(STYLE_CHAT_EDITOR_PREFIX + "table");
    int row = 0;

    Label nameLabel = new Label(Localized.dictionary().chatName());
    table.setWidgetAndStyle(row, 0, nameLabel, STYLE_CHAT_EDITOR_PREFIX + "nameLabel");

    final InputText nameInput = new InputText();
    nameInput.setMaxLength(Data.getColumnPrecision(TBL_CHATS, COL_CHAT_NAME));
    if (!BeeUtils.isEmpty(settings.getName())) {
      nameInput.setValue(settings.getName());
    }
    table.setWidgetAndStyle(row, 1, nameInput, STYLE_CHAT_EDITOR_PREFIX + "nameInput");

    row++;
    Label usersLabel = new Label(Localized.dictionary().users());
    usersLabel.addStyleName(StyleUtils.NAME_REQUIRED);
    table.setWidgetAndStyle(row, 0, usersLabel, STYLE_CHAT_EDITOR_PREFIX + "usersLabel");

    final Long userId = BeeKeeper.getUser().getUserId();

    Relation relation = Relation.create(AdministrationConstants.VIEW_USERS,
        Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, ALS_POSITION_NAME, ALS_COMPANY_NAME));

    relation.setFilter(Filter.idNotIn(Collections.singleton(userId)));
    relation.disableNewRow();

    final MultiSelector usersWidget = MultiSelector.autonomous(relation,
        Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME));

    if (!isNew && !BeeUtils.isEmpty(settings.getUsers())) {
      List<Long> ids = new ArrayList<>(settings.getUsers());
      ids.remove(userId);

      if (!ids.isEmpty()) {
        usersWidget.setIds(ids);
      }
    }

    table.setWidgetAndStyle(row, 1, usersWidget, STYLE_CHAT_EDITOR_PREFIX + "usersInput");

    row++;
    Flow commands = new Flow();

    Button save = new Button(Localized.dictionary().actionSave());
    save.addStyleName(STYLE_CHAT_EDITOR_PREFIX + "save");

    save.addClickHandler(event -> {
      List<Long> users = usersWidget.getIds();
      if (users.isEmpty()) {
        usersWidget.setFocus(true);
        return;
      }

      result.setName(BeeUtils.trim(nameInput.getValue()));

      if (!users.contains(userId)) {
        users.add(0, userId);
      }
      BeeUtils.overwrite(result.getUsers(), users);

      dialog.close();
      consumer.accept(result);
    });

    commands.add(save);

    Button cancel = new Button(Localized.dictionary().actionCancel());
    cancel.addStyleName(STYLE_CHAT_EDITOR_PREFIX + "cancel");

    cancel.addClickHandler(event -> dialog.close());
    commands.add(cancel);

    table.setWidgetAndStyle(row, 1, commands, STYLE_CHAT_EDITOR_PREFIX + "commands");

    dialog.setWidget(table);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.focusOnOpen(usersWidget);

    dialog.center();
  }

  private void setChatsCommand(Widget chatsCommand) {
    this.chatsCommand = chatsCommand;
  }

  private void setChatsPanel(ChatsPanel chatsPanel) {
    this.chatsPanel = chatsPanel;
  }

  private void setUnreadBadge(Widget unreadBadge) {
    this.unreadBadge = unreadBadge;
  }

  private void showAll() {
    closeChatsPopup();

    Long userId = BeeKeeper.getUser().getUserId();
    if (DataUtils.isId(userId)) {
      Filter filter = Filter.or(Filter.equals(COL_CHAT_CREATOR, userId),
          Filter.in(Data.getIdColumn(VIEW_CHATS), VIEW_CHAT_USERS, COL_CHAT,
              Filter.equals(COL_CHAT_USER, userId)));

      GridFactory.openGrid(GRID_CHATS, new ChatGrid(), GridOptions.forFilter(filter));
    }
  }

  private void showChats() {
    List<Chat> list = getSortedChats();
    setChatsPanel(new ChatsPanel(list));

    Popup popup = new Popup(OutsideClick.CLOSE, STYLE_CHATS_POPUP);
    popup.setWidget(getChatsPanel());

    popup.setHideOnEscape(true);
    popup.addCloseHandler(event -> setChatsPanel(null));

    popup.showRelativeTo(getChatsCommand().getElement());
  }

  private static void showInfo(Chat chat) {
    HtmlTable table = new HtmlTable(STYLE_CHAT_INFO_PREFIX + "details");
    int row = 0;

    table.setText(row, 0, Localized.dictionary().captionId());
    table.setText(row, 2, BeeUtils.toString(chat.getId()));

    if (chat.getCreated() > 0) {
      row++;
      table.setText(row, 0, Localized.dictionary().creationDate());
      table.setText(row, 2, Format.renderDateTime(chat.getCreated()));
    }

    if (DataUtils.isId(chat.getCreator())) {
      row++;
      table.setText(row, 0, Localized.dictionary().creator());
      table.setText(row, 2, Global.getUsers().getSignature(chat.getCreator()));
    }

    if (!BeeUtils.isEmpty(chat.getName())) {
      row++;
      table.setText(row, 0, Localized.dictionary().chatName());
      table.setText(row, 2, chat.getName());
    }

    row++;
    table.setText(row, 0, Localized.dictionary().users());
    table.setText(row, 1, BeeUtils.bracket(BeeUtils.size(chat.getUsers())));
    if (!BeeUtils.isEmpty(chat.getUsers())) {
      table.setText(row, 2, BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
          Global.getUsers().getSignatures(chat.getUsers())));
    }

    row++;
    table.setText(row, 0, Localized.dictionary().chatUpdateTime());
    table.setText(row, 1, BeeUtils.bracket(chat.getMessageCount()));
    if (chat.getMaxTime() > 0) {
      table.setText(row, 2, BeeUtils.joinWords(ChatUtils.elapsed(chat.getMaxTime()),
          Format.renderDateTime(TimeUtils.dropMillis(chat.getMaxTime()))));
    }

    Global.showModalWidget(Localized.dictionary().chat(), table);
  }

  private void updateUnreadBadge() {
    if (getUnreadBadge() != null) {
      int count = 0;

      for (Chat chat : chats) {
        if (chat.getUnreadCount() > 0) {
          count += chat.getUnreadCount();
        }
      }

      String text = (count > 0) ? BeeUtils.toString(count) : BeeConst.STRING_EMPTY;
      getUnreadBadge().getElement().setInnerText(text);

      StyleUtils.setEmptiness(getUnreadBadge(), count <= 0);
    }
  }

  private void updateChat(Chat source) {
    Chat target = findChat(source.getId());

    if (target != null) {
      target.setName(source.getName());

      BeeUtils.overwrite(target.getUsers(), source.getUsers());

      target.setMessageCount(source.getMessageCount());
      target.setLastMessage(source.getLastMessage());

      maybeRefreshChatWidget(target);
    }
  }

}
