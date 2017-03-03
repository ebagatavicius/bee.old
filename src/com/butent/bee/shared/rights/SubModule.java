package com.butent.bee.shared.rights;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum SubModule implements HasLocalizedCaption {
  ADMINISTRATION {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.administration();
    }
  },
  CONTACTS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.contacts();
    }
  },
  SELFSERVICE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trSelfService();
    }

    @Override
    public String getName() {
      return "SelfService";
    }
  },
  LOGISTICS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trLogistics();
    }
  },
  TEMPLATES {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.template();
    }
  },
  CLASSIFIERS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.classifiers();
    }
  },
  SERVICE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.svcModule();
    }
  },
  ACTS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.tradeActs();
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

  public String getName() {
    return BeeUtils.proper(name());
  }
}
