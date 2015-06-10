package com.butent.bee.shared.rights;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum SubModule implements HasLocalizedCaption {
  ADMINISTRATION {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.administration();
    }
  },
  CONTACTS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.contacts();
    }
  },
  SELFSERVICE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trSelfService();
    }

    @Override
    public String getName() {
      return "SelfService";
    }
  },
  LOGISTICS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trLogistics();
    }
  },
  TEMPLATES {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.template();
    }
  },
  CLASSIFIERS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.classifiers();
    }
  },
  ACTS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.tradeActs();
    }
  },
  LOGISTICS_SELFSERVICE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trLogisticsSelfService();
    }

    @Override
    public String getName() {
      return "LogisticsSelfService";
    }
  };

  public static SubModule parse(String input) {
    for (SubModule subModule : values()) {
      if (BeeUtils.same(subModule.getName(), input)) {
        return subModule;
      }
    }
    return null;
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public String getName() {
    return BeeUtils.proper(name());
  }
}
