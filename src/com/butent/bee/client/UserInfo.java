package com.butent.bee.client;

import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

/**
 * gets user login status, session ID and stores them.
 * 
 */

public class UserInfo implements HasInfo {

  private String sessionId;
  private UserData userData;

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

  public Long getCompany() {
    return isLoggedIn() ? userData.getCompany() : null;
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

  public boolean isMenuVisible(String object) {
    return isLoggedIn() && userData.isMenuVisible(object);
  }

  public boolean isModuleVisible(ModuleAndSub moduleAndSub) {
    return isLoggedIn() && userData.isModuleVisible(moduleAndSub);
  }

  public boolean isModuleVisible(String object) {
    return isLoggedIn() && userData.isModuleVisible(object);
  }

  public boolean isWidgetVisible(RegulatedWidget widget) {
    return isLoggedIn() && userData.isWidgetVisible(widget);
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setUserData(UserData userData) {
    this.userData = userData;
  }
}
