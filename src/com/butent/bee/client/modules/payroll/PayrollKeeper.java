package com.butent.bee.client.modules.payroll;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.rights.Module;

public final class PayrollKeeper {

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "payroll-";

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.PAYROLL, method);
  }

  public static void register() {
    ColorStyleProvider csp = ColorStyleProvider.createDefault(VIEW_TIME_CARD_CODES);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TIME_CARD_CODES, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TIME_CARD_CODES, COL_FOREGROUND, csp);

    csp = ColorStyleProvider.createDefault(VIEW_TIME_RANGES);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TIME_RANGES, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TIME_RANGES, COL_FOREGROUND, csp);

    csp = ColorStyleProvider.createDefault(VIEW_TIME_CARD_CHANGES);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TIME_CARD_CHANGES,
        COL_TIME_CARD_CHANGES_FROM, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TIME_CARD_CHANGES,
        COL_TIME_CARD_CHANGES_UNTIL, csp);

    csp = ColorStyleProvider.create(VIEW_WORK_SCHEDULE, ALS_TC_BACKGROUND, ALS_TC_FOREGROUND);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_WORK_SCHEDULE_DAY,
        COL_TIME_CARD_CODE, csp);

    csp = ColorStyleProvider.create(VIEW_WORK_SCHEDULE, ALS_TR_BACKGROUND, ALS_TR_FOREGROUND);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_WORK_SCHEDULE_DAY,
        COL_TIME_RANGE_CODE, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_WORK_SCHEDULE_DAY, ALS_TR_FROM, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_WORK_SCHEDULE_DAY, ALS_TR_UNTIL, csp);

    GridFactory.registerGridInterceptor(GRID_TIME_RANGES, new TimeRangesGrid());

    FormFactory.registerFormInterceptor(FORM_LOCATION, new LocationForm());
    FormFactory.registerFormInterceptor(FORM_EMPLOYEE, new EmployeeForm());

    FormFactory.registerFormInterceptor(FORM_WORK_SCHEDULE,
        new WorkScheduleForm(WorkScheduleKind.PLANNED));
    FormFactory.registerFormInterceptor(FORM_TIME_SHEET,
        new WorkScheduleForm(WorkScheduleKind.ACTUAL));

    FormFactory.registerFormInterceptor(FORM_NEW_SUBSTITUTION, new NewSubstitutionForm());
  }

  private PayrollKeeper() {
  }
}
