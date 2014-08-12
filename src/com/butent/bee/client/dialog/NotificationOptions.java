package com.butent.bee.client.dialog;

import com.google.gwt.core.client.JavaScriptObject;

import com.butent.bee.shared.utils.BeeUtils;

public class NotificationOptions {

  public enum NotificationDirection {
    AUTO("auto"), LTR("ltr"), RTL("rtl");

    private final String value;

    private NotificationDirection(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

//@formatter:off
  public static native JavaScriptObject toJso(NotificationOptions options) /*-{
    var obj = {};
    if (options) {
      if (options.@com.butent.bee.client.dialog.NotificationOptions::dir) {
        obj.dir = options.@com.butent.bee.client.dialog.NotificationOptions::dir;
      }
      if (options.@com.butent.bee.client.dialog.NotificationOptions::lang) {
        obj.lang = options.@com.butent.bee.client.dialog.NotificationOptions::lang;
      }
      if (options.@com.butent.bee.client.dialog.NotificationOptions::body) {
        obj.body = options.@com.butent.bee.client.dialog.NotificationOptions::body;
      }
      if (options.@com.butent.bee.client.dialog.NotificationOptions::tag) {
        obj.tag = options.@com.butent.bee.client.dialog.NotificationOptions::tag;
      }
      if (options.@com.butent.bee.client.dialog.NotificationOptions::icon) {
        obj.icon = options.@com.butent.bee.client.dialog.NotificationOptions::icon;
      }
    }
    return obj;
  }-*/;
//@formatter:on

  private String dir;
  private String lang;
  private String body;
  private String tag;
  private String icon;

  public NotificationOptions() {
    super();
  }

  public String getBody() {
    return body;
  }

  public NotificationDirection getDir() {
    if (dir == null) {
      return null;
    }
    for (NotificationDirection notificationDirection : NotificationDirection.values()) {
      if (notificationDirection.getValue().equals(dir)) {
        return notificationDirection;
      }
    }
    return null;
  }

  public String getIcon() {
    return icon;
  }

  public String getLang() {
    return lang;
  }

  public String getTag() {
    return tag;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setDir(NotificationDirection notificationDirection) {
    this.dir = (notificationDirection == null) ? null : notificationDirection.toString();
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("dir", dir, "lang", lang, "body", body, "tag", tag, "icon", icon);
  }
}
