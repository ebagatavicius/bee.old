package com.butent.bee.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

import static com.butent.bee.shared.Service.PROPERTY_ACTIVE_LOCALES;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.animation.RafCallback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.decorator.TuningFactory;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.ModuleManager;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.screen.Workspace;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.utils.LayoutEngine;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The entry point class of the application, initializes <code>BeeKeeper</code> class.
 */

public class Bee implements EntryPoint, ClosingHandler {

  public static void exit() {
    setState(State.UNLOADING);

    final String workspace = BeeKeeper.getScreen().serialize();

    ClientLogManager.close();
    BodyPanel.get().clear();

    Endpoint.close();

    if (BeeKeeper.getRpc().hasPendingRequests()) {
      RafCallback callback = new RafCallback(3_000) {
        @Override
        protected boolean run(double elapsed) {
          return BeeKeeper.getRpc().hasPendingRequests();
        }

        @Override
        protected void onComplete() {
          logout(workspace);
        }
      };

      callback.start();

    } else {
      logout(workspace);
    }
  }

  public static long getEntryTime() {
    return entryTime;
  }

  public static long getReadyTime() {
    return readyTime;
  }

  public static State getState() {
    return state;
  }

  public static boolean isEnabled() {
    return !(getState() == State.UNLOADING || getState() == State.CLOSED);
  }

  private static void initWorkspace() {
    List<String> spaces = new ArrayList<>();
    JSONObject onStartup = Settings.getOnStartup();

    if (BeeKeeper.getUser().workspaceContinue()) {
      String workspace = BeeKeeper.getUser().getLastWorkspace();

      if (!BeeUtils.isEmpty(workspace) && !BeeConst.EMPTY.equals(workspace)) {
        if (Workspace.isForced(onStartup)) {
          spaces.addAll(Workspace.maybeForceSpace(Collections.singletonList(workspace), onStartup));
        } else {
          spaces.add(workspace);
        }

      } else {
        JSONObject onEmpty = Settings.getOnEmptyWorkspace();

        if (onEmpty != null) {
          spaces.add(onEmpty.toString());
        } else if (Workspace.isForced(onStartup)) {
          spaces.add(onStartup.toString());
        }
      }

    } else {
      List<String> home = Global.getSpaces().getStartup();

      if (BeeUtils.isEmpty(home)) {
        if (onStartup != null) {
          spaces.add(onStartup.toString());
        } else {
          JSONObject onEmpty = Settings.getOnEmptyWorkspace();
          if (onEmpty != null) {
            spaces.add(onEmpty.toString());
          }
        }

      } else if (Workspace.isForced(onStartup)) {
        spaces.addAll(Workspace.maybeForceSpace(home, onStartup));

      } else {
        spaces.addAll(home);
      }
    }

    if (!spaces.isEmpty()) {
      BeeKeeper.getScreen().restore(spaces, false);
    }
  }

  private static void load(Map<String, String> data) {
    for (Map.Entry<String, String> entry : data.entrySet()) {
      String value = entry.getValue();

      switch (entry.getKey()) {
        case Service.VAR_USER:
          BeeKeeper.getUser().setUserData(UserData.restore(value));
          break;

        case Service.PROPERTY_MODULES:
          Module.setEnabledModules(value);
          break;

        case Service.PROPERTY_VIEW_MODULES:
          RightsUtils.setViewModules(Codec.deserializeHashMap(value));
          break;

        case PROPERTY_ACTIVE_LOCALES:
          SupportedLocale.ACTIVE_LOCALES.clear();
          SupportedLocale.ACTIVE_LOCALES
              .addAll(Arrays.asList(Codec.beeDeserializeCollection(value)));
          break;

        case PRM_CURRENCY:
          ClientDefaults.setCurrency(Pair.restore(value));
          break;

        case TBL_PARAMETERS:
          for (String s : Codec.beeDeserializeCollection(value)) {
            Global.storeParameter(BeeParameter.restore(s));
          }
          LogUtils.getRootLogger().info("parameters", value.length());
          break;

        case TBL_DICTIONARY:
          Localized.setGlossary(Codec.deserializeHashMap(value));
          break;

        case VAR_LOCALE:
          BeeKeeper.getUser().setSupportedLocale(SupportedLocale.getByLanguage(value));
          break;

        case COL_USER_DATE_FORMAT:
          SupportedLocale dtfLocale = SupportedLocale.getByLanguage(value);
          if (dtfLocale != null) {
            BeeKeeper.getUser().setDateTimeFormatInfo(dtfLocale.getDateTimeFormatInfo());
          }
          break;
      }
    }

    String userSettings = data.get(UserInterface.Component.SETTINGS.key());
    if (!BeeUtils.isEmpty(userSettings)) {
      Pair<String, String> pair = Pair.restore(userSettings);

      Theme.load(pair.getB());
      BeeKeeper.getUser().loadSettings(pair.getA());
    }

    for (UserInterface.Component component : UserInterface.Component.values()) {
      String serialized = data.get(component.key());

      if (!BeeUtils.isEmpty(serialized)) {
        switch (component) {
          case AUTOCOMPLETE:
            AutocompleteProvider.load(serialized);
            break;

          case CHATS:
            Global.getChatManager().load(serialized);
            break;

          case DATA_INFO:
            Data.getDataInfoProvider().restore(serialized);
            break;

          case DECORATORS:
            TuningFactory.parseDecorators(serialized);
            break;

          case DIMENSIONS:
            Dimensions.load(serialized);
            break;

          case FAVORITES:
            Global.getFavorites().load(serialized);
            break;

          case FILTERS:
            Global.getFilters().load(serialized);
            break;

          case GRIDS:
            Pair<String, String> settings = Pair.restore(serialized);
            GridSettings.load(settings.getA(), settings.getB());
            break;

          case MAIL:
            ScreenImpl.updateOnlineEmails(BeeUtils.toInt(serialized));
            break;

          case MENU:
            BeeKeeper.getMenu().restore(serialized);
            break;

          case MONEY:
            Money.load(serialized);
            break;

          case NEWS:
            Global.getNewsAggregator().loadSubscriptions(serialized, false);
            break;

          case REPORTS:
            Global.getReportSettings().load(serialized);
            break;

          case SETTINGS:
            break;

          case USERS:
            Global.getUsers().loadUserData(serialized);
            break;

          case WORKSPACES:
            Global.getSpaces().load(serialized);
            break;
        }
      }
    }
  }

