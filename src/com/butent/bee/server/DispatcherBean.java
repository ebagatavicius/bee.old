package com.butent.bee.server;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.data.DataServiceBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.LocalizationBean;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.ui.UiHolderBean;
import com.butent.bee.server.ui.UiServiceBean;
import com.butent.bee.server.utils.Reflection;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Receives a service request and then passes it along, depending on service name structure, to an
 * according class for execution.
 */

@Stateless
public class DispatcherBean {

  private static BeeLogger logger = LogUtils.getLogger(DispatcherBean.class);

  @EJB
  ModuleHolderBean moduleHolder;
  @EJB
  SystemServiceBean systemService;
  @EJB
  DataServiceBean dataService;
  @EJB
  UiServiceBean uiService;
  @EJB
  UiHolderBean uiHolder;
  @EJB
  Invocation invocation;
  @EJB
  UserServiceBean userService;
  @EJB
  SystemBean sys;
  @EJB
  NewsBean news;
  @EJB
  ParamHolderBean prm;
  @EJB
  QueryServiceBean qs;
  @EJB
  MailModuleBean mail;
  @EJB
  ChatBean chat;
  @EJB
  LocalizationBean loc;

  public void beforeLogout(RequestInfo reqInfo) {
    String workspace = reqInfo.getParameter(COL_LAST_WORKSPACE);

    if (!BeeUtils.isEmpty(workspace)) {
      userService.saveWorkspace(userService.getCurrentUserId(), workspace);
    }
  }

  public ResponseObject doLogin(RequestInfo reqInfo) {
    ResponseObject response = new ResponseObject();
    Map<String, Object> data = new HashMap<>();

    ResponseObject userData = userService.login(reqInfo.getRemoteAddr(), reqInfo.getUserAgent());
    response.addMessagesFrom(userData);
    if (userData.hasErrors()) {
      return response;
    }

    data.put(Service.VAR_USER, userData.getResponse());

    data.put(Service.PROPERTY_MODULES, Module.getEnabledModulesAsString());
    data.put(Service.PROPERTY_VIEW_MODULES, RightsUtils.getViewModulesAsString());

    data.put(Service.PROPERTY_DEFAULT_LOCALE, SupportedLocale.getUserDefault().getLanguage());
    data.put(Service.PROPERTY_ACTIVE_LOCALES, SupportedLocale.getActiveLocales());

    data.put(PRM_CURRENCY, prm.getRelationInfo(PRM_CURRENCY));

    List<BeeParameter> params = new ArrayList<>();
    Arrays.stream(Module.values()).filter(Module::isEnabled).map(Module::getName)
        .forEach(moduleName -> params.addAll(prm.getParameters(moduleName)));

    data.put(TBL_PARAMETERS, params);

    SupportedLocale locale = userService.getSupportedLocale();
    data.put(VAR_LOCALE, locale.getLanguage());

    data.put(TBL_DICTIONARY, Localizations.getGlossary(locale));

    SupportedLocale dateTimeFormatLocale = userService.getDateTimeFormatLocale();
    data.put(COL_USER_DATE_FORMAT, dateTimeFormatLocale.getLanguage());

    return response.setResponse(data);
  }

  public void doLogout(long userId, long historyId) {
    userService.logout(userId, historyId);
  }

  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (moduleHolder.hasModule(svc)) {
      response = moduleHolder.doModule(svc, reqInfo);

    } else if (Service.isDataService(svc)) {
      response = uiService.doService(reqInfo);

    } else if (Service.isChatService(svc)) {
      response = chat.doService(reqInfo);

    } else if (Service.isDbService(svc)) {
      response = dataService.doService(svc, reqInfo);

    } else if (Service.isSysService(svc)) {
      response = systemService.doService(svc, reqInfo);

    } else if (Service.isL10nService(svc)) {
      response = loc.doService(reqInfo);

    } else if (BeeUtils.same(svc, Service.INIT)) {
      response = doInit(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_MENU)) {
      response = uiHolder.getMenu(reqInfo.hasParameter(Service.VAR_RIGHTS),
          reqInfo.hasParameter(Service.VAR_TRANSFORM));

    } else if (BeeUtils.same(svc, Service.WHERE_AM_I)) {
      response = ResponseObject.info(System.currentTimeMillis(), BeeConst.whereAmI());

    } else if (BeeUtils.same(svc, Service.INVOKE)) {
      response = Reflection.invoke(invocation, reqInfo.getSubService(), reqInfo);

    } else if (BeeUtils.same(svc, Service.RESPECT_MY_AUTHORITAH)) {
      response = userService.respectMyAuthoritah();

    } else {
      String msg = BeeUtils.joinWords(svc, "service not recognized");
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    return response;
  }

