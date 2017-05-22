package com.butent.bee.shared.rights;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum RegulatedWidget implements HasLocalizedCaption {

  NEWS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.domainNews();
    }
  },
  ONLINE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.domainOnline();
    }
  },
  ADMIN(ModuleAndSub.of(Module.ADMINISTRATION)) {
    @Override
    public String getCaption(Dictionary constants) {
      return "Admin";
    }
  },
  AUDIT {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.actionAudit();
    }
  },
  DOCUMENT_TREE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.documentTree();
    }
  },
  COMPANY_STRUCTURE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.companyStructure();
    }
  },
  EXPORT_TO_XLS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.exportToMsExcel();
    }
  },
  TIME_SHEET {
    @Override
    public String getCaption(Dictionary constants) {
      return BeeUtils.join(BeeConst.STRING_POINT, Module.PAYROLL.getCaption(constants),
        constants.timeSheet());
    }

    @Override
    public ModuleAndSub getModuleAndSub() {
      return ModuleAndSub.of(Module.PAYROLL);
    }
  },
  WORK_SCHEDULE {
    @Override
    public String getCaption(Dictionary constants) {
      return BeeUtils.join(BeeConst.STRING_POINT, Module.PAYROLL.getCaption(constants),
        constants.workSchedule());
    }

    @Override
    public ModuleAndSub getModuleAndSub() {
      return ModuleAndSub.of(Module.PAYROLL);
    }
  },
  EARNINGS {
    @Override
    public String getCaption(Dictionary constants) {
      return BeeUtils.join(BeeConst.STRING_POINT, Module.PAYROLL.getCaption(constants),
        constants.payrollEarnings());
    }

    @Override
    public ModuleAndSub getModuleAndSub() {
      return ModuleAndSub.of(Module.PAYROLL);
    }
  };

  private final ModuleAndSub moduleAndSub;

  RegulatedWidget() {
    this(null);
  }

  RegulatedWidget(ModuleAndSub moduleAndSub) {
    this.moduleAndSub = moduleAndSub;
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getName() {
    return BeeUtils.proper(name());
  }
}
