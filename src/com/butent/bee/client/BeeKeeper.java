package com.butent.bee.client;

import com.google.gwt.core.client.GWT;

import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.ui.FormFactory;

public final class BeeKeeper {

  private static Screen screen;
  private static RpcFactory rpc;
  private static EventManager bus;

  private static UserInfo user;
  private static Storage stor;
  private static MenuManager menu;

  public static EventManager getBus() {
    return bus;
  }

  public static MenuManager getMenu() {
    return menu;
  }

  public static RpcFactory getRpc() {
    return rpc;
  }

  public static Screen getScreen() {
    return screen;
  }

  public static Storage getStorage() {
    return stor;
  }

  public static UserInfo getUser() {
    return user;
  }

  public static void onRightsChange() {
    if (getScreen().getUserInterface().hasMenu()) {
      getMenu().loadMenu();
    }

    GridFactory.clearDescriptionCache();
    FormFactory.clearDescriptionCache();
  }

  static void init() {
    screen = GWT.create(Screen.class);

    bus = new EventManager();
    rpc = new RpcFactory();
    user = new UserInfo();
    stor = new Storage();
    menu = new MenuManager();
  }

  private BeeKeeper() {
  }
}
