package com.butent.bee.client.dialog;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import elemental.events.EventListener;

public class WebNotification extends JavaScriptObject {

  public enum NotificationPermission {
    DEFAULT("default"), DENIED("denied"), GRANTED("granted");

    private final String value;

    private NotificationPermission(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public interface PermissionCallback {

    void onFailure();

    void onSuccess();
  }

  private static final int PERMISSION_TIMEOUT = 5000;

  public static void create(final String title, final NotificationOptions options,
      final Callback<WebNotification> callback) {

    if (BeeUtils.isEmpty(title) && options == null) {
      if (callback != null) {
        callback.onFailure("cannot create empty notification");
      }

    } else {

      final Timer timer = new Timer() {
        @Override
        public void run() {
          fallback(title, options);
          if (callback != null) {
            callback.onFailure("timeout");
          }
        }
      };
      timer.schedule(PERMISSION_TIMEOUT);

      PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public void onSuccess() {
          timer.cancel();

          WebNotification notification;
          if (options == null) {
            notification = createImpl(title);
          } else {
            notification = createImpl(title, NotificationOptions.toJso(options));
          }

          if (callback != null) {
            callback.onSuccess(notification);
          }
        }

        @Override
        public void onFailure() {
          if (timer.isRunning()) {
            timer.cancel();
            fallback(title, options);
          }

          if (callback != null) {
            callback.onFailure(NotificationPermission.DENIED.getValue());
          }
        }
      };

      requestPermission(permissionCallback);
    }
  }

  public static void requestPermission(PermissionCallback callback) {
    Assert.notNull(callback);

    if (!Features.supportsNotifications()) {
      callback.onFailure();

    } else if (isGrantedImpl()) {
      callback.onSuccess();

    } else if (isDeniedImpl()) {
      callback.onFailure();

    } else {
      requestPermissionImpl(callback);
    }
  }

//@formatter:off
  private static native WebNotification createImpl(String title, JavaScriptObject options) /*-{
    return new Notification(title, options);
  }-*/;

  private static native WebNotification createImpl(String title) /*-{
    return new Notification(title);
  }-*/;
//@formatter:on

  private static void fallback(String title, NotificationOptions options) {
    if (options == null) {
      BeeKeeper.getScreen().notifyInfo(title);

    } else {
      Image image = BeeUtils.isEmpty(options.getIcon()) ? null : new Image(options.getIcon());
      if (image != null) {
        StyleUtils.setMaxWidth(image, 40);
        StyleUtils.setMaxHeight(image, 40);
      }

      String body = (image == null) ? options.getBody()
          : BeeUtils.joinWords(image.getElement().getString(), options.getBody());

      BeeKeeper.getScreen().notifyWarning(title, body);
    }
  }

//@formatter:off
  private static native boolean isDeniedImpl() /*-{
    return Notification.permission === "denied";
  }-*/;

  private static native boolean isGrantedImpl() /*-{
    return Notification.permission === "granted";
  }-*/;

  private static native void requestPermissionImpl(PermissionCallback callback) /*-{
    var success = $entry(function() {
      callback.@com.butent.bee.client.dialog.WebNotification.PermissionCallback::onSuccess()();
    });

    var failure = $entry(function() {
      callback.@com.butent.bee.client.dialog.WebNotification.PermissionCallback::onFailure()();
    });

    Notification.requestPermission(function(permission) {
      if (!('permission' in Notification)) {
        Notification.permission = permission;
      }

      if (permission === "granted") {
        success();
      } else {
        failure();
      }
    });
  }-*/;
//@formatter:on

  protected WebNotification() {
  }

//@formatter:off
  public final native void close() /*-{
    this.close();
  }-*/;

  public final native String getBody() /*-{
    return this.body;
  }-*/;

  public final native String getDir() /*-{
    return this.dir;
  }-*/;

  public final native String getIcon() /*-{
    return this.icon;
  }-*/;

  public final native String getLang() /*-{
    return this.lang;
  }-*/;

  // CHECKSTYLE:OFF
  public final native EventListener getOnClick() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onclick);
  }-*/;

  public final native EventListener getOnClose() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onclose);
  }-*/;

  public final native EventListener getOnError() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native EventListener getOnShow() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onshow);
  }-*/;

  // CHECKSTYLE:ON

  public final native String getTag() /*-{
    return this.tag;
  }-*/;

  // CHECKSTYLE:OFF
  public final native void setOnClick(EventListener listener) /*-{
    this.onclick = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;

  public final native void setOnClose(EventListener listener) /*-{
    this.onclose = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;

  public final native void setOnError(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;

  public final native void setOnShow(EventListener listener) /*-{
    this.onshow = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  // CHECKSTYLE:ON
//@formatter:on
}