  private static void logout(String workspace) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.LOGOUT);

    if (!BeeUtils.isEmpty(workspace)) {
      params.addDataItem(COL_LAST_WORKSPACE, workspace);
    } else if (BeeKeeper.getUser().workspaceContinue()) {
      params.addQueryItem(COL_LAST_WORKSPACE, BeeConst.EMPTY);
    }

    BeeKeeper.getRpc().makeRequest(params);
    setState(State.CLOSED);
  }

  private static void setState(State state) {
    Bee.state = state;
  }

  private static void start() {
    BeeKeeper.getScreen().onLoad();

    ModuleManager.onLoad();

    Historian.start();

    Endpoint.open(BeeKeeper.getUser().getUserId(), input -> {
      initWorkspace();
      Global.getChatManager().start();
    });
  }

  private static long entryTime;
  private static long readyTime;

  private static State state;

  public static List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties("State", getState());

    if (getEntryTime() > 0) {
      info.add(new Property("Entry Time", Format.renderDateTime(getEntryTime())));
    }

    if (getReadyTime() > 0) {
      info.add(new Property("Ready Time", Format.renderDateTime(getReadyTime())));
      info.add(new Property("Ready Seconds", TimeUtils.toSeconds(getReadyTime() - getEntryTime())));
    }

    return info;
  }

  @Override
  public void onModuleLoad() {
    Bee.entryTime = System.currentTimeMillis();
    setState(State.LOADING);

    BeeConst.setClient();
    LogUtils.setLoggerFactory(new ClientLogManager());

    LayoutEngine layoutEngine = LayoutEngine.detect();
    if (layoutEngine != null && layoutEngine.hasStyleSheet()) {
      DomUtils.injectStyleSheet(layoutEngine.getStyleSheet());
    }

    List<String> extStyleSheets = Settings.getStyleSheets();
    if (!BeeUtils.isEmpty(extStyleSheets)) {
      for (String styleSheet : extStyleSheets) {
        DomUtils.injectStyleSheet(styleSheet);
      }
    }

    BeeKeeper.init();
    Global.init();

    if (GWT.isProdMode()) {
      GWT.setUncaughtExceptionHandler(new ExceptionHandler());
    }

    ParameterList params = BeeKeeper.getRpc().createParameters(Service.LOGIN);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        load(Codec.deserializeLinkedHashMap(response.getResponseAsString()));
        onLogin();
      }
    });
  }

  @Override
  public void onWindowClosing(ClosingEvent event) {
    event.setMessage("Don't leave me this way");
  }

  private void onLogin() {
    BeeKeeper.getScreen().init();
    BeeKeeper.getScreen().start(BeeKeeper.getUser().getUserData());

    Window.addResizeHandler(event -> {
      BeeKeeper.getScreen().getScreenPanel().onResize();

      Collection<Popup> popups = Popup.getVisiblePopups();
      if (!BeeUtils.isEmpty(popups)) {
        for (Popup popup : popups) {
          popup.onResize();
        }
      }
    });

    ParameterList params = BeeKeeper.getRpc().createParameters(Service.INIT);
    params.addQueryItem(Service.VAR_UI, BeeKeeper.getScreen().getUserInterface().getShortName());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        load(Codec.deserializeLinkedHashMap(response.getResponseAsString()));

        BeeKeeper.getBus().registerExitHandler(Bee.this);

        start();

        setState(State.INITIALIZED);
        Bee.readyTime = System.currentTimeMillis();
      }
    });
  }
}
