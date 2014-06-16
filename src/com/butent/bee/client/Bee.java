package com.butent.bee.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcInfo;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.decorator.TuningFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.ModuleManager;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.utils.LayoutEngine;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.LocalizableMessages;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;

/**
 * The entry point class of the application, initializes <code>BeeKeeper</code> class.
 */

public class Bee implements EntryPoint {

  public static void exit() {
    ClientLogManager.close();
    BodyPanel.get().clear();

    Endpoint.close();

    if (BeeKeeper.getRpc().hasPendingRequests()) {
      Timer timer = new Timer() {
        @Override
        public void run() {
          List<RpcInfo> pendingRequests = BeeKeeper.getRpc().getPendingRequests();
          for (RpcInfo info : pendingRequests) {
            info.cancel();
          }

          logout();
        }
      };

      timer.schedule(1000);

    } else {
      logout();
    }
  }

  private static void logout() {
    BeeKeeper.getRpc().makeGetRequest(Service.LOGOUT);
  }

  @Override
  public void onModuleLoad() {
    BeeConst.setClient();
    LogUtils.setLoggerFactory(new ClientLogManager());

    Localized.setConstants((LocalizableConstants) GWT.create(LocalizableConstants.class));
    Localized.setMessages((LocalizableMessages) GWT.create(LocalizableMessages.class));

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

    BeeKeeper.getScreen().init();
    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        BeeKeeper.getScreen().getScreenPanel().onResize();
      }
    });

    ParameterList params = BeeKeeper.getRpc().createParameters(Service.LOGIN);
    params.addQueryItem(Service.VAR_UI, BeeKeeper.getScreen().getUserInterface().getShortName());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        load(Codec.deserializeMap((String) response.getResponse()));
        start();
      }
    });

    List<String> extScripts = Settings.getScripts();
    if (!BeeUtils.isEmpty(extScripts)) {
      for (String script : extScripts) {
        DomUtils.injectExternalScript(script);
      }
    }
  }

  private static void load(Map<String, String> data) {
    UserData userData = UserData.restore(data.get(Service.VAR_USER));
    BeeKeeper.getUser().setUserData(userData);

    Module.setEnabledModules(data.get(Service.PROPERTY_MODULES));

    ClientDefaults.setCurrency(BeeUtils
        .toLongOrNull(data.get(AdministrationConstants.COL_CURRENCY)));
    ClientDefaults.setCurrencyName(data.get(AdministrationConstants.ALS_CURRENCY_NAME));

    BeeKeeper.getScreen().start(userData);

    for (UserInterface.Component component : UserInterface.Component.values()) {
      String serialized = data.get(component.key());

      if (!BeeUtils.isEmpty(serialized)) {
        switch (component) {
          case AUTOCOMPLETE:
            AutocompleteProvider.load(serialized);
            break;

          case DATA_INFO:
            Data.getDataInfoProvider().restore(serialized);
            break;

          case DICTIONARY:
            Localized.setDictionary(Codec.deserializeMap(serialized));
            break;

          case DECORATORS:
            TuningFactory.parseDecorators(serialized);
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

          case MENU:
            BeeKeeper.getMenu().restore(serialized);
            break;

          case NEWS:
            Global.getNewsAggregator().loadSubscriptions(serialized);
            break;

          case REPORTS:
            Global.getReportSettings().load(serialized);
            break;
            
          case SETTINGS:
            BeeKeeper.getUser().loadSettings(serialized);
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

  private static void start() {
    BeeKeeper.getScreen().onLoad();

    ModuleManager.onLoad();

    Historian.start();

    Endpoint.open(BeeKeeper.getUser().getUserId());

    List<String> onStartup = Global.getSpaces().getStartup();

    if (BeeUtils.isEmpty(onStartup)) {
      onStartup = Settings.getOnStartup();
      if (!BeeUtils.isEmpty(onStartup) && !BeeKeeper.getMenu().isEmpty()) {
        for (String item : onStartup) {
          BeeKeeper.getMenu().executeItem(item);
        }
      }

    } else {
      for (int i = 0; i < onStartup.size(); i++) {
        BeeKeeper.getScreen().restore(onStartup.get(i), i > 0);
      }
    }

    BeeKeeper.getBus().registerExitHandler("Don't leave me this way");
  }
}
