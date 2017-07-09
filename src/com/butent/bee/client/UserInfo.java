package com.butent.bee.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.user.client.Timer;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.websocket.messages.PresenceMessage;

import java.util.List;

public class UserInfo implements HasInfo {

  private static final BeeLogger logger = LogUtils.getLogger(UserInfo.class);

  private String sessionId;
  private UserData userData;

  private BeeRowSet settings;

  private boolean openInNewTab;
  private boolean showNewMessagesNotifier;

  private boolean assistant;

  private int clickSensitivityMillis;
  private int clickSensitivityDistance;

  private int actionSensitivityMillis;

  private int newsRefreshIntervalSeconds;
  private int loadingStateDelayMillis;

  private Boolean showGridFilterCommand;

  private String styleId;

  SupportedLocale supportedLocale;
  DateTimeFormatInfo dateTimeFormatInfo;

  private Presence presence = Presence.ONLINE;
  private Timer presenceTimer;

  public boolean canCreateData(String object) {
    return isLoggedIn() && userData.canCreateData(object);
  }

  UserInfo() {
  }

  public boolean canDeleteData(String object) {
    return isLoggedIn() && userData.canDeleteData(object);
  }

  public boolean canEditColumn(String viewName, String column) {
    return isLoggedIn() && userData.canEditColumn(viewName, column);
  }

  public boolean canEditData(String object) {
    return isLoggedIn() && userData.canEditData(object);
  }

  public boolean canMergeData(String object) {
    return isLoggedIn() && userData.canMergeData(object);
  }

  public void checkPresence(Presence p) {
    if (p == Presence.ONLINE && getPresence() == Presence.IDLE) {
      maybeUpdatePresence(p);

    } else if (p == Presence.IDLE && getPresence() == Presence.ONLINE) {
      long minutes = Settings.getReducedInteractionStatusMinutes();
      long idleMillis = Previewer.getIdleMillis();

      if (minutes > 0 && idleMillis >= minutes * TimeUtils.MILLIS_PER_MINUTE) {
        maybeUpdatePresence(p);
      }
    }
  }

  public int getActionSensitivityMillis() {
    return actionSensitivityMillis;
  }

  public int getClickSensitivityDistance() {
    return clickSensitivityDistance;
  }

  public int getClickSensitivityMillis() {
    return clickSensitivityMillis;
  }

  public Long getCompany() {
    return isLoggedIn() ? userData.getCompany() : null;
  }

  public String getCompanyName() {
    return isLoggedIn() ? userData.getCompanyName() : null;
  }

  public DateTimeFormatInfo getDateTimeFormatInfo() {
    return dateTimeFormatInfo;
  }

  public Filter getFilter(String column) {
    if (isLoggedIn() && !BeeUtils.isEmpty(column)) {
      return Filter.equals(column, getUserId());
    } else {
      return null;
    }
  }

  public String getFirstName() {
    return isLoggedIn() ? userData.getFirstName() : null;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties("Is Logged In", isLoggedIn(),
        "Session Id", getSessionId(),
        "Signature", getUserSign());

    if (userData != null) {
      info.addAll(userData.getInfo());
    }
    return info;
  }

  public String getLastName() {
    return isLoggedIn() ? userData.getLastName() : null;
  }

  public String getLastWorkspace() {
    return getSetting(COL_LAST_WORKSPACE);
  }

  public int getLoadingStateDelayMillis() {
    return loadingStateDelayMillis;
  }

  public String getLogin() {
    if (!isLoggedIn()) {
      return null;
    }
    return userData.getLogin();
  }

  public int getNewsRefreshIntervalSeconds() {
    return newsRefreshIntervalSeconds;
  }

  public Presence getPresence() {
    return presence;
  }

  public String getProperty(String property) {
    if (!isLoggedIn()) {
      return null;
    }
    return userData.getProperty(property);
  }

  public String getSessionId() {
    return sessionId;
  }

  public BeeRow getSettingsRow() {
    if (DataUtils.isEmpty(settings)) {
      return null;
    } else {
      return settings.getRow(0);
    }
  }

  public Boolean getShowGridFilterCommand() {
    return showGridFilterCommand;
  }

  public String getStyle() {
    return getSetting(COL_USER_STYLE);
  }

  public SupportedLocale getSupportedLocale() {
    return supportedLocale;
  }

  public UserData getUserData() {
    return userData;
  }

  public Long getUserId() {
    if (!isLoggedIn()) {
      return null;
    }
    return userData.getUserId();
  }

  public String getUserSign() {
    if (!isLoggedIn()) {
      return null;
    }
    return userData.getUserSign();
  }

  public Long idOrNull(Boolean userMode) {
    return BeeUtils.isTrue(userMode) ? getUserId() : null;
  }

  public boolean getCommentsLayout() {
    return getBooleanSetting(COL_COMMENTS_LAYOUT);
  }

  public boolean hasAuthoritah() {
    return isLoggedIn() && userData.hasAuthoritah();
  }

