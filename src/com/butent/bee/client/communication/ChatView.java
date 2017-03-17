package com.butent.bee.client.communication;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.communication.ChatConstants.COL_CHAT_MESSAGE;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Search;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.js.Markdown;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.Chat;
import com.butent.bee.shared.communication.ChatItem;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.ChatMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ChatView extends Flow implements Presenter, View,
    VisibilityChangeEvent.Handler, HasWidgetSupplier {

  private static final class MessageWidget extends Flow {

    private final long time;
    private final Label timeLabel;

    private MessageWidget(ChatItem message, boolean addPhoto) {
      super(STYLE_MESSAGE_WRAPPER);

      this.time = message.getTime();

      this.timeLabel = new Label();
      timeLabel.addStyleName(STYLE_MESSAGE_TIME);
      ChatUtils.updateTime(timeLabel, time);
      add(timeLabel);

      Flow body = new Flow(STYLE_MESSAGE_BODY);

      if (addPhoto) {
        Image photo = Global.getUsers().getPhoto(message.getUserId());

        if (DataUtils.isId(message.getUserId())) {
            CustomDiv signature = new CustomDiv(STYLE_MESSAGE_SIGNATURE);
            signature.setText(Global.getUsers().getSignature(message.getUserId()));
            add(signature);
        }
        photo.addStyleName(STYLE_MESSAGE_PHOTO);
        body.add(photo);
      }

      if (message.hasText() && !BeeUtils.isEmpty(message.getLinkData())) {

        Flow linkContainer = new Flow(STYLE_MESSAGE_TEXT);
        for (String view : message.getLinkData().keySet()) {
          InternalLink link = new InternalLink(message.getText());
          link.addClickHandler(arg0 -> RowEditor.open(view,
              BeeUtils.toLong(message.getLinkData().get(view)), Opener.NEW_TAB));
          linkContainer.add(link);
        }

        body.add(linkContainer);

      } else if (message.hasText()) {
        Label text = new Label(Markdown.toHtml(message.getText()));
        text.addStyleName(STYLE_MESSAGE_TEXT);
        body.add(text);

      } else if (message.hasFiles()) {
        body.add(renderFiles(message));
      }

      add(body);

      if (message.hasText() && message.hasFiles()) {
        Flow fileBody = new Flow(STYLE_MESSAGE_BODY);
        fileBody.add(renderFiles(message));
        add(fileBody);
      }
    }

    private boolean refresh() {
      if (ChatUtils.needsRefresh(time)) {
        timeLabel.setText(ChatUtils.elapsed(time));
        return true;
      } else {
        return false;
      }
    }

    private static Widget renderFiles(ChatItem message) {
      Flow fileContainer = new Flow(STYLE_MESSAGE_FILES);

      for (FileInfo fileInfo : message.getFiles()) {
        Widget link = FileUtils.getLink(fileInfo);
        link.addStyleName(STYLE_MESSAGE_FILE);
        link.setTitle(BeeUtils.buildLines(fileInfo.getName(),
            FileUtils.sizeToText(fileInfo.getSize())));

        fileContainer.add(link);
      }

      return fileContainer;
    }
  }

  private enum WindowState {
    NORMAL, MINIMIZED, MAXIMIZED, UNKNOWN
  }

  private static final BeeLogger logger = LogUtils.getLogger(ChatView.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Chat-";

  private static final String STYLE_MAXIMIZED = STYLE_PREFIX + "maximized";
  private static final String STYLE_VIEW = STYLE_PREFIX + "view";
  private static final String STYLE_HAS_UNREAD = STYLE_PREFIX + "hasUnread";

  private static final String STYLE_MESSAGE_WRAPPER = STYLE_PREFIX + "message";

  private static final String STYLE_MESSAGE_PREFIX = STYLE_PREFIX + "message-";
  private static final String STYLE_MESSAGE_INCOMING = STYLE_MESSAGE_PREFIX + "incoming";
  private static final String STYLE_MESSAGE_OUTGOING = STYLE_MESSAGE_PREFIX + "outgoing";
  private static final String STYLE_MESSAGE_FAST = STYLE_MESSAGE_PREFIX + "fast";

  private static final String STYLE_MESSAGE_TIME = STYLE_MESSAGE_PREFIX + "time";
  private static final String STYLE_MESSAGE_PHOTO = STYLE_MESSAGE_PREFIX + "photo";
  private static final String STYLE_MESSAGE_BODY = STYLE_MESSAGE_PREFIX + "body";
  private static final String STYLE_MESSAGE_SIGNATURE = STYLE_MESSAGE_PREFIX + "signature";
  private static final String STYLE_MESSAGE_TEXT = STYLE_MESSAGE_PREFIX + "text";
  private static final String STYLE_MESSAGE_FILES = STYLE_MESSAGE_PREFIX + "files";
  private static final String STYLE_MESSAGE_FILE = STYLE_MESSAGE_PREFIX + "file";

  private static final String STYLE_AUTO_SCROLL_PREFIX = STYLE_PREFIX + "autoScroll-";
  private static final String STYLE_AUTO_SCROLL_CONTAINER = STYLE_AUTO_SCROLL_PREFIX + "container";
  private static final String STYLE_AUTO_SCROLL_LABEL = STYLE_AUTO_SCROLL_PREFIX + "label";
  private static final String STYLE_AUTO_SCROLL_TOGGLE = STYLE_AUTO_SCROLL_PREFIX + "toggle";

  private static final String AUTO_SCROLL_LABEL = "Auto Scroll";

  private static final int TIMER_PERIOD = 5_000;
  private static final long FAST_INTERVAL = 30_000;

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.VIEW);

  private final long chatId;
  private final List<Long> otherUsers;

  private final HeaderView headerView;
  private final Toggle autoScrollToggle;

  private final Flow messagePanel;
  private final InputArea inputArea;
  private final FileCollector fileCollector;
  private final Flow onlinePanel;

  private final Timer timer;
  private long lastMessageTime;

  private boolean enabled = true;

  private final List<HandlerRegistration> registry = new ArrayList<>();

  public ChatView(Chat chat, Set<Action> enabledActions, Set<Action> hiddenActions,
      boolean showAutoScroll) {
    super(STYLE_VIEW);
    addStyleName(UiOption.getStyleName(uiOptions));

    this.chatId = chat.getId();
    this.otherUsers = ChatUtils.getOtherUsers(chat.getUsers());

    String caption = ChatUtils.getChatCaption(chat.getName(), otherUsers);

    this.headerView = new HeaderImpl();
    headerView.create(caption, false, true, null, uiOptions,
        enabledActions,
        Action.NO_ACTIONS, hiddenActions);

    headerView.setViewPresenter(this);

    Flow autoScrollContainer = new Flow(STYLE_AUTO_SCROLL_CONTAINER);

    Label autoScrollLabel = new Label(AUTO_SCROLL_LABEL);
    autoScrollLabel.addStyleName(STYLE_AUTO_SCROLL_LABEL);
    autoScrollContainer.add(autoScrollLabel);

    this.autoScrollToggle = new Toggle(FontAwesome.TOGGLE_OFF, FontAwesome.TOGGLE_ON,
        STYLE_AUTO_SCROLL_TOGGLE, true);
    autoScrollToggle.addStyleName(FaLabel.STYLE_NAME);
    autoScrollToggle.addClickHandler(event -> maybeScroll(false));
    autoScrollContainer.add(autoScrollToggle);

    if (showAutoScroll) {
      headerView.addCommandItem(autoScrollContainer);
    }

    add(headerView);

    this.messagePanel = new Flow(STYLE_PREFIX + "messages");
    StyleUtils.setTop(messagePanel, headerView.getHeight());
    add(messagePanel);

    this.inputArea = new InputArea();
    inputArea.addStyleName(STYLE_PREFIX + "inputArea");
    inputArea.setMaxLength(TextMessage.MAX_LENGTH);

    inputArea.addKeyDownHandler(event -> {
      if (isSubmit(event.getNativeEvent()) && compose()) {
        event.preventDefault();
        event.stopPropagation();

        inputArea.clearValue();
      }
    });

    Flow inputPanel = new Flow(STYLE_PREFIX + "inputPanel");
    inputPanel.add(inputArea);

    FaLabel submit = new FaLabel(FontAwesome.REPLY_ALL);
    submit.addStyleName(STYLE_PREFIX + "submit");
    submit.setTitle(Localized.dictionary().send());

    submit.addClickHandler(event -> {
      if (compose()) {
        inputArea.clearValue();
      }
    });

    this.fileCollector = FileCollector.headless(this::addFiles);
    fileCollector.bindDnd(this);

    FaLabel attach = new FaLabel(FontAwesome.PAPERCLIP);
    attach.addStyleName(STYLE_PREFIX + "attach");
    attach.setTitle(Localized.dictionary().chooseFiles());
    attach.addClickHandler(event -> fileCollector.clickInput());

    Flow commandPanel = new Flow(STYLE_PREFIX + "commandPanel");
    commandPanel.add(submit);
    commandPanel.add(attach);
    commandPanel.add(fileCollector);

    this.onlinePanel = new Flow(STYLE_PREFIX + "onlinePanel");

    Flow footer = new Flow(STYLE_PREFIX + "footer");
    footer.add(inputPanel);
    footer.add(commandPanel);
    footer.add(onlinePanel);

    add(footer);

    if (!chat.getMessages().isEmpty()) {
      for (ChatItem message : chat.getMessages()) {
        addMessage(message, false);
      }

      updateUnreadCount(chat.getUnreadCount());
    }

    updateOnlinePanel();

    this.timer = new Timer() {
      @Override
      public void run() {
        onTimer();
      }
    };
  }

  public ChatView(Chat chat) {
    this(chat, EnumSet.of(Action.CONFIGURE, Action.MINIMIZE, Action.MAXIMIZE, Action.CLOSE),
        Action.NO_ACTIONS, true);
  }

  public void addMessage(ChatItem message, boolean update) {
    if (message != null
        && (message.isValid() || Global.getChatManager().isAssistant(message.getUserId()))) {
      boolean incoming = !BeeKeeper.getUser().is(message.getUserId());

      MessageWidget messageWidget = new MessageWidget(message, incoming);

      messageWidget.addStyleName(incoming ? STYLE_MESSAGE_INCOMING : STYLE_MESSAGE_OUTGOING);
      if (message.getTime() - getLastMessageTime() < FAST_INTERVAL) {
        messageWidget.addStyleName(STYLE_MESSAGE_FAST);
      }

      messagePanel.add(messageWidget);
      setLastMessageTime(message.getTime());

      if (update) {
        if (incoming) {
          maybeScroll(true);
        } else {
          DomUtils.scrollToBottom(messagePanel);
        }
      }
    }
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public String getCaption() {
    return headerView.getCaption();
  }

  public long getChatId() {
    return chatId;
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public String getIdPrefix() {
    return "chat";
  }

  public InputArea getInputArea() {
    return inputArea;
  }

  @Override
  public View getMainView() {
    return this;
  }

  public ChatPopup getPopup() {
    if (getParent() instanceof ChatPopup) {
      return (ChatPopup) getParent();
    } else {
      return null;
    }
  }

  @Override
  public String getSupplierKey() {
    return ViewFactory.SupplierKind.CHAT.getKey(BeeUtils.toString(chatId));
  }

  @Override
  public String getViewKey() {
    return getSupplierKey();
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case CONFIGURE:
        Global.getChatManager().configure(getChatId());
        break;

      case MINIMIZE:
        minimize();
        break;

      case MAXIMIZE:
        maximize();
        break;

      case CANCEL:
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public boolean isInteractive() {
    switch (getWindowState()) {
      case MAXIMIZED:
        return DomUtils.isVisible(this);
      case NORMAL:
        return true;
      default:
        return false;
    }
  }

  public void onChatUpdate(Chat chat) {
    Assert.notNull(chat);

    BeeUtils.overwrite(otherUsers, ChatUtils.getOtherUsers(chat.getUsers()));

    headerView.setCaption(ChatUtils.getChatCaption(chat.getName(), otherUsers));
    updateOnlinePanel();

    if (isMaximized()) {
      BeeKeeper.getScreen().onWidgetChange(this);
    }
  }

  @Override
  public void onViewUnload() {
  }

  public void onUserPresenceChange() {
    updateOnlinePanel();
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      maybeScroll(false);
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    return EnumUtils.in(action, Action.CONFIGURE, Action.PRINT, Action.CANCEL, Action.CLOSE);
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  public void updateUnreadCount(int unreadCount) {
    String text = (unreadCount > 0) ? BeeUtils.toString(unreadCount) : BeeConst.STRING_EMPTY;
    headerView.setMessage(0, text, null);

    setStyleName(STYLE_HAS_UNREAD, unreadCount > 0);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    setStyleName(STYLE_MAXIMIZED, isMaximized());

    EventUtils.clearRegistry(registry);
    registry.add(VisibilityChangeEvent.register(this));

    maybeScroll(false);
    timer.scheduleRepeating(TIMER_PERIOD);

    ReadyEvent.fire(this);
  }

  @Override
  protected void onUnload() {
    timer.cancel();
    EventUtils.clearRegistry(registry);

    super.onUnload();
  }

  private void addFiles(Collection<? extends FileInfo> input) {
    if (!BeeUtils.isEmpty(input)) {
      Latch latch = new Latch(input.size());
      List<FileInfo> files = new ArrayList<>();

      for (FileInfo fileInfo : input) {
        FileUtils.commitFile(fileInfo, info -> {
          files.add(FileInfo.restore(info.serialize()));
          latch.decrement();

          if (latch.isOpen()) {
            sendFiles(files);
          }
        });
      }

      fileCollector.clear();
    }
  }

  private void sendFiles(List<FileInfo> files) {
    if (!BeeUtils.isEmpty(files)) {
      ChatItem item = new ChatItem(BeeKeeper.getUser().getUserId(), files);
      send(item);
    }
  }

  private void send(ChatItem item) {
    if (!Endpoint.isOpen()) {
      logger.warning("cannot send message");
    }

    ChatMessage chatMessage = new ChatMessage(chatId, item);

    Global.getChatManager().addMessage(chatMessage);

    if (Global.getChatManager().isAssistant(chatMessage.getChatId())) {
      Search.doQuery(chatMessage.getChatItem().getText(), null);
      return;
    }

    ParameterList params = BeeKeeper.getRpc().createParameters(Service.SEND_CHAT_MESSAGE);
    params.addDataItem(COL_CHAT_MESSAGE, chatMessage.encode());

    String from = Endpoint.getSessionId();
    if (!BeeUtils.isEmpty(from)) {
      params.addDataItem(Service.VAR_FROM, from);
    }

    BeeKeeper.getRpc().makeRequest(params);
  }

  private boolean autoScroll() {
    return autoScrollToggle.isChecked() || getPopup() != null;
  }

  private boolean compose() {
    String text = BeeUtils.trim(inputArea.getValue());
    if (BeeUtils.isEmpty(text)) {
      return false;
    }

    ChatItem item = new ChatItem(BeeKeeper.getUser().getUserId(), text);
    send(item);

    return true;
  }

  private long getLastMessageTime() {
    return lastMessageTime;
  }

  private WindowState getWindowState() {
    if (!isAttached()) {
      return WindowState.UNKNOWN;

    } else if (getPopup() == null) {
      return WindowState.MAXIMIZED;

    } else {
      return getPopup().isMinimized() ? WindowState.MINIMIZED : WindowState.NORMAL;
    }
  }

  private boolean isMaximized() {
    return getWindowState() == WindowState.MAXIMIZED;
  }

  public static boolean isSubmit(NativeEvent event) {
    return event != null && event.getKeyCode() == KeyCodes.KEY_ENTER
        && !EventUtils.hasModifierKey(event);
  }

  private void maximize() {
    if (getPopup() != null) {
      Global.getChatManager().markAsRead(chatId);

      if (getPopup().isMinimized()) {
        getPopup().setMinimized(false);

      } else {
        getPopup().close();
        BeeKeeper.getScreen().showInNewPlace(this);
      }
    }
  }

  private void maybeScroll(boolean checkVisibility) {
    if (autoScroll() && messagePanel.getWidgetCount() > 1
        && (!checkVisibility || DomUtils.isVisible(this))) {
      DomUtils.scrollToBottom(messagePanel);
    }
  }

  private void minimize() {
    if (getPopup() != null) {
      getPopup().setMinimized(true);

    } else if (isMaximized() && BeeKeeper.getScreen().closeWidget(this)) {
      ChatPopup.openMinimized(this);
    }
  }

  private void onTimer() {
    int count = messagePanel.getWidgetCount();

    if (count > 0) {
      for (int i = count - 1; i >= 0; i--) {
        Widget widget = messagePanel.getWidget(i);

        if (widget instanceof MessageWidget) {
          boolean updated = ((MessageWidget) widget).refresh();
          if (!updated) {
            break;
          }
        }
      }
    }
  }

  private void setLastMessageTime(long lastMessageTime) {
    this.lastMessageTime = lastMessageTime;
  }

  private void updateOnlinePanel() {
    if (!onlinePanel.isEmpty()) {
      onlinePanel.clear();
    }

    for (Long userId : otherUsers) {
      UserData userData = Global.getUsers().getUserData(userId);
      if (userData != null) {
        CustomDiv label = new CustomDiv(STYLE_PREFIX + "userLabel");
        label.setText(userData.getFirstName());
        label.setTitle(userData.getUserSign());

        onlinePanel.add(label);
      }

      Presence presence = Global.getUsers().getUserPresence(userId);
      if (presence != null) {
        FaLabel icon = new FaLabel(presence.getIcon(), presence.getStyleName());
        icon.addStyleName(STYLE_PREFIX + "userPresence");
        icon.setTitle(presence.getCaption());

        onlinePanel.add(icon);
      }
    }
  }
}
