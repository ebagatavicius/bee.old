package com.butent.bee.shared.rights;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.List;
import java.util.Set;

public enum Module implements HasLocalizedCaption {

  CLASSIFIERS(SubModule.CONTACTS) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.classifiers();
    }
  },
  CALENDAR {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.calendar();
    }
  },
  DOCUMENTS(SubModule.TEMPLATES) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.documents();
    }
  },
  TASKS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.crmTasks();
    }
  },
  DISCUSSIONS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.discussions();
    }
  },
  MAIL(SubModule.ADMINISTRATION) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.mail();
    }
  },
  ECOMMERCE(SubModule.ADMINISTRATION, SubModule.CLASSIFIERS) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.ecModule();
    }

    @Override
    public String getName() {
      return "Ec";
    }
  },
  TRADE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trade();
    }
  },
  TRANSPORT(SubModule.SELFSERVICE, SubModule.LOGISTICS, SubModule.ADMINISTRATION) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.transport();
    }
  },
  SERVICE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.svcModule();
    }
  },
  ADMINISTRATION {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.administration();
    }
  };

  static final Set<ModuleAndSub> ENABLED_MODULES = Sets.newHashSet();

  public static String getEnabledModulesAsString() {
    return BeeUtils.joinItems(ENABLED_MODULES);
  }

  public static boolean isEnabled(String input) {
    if (BeeUtils.isEmpty(input)) {
      return true;
    }

    ModuleAndSub ms = ModuleAndSub.parse(input);
    if (ms == null) {
      return false;
    } else {
      return ENABLED_MODULES.contains(ms);
    }
  }

  public static void setEnabledModules(String moduleList) {
    if (!ENABLED_MODULES.isEmpty()) {
      ENABLED_MODULES.clear();
    }

    if (BeeUtils.isEmpty(moduleList) || BeeUtils.same(moduleList, BeeConst.ALL)) {
      for (Module module : Module.values()) {
        ENABLED_MODULES.add(ModuleAndSub.of(module));

        if (!BeeUtils.isEmpty(module.getSubModules())) {
          for (SubModule subModule : module.getSubModules()) {
            ENABLED_MODULES.add(ModuleAndSub.of(module, subModule));
          }
        }
      }

    } else {
      Set<String> modules = NameUtils.toSet(moduleList);

      for (String input : modules) {
        ModuleAndSub ms = ModuleAndSub.parse(input);

        if (ms != null) {
          if (ms.getSubModule() != null) {
            ModuleAndSub parent = ModuleAndSub.of(ms.getModule());
            if (!ENABLED_MODULES.contains(parent)) {
              ENABLED_MODULES.add(parent);
            }
          }

          ENABLED_MODULES.add(ms);
        }
      }
    }
  }

  private final List<SubModule> subModules = Lists.newArrayList();

  private Module(SubModule... subModules) {
    if (subModules != null) {
      for (SubModule subModule : subModules) {
        if (!this.subModules.contains(subModule)) {
          this.subModules.add(subModule);
        }
      }
    }
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public String getName() {
    return BeeUtils.proper(name());
  }

  public List<SubModule> getSubModules() {
    return subModules;
  }

  public boolean isEnabled() {
    return ENABLED_MODULES.contains(ModuleAndSub.of(this));
  }
}
