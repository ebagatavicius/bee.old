package com.butent.bee.client.modules.tasks;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;

public class TasksReportsInterceptor extends AbstractFormInterceptor {

  public enum ReportType implements HasWidgetSupplier {
    TYPE_HOURS, COMPANY_TIMES, USERS_HOURS;

    @Override
    public String getSupplierKey() {
      return "tasks_report_" + name().toLowerCase();
    }

    void register() {
      ViewFactory.registerSupplier(getSupplierKey(), new ViewSupplier() {
        @Override
        public void create(final ViewCallback callback) {
          FormFactory.getFormDescription(TaskConstants.FORM_TASKS_REPORT,
              new Callback<FormDescription>() {
                @Override
                public void onSuccess(FormDescription result) {
                  FormFactory.openForm(result, new TasksReportsInterceptor(ReportType.this),
                      ViewFactory.getPresenterCallback(callback));
                }
              });
        }
      });
    }
  }

  private static class ClearReportsFilter implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      final FormView form = ViewHelper.getForm((Widget) event.getSource());

      InputDate fromDate = (InputDate) form.getWidgetByName(WIDGET_DATE_FROM_NAME);

      if (fromDate != null) {
        fromDate.clearValue();
      }

      InputDate tillDate = (InputDate) form.getWidgetByName(WIDGET_DATE_TILL_NAME);

      if (tillDate != null) {
        tillDate.clearValue();
      }

      MultiSelector userId = (MultiSelector) form.getWidgetByName(WIDGET_USER_NAME);

      if (userId != null) {
        userId.clearValue();
      }

      MultiSelector companyId = (MultiSelector) form.getWidgetByName(WIDGET_COMPANY_NAME);

      if (companyId != null) {
        companyId.clearValue();
      }

      MultiSelector durationTId = (MultiSelector) form.getWidgetByName(WIDGET_DURATION_TYPE_NAME);

