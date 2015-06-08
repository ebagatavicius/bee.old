package com.butent.bee.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleElement;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

/**
 * gets user login status, session ID and stores them.
 */

public class UserInfo implements HasInfo {

  private static final BeeLogger logger = LogUtils.getLogger(UserInfo.class);

  private String sessionId;
  private UserData userData;

  private BeeRowSet settings;

  private boolean openInNewTab;

  private int clickSensitivityMillis;
  private int clickSensitivityDistance;

  private String styleId;

  public boolean canCreateData(String object) {
    return isLoggedIn() && userData.canCreateData(object);
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

  public String getLogin() {
    if (!isLoggedIn()) {
      return null;
    }
    return userData.getLogin();
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

  public String getStyle() {
    return getSetting(COL_USER_STYLE);
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

  public boolean is(Long id) {
    return id != null && id.equals(getUserId());
  }

  public boolean isAdministrator() {
    return isModuleVisible(ModuleAndSub.of(Module.ADMINISTRATION));
  }

  public boolean isAnyModuleVisible(String input) {
    return isLoggedIn() && userData.isAnyModuleVisible(input);
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

  public boolean openInNewTab() {
    return openInNewTab;
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

  private int getIntSetting(String colName) {
    if (DataUtils.isEmpty(settings)) {
      return BeeConst.UNDEF;

    } else {
      int index = getSettingsIndex(colName);

      if (BeeConst.isUndef(index)) {
        return BeeConst.UNDEF;
      } else {
        Integer value = settings.getInteger(0, index);
        return BeeUtils.nvl(value, BeeConst.UNDEF);
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

  private void setClickSensitivityDistance(int clickSensitivityDistance) {
    this.clickSensitivityDistance = clickSensitivityDistance;
  }

  private void setClickSensitivityMillis(int clickSensitivityMillis) {
    this.clickSensitivityMillis = clickSensitivityMillis;
  }

  private void setOpenInNewTab(boolean openInNewTab) {
    this.openInNewTab = openInNewTab;
  }

  private void setStyleId(String styleId) {
    this.styleId = styleId;
  }

  private void updateFields() {
    setOpenInNewTab(getBooleanSetting(COL_OPEN_IN_NEW_TAB));

    setClickSensitivityMillis(getIntSetting(COL_CLICK_SENSITIVITY_MILLIS));
    setClickSensitivityDistance(getIntSetting(COL_CLICK_SENSITIVITY_DISTANCE));
  }

  private void updateStyle(String css) {
    DomUtils.setText(getStyleId(), css);
  }
}