  public boolean is(Long id) {
    return id != null && id.equals(getUserId());
  }

  public boolean isAdministrator() {
    return isModuleVisible(ModuleAndSub.of(Module.ADMINISTRATION));
  }

  public boolean isAnyModuleVisible(String input) {
    return isLoggedIn() && userData.isAnyModuleVisible(input);
  }

  public boolean isColumnRequired(String viewName, String column) {
    return isLoggedIn() && userData.isColumnRequired(viewName, column);
  }

  public boolean isColumnVisible(DataInfo dataInfo, String column) {
    if (!isLoggedIn()) {
      return false;

    } else if (dataInfo == null || BeeUtils.isEmpty(column)) {
      return true;

    } else if (!userData.isColumnVisible(dataInfo.getViewName(), column)) {
      return false;

    } else {
      String root = dataInfo.getRootField(column);

      if (!BeeUtils.isEmpty(root) && !BeeUtils.same(column, root)) {
        return userData.isColumnVisible(dataInfo.getViewName(), root);
      } else {
        return true;
      }
    }
  }

  public boolean isDataVisible(String object) {
    return isLoggedIn() && userData.isDataVisible(object);
  }

  public boolean isLoggedIn() {
    return userData != null;
  }

  public boolean isMenuVisible() {
    return !getBooleanSetting(COL_MENU_HIDE);
  }

  public boolean isMenuVisible(String object) {
    return isLoggedIn() && userData.isMenuVisible(object);
  }

  public boolean isModuleVisible(ModuleAndSub moduleAndSub) {
    return isLoggedIn() && userData.isModuleVisible(moduleAndSub);
  }

  public boolean isWidgetVisible(RegulatedWidget widget) {
    return isLoggedIn() && userData.isWidgetVisible(widget);
  }

  public void loadSettings(String serialized) {
    if (!BeeUtils.isEmpty(serialized)) {
      settings = BeeRowSet.restore(serialized);

      updateFields();

      String css = getStyle();
      if (!BeeUtils.isEmpty(css)) {
        createStyle(css);
      }
    }
  }

  public void maybeUpdatePresence(Presence p) {
    if (p != null && isLoggedIn() && getPresence() != p && Endpoint.isOpen()) {
      setPresence(p);
      BeeKeeper.getScreen().updateUserPresence(p);

      Endpoint.send(new PresenceMessage(Endpoint.getSessionId(), getUserId(), p));
    }
  }

  public boolean openInNewTab() {
    return openInNewTab;
  }

  public void setCommentsLayout(boolean isDefault) {
    if (!DataUtils.isEmpty(settings)) {
      int index = getSettingsIndex(COL_COMMENTS_LAYOUT);

      if (!BeeConst.isUndef(index)) {

        BeeRow row = getSettingsRow();
        row.setValue(index, isDefault);

        Queries.update(settings.getViewName(), row.getId(), COL_COMMENTS_LAYOUT, BooleanValue
            .of(isDefault));
      }
    }
  }

