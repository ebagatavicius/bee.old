package com.butent.bee.server.modules;

import com.google.common.collect.ImmutableSet;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
@Lock(LockType.READ)
public class ModuleHolderBean {

  private enum TableActivationMode {
    NEW, ACTIVE, ALL
  }

  private static BeeLogger logger = LogUtils.getLogger(ModuleHolderBean.class);

  private final Map<String, BeeModule> modules = new LinkedHashMap<>();

  @EJB
  SystemBean sys;
  @EJB
  ParamHolderBean prm;
  @EJB
  UserServiceBean usr;

  public ResponseObject doModule(String moduleName, RequestInfo reqInfo) {
    Assert.notNull(reqInfo);
    return getModule(moduleName).doService(reqInfo.getParameter(Service.VAR_METHOD), reqInfo);
  }

  public List<SearchResult> doSearch(String query) {
    Assert.notEmpty(query);
    List<SearchResult> results = new ArrayList<>();

    for (BeeModule module : modules.values()) {
      if (usr.isModuleVisible(ModuleAndSub.of(module.getModule()))) {
        List<SearchResult> found = module.doSearch(query);
        if (found != null && !found.isEmpty()) {
          results.addAll(found);
        }
      }
    }
    return results;
  }

  public Collection<BeeParameter> getModuleDefaultParameters(String moduleName) {
    return getModule(moduleName).getDefaultParameters();
  }

  public Collection<String> getModules() {
    return ImmutableSet.copyOf(modules.keySet());
  }

  public String getResourcePath(String moduleName, String... resources) {
    Assert.isTrue(!ArrayUtils.isEmpty(resources));
    String resource = ArrayUtils.join("/", resources);

    if (!BeeUtils.isEmpty(moduleName)) {
      resource = BeeUtils.join("/", BeeUtils.normalize(Service.PROPERTY_MODULES),
          getModule(moduleName).getResourcePath(), resource);
    }
    return resource;
  }

  public boolean hasModule(String moduleName) {
    Assert.notEmpty(moduleName);
    return modules.containsKey(moduleName);
  }

  public void initModules() {
    TableActivationMode mode = EnumUtils.getEnumByName(TableActivationMode.class,
        Config.getProperty("TableActivationMode"));

    for (String mod : getModules()) {
      prm.refreshParameters(mod);
    }
    if (mode != null) {
      switch (mode) {
        case NEW:
          sys.ensureTables();
          break;
        case ACTIVE:
          sys.rebuildActiveTables();
          break;
        case ALL:
          for (String tblName : sys.getTableNames()) {
            sys.rebuildTable(tblName);
          }
          break;
      }
    }
    for (String mod : getModules()) {
      getModule(mod).init();
    }
  }

  private BeeModule getModule(String moduleName) {
    Assert.state(hasModule(moduleName), "Unknown module name: " + moduleName);
    return modules.get(moduleName);
  }

  @SuppressWarnings("unchecked")
  @PostConstruct
  private void init() {
    Module.setEnabledModules(Config.getProperty(Service.PROPERTY_MODULES));

    List<String> mods = new ArrayList<>();

    for (Module modul : Module.values()) {
      if (BeeUtils.isEmpty(modul.getName())) {
        logger.severe("Module", BeeUtils.bracket(modul.name()), "does not have name");
      } else {
        mods.add(modul.getName());
      }
    }
    for (String moduleName : mods) {
      if (hasModule(moduleName)) {
        logger.severe("Duplicate module name:", BeeUtils.bracket(moduleName));
      } else {
        try {
          Class<BeeModule> clazz = (Class<BeeModule>) Class.forName(BeeUtils.join(".",
              this.getClass().getPackage().getName(), moduleName.toLowerCase(),
              moduleName + "ModuleBean"));

          BeeModule module = Invocation.locateRemoteBean(clazz);

          if (module != null) {
            modules.put(moduleName, module);
            logger.info("Registered module:", BeeUtils.bracket(moduleName));
          }
        } catch (ClassNotFoundException | ClassCastException e) {
          logger.error(e);
        }
      }
    }
    if (modules.isEmpty()) {
      logger.warning("No modules registered");
    }
  }
}
