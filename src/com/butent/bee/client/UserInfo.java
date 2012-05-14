package com.butent.bee.client;

import com.google.common.collect.Maps;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

/**
 * gets user login status, session ID and stores them.
 * 
 */

public class UserInfo implements Module {

  private String sessionId = null;
  private UserData userData = null;

  public void end() {
  }

  public String getDsn() {
    if (!isLoggedIn()) {
      return null;
    }
    return userData.getProperty("dsn");
  }
  
  public Filter getFilter(String column) {
    if (isLoggedIn()) {
      return ComparisonFilter.isEqual(column, new LongValue(getUserId()));
    } else {
      return null;
    }
  }

  public String getFirstName() {
    return isLoggedIn() ? userData.getFirstName() : null;
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

  public String getName() {
    return getClass().getName();
  }

  public int getPriority(int p) {
    switch (p) {
      case PRIORITY_INIT:
        return DO_NOT_CALL;
      case PRIORITY_START:
        return DO_NOT_CALL;
      case PRIORITY_END:
        return DO_NOT_CALL;
      default:
        return DO_NOT_CALL;
    }
  }

  public String getSessionId() {
    return sessionId;
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

  public Map<String, String> getViews() {
    if (!isLoggedIn()) {
      return null;
    }

    String views = userData.getProperty("views");
    if (BeeUtils.isEmpty(views)) {
      views = Settings.getProperty("views");
    }
    if (BeeUtils.isEmpty(views)) {
      return null;
    }

    Map<String, String> result = Maps.newLinkedHashMap();
    for (String view : BeeUtils.split(views, BeeConst.STRING_COMMA)) {
      if (BeeUtils.isEmpty(view)) {
        continue;
      }

      String name = BeeUtils.getPrefix(view, BeeConst.CHAR_COLON);
      if (BeeUtils.isEmpty(name)) {
        result.put(view, view);
      } else {
        result.put(name, BeeUtils.getSuffix(view, BeeConst.CHAR_COLON));
      }
    }
    return result;
  }

  public void init() {
  }

  public boolean isLoggedIn() {
    return userData != null;
  }

  public void setDsn(String dsn) {
    if (isLoggedIn()) {
      userData.setProperty("dsn", dsn);
    }
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setUserData(UserData userData) {
    this.userData = userData;
  }

  public void start() {
  }
}