  public void setDateTimeFormatInfo(DateTimeFormatInfo dateTimeFormatInfo) {
    this.dateTimeFormatInfo = dateTimeFormatInfo;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setMenuVisible(boolean visible) {
    if (isMenuVisible() != visible && !DataUtils.isEmpty(settings)) {
      int index = getSettingsIndex(COL_MENU_HIDE);

      if (!BeeConst.isUndef(index)) {
        boolean hide = !visible;

        BeeRow row = getSettingsRow();
        row.setValue(index, hide);

        Queries.update(settings.getViewName(), row.getId(), COL_MENU_HIDE, BooleanValue.of(hide));
      }
    }
  }

  public void setUserData(UserData userData) {
    this.userData = userData;

    if (presenceTimer == null && userData != null) {
      this.presenceTimer = new Timer() {
        @Override
        public void run() {
          checkPresence(Presence.IDLE);
        }
      };

      presenceTimer.scheduleRepeating(TimeUtils.MILLIS_PER_MINUTE / 3);
    }
  }

  public void setSupportedLocale(SupportedLocale supportedLocale) {
    this.supportedLocale = supportedLocale;
  }

  public void updateSettings(BeeRow row) {
    if (settings != null && row != null) {
      if (!settings.isEmpty()) {
        settings.clearRows();
      }

      settings.addRow(row);

      updateFields();

      String css = getStyle();

      if (BeeUtils.isEmpty(css)) {
        if (!BeeUtils.isEmpty(getStyleId())) {
          updateStyle(BeeConst.STRING_EMPTY);
        }
      } else if (BeeUtils.isEmpty(getStyleId())) {
        createStyle(css);
      } else {
        updateStyle(css.trim());
      }
    }
  }

  public boolean workspaceContinue() {
    return getBooleanSetting(COL_WORKSPACE_CONTINUE);
  }

  private void createStyle(String css) {
    StyleElement element = Document.get().createStyleElement();

    String id = DomUtils.createId(element, "style-");
    setStyleId(id);

    element.setInnerText(css.trim());
    DomUtils.getHead().appendChild(element);
  }

  private boolean getBooleanSetting(String colName) {
    if (DataUtils.isEmpty(settings)) {
      return false;

    } else {
      int index = getSettingsIndex(colName);

      if (BeeConst.isUndef(index)) {
        return false;
      } else {
        return BeeUtils.unbox(settings.getBoolean(0, index));
      }
    }
  }

  private <E extends Enum<?>> E getEnumSetting(String colName, Class<E> clazz) {
    if (DataUtils.isEmpty(settings)) {
      return null;

    } else {
      int index = getSettingsIndex(colName);

      if (BeeConst.isUndef(index)) {
        return null;
      } else {
        return settings.getEnum(0, index, clazz);
      }
    }
  }

  private int getIntSetting(String colName, int def) {
    if (DataUtils.isEmpty(settings)) {
      return def;

    } else {
      int index = getSettingsIndex(colName);

      if (BeeConst.isUndef(index)) {
        return def;
      } else {
        Integer value = settings.getInteger(0, index);
        return BeeUtils.nvl(value, def);
      }
    }
  }

  private String getSetting(String colName) {
    if (DataUtils.isEmpty(settings)) {
      return null;

    } else {
      int index = getSettingsIndex(colName);

      if (BeeConst.isUndef(index)) {
        return null;
      } else {
        return settings.getString(0, index);
      }
    }
  }

  private int getSettingsIndex(String colName) {
    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "not found");
    }
    return index;
  }

  private String getStyleId() {
    return styleId;
  }

  public boolean assistant() {
    return assistant;
  }

  public boolean showNewMessagesNotifier() {
    return showNewMessagesNotifier;
  }

  private void setActionSensitivityMillis(int actionSensitivityMillis) {
    this.actionSensitivityMillis = actionSensitivityMillis;
  }

  private void setClickSensitivityDistance(int clickSensitivityDistance) {
    this.clickSensitivityDistance = clickSensitivityDistance;
  }

  private void setClickSensitivityMillis(int clickSensitivityMillis) {
    this.clickSensitivityMillis = clickSensitivityMillis;
  }

  private void setLoadingStateDelayMillis(int loadingStateDelayMillis) {
    this.loadingStateDelayMillis = loadingStateDelayMillis;
  }

  private void setNewsRefreshIntervalSeconds(int newsRefreshIntervalSeconds) {
    this.newsRefreshIntervalSeconds = newsRefreshIntervalSeconds;
  }

  private void setOpenInNewTab(boolean openInNewTab) {
    this.openInNewTab = openInNewTab;
  }

  private void setPresence(Presence presence) {
    this.presence = presence;
  }

  private void setShowGridFilterCommand(int value) {
    if (value < 0) {
      this.showGridFilterCommand = null;
    } else {
      this.showGridFilterCommand = value > 0;
    }
  }

  private void setStyleId(String styleId) {
    this.styleId = styleId;
  }

  private void updateFields() {
    setOpenInNewTab(getBooleanSetting(COL_OPEN_IN_NEW_TAB));
    setShowNewMessagesNotifier(getBooleanSetting(COL_SHOW_NEW_MESSAGES_NOTIFIER));
    setAssistant(getBooleanSetting(COL_ASSISTANT));
    Global.getChatManager().updateAssistantChat(assistant);

    setClickSensitivityMillis(getIntSetting(COL_CLICK_SENSITIVITY_MILLIS, BeeConst.UNDEF));
    setClickSensitivityDistance(getIntSetting(COL_CLICK_SENSITIVITY_DISTANCE, BeeConst.UNDEF));

    setActionSensitivityMillis(getIntSetting(COL_ACTION_SENSITIVITY_MILLIS, BeeConst.UNDEF));

    setNewsRefreshIntervalSeconds(getIntSetting(COL_NEWS_REFRESH_INTERVAL_SECONDS, BeeConst.UNDEF));
    setLoadingStateDelayMillis(getIntSetting(COL_LOADING_STATE_DELAY_MILLIS, BeeConst.UNDEF));

    setShowGridFilterCommand(getIntSetting(COL_SHOW_GRID_FILTER_COMMAND, BeeConst.UNDEF));

    SupportedLocale dfSetting = getEnumSetting(COL_USER_DATE_FORMAT, SupportedLocale.class);
    if (dfSetting == null) {
      dfSetting = getSupportedLocale();
    }
    if (dfSetting != null) {
      setDateTimeFormatInfo(dfSetting.getDateTimeFormatInfo());
    }
  }

  private void updateStyle(String css) {
    DomUtils.setText(getStyleId(), css);
  }

  public void setAssistant(boolean assistant) {
    this.assistant = assistant;
  }

  public void setShowNewMessagesNotifier(boolean showNewMessagesNotifier) {
    this.showNewMessagesNotifier = showNewMessagesNotifier;
  }
}
