package com.butent.bee.client.websocket;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.geolocation.client.Position;
import com.google.gwt.geolocation.client.Position.Coordinates;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.cli.CliWorker;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.NotificationOptions;
import com.butent.bee.client.dialog.WebNotification;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.maps.MapUtils;
import com.butent.bee.client.modules.mail.MailKeeper;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Paragraph;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.communication.ChatRoom;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.data.PropertiesData;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.mail.MailConstants.MessageFlag;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.websocket.SessionUser;
import com.butent.bee.shared.websocket.WsUtils;
import com.butent.bee.shared.websocket.messages.AdminMessage;
import com.butent.bee.shared.websocket.messages.ChatMessage;
import com.butent.bee.shared.websocket.messages.EchoMessage;
import com.butent.bee.shared.websocket.messages.InfoMessage;
import com.butent.bee.shared.websocket.messages.LocationMessage;
import com.butent.bee.shared.websocket.messages.LogMessage;
import com.butent.bee.shared.websocket.messages.MailMessage;
import com.butent.bee.shared.websocket.messages.Message;
import com.butent.bee.shared.websocket.messages.ModificationMessage;
import com.butent.bee.shared.websocket.messages.NotificationMessage;
import com.butent.bee.shared.websocket.messages.OnlineMessage;
import com.butent.bee.shared.websocket.messages.ProgressMessage;
import com.butent.bee.shared.websocket.messages.RoomStateMessage;
import com.butent.bee.shared.websocket.messages.RoomUserMessage;
import com.butent.bee.shared.websocket.messages.RoomsMessage;
import com.butent.bee.shared.websocket.messages.SessionMessage;
import com.butent.bee.shared.websocket.messages.ShowMessage;
import com.butent.bee.shared.websocket.messages.ShowMessage.Subject;
import com.butent.bee.shared.websocket.messages.UsersMessage;

import java.util.List;
import java.util.Objects;

class MessageDispatcher {

  private static BeeLogger logger = LogUtils.getLogger(MessageDispatcher.class);

  private static final String CONVERSATION_STYLE_PREFIX = "bee-Conversation-";
  private static final String CONVERSATION_MESSAGE_STYLE_PREFIX = CONVERSATION_STYLE_PREFIX
      + "message-";

