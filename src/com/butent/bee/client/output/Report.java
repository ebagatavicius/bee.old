package com.butent.bee.client.output;

import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.modules.classifiers.CompanyTypeReport;
import com.butent.bee.client.modules.classifiers.CompanyUsageReport;
import com.butent.bee.client.modules.trade.acts.TradeActItemsByCompanyReport;
import com.butent.bee.client.modules.trade.acts.TradeActServicesReport;
import com.butent.bee.client.modules.trade.acts.TradeActStockReport;
import com.butent.bee.client.modules.trade.acts.TradeActTransferReport;
import com.butent.bee.client.modules.transport.AssessmentQuantityReport;
import com.butent.bee.client.modules.transport.AssessmentTurnoverReport;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.form.interceptor.ExtendedReportInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectPriority;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.TripStatus;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public enum Report implements HasWidgetSupplier {
  COMPANY_TYPES(ModuleAndSub.of(Module.CLASSIFIERS), "CompanyTypes", "CompanyRelationTypeReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new CompanyTypeReport();
    }
  },
  COMPANY_USAGE(ModuleAndSub.of(Module.CLASSIFIERS), "CompanyUsage", "CompanyUsageReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new CompanyUsageReport();
    }
  },

  ASSESSMENT_QUANTITY(ModuleAndSub.of(Module.TRANSPORT), "AssessmentQuantity",
      "AssessmentQuantityReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new AssessmentQuantityReport();
    }
  },
  ASSESSMENT_TURNOVER(ModuleAndSub.of(Module.TRANSPORT), "AssessmentTurnover",
      "AssessmentTurnoverReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new AssessmentTurnoverReport();
    }
  },

  TRADE_ACT_ITEMS_BY_COMPANY(ModuleAndSub.of(Module.TRADE, SubModule.ACTS),
      "TradeActItemsByCompany", "TradeActItemsByCompanyReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActItemsByCompanyReport();
    }
  },
  TRADE_ACT_STOCK(ModuleAndSub.of(Module.TRADE, SubModule.ACTS), "TradeActStock",
      "TradeActStockReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActStockReport();
    }
  },
  TRADE_ACT_SERVICES(ModuleAndSub.of(Module.TRADE, SubModule.ACTS), "TradeActServices",
      "TradeActServicesReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActServicesReport();
    }
  },
  TRADE_ACT_TRANSFER(ModuleAndSub.of(Module.TRADE, SubModule.ACTS), "TradeActTransfer",
      "TradeActTransferReport") {
    @Override
    protected ReportInterceptor getInterceptor() {
      return new TradeActTransferReport();
    }
  },

  TRANSPORT_TRIP_PROFIT(ModuleAndSub.of(Module.TRANSPORT),
      TransportConstants.SVC_TRIP_PROFIT_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      LocalizableConstants loc = Localized.getConstants();

      return Arrays.asList(
          new ReportTextItem(TransportConstants.COL_TRIP,
              Data.getColumnLabel(TransportConstants.TBL_TRIP_COSTS, TransportConstants.COL_TRIP)),
          new ReportTextItem(TransportConstants.COL_TRIP_NO,
              Data.getColumnLabel(TransportConstants.TBL_TRIPS, TransportConstants.COL_TRIP_NO)),
          new ReportDateTimeItem(TransportConstants.COL_TRIP_DATE, loc.date()),
          new ReportDateItem(TransportConstants.COL_TRIP_DATE_FROM, loc.dateFrom()),
          new ReportDateItem(TransportConstants.COL_TRIP_DATE_TO, loc.dateTo()),
          new ReportTextItem(TransportConstants.COL_VEHICLE,
              Data.getColumnLabel(TransportConstants.TBL_TRIPS, TransportConstants.COL_VEHICLE)),
          new ReportTextItem(TransportConstants.COL_TRAILER,
              Data.getColumnLabel(TransportConstants.TBL_TRIPS, TransportConstants.COL_TRAILER)),
          new ReportTextItem("Route",
              Data.getColumnLabel(TransportConstants.TBL_TRIP_ROUTES, "Route")),
          new ReportEnumItem(TransportConstants.COL_TRIP_STATUS,
              Data.getColumnLabel(TransportConstants.TBL_TRIPS, TransportConstants.COL_TRIP_STATUS),
              TripStatus.class),
          new ReportNumericItem("Kilometers", "Kilometrai"),
          new ReportNumericItem("FuelCosts", "Kuro išl.").setPrecision(2),
          new ReportNumericItem("DailyCosts", "Dienpinigių išl.").setPrecision(2),
          new ReportNumericItem("RoadCosts", "Kelių išl.").setPrecision(2),
          new ReportNumericItem("ConstantCosts", Localized.getConstants().trConstantCosts())
              .setPrecision(2),
          new ReportNumericItem("OtherCosts", "Kitos išl.").setPrecision(2),
          new ReportNumericItem("TripIncome", "Pajamos").setPrecision(2));
    }

    @Override
    public String getReportCaption() {
      return Localized.maybeTranslate("=trReportTripProfit");
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getName(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      for (String item : new String[] {
          TransportConstants.COL_TRIP, TransportConstants.COL_TRIP_NO,
          TransportConstants.COL_TRIP_DATE_FROM, TransportConstants.COL_TRIP_DATE_TO,
          TransportConstants.COL_TRAILER}) {
        report.addRowItem(items.get(item));
      }
      report.setRowGrouping(items.get(TransportConstants.COL_VEHICLE));

      report.addColItem(items.get("Kilometers"));

      ReportItem income = items.get("TripIncome");
      report.addColItem(income.clone());

      ReportFormulaItem costs = new ReportFormulaItem("Išlaidos");
      costs.setPrecision(2);

      for (String item : new String[] {
          "FuelCosts", "DailyCosts", "RoadCosts", "ConstantCosts", "OtherCosts"}) {
        costs.plus(items.get(item));
      }
      report.addColItem(costs.clone());
      report.addColItem(new ReportFormulaItem("Pelnas").plus(income).minus(costs).setPrecision(2));

      report.getFilterItems().add(items.get(TransportConstants.COL_TRIP_STATUS)
          .setFilter(BeeUtils.toString(TripStatus.COMPLETED.ordinal())));

      return Collections.singletonList(report);
    }
  },

  PROJECT_REPORT(ModuleAndSub.of(Module.PROJECTS), ProjectConstants.SVC_PROJECT_REPORT) {
    @Override
    public List<ReportItem> getItems() {
      LocalizableConstants loc = Localized.getConstants();

      return Arrays.asList(
          new ReportTextItem(ProjectConstants.COL_PROJECT_NAME, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_NAME)),
          new ReportTextItem(ClassifierConstants.ALS_COMPANY_NAME, loc.client()),
          new ReportTextItem(ProjectConstants.ALS_STAGE_NAME, loc.prjStage()),
          new ReportTextItem(ProjectConstants.COL_PROJECT_OWNER, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_OWNER)),
          new ReportTextItem(ProjectConstants.COL_PROJECT, loc.project()),
          new ReportEnumItem(ProjectConstants.COL_PROJECT_STATUS, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_STATUS),
              ProjectStatus.class),
          new ReportTextItem(ProjectConstants.COL_PROJECT_TYPE, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_TYPE)),
          new ReportEnumItem(ProjectConstants.COL_PROJECT_PRIORITY, Data.getColumnLabel(
              ProjectConstants.VIEW_PROJECTS, ProjectConstants.COL_PROJECT_PRIORITY),
              ProjectPriority.class),
          new ReportTextItem(ProjectConstants.ALS_TERM, loc.prjTerm()),

          /* calc */
          new ReportNumericItem(TaskConstants.COL_ACTUAL_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(TaskConstants.VIEW_TASKS, TaskConstants.COL_ACTUAL_DURATION),
                  loc.unitHourShort())).setPrecision(2),

          new ReportNumericItem(TaskConstants.COL_EXPECTED_DURATION,
              BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
                  Data.getColumnLabel(TaskConstants.VIEW_TASKS,
                      TaskConstants.COL_EXPECTED_DURATION), loc.unitHourShort())).setPrecision(2),
          new ReportNumericItem(TaskConstants.COL_EXPECTED_EXPENSES, loc.crmTaskExpectedExpenses())
              .setPrecision(2),

          new ReportNumericItem(TaskConstants.COL_ACTUAL_EXPENSES,
              Data.getColumnLabel(TaskConstants.VIEW_TASKS, TaskConstants.COL_ACTUAL_EXPENSES))
              .setPrecision(2),

          new ReportNumericItem(TaskConstants.COL_TASK, loc.crmTask()),
          new ReportNumericItem(ProjectConstants.ALS_PROFIT, loc.profit()).setPrecision(2),

          new ReportEnumItem(ProjectConstants.ALS_TASK_STATUS, BeeUtils.joinWords(Data
                  .getColumnLabel(TaskConstants.VIEW_TASKS, TaskConstants.COL_STATUS),
              BeeUtils.parenthesize(loc.crmTasks())), TaskStatus.class)
      );
    }

    @Override
    public Collection<ReportInfo> getReports() {
      Map<String, ReportItem> items = new HashMap<>();

      for (ReportItem item : getItems()) {
        items.put(item.getName(), item);
      }
      ReportInfo report = new ReportInfo(getReportCaption());

      for (String item : new String[] {
          ProjectConstants.COL_PROJECT_NAME,
          ProjectConstants.COL_PROJECT_OWNER,
          ProjectConstants.COL_PROJECT_STATUS,
          ProjectConstants.ALS_TERM
      }) {
        report.addRowItem(items.get(item));
      }
      report.setRowGrouping(items.get(ClassifierConstants.ALS_COMPANY_NAME));

      for (String item : new String[] {
          TaskConstants.COL_EXPECTED_DURATION,
          TaskConstants.COL_ACTUAL_DURATION,
          TaskConstants.COL_EXPECTED_EXPENSES,
          TaskConstants.COL_ACTUAL_EXPENSES,
          ProjectConstants.ALS_PROFIT
      }) {
        report.addColItem(items.get(item));
      }
      report.setColGrouping(items.get(ProjectConstants.ALS_TASK_STATUS));
      return Arrays.asList(report);
    }
  };

  private static BeeLogger logger = LogUtils.getLogger(Report.class);

  public static void open(String reportName) {
    Report report = parse(reportName);
    if (report != null) {
      report.open();
    }
  }

  public static void open(String reportName, final ViewCallback callback) {
    Assert.notNull(callback);

    final Report report = parse(reportName);
    if (report != null) {
      FormFactory.getFormDescription(report.getFormName(), new Callback<FormDescription>() {
        @Override
        public void onSuccess(FormDescription result) {
          FormFactory.openForm(result, report.getInterceptor(),
              ViewFactory.getPresenterCallback(callback));
        }
      });
    }
  }

  public static Report parse(String input) {
    for (Report report : values()) {
      if (BeeUtils.same(report.reportName, input)) {
        return report;
      }
    }

    logger.severe("report not recognized:", input);
    return null;
  }

  private final ModuleAndSub moduleAndSub;
  private final String reportName;
  private final String formName;

  private Report(ModuleAndSub module, String reportName) {
    this(module, reportName, "ExtendedReport");
  }

  private Report(ModuleAndSub moduleAndSub, String reportName, String formName) {
    this.moduleAndSub = Assert.notNull(moduleAndSub);
    this.reportName = Assert.notEmpty(reportName);
    this.formName = formName;
  }

  public String getFormName() {
    return formName;
  }

  public List<ReportItem> getItems() {
    return new ArrayList<>();
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getReportCaption() {
    return getReportName();
  }

  public String getReportName() {
    return reportName;
  }

  public Collection<ReportInfo> getReports() {
    return new LinkedHashSet<>();
  }

  @Override
  public String getSupplierKey() {
    return ViewFactory.SupplierKind.REPORT.getKey(reportName);
  }

  public void open() {
    FormFactory.openForm(formName, getInterceptor());
  }

  public void open(ReportParameters parameters) {
    ReportInterceptor interceptor = getInterceptor();
    interceptor.setInitialParameters(parameters);

    FormFactory.openForm(formName, interceptor);
  }

  protected ReportInterceptor getInterceptor() {
    return new ExtendedReportInterceptor(this);
  }
}
