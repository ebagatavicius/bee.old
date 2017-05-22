package com.butent.bee.client.modules.payroll;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.layout.SummaryProxy;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.payroll.PayrollConstants.WorkScheduleKind;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;

class LocationForm extends AbstractFormInterceptor {

  private static void refreshSchedule(FormView form, IsRow row, String widgetName,
      WorkScheduleKind kind) {

    Widget container = form.getWidgetByName(widgetName, false);
    if (container instanceof SummaryProxy) {
      SummaryProxy panel = (SummaryProxy) container;

      if (DataUtils.hasId(row)) {
        if (kind == null) {
          EarningsWidget earn = new LocationEarnings(row.getId());
          panel.setWidget(earn);
          earn.refresh();

        } else {
          WorkScheduleWidget ws = new LocationSchedule(row.getId(), kind);
          panel.setWidget(ws);
          ws.refresh();
        }

      } else if (!panel.isEmpty()) {
        panel.clear();
      }
    }
  }

  private TabbedPages tabbedPages;

  LocationForm() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
                                FormFactory.WidgetDescriptionCallback callback) {
    if (widget instanceof TabbedPages) {
      tabbedPages = (TabbedPages) widget;
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    boolean enabledWSch = BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.WORK_SCHEDULE);
    boolean enableTSh =  BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.TIME_SHEET);

    if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.PAYROLL))) {
      if (enabledWSch) {
        refreshSchedule(form, row, "WorkSchedule", WorkScheduleKind.PLANNED);
      }

      if (enableTSh) {
        refreshSchedule(form, row, "TimeSheet", WorkScheduleKind.ACTUAL);
      }

      refreshSchedule(form, row, "Earnings", null);
    }
    PayrollHelper.disableWidgetTab(tabbedPages);
    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new LocationForm();
  }
}