  private static void onNotification(final NotificationMessage notificationMessage) {
    int size = notificationMessage.getMessages().size();
    TextMessage lastMessage = notificationMessage.getMessages().get(size - 1);

    Long fromUser = Global.getUsers().getUserIdBySession(notificationMessage.getFrom());
    if (fromUser == null) {
      fromUser = lastMessage.getUserId();
    }

    UserData userData = Global.getUsers().getUserData(fromUser);
    String title = (userData == null) ? BeeUtils.joinWords("Message from user", fromUser)
        : userData.getUserSign();

    Icon icon = BeeUtils.isEmpty(notificationMessage.getIcon()) ? null
        : EnumUtils.getEnumByName(Icon.class, notificationMessage.getIcon());

    switch (notificationMessage.getDisplayMode()) {
      case DIALOG:
        final DialogBox dialog = DialogBox.create(title, CONVERSATION_STYLE_PREFIX + "dialog");
        Flow container = new Flow(CONVERSATION_STYLE_PREFIX + "container");

        HtmlTable table = new HtmlTable(CONVERSATION_STYLE_PREFIX + "messages");
        int row = 0;

        for (TextMessage message : notificationMessage.getMessages()) {
          if (message != null && message.isValid()) {
            if (size == 1 && icon != null) {
              Image image = new Image(icon.getImageResource());
              table.setWidgetAndStyle(row, 0, image, CONVERSATION_MESSAGE_STYLE_PREFIX + "icon");
            } else {
              Image photo = Global.getUsers().getPhoto(message.getUserId());
              if (photo != null) {
                table.setWidgetAndStyle(row, 0, photo, CONVERSATION_MESSAGE_STYLE_PREFIX + "photo");
              }
            }

            Flow content = new Flow();

            String signature = Global.getUsers().getSignature(message.getUserId());
            if (!BeeUtils.isEmpty(signature)) {
              Label header = new Label(signature);
              header.addStyleName(CONVERSATION_MESSAGE_STYLE_PREFIX + "header");
              content.add(header);
            }

            Paragraph body = new Paragraph(CONVERSATION_MESSAGE_STYLE_PREFIX + "body");
            body.setHtml(message.getText());
            content.add(body);

            table.setWidgetAndStyle(row, 1, content, CONVERSATION_MESSAGE_STYLE_PREFIX + "content");
            row++;
          }
        }

        final Simple wrapper = new Simple(table);
        wrapper.addStyleName(CONVERSATION_STYLE_PREFIX + "wrapper");

        container.add(wrapper);

        final InputText input = new InputText();
        input.addStyleName(CONVERSATION_STYLE_PREFIX + "input");
        input.setMaxLength(NotificationMessage.MAX_LENGTH);

        input.addKeyDownHandler(new KeyDownHandler() {
          @Override
          public void onKeyDown(KeyDownEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !input.isEmpty()) {
              if (reply(notificationMessage, input.getValue())) {
                dialog.close();
              }
            }
          }
        });

        container.add(input);

        Flow commands = new Flow(CONVERSATION_STYLE_PREFIX + "commands");

        FaLabel reply = new FaLabel(FontAwesome.REPLY);
        reply.addStyleName(CONVERSATION_STYLE_PREFIX + "reply");

        reply.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (input.isEmpty()) {
              input.setFocus(true);
            } else if (reply(notificationMessage, input.getValue())) {
              dialog.close();
            }
          }
        });

        commands.add(reply);

        FaLabel cancel = new FaLabel(FontAwesome.TIMES);
        cancel.addStyleName(CONVERSATION_STYLE_PREFIX + "cancel");

        cancel.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            dialog.close();
          }
        });

        commands.add(cancel);

        container.add(commands);
        dialog.setWidget(container);

        dialog.setAnimationEnabled(true);
        dialog.setHideOnEscape(true);

        dialog.addOpenHandler(new OpenEvent.Handler() {
          @Override
          public void onOpen(OpenEvent event) {
            DomUtils.scrollToBottom(wrapper);
            input.setFocus(true);
          }
        });

        dialog.center();
        break;

      case EXTERNAL_NOTIFICATION:
        NotificationOptions options = new NotificationOptions();
        options.setBody(lastMessage.getText());

        if (icon != null) {
          options.setIcon(icon.getImageResource().getSafeUri().asString());
        } else if (userData != null && !BeeUtils.isEmpty(userData.getPhotoFileName())) {
          options.setIcon(PhotoRenderer.getUrl(userData.getPhotoFileName()));
        }

        WebNotification.create(title, options, null);
        break;

      case INTERNAL_NOTIFICATION:
        String line1 = title + BeeConst.STRING_COLON;
        String line2 = (icon == null) ? lastMessage.getText()
            : BeeUtils.joinWords(Images.asString(icon.getImageResource()), lastMessage.getText());

        if (Icon.ALARM == icon || Icon.ERROR == icon) {
          BeeKeeper.getScreen().notifySevere(line1, line2);
        } else if (Icon.WARNING == icon) {
          BeeKeeper.getScreen().notifyWarning(line1, line2);
        } else {
          BeeKeeper.getScreen().notifyInfo(line1, line2);
        }
        break;

      case POPUP:
        Global.messageBox(title, icon, lastMessage.getText());
        break;
    }
  }

  private static boolean reply(NotificationMessage notificationMessage, String text) {
    Long userId = BeeKeeper.getUser().getUserId();
    if (userId == null) {
      return false;
    }

    if (!Global.getUsers().isOpen(notificationMessage.getFrom())) {
      String signature = Global.getUsers().getSignature(notificationMessage.getUserId());
      String msg = BeeUtils.joinWords(signature, "disconnected");

      logger.warning(msg);
      BeeKeeper.getScreen().notifyWarning(msg);
      return false;
    }

    TextMessage textMessage = new TextMessage(userId, text);
    if (textMessage.isValid()) {
      Endpoint.send(notificationMessage.reply(textMessage));
      return true;
    } else {
      return false;
    }
  }

  MessageDispatcher() {
  }

  void dispatch(Message message) {
    String caption;

    switch (message.getType()) {
      case ADMIN:
        AdminMessage am = (AdminMessage) message;

        if (!BeeUtils.isEmpty(am.getResponse())) {
          logger.debug(message.getClass().getSimpleName(), "response", am.getResponse());

        } else if (!BeeUtils.isEmpty(am.getCommand())) {
          boolean ok = CliWorker.execute(am.getCommand(), false);
          if (!ok) {
            Endpoint.send(AdminMessage.response(Endpoint.getSessionId(), am.getFrom(),
                "command not recognized: " + am.getCommand()));
          }

        } else {
          WsUtils.onEmptyMessage(message);
        }
        break;

      case CHAT:
        ChatMessage chatMessage = (ChatMessage) message;

        if (chatMessage.isValid()) {
          Global.getRooms().addMessage(chatMessage);
        } else {
          WsUtils.onEmptyMessage(message);
        }
        break;

      case ECHO:
        BeeKeeper.getScreen().notifyInfo(((EchoMessage) message).getText());
        break;

      case INFO:
        caption = ((InfoMessage) message).getCaption();
        List<Property> info = ((InfoMessage) message).getInfo();

        if (BeeUtils.isEmpty(info)) {
          WsUtils.onEmptyMessage(message);
        } else {
          Global.showTable(caption, new PropertiesData(info));
        }
        break;

      case LOCATION:
        LocationMessage lm = (LocationMessage) message;
        final String from = lm.getFrom();

        if (lm.isQuery()) {
          MapUtils.getCurrentPosition(new Consumer<Position>() {
            @Override
            public void accept(Position input) {
              Coordinates coords = input.getCoordinates();
              Endpoint.send(LocationMessage.coordinates(Endpoint.getSessionId(), from,
                  coords.getLatitude(), coords.getLongitude(), coords.getAccuracy()));
            }
          }, new Consumer<String>() {
            @Override
            public void accept(String input) {
              Endpoint.send(LocationMessage.response(Endpoint.getSessionId(), from, input));
            }
          });

        } else if (lm.hasCoordinates()) {
          String title = Global.getUsers().getUserSignatureBySession(from);
          if (BeeUtils.isPositive(lm.getAccuracy())) {
            caption = BeeUtils.joinWords(title, BeeConst.CHAR_PLUS_MINUS, lm.getAccuracy(), "m");
          } else {
            caption = title;
          }

          MapUtils.showPosition(caption, lm.getLatitude(), lm.getLongitude(), title);

        } else if (!BeeUtils.isEmpty(lm.getResponse())) {
          logger.debug(message.getClass().getSimpleName(), "response", lm.getResponse());

        } else {
          WsUtils.onEmptyMessage(message);
        }
        break;

      case LOG:
        LogMessage logMessage = (LogMessage) message;

        if (logMessage.getLevel() != null && !BeeUtils.isEmpty(logMessage.getText())) {
          logger.log(logMessage.getLevel(), logMessage.getText());
        } else {
          WsUtils.onEmptyMessage(message);
        }
        break;

      case MAIL:
        MailMessage mailMessage = (MailMessage) message;

        if (mailMessage.isValid()) {
          boolean refreshFolders = mailMessage.messagesUpdated() || mailMessage.foldersUpdated()
              || Objects.equals(mailMessage.getFlag(), MessageFlag.SEEN);

          if (Global.getNewsAggregator().hasSubscription(Feed.MAIL) && refreshFolders) {
            Global.getNewsAggregator().refresh();
          }
          MailKeeper.refreshActivePanel(refreshFolders, mailMessage.getFolderId());

        } else {
          logger.severe(mailMessage.getError());
          BeeKeeper.getScreen().notifySevere(mailMessage.getError());
        }
        break;

      case MODIFICATION:
        ModificationMessage modificationMessage = (ModificationMessage) message;

        if (modificationMessage.isValid()) {
          ModificationEvent<?> event = modificationMessage.getEvent();
          event.setLocality(Locality.ENTANGLED);

          BeeKeeper.getBus().fireEvent(modificationMessage.getEvent());

        } else {
          WsUtils.onEmptyMessage(message);
        }
        break;

      case NOTIFICATION:
        NotificationMessage notificationMessage = (NotificationMessage) message;

        if (notificationMessage.isValid()) {
          onNotification(notificationMessage);
        } else {
          WsUtils.onEmptyMessage(message);
        }
        break;

      case ONLINE:
        List<SessionUser> sessionUsers = ((OnlineMessage) message).getSessionUsers();
        if (sessionUsers.size() > 1) {
          for (int i = 0; i < sessionUsers.size() - 1; i++) {
            SessionUser sessionUser = sessionUsers.get(i);
            Global.getUsers().addSession(sessionUser.getSessionId(), sessionUser.getUserId(), true);
          }
        }

        if (sessionUsers.isEmpty()) {
          WsUtils.onEmptyMessage(message);
        } else {
          Endpoint.setSessionId(sessionUsers.get(sessionUsers.size() - 1).getSessionId());
        }
        break;

      case PROGRESS:
        ProgressMessage pm = (ProgressMessage) message;
        String progressId = pm.getProgressId();

        if (BeeUtils.isEmpty(progressId)) {
          WsUtils.onEmptyMessage(message);

        } else if (!Endpoint.handleProgress(pm)) {
          if (pm.isActivated()) {
            boolean started = Endpoint.startPropgress(progressId);
            if (started) {
              logger.debug("progress", progressId, "started");
            } else {
              logger.warning("cannot start progress", progressId);
            }
          } else if (pm.isUpdate()) {
            BeeKeeper.getScreen().updateProgress(progressId, pm.getValue());

          } else if (pm.isCanceled() || pm.isClosed()) {
            Endpoint.removeProgress(progressId);

          } else {
            WsUtils.onInvalidState(message);
          }
        }
        break;

      case ROOM_STATE:
        RoomStateMessage rsm = (RoomStateMessage) message;

        if (rsm.isValid()) {
          Global.getRooms().onRoomState(rsm);
        } else {
          WsUtils.onInvalidState(message);
        }
        break;

      case ROOM_USER:
        Global.getRooms().onRoomUser((RoomUserMessage) message);
        break;

      case ROOMS:
        List<ChatRoom> rooms = ((RoomsMessage) message).getData();

        if (BeeUtils.isEmpty(rooms)) {
          WsUtils.onEmptyMessage(message);
        } else {
          Global.getRooms().setRoomData(rooms);
        }
        break;

      case SESSION:
        SessionMessage sm = (SessionMessage) message;
        SessionUser su = sm.getSessionUser();

        if (sm.isOpen()) {
          Global.getUsers().addSession(su.getSessionId(), su.getUserId(), false);
        } else if (sm.isClosed()) {
          Global.getUsers().removeSession(su.getSessionId());
        } else {
          WsUtils.onInvalidState(message);
        }
        break;

      case SHOW:
        Subject subject = ((ShowMessage) message).getSubject();
        if (subject == Subject.SESSION) {
          Global.showTable(subject.getCaption(), new PropertiesData(Endpoint.getInfo()));
        } else {
          WsUtils.onInvalidState(message);
        }
        break;

      case USERS:
        List<UserData> users = ((UsersMessage) message).getData();

        if (BeeUtils.isEmpty(users)) {
          WsUtils.onEmptyMessage(message);

        } else {
          Global.getUsers().updateUserData(users);

          for (UserData userData : users) {
            if (Objects.equals(BeeKeeper.getUser().getUserId(), userData.getUserId())) {
              BeeKeeper.getUser().setUserData(userData);
              BeeKeeper.getScreen().updateUserData(userData);
              break;
            }
          }

          BeeKeeper.onRightsChange();
        }
        break;
    }
  }
}
