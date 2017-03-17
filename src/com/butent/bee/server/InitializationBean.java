package com.butent.bee.server;

import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.i18n.LocalizationBean;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.logging.LogbackFactory;
import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.ui.UiHolderBean;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class InitializationBean {

  @EJB
  ModuleHolderBean moduleBean;
  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  ParamHolderBean prm;
  @EJB
  ConcurrencyBean cb;
  @EJB
  ChatBean chat;
  @EJB
  UiHolderBean ui;
  @EJB
  LocalizationBean loc;

  @PostConstruct
  public void init() {
    stop();
    Config.setInitialized(false);

    LogUtils.setLoggerFactory(new LogbackFactory());
    Config.init();

    Localizations.init();
    Localized.setGlossary(Localizations.getGlossary(SupportedLocale.USER_DEFAULT));

    sys.init();
    prm.init();
    cb.init();

    moduleBean.getModules().forEach(moduleName -> prm.refreshParameters(moduleName));

    Map<String, String> props = prm.getMap(AdministrationConstants.PRM_SERVER_PROPERTIES);
    if (!BeeUtils.isEmpty(props)) {
      props.forEach(Config::setProperty);
    }
    SupportedLocale.ACTIVE_LOCALES.clear();
    SupportedLocale.ACTIVE_LOCALES.addAll(Config.getList(Service.PROPERTY_ACTIVE_LOCALES));

    sys.initViews();
    chat.init();

    moduleBean.initModules();

    usr.initRights();
    usr.initUsers();
    usr.initIpFilters();

    Collection<SupportedLocale> customizedLocales = loc.customizeGlossaries();
    if (BeeUtils.contains(customizedLocales, SupportedLocale.USER_DEFAULT)
        || BeeUtils.contains(customizedLocales, SupportedLocale.DICTIONARY_DEFAULT)) {
      Localized.setGlossary(Localizations.getGlossary(SupportedLocale.USER_DEFAULT));
    }

    ui.init();

    Config.setInitialized(true);
  }

  @PreDestroy
  public void stop() {
    LogUtils.stopLogger();
  }
}