  private ResponseObject doInit(RequestInfo reqInfo) {
    ResponseObject response = new ResponseObject();
    Map<String, Object> data = new HashMap<>();

    UserInterface userInterface = null;

    String ui = reqInfo.getParameter(Service.VAR_UI);
    if (!BeeUtils.isEmpty(ui)) {
      userInterface = UserInterface.getByShortName(ui);
    }
    if (userInterface == null) {
      userInterface = userService.getUserInterface(reqInfo.getRemoteUser());
    }

    Collection<Component> components = UserInterface.normalize(userInterface).getComponents();

    Collection<Component> requiredComponents = UserInterface.getRequiredComponents();
    if (!BeeUtils.isEmpty(requiredComponents)) {
      if (components == null) {
        components = requiredComponents;
      } else {
        for (Component component : requiredComponents) {
          if (!components.contains(component)) {
            components.add(component);
          }
        }
      }
    }

    if (!BeeUtils.isEmpty(components)) {
      for (Component component : components) {
        long millis = System.currentTimeMillis();

        switch (component) {
          case AUTOCOMPLETE:
            ResponseObject acData = uiService.getAutocomplete();
            if (acData != null) {
              response.addMessagesFrom(acData);
              if (!acData.hasErrors() && acData.hasResponse()) {
                data.put(component.key(), acData.getResponse());
              }
            }
            break;

          case CHATS:
            ResponseObject chats = chat.getChats();
            if (chats != null) {
              response.addMessagesFrom(chats);
              if (!chats.hasErrors() && chats.hasResponse()) {
                data.put(component.key(), chats.getResponse());
              }
            }
            break;

          case DATA_INFO:
            data.put(component.key(), sys.getDataInfo());
            break;

          case DECORATORS:
            ResponseObject decorators = uiService.getDecorators();
            if (decorators != null) {
              response.addMessagesFrom(decorators);
              if (!decorators.hasErrors() && decorators.hasResponse()) {
                data.put(component.key(), decorators.getResponse());
              }
            }
            break;

          case DIMENSIONS:
            BeeRowSet dimensionNames = qs.getViewData(Dimensions.VIEW_NAMES);
            if (dimensionNames != null) {
              Integer count = prm.getInteger(Dimensions.PRM_DIMENSIONS);
              if (BeeUtils.isPositive(count)) {
                dimensionNames.setTableProperty(Dimensions.PRM_DIMENSIONS,
                    BeeUtils.toString(count));
              }
              data.put(component.key(), dimensionNames);
            }
            break;

          case FAVORITES:
            BeeRowSet favorites = uiService.getFavorites();
            if (!DataUtils.isEmpty(favorites)) {
              data.put(component.key(), favorites);
            }
            break;

          case FILTERS:
            BeeRowSet filters = uiService.getFilters();
            if (!DataUtils.isEmpty(filters)) {
              data.put(component.key(), filters);
            }
            break;

          case GRIDS:
            ResponseObject settingsData = uiService.getGridAndColumnSettings();
            if (settingsData != null && settingsData.hasResponse()) {
              data.put(component.key(), settingsData.getResponse());
            }
            break;

          case MAIL:
            int unread = mail.countUnread();
            if (unread > 0) {
              data.put(component.key(), unread);
            }
            break;

          case MENU:
            ResponseObject menuData = uiHolder.getMenu(true, true);
            if (menuData != null) {
              response.addMessagesFrom(menuData);
              if (!menuData.hasErrors() && menuData.hasResponse()) {
                data.put(component.key(), menuData.getResponse());
              }
            }
            break;

          case MONEY:
            BeeRowSet rates = qs.getViewData(VIEW_CURRENCY_RATES);
            if (!DataUtils.isEmpty(rates)) {
              data.put(component.key(), rates);
            }
            break;

          case NEWS:
            ResponseObject newsData = news.getNews(Feed.ALL);
            if (newsData != null) {
              response.addMessagesFrom(newsData);
              if (!newsData.hasErrors() && newsData.hasResponse()) {
                data.put(component.key(), newsData.getResponse());
              }
            }
            break;

          case REPORTS:
            BeeRowSet reportSettings = uiService.getReportSettings();
            if (!DataUtils.isEmpty(reportSettings)) {
              data.put(component.key(), reportSettings);
            }
            break;

          case SETTINGS:
            BeeRowSet userSettings = userService.ensureUserSettings();
            if (!DataUtils.isEmpty(userSettings)) {
              Long themeId = userSettings.getLong(0, COL_UI_THEME);
              BeeRowSet theme;

              if (DataUtils.isId(themeId)) {
                theme = qs.getViewData(VIEW_UI_THEMES, Filter.compareId(themeId));
              } else {
                theme = null;
              }

              data.put(component.key(), Pair.of(userSettings, theme));
            }
            break;

          case USERS:
            data.put(component.key(), userService.getAllUserData());
            break;

          case WORKSPACES:
            BeeRowSet workspaces = uiService.getWorkspaces();
            if (!DataUtils.isEmpty(workspaces)) {
              data.put(component.key(), workspaces);
            }
            break;
        }

        logger.debug(reqInfo.getService(), component, TimeUtils.elapsedMillis(millis));
      }
    }

    if (!data.isEmpty()) {
      response.setResponse(data);
    }
    return response;
  }
}