      if (durationTId != null) {
        durationTId.clearValue();
      }
    }
  }
  private static final class ReportsFilter implements ClickHandler {

    private ReportType reportType;

    private ReportsFilter(ReportType rt) {
      this.reportType = rt;
    }

    @Override
    public void onClick(ClickEvent event) {
      final FormView form = ViewHelper.getForm((Widget) event.getSource());
      ParameterList params = null;

      switch (reportType) {
        case TYPE_HOURS:
          params = TasksKeeper.createArgs(TaskConstants.SVC_TASKS_REPORTS_TYPE_HOURS);
          break;
        case COMPANY_TIMES:
          params = TasksKeeper.createArgs(TaskConstants.SVC_TASKS_REPORTS_COMPANY_TIMES);
          break;
        case USERS_HOURS:
          params = TasksKeeper.createArgs(TaskConstants.SVC_TASKS_REPORTS_USERS_HOURS);
          break;
        default:
          params = TasksKeeper.createArgs(TaskConstants.SVC_TASKS_REPORTS_TYPE_HOURS);
          break;
      }

      /* Hiding zero times */
      params.addQueryItem(TaskConstants.VAR_TASK_DURATION_HIDE_ZEROS, Boolean.TRUE.toString());

      InputDate fromDate = (InputDate) form.getWidgetByName(WIDGET_DATE_FROM_NAME);

      if (fromDate != null) {
        if (!BeeUtils.isEmpty(fromDate.getValue())) {
          DateTime time = fromDate.getDate().getDateTime();
          time.setHour(MIDNIGHT_HOUR);
          time.setMinute(START_MINUTE_OF_HOUR);
          params.addQueryItem(TaskConstants.VAR_TASK_DURATION_DATE_FROM, time.getTime());
        }
      }

      InputDate tillDate = (InputDate) form.getWidgetByName(WIDGET_DATE_TILL_NAME);

      if (tillDate != null) {
        if (!BeeUtils.isEmpty(tillDate.getValue())) {
          DateTime time = tillDate.getDate().getDateTime();
          time.setHour(LAST_HOUR_OF_DAY);
          time.setMinute(LAST_MINUTE_OF_HOUR);
          params.addQueryItem(TaskConstants.VAR_TASK_DURATION_DATE_TO, time.getTime());
        }
      }

      MultiSelector userId = (MultiSelector) form.getWidgetByName(WIDGET_USER_NAME);

      if (userId != null) {
        if (!BeeUtils.isEmpty(userId.getValue())) {
          params.addQueryItem(TaskConstants.VAR_TASK_PUBLISHER, userId.getValue().trim());
        }
      }

      MultiSelector companyId = (MultiSelector) form.getWidgetByName(WIDGET_COMPANY_NAME);

      if (companyId != null) {
        if (!BeeUtils.isEmpty(companyId.getValue())) {
          params.addQueryItem(TaskConstants.VAR_TASK_COMPANY, companyId.getValue().trim());
        }
      }

      MultiSelector durationTId = (MultiSelector) form.getWidgetByName(WIDGET_DURATION_TYPE_NAME);

      if (durationTId != null) {
        if (!BeeUtils.isEmpty(durationTId.getValue())) {
          params.addQueryItem(TaskConstants.VAR_TASK_DURATION_TYPE, durationTId.getValue().trim());
        }
      }

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

        @Override
        public void onResponse(ResponseObject response) {
          SimpleRowSet durationTimesSet = null;
          Assert.notNull(response);

          if (response.hasResponse(SimpleRowSet.class)) {
            durationTimesSet = SimpleRowSet.restore((String) response.getResponse());
            if (!durationTimesSet.isEmpty()) {
              doReport(form, durationTimesSet);
            }
          } else {
            if (response.hasErrors()) {
              LogUtils.getLogger(TasksReportsInterceptor.class).debug(
                  (Object[]) response.getErrors());
            }
            if (response.hasNotifications()) {
              LogUtils.getLogger(TasksReportsInterceptor.class).debug(
                  (Object[]) response.getNotifications());
            }
          }
        }

        private void doReport(FormView formView, SimpleRowSet rowSet) {
          FlowPanel reportPanel = (FlowPanel) formView.getWidgetByName(WIDGET_REPORT_TABLE_NAME);
          reportPanel.clear();

          int gridRows = rowSet.getNumberOfRows();
          int gridCols = rowSet.getNumberOfColumns();
          HtmlTable g = new HtmlTable();

          if (gridRows < MIN_ROW_SET) {
            g.setHtml(0, 0, Localized.getConstants().noData());
            reportPanel.add(g);
            return;
          }

          g.addStyleName(BeeConst.CSS_CLASS_PREFIX + "crm-taskDuration-display");
          for (int i = 0; i < gridRows; i++) {
            for (int j = 0; j < gridCols; j++) {
              g.setHtml(i, j, rowSet.getValue(i, j));

              if (i == 0) {
                g.getCellFormatter().addStyleName(i, j,
                    BeeConst.CSS_CLASS_PREFIX + "crm-taskDuration-colLabel");
              }
              if (i == gridRows - 1) {
                g.getCellFormatter().addStyleName(i, j,
                    BeeConst.CSS_CLASS_PREFIX + "crm-taskDuration-rowTotal");
              }
            }
          }
          reportPanel.add(g);
        }
      });
    }
  }

  private static final String WIDGET_DATE_FROM_NAME = "dateFrom";
  private static final String WIDGET_DATE_TILL_NAME = "dateTill";
  private static final String WIDGET_FILTER_NAME = "Filter";
  private static final String WIDGET_CLEAR_FILTER_NAME = "ClearFilter";
  private static final String WIDGET_REPORT_TABLE_NAME = "reportTable";
  private static final String WIDGET_USER_NAME = "User";

  private static final String WIDGET_COMPANY_NAME = "Company";
  private static final String WIDGET_DURATION_TYPE_NAME = "DurationType";
  private static final int FIRST_DAY_OF_MONTH = 1;
  private static final int MIDNIGHT_HOUR = 0;
  private static final int START_MINUTE_OF_HOUR = 0;
  private static final int LAST_HOUR_OF_DAY = 23;

  private static final int LAST_MINUTE_OF_HOUR = 59;

  private static final int MIN_ROW_SET = 3;

  private final ReportType reportType;

  TasksReportsInterceptor(ReportType rt) {
    this.reportType = rt;
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
    if (BeeUtils.same(name, WIDGET_FILTER_NAME) && (widget instanceof HasClickHandlers)) {
      ((HasClickHandlers) widget)
          .addClickHandler(new ReportsFilter(this.reportType));
    }

    if (BeeUtils.same(name, WIDGET_DATE_FROM_NAME) && widget instanceof InputDate) {
      InputDate dateFrom = (InputDate) widget;
      DateTime thMonth = new DateTime();
      thMonth.setDom(FIRST_DAY_OF_MONTH);
      dateFrom.setDate(thMonth);
    }
    if (BeeUtils.same(name, WIDGET_DATE_TILL_NAME) && widget instanceof InputDate) {
      InputDate dateFrom = (InputDate) widget;
      DateTime thMonth = new DateTime();
      dateFrom.setDate(thMonth);
    }
    if (BeeUtils.same(name, WIDGET_CLEAR_FILTER_NAME) && (widget instanceof HasClickHandlers)) {
      ((HasClickHandlers) widget)
          .addClickHandler(new ClearReportsFilter());
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new TasksReportsInterceptor(reportType);
  }

  @Override
  public String getSupplierKey() {
    return reportType.getSupplierKey();
  }

  @Override
  public void onStart(FormView form) {
    Widget widget = form.getWidgetByName(WIDGET_FILTER_NAME);
    HeaderView header = form.getViewPresenter().getHeader();
    String reportCaption;

    switch (reportType) {
      case TYPE_HOURS:
        reportCaption = Localized.getConstants().hoursByTypes();
        break;
      case COMPANY_TIMES:
        reportCaption = Localized.getConstants().hoursByCompanies();
        break;
      case USERS_HOURS:
        reportCaption = Localized.getConstants().hoursByUsers();
        break;
      default:
        reportCaption = Localized.getConstants().hoursByTypes();
        break;
    }

    header.setCaption(reportCaption);

    if (widget instanceof Button) {
      Button btn = (Button) widget;
      btn.click();
    }
  }
}
