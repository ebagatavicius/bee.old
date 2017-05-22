package com.butent.bee.client.modules.payroll;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.layout.SummaryProxy;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.shared.modules.payroll.PayrollConstants;
import com.butent.bee.shared.rights.RegulatedWidget;

public final class PayrollHelper {

  private PayrollHelper() {
  }

  public static void disableWidgetTab(TabbedPages tabbedPages) {
    if (tabbedPages != null) {

      for (int i = 0; i < tabbedPages.getPageCount(); i++) {
        Widget tw = tabbedPages.getContentWidget(i);
        if (tw instanceof SummaryProxy) {
          Widget w = ((SummaryProxy) tw).getWidget();
          if (w instanceof WorkScheduleWidget) {
            PayrollConstants.WorkScheduleKind kind = ((WorkScheduleWidget) w).getWorkScheduleKind();

            if (isWorkScheduleWidgetEnabled(kind)) {
              tabbedPages.enablePage(i);
            } else {
              tabbedPages.disablePage(i);
            }
          } else if (w instanceof EarningsWidget) {
            if (isEarningsWidgetEnabled()) {
              tabbedPages.enablePage(i);
            } else {
              tabbedPages.disablePage(i);
            }
          } else if (w == null) {
            tabbedPages.disablePage(i);
          } else {
            tabbedPages.enablePage(i);
          }
        }
      }
    }
  }

  public static boolean isEarningsWidgetEnabled() {
    return BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.EARNINGS);
  }

  public static boolean isWorkScheduleWidgetEnabled(PayrollConstants.WorkScheduleKind kind) {

    return !((kind == PayrollConstants.WorkScheduleKind.PLANNED
      && !BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.WORK_SCHEDULE))
      || (kind == PayrollConstants.WorkScheduleKind.ACTUAL
      && !BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.TIME_SHEET)));
  }

}
