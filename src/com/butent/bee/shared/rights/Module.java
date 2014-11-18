package com.butent.bee.shared.rights;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public enum Module implements HasLocalizedCaption {

  CLASSIFIERS(SubModule.CONTACTS) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.references();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "commons";
    }
  },

  CALENDAR {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.calendar();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "calendar";
    }
  },

  DOCUMENTS(SubModule.TEMPLATES) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.documents();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "documents";
    }
  },

  TASKS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.crmTasks();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "task";
    }
  },

  DISCUSSIONS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.discussions();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "discuss";
    }
  },

  MAIL(SubModule.ADMINISTRATION) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.mail();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "mail";
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

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "ecommerce";
    }
  },

  TRADE(SubModule.ACTS) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trade();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return (subModule == SubModule.ACTS) ? "tradeact" : "trade";
    }
  },

  TRANSPORT(SubModule.SELFSERVICE, SubModule.LOGISTICS, SubModule.ADMINISTRATION) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.transport();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "transport";
    }
  },

  SERVICE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.svcModule();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return "service";
    }
  },

  ADMINISTRATION {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.administration();
    }

    @Override
    public String getStyleSheet(SubModule subModule) {
      return null;
    }
  };

  static final List<ModuleAndSub> ENABLED_MODULES = new ArrayList<>();

  public static String getEnabledModulesAsString() {
    return BeeUtils.joinItems(ENABLED_MODULES);
  }

  public static List<String> getEnabledStyleSheets() {
    List<String> sheets = new ArrayList<>();

    for (ModuleAndSub ms : ENABLED_MODULES) {
      String sheet = ms.getModule().getStyleSheet(ms.getSubModule());
      if (!BeeUtils.isEmpty(sheet) && !sheets.contains(sheet)) {
        sheets.add(sheet);
      }
    }

    return sheets;
  }

  public static boolean isAnyEnabled(String input) {
    if (BeeUtils.isEmpty(input)) {
      return true;
    } else {
      return BeeUtils.intersects(ENABLED_MODULES, ModuleAndSub.parseList(input));
    }
  }

  public static void setEnabledModules(String input) {
    if (!ENABLED_MODULES.isEmpty()) {
      ENABLED_MODULES.clear();
    }

    if (BeeUtils.isEmpty(input) || BeeUtils.same(input, BeeConst.ALL)) {
      for (Module module : Module.values()) {
        ENABLED_MODULES.add(ModuleAndSub.of(module));

        if (!BeeUtils.isEmpty(module.getSubModules())) {
          for (SubModule subModule : module.getSubModules()) {
            ENABLED_MODULES.add(ModuleAndSub.of(module, subModule));
          }
        }
      }

    } else {
      List<ModuleAndSub> list = ModuleAndSub.parseList(input);

      for (ModuleAndSub ms : list) {
        if (ms != null) {
          if (ms.getSubModule() != null) {
            ModuleAndSub parent = ModuleAndSub.of(ms.getModule());
            if (!ENABLED_MODULES.contains(parent)) {
              ENABLED_MODULES.add(parent);
            }
          }

          if (!ENABLED_MODULES.contains(ms)) {
            ENABLED_MODULES.add(ms);
          }
        }
      }
    }
  }

  private final List<SubModule> subModules = new ArrayList<>();

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

  public abstract String getStyleSheet(SubModule subModule);

  public List<SubModule> getSubModules() {
    return subModules;
  }

  public boolean isEnabled() {
    return ENABLED_MODULES.contains(ModuleAndSub.of(this));
  }
}
