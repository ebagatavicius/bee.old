package com.butent.bee.shared.rights;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum RegulatedWidget implements HasLocalizedCaption {

  NEWS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.domainNews();
    }
  },
  ONLINE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.domainOnline();
    }
  },
  ROOMS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.domainRooms();
    }
  },
  ADMIN(ModuleAndSub.of(Module.ADMINISTRATION)) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return "Admin";
    }
  },
  AUDIT {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.actionAudit();
    }
  },
  DOCUMENT_TREE {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.documentTree();
    }
  };
  private final ModuleAndSub moduleAndSub;

  private RegulatedWidget() {
    this(null);
  }

  private RegulatedWidget(ModuleAndSub moduleAndSub) {
    this.moduleAndSub = moduleAndSub;
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getName() {
    return BeeUtils.proper(name());
  }
}
