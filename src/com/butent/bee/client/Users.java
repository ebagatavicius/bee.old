package com.butent.bee.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Badge;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.communication.TextMessage;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.LocationMessage;
import com.butent.bee.shared.websocket.messages.NotificationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Users {

  private static final class OnlinePanel extends Flow {

    private OnlinePanel() {
      super(STYLE_PREFIX + "panel");
    }

    private OnlineWidget findBySession(String sessionId) {
      for (Widget widget : this) {
        if (widget instanceof OnlineWidget
            && ((OnlineWidget) widget).getSessionId().equals(sessionId)) {
          return (OnlineWidget) widget;
        }
      }

      logger.warning("widget not found for session", sessionId);
      return null;
    }
  }

  private static final class OnlineWidget extends Flow {

    private static final int NAME_INDEX = 0;

    private final String sessionId;

    private final Long userId;
    private final Long personId;

    private OnlineWidget(String sessionId, UserData userData) {
      super(STYLE_PREFIX + "item");

      this.sessionId = sessionId;
      this.userId = userData.getUserId();
      this.personId = userData.getPerson();

      CustomDiv nameWidget = new CustomDiv(STYLE_PREFIX + "name");
      nameWidget.setText(userData.getUserSign());

      nameWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          RowEditor.open(ClassifierConstants.VIEW_PERSONS, personId, Opener.MODAL);
        }
      });

      add(nameWidget);

      FaLabel tweetWidget = new FaLabel(FontAwesome.TWITTER);
      tweetWidget.addStyleName(STYLE_PREFIX + "tweet");

      tweetWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Global.inputString(Global.getUsers().getFirstName(userId), null, new StringCallback() {
            @Override
            public void onSuccess(String value) {
              String fromSession = Endpoint.getSessionId();
              Long fromUser = BeeKeeper.getUser().getUserId();

              if (!BeeUtils.isEmpty(fromSession) && fromUser != null) {
                Endpoint.send(NotificationMessage.dialog(fromSession, getSessionId(),
                    new TextMessage(fromUser, BeeUtils.trim(value))));
              }
            }
          }, null, null, NotificationMessage.MAX_LENGTH);
        }
      });

      add(tweetWidget);

      FaLabel homeWidget = new FaLabel(FontAwesome.HOME);
      homeWidget.addStyleName(STYLE_PREFIX + "home");

      homeWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          String from = Endpoint.getSessionId();
          if (!BeeUtils.isEmpty(from)) {
            Endpoint.send(LocationMessage.query(from, getSessionId()));
          }
        }
      });

      add(homeWidget);
    }

    private String getSessionId() {
      return sessionId;
    }

    private void updateData(UserData userData) {
      if (userData != null) {
        getWidget(NAME_INDEX).getElement().setInnerText(userData.getUserSign());
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Users.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Online-";

  private Map<Long, UserData> users = new HashMap<>();

  private final Map<String, Long> openSessions = new LinkedHashMap<>();
  private final Map<String, Presence> sessionPresence = new HashMap<>();

  private final OnlinePanel onlinePanel = new OnlinePanel();

  private Badge sizeBadge;

  Users() {
  }

  public void addSession(String sessionId, long userId, Presence presence) {
    Assert.notEmpty(sessionId, "attempt to add empty session");
    if (openSessions.containsKey(sessionId)) {
      logger.severe("session", sessionId, "already exists");
      return;
    }

    UserData userData = users.get(userId);
    if (userData == null) {
      logger.severe("user data not available", userId);
      return;
    }

    openSessions.put(sessionId, userId);
    if (presence != null) {
      sessionPresence.put(sessionId, presence);
    }

    OnlineWidget widget = new OnlineWidget(sessionId, userData);
    onlinePanel.add(widget);

    BeeKeeper.getScreen().updateUserCount(openSessions.size());
    updateHeader();
  }

  public Set<String> getAllSessions() {
    return openSessions.keySet();
  }

  public String getFirstName(Long userId) {
    UserData userData = getUserData(userId);
    return (userData == null) ? null : userData.getFirstName();
  }

  @Deprecated
  public IdentifiableWidget getOnlinePanel() {
    return onlinePanel;
  }

  public Image getPhoto(Long userId) {
    Image image;

    if (DataUtils.isId(userId) && Objects.nonNull(getUserData(userId))) {
      UserData userData = getUserData(userId);
      image = new Image(PhotoRenderer.getPhotoUrl(userData.getPhotoFile()));
      image.setAlt(userData.getLogin());
      image.setTitle(userData.getUserSign());
    } else {
      image = new Image(PhotoRenderer.getPhotoUrl(null));
    }
    return image;
  }

  public Presence getPresenceBySession(String sessionId) {
    return sessionPresence.get(sessionId);
  }

  public Set<String> getSessions(Collection<Long> userIds) {
    Set<String> result = new HashSet<>();

    if (!BeeUtils.isEmpty(userIds) && !openSessions.isEmpty()) {
      for (Map.Entry<String, Long> entry : openSessions.entrySet()) {
        if (userIds.contains(entry.getValue())) {
          result.add(entry.getKey());
        }
      }
    }
    return result;
  }

  public String getSignature(Long userId) {
    UserData userData = getUserData(userId);
    return (userData == null) ? null : userData.getUserSign();
  }

  public List<String> getSignatures(Collection<Long> userIds) {
    List<String> result = new ArrayList<>();
    if (BeeUtils.isEmpty(userIds)) {
      return result;
    }

    for (Long userId : userIds) {
      String signature = getSignature(userId);
      if (!BeeUtils.isEmpty(signature)) {
        result.add(signature);
      }
    }

    return result;
  }

  public Map<Long, UserData> getUserData() {
    return users;
  }

  public UserData getUserData(Long userId) {
    UserData userData = users.get(userId);
    if (userData == null) {
      logger.warning(NameUtils.getName(this), "user not found:", userId);
    }
    return userData;
  }

  public Long getUserIdBySession(String sessionId) {
    return openSessions.get(sessionId);
  }

  public Presence getUserPresence(Long userId) {
    Presence result = Presence.OFFLINE;
    if (!DataUtils.isId(userId)) {
      return result;
    }

    for (String session : sessionPresence.keySet()) {
      if (userId.equals(openSessions.get(session))) {
        Presence p = sessionPresence.get(session);

        if (p != null && BeeUtils.isLess(p, result)) {
          result = p;
        }
      }
    }
    return result;
  }

  public String getUserSignatureBySession(String sessionId) {
    Long userId = openSessions.get(sessionId);
    UserData userData = (userId == null) ? null : users.get(userId);

    return (userData == null) ? null : userData.getUserSign();
  }

  public boolean isOpen(String sessionId) {
    return !BeeUtils.isEmpty(sessionId) && openSessions.containsKey(sessionId);
  }

  public void loadUserData(String serialized) {
    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (ArrayUtils.isEmpty(arr)) {
      logger.severe("cannot deserialize user data");
      return;
    }

    List<UserData> data = new ArrayList<>();
    for (String s : arr) {
      data.add(UserData.restore(s));
    }

    updateUserData(data);
  }

  public Long parseUserName(String input, boolean online) {
    if (users.isEmpty() || BeeUtils.isEmpty(input)) {
      return null;
    }
    if (online && openSessions.isEmpty()) {
      return null;
    }

    List<Long> loginEquals = new ArrayList<>();
    List<Long> loginContains = new ArrayList<>();

    List<Long> firstNameEquals = new ArrayList<>();
    List<Long> firstNameContains = new ArrayList<>();

    List<Long> lastNameEquals = new ArrayList<>();
    List<Long> lastNameContains = new ArrayList<>();

    for (UserData userData : users.values()) {
      long userId = userData.getUserId();
      if (online && !openSessions.containsValue(userId)) {
        continue;
      }

      if (BeeUtils.same(userData.getLogin(), input)) {
        loginEquals.add(userId);
      }
      if (BeeUtils.containsSame(userData.getLogin(), input)) {
        loginContains.add(userId);
      }

      if (BeeUtils.same(userData.getFirstName(), input)) {
        firstNameEquals.add(userId);
      }
      if (BeeUtils.containsSame(userData.getFirstName(), input)) {
        firstNameContains.add(userId);
      }

      if (BeeUtils.same(userData.getLastName(), input)) {
        lastNameEquals.add(userId);
      }
      if (BeeUtils.containsSame(userData.getLastName(), input)) {
        lastNameContains.add(userId);
      }
    }

    if (firstNameEquals.size() == 1) {
      return firstNameEquals.get(0);
    }
    if (lastNameEquals.size() == 1) {
      return lastNameEquals.get(0);
    }
    if (loginEquals.size() == 1) {
      return loginEquals.get(0);
    }

    if (firstNameContains.size() == 1) {
      return firstNameContains.get(0);
    }
    if (lastNameContains.size() == 1) {
      return lastNameContains.get(0);
    }
    if (loginContains.size() == 1) {
      return loginContains.get(0);
    }

    if (loginContains.isEmpty() && firstNameContains.isEmpty() && lastNameContains.isEmpty()) {
      logger.warning("cannot parse user name:", input);
    } else {
      logger.warning("ambiguous user name:", input);
    }
    return null;
  }

  public void removeSession(String sessionId) {
    if (BeeUtils.isEmpty(sessionId)) {
      logger.severe("remove session: id is empty");

    } else if (openSessions.containsKey(sessionId)) {
      openSessions.remove(sessionId);
      sessionPresence.remove(sessionId);

      OnlineWidget widget = onlinePanel.findBySession(sessionId);
      if (widget != null) {
        onlinePanel.remove(widget);
      }

      BeeKeeper.getScreen().updateUserCount(openSessions.size());
      updateHeader();

    } else {
      logger.warning("session not found:", sessionId);
    }
  }

  public void updateSession(String sessionId, long userId, Presence presence) {
    if (BeeUtils.isEmpty(sessionId)) {
      logger.severe("update session: id is empty");

    } else if (openSessions.containsKey(sessionId)) {
      if (presence == Presence.OFFLINE) {
        removeSession(sessionId);

      } else if (presence != null) {
        sessionPresence.put(sessionId, presence);
      }

    } else {
      addSession(sessionId, userId, presence);
    }
  }

  public void updateUserData(Collection<UserData> data) {
    users.clear();
    if (BeeUtils.isEmpty(data)) {
      openSessions.clear();
      onlinePanel.clear();

      logger.warning("user data is empty");
      return;
    }

    for (UserData userData : data) {
      users.put(userData.getUserId(), userData);
    }

    if (!openSessions.isEmpty()) {
      Set<String> sessionsToRemove = new HashSet<>();
      for (Map.Entry<String, Long> entry : openSessions.entrySet()) {
        if (!users.containsKey(entry.getValue())) {
          sessionsToRemove.add(entry.getKey());
        }
      }

      if (!sessionsToRemove.isEmpty()) {
        for (String sessionId : sessionsToRemove) {
          removeSession(sessionId);
        }
      }
    }

    if (!openSessions.isEmpty()) {
      for (Map.Entry<String, Long> entry : openSessions.entrySet()) {
        OnlineWidget widget = onlinePanel.findBySession(entry.getKey());
        if (widget != null) {
          widget.updateData(users.get(entry.getValue()));
        }
      }
    }

    logger.info("users", users.size());
  }

  private Badge getSizeBadge() {
    return sizeBadge;
  }

  private void setSizeBadge(Badge sizeBadge) {
    this.sizeBadge = sizeBadge;
  }

  private void updateHeader() {
    Flow header = BeeKeeper.getScreen().getDomainHeader(Domain.ONLINE, null);
    if (header == null) {
      return;
    }

    int size = openSessions.size();

    if (getSizeBadge() == null) {
      Badge badge = new Badge(size, STYLE_PREFIX + "size");

      header.add(badge);
      setSizeBadge(badge);

    } else {
      getSizeBadge().setValue(size);
    }
  }
}
