package com.butent.bee.server;

import com.google.common.collect.Maps;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.server.data.DataServiceBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.ui.UiHolderBean;
import com.butent.bee.server.ui.UiServiceBean;
import com.butent.bee.server.utils.Reflection;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
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

  public ResponseObject doLogin(RequestInfo reqInfo) {
    ResponseObject response = new ResponseObject();
    Map<String, Object> data = Maps.newHashMap();

    ResponseObject userData = userService.login(reqInfo.getRemoteAddr(), reqInfo.getUserAgent());
    response.addMessagesFrom(userData);
    if (userData.hasErrors()) {
      return response;
    }
    data.put(Service.VAR_USER, userData.getResponse());
    data.put(Service.PROPERTY_MODULES, Module.getEnabledModulesAsString());

    Long currency = prm.getRelation(PRM_CURRENCY);

    if (DataUtils.isId(currency)) {
      data.put(COL_CURRENCY, currency);
      data.put(ALS_CURRENCY_NAME, qs.getValue(new SqlSelect()
          .addFields(TBL_CURRENCIES, COL_CURRENCY_NAME)
          .addFrom(TBL_CURRENCIES)
          .setWhere(sys.idEquals(TBL_CURRENCIES, currency))));
    }

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

          case DATA_INFO:
            data.put(component.key(), sys.getDataInfo());
            break;

          case DICTIONARY:
            data.put(component.key(),
                Localizations.getPreferredDictionary(userService.getLanguage()));
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
            Pair<BeeRowSet, BeeRowSet> settings = uiService.getGridAndColumnSettings();
            if (settings != null && !settings.isNull()) {
              data.put(component.key(), settings);
            }
            break;

          case MENU:
            ResponseObject menuData = uiHolder.getMenu(true);
            if (menuData != null) {
              response.addMessagesFrom(menuData);
              if (!menuData.hasErrors() && menuData.hasResponse()) {
                data.put(component.key(), menuData.getResponse());
              }
            }
            break;

          case NEWS:
            ResponseObject newsData = news.getNews();
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
              data.put(component.key(), userSettings);
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

    return response.setResponse(data);
  }

  public void doLogout(long userId, long historyId) {
    userService.logout(userId, historyId);
  }

  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (moduleHolder.hasModule(svc)) {
      response = moduleHolder.doModule(reqInfo);

    } else if (Service.isDataService(svc)) {
      response = uiService.doService(reqInfo);

    } else if (Service.isDbService(svc)) {
      response = dataService.doService(svc, reqInfo);

    } else if (Service.isSysService(svc)) {
      response = systemService.doService(svc, reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_MENU)) {
      response = uiHolder.getMenu(reqInfo.hasParameter(Service.VAR_RIGHTS));

    } else if (BeeUtils.same(svc, Service.WHERE_AM_I)) {
      response = ResponseObject.info(System.currentTimeMillis(), BeeConst.whereAmI());

    } else if (BeeUtils.same(svc, Service.INVOKE)) {
      response = Reflection.invoke(invocation, reqInfo.getParameter(Service.RPC_VAR_METH), reqInfo);

    } else {
      String msg = BeeUtils.joinWords(svc, "service not recognized");
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    return response;
  }
}
