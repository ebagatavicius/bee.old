package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.validation.CellValidateEvent.Handler;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.css.values.Display;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants.AppointmentStatus;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Map;

final class CompanyActionForm extends AbstractFormInterceptor {

  private static final String WIDGET_REGISTER_RESULT = "RegisterResult";
  private static final String WIDGET_ACTION_RESULT_LABEL = "ActionResultLabel";
  private static final String WIDGET_COMPANY = "Company";

  private final Map<Long, IsRow> plannedDurations = Maps.newHashMap();
  private int planedDurationColumnIndex = -1;
  private boolean registerResultActive;
  private boolean canModify = true;

  private InputBoolean registerResult;
  private InputArea actionResult;
  private Label actionResultLabel;
  private DataSelector companyWidget;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(WIDGET_REGISTER_RESULT, name) && widget instanceof InputBoolean) {
      registerResult = (InputBoolean) widget;
    }

    if (BeeUtils.same(CalendarConstants.COL_ACTION_RESULT, name) && widget instanceof InputArea) {
      actionResult = (InputArea) widget;
    }

    if (BeeUtils.same(WIDGET_ACTION_RESULT_LABEL, name) && widget instanceof Label) {
      actionResultLabel = (Label) widget;
    }

    if (BeeUtils.same(WIDGET_COMPANY, name) && widget instanceof DataSelector) {
      companyWidget = (DataSelector) widget;
    }
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {

    if (action.equals(Action.SAVE) && getFormView() != null && getActiveRow() != null) {
      FormView form = getFormView();
      IsRow row = getActiveRow();

      if (canModify) {
        return isValidData(form, row);
      }
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyActionForm();
  }

  @Override
  public HeaderView getHeaderView() {
    HeaderView header = super.getHeaderView();
    header.clearCommandPanel();
    return header;
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    if (registerResultActive) {
      hideActionResultInput();
      if (registerResult != null) {
        registerResult.setChecked(false);
      }
    }
    super.onClose(messages, oldRow, newRow);
  }

  @Override
  public void onLoad(FormView form) {
    List<String> columns = Lists.newArrayList(CalendarConstants.COL_APPOINTMENT_TYPE_NAME,
        CalendarConstants.COL_APPOINTMENT_TYPE_DURATION);

    Filter filter = Filter.notNull(CalendarConstants.COL_APPOINTMENT_TYPE_DURATION);
    Queries.getRowSet(CalendarConstants.VIEW_APPOINTMENT_TYPES, columns, filter,
        getAppointmentTypesCallback());
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    HeaderView header = getHeaderView();

    ensureCanModify(form, row);

    if (form.isEnabled() && canModify(form, row)) {
      createRegisterResultAction(header, form, row);
      form.addCellValidationHandler(CalendarConstants.COL_STATUS, getStatusValidationHandler(
          form, row));
    }
    return super.onStartEdit(form, row, focusCommand);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    GridView parentGrid = getGridView();
    FormView parentForm = null;
    IsRow parentRow = null;

    int idxCompany = form.getDataIndex(ClassifierConstants.COL_COMPANY);
    int idxStartDate = form.getDataIndex(CalendarConstants.COL_START_DATE_TIME);

    if (idxStartDate > -1) {
      newRow.setValue(idxStartDate, TimeUtils.nowMinutes());
    }

    if (parentGrid != null) {
      parentForm = ViewHelper.getForm(parentGrid.asWidget());
    }

    if (parentForm != null) {
      parentRow = parentForm.getActiveRow();
    }

    if (parentRow != null
        && BeeUtils.same(parentForm.getFormName(), ClassifierConstants.FORM_COMPANY)
        && idxCompany > -1) {
      newRow.setValue(idxCompany, parentRow.getId());

      int idxCompanyName = form.getDataIndex(ClassifierConstants.ALS_COMPANY_NAME);
      int idxParentCompanyName = parentForm.getDataIndex(ClassifierConstants.COL_COMPANY_NAME);

      if (idxCompanyName > -1 && idxParentCompanyName > -1) {
        newRow.setValue(idxCompanyName, parentRow.getValue(idxParentCompanyName));
        form.refreshBySource(ClassifierConstants.COL_COMPANY);
        if (companyWidget != null) {
          companyWidget.setEnabled(false);
        }
      }
    }

    createRegisterResultCheckBox(form, newRow);
    form.addCellValidationHandler(CalendarConstants.COL_STATUS, getStatusValidationHandler(
        form, newRow));
    form.addCellValidationHandler(CalendarConstants.COL_APPOINTMENT_TYPE,
        getAppointmentTypeValidation(form, newRow));
    super.onStartNewRow(form, oldRow, newRow);
  }

  private static boolean canModify(FormView form, IsRow row) {
    if (form == null || row == null) {
      form.setEnabled(false);
    }

    int idxStatus = form.getDataIndex(CalendarConstants.COL_STATUS);
    boolean canModify = false;
    boolean isCompleted = false;

    if (idxStatus > -1) {
      isCompleted = AppointmentStatus.COMPLETED.ordinal() == row.getInteger(idxStatus);
    }

    canModify = !isCompleted;

    return canModify;
  }

  private void createRegisterResultAction(HeaderView header, FormView form, IsRow row) {
    Button action = new Button(Localized.getConstants().calActionRegisterResult());
    action.addClickHandler(getShowAndHideResultClickHandler(form, row));
    hideActionResultInput(form, row);
    header.addCommandItem(action);
  }

  private void createRegisterResultCheckBox(FormView form, IsRow row) {
    if (registerResult == null) {
      return;
    }

    StyleUtils.setDisplay(registerResult, Display.INLINE);
    registerResult.addValueChangeHandler(getShowAndHideResultValueChangeHandler(form, row));
    registerResult.setChecked(false);
    hideActionResultInput(form, row);
  }

  private void ensureCanModify(FormView form, IsRow row) {

    canModify = canModify(form, row);

    form.setEnabled(canModify);

    if (!canModify) {
      showActionResultInput();
    } else {
      hideActionResultInput();
    }
  }

  private RowSetCallback getAppointmentTypesCallback() {
    return new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        plannedDurations.clear();
        setPlanedDurationColumnIndex(-1);

        if (result == null) {
          return;
        }

        if (result.isEmpty()) {
          return;
        }

        setPlanedDurationColumnIndex(result
            .getColumnIndex(CalendarConstants.COL_APPOINTMENT_TYPE_DURATION));

        for (IsRow row : result) {
          plannedDurations.put(row.getId(), row);
        }

      }
    };
  }

  private Handler getAppointmentTypeValidation(final FormView form, final IsRow row) {
    return new Handler() {

      @Override
      public Boolean validateCell(CellValidateEvent event) {
        IsRow appointmentTypeRow = null;

        if (getPlanedDurationColumnIndex() < 0) {
          return true;
        }

        if (!BeeUtils
            .isEmpty(row.getString(form.getDataIndex(CalendarConstants.COL_END_DATE_TIME)))) {
          return true;
        }

        if (BeeUtils
            .isEmpty(row.getString(form.getDataIndex(CalendarConstants.COL_START_DATE_TIME)))) {
          return true;
        }

        if (!DataUtils.isId(BeeUtils.toLongOrNull(event.getNewValue()))) {
          return true;
        }

        appointmentTypeRow = plannedDurations.get(BeeUtils.toLong(event.getNewValue()));

        if (appointmentTypeRow == null) {
          return true;
        }

        DateTime time =
            row.getDateTime(form.getDataIndex(CalendarConstants.COL_START_DATE_TIME));
        DateTime plannedDurationTime = new DateTime(TimeUtils.parseTime(
            appointmentTypeRow.getString(getPlanedDurationColumnIndex())));

        DateTime plannedEndTime = new DateTime(time.getTime() + plannedDurationTime.getTime());

        row.setValue(form.getDataIndex(CalendarConstants.COL_END_DATE_TIME), plannedEndTime);
        form.refreshBySource(CalendarConstants.COL_END_DATE_TIME);

        return true;
      }
    };
  }

  private int getPlanedDurationColumnIndex() {
    return planedDurationColumnIndex;
  }

  private ClickHandler getShowAndHideResultClickHandler(final FormView form, final IsRow row) {
    return new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        showActionResultInput(form, row);
      }
    };
  }

  private ValueChangeHandler<String> getShowAndHideResultValueChangeHandler(final FormView form,
      final IsRow row) {
    return new ValueChangeHandler<String>() {

      @Override
      public void onValueChange(ValueChangeEvent<String> value) {
        Boolean boolValue = BeeUtils.toBooleanOrNull(value.getValue());

        if (BeeUtils.isTrue(boolValue)) {
          showActionResultInput(form, row);
        } else {
          hideActionResultInput(form, row);
        }
      }
    };
  }

  private Handler getStatusValidationHandler(final FormView form, final IsRow row) {
    return new Handler() {

      @Override
      public Boolean validateCell(CellValidateEvent event) {
        String newValue = event.getNewValue();
        AppointmentStatus newStatus = EnumUtils.getEnumByIndex(AppointmentStatus.class, newValue);

        if (newStatus.compareTo(AppointmentStatus.COMPLETED) == 0) {
          showActionResultInput(form, row);
          if (registerResult != null) {
            registerResult.setChecked(true);
          }
        } else {
          hideActionResultInput(form, row);
          if (registerResult != null) {
            registerResult.setChecked(false);
          }
        }
        row.setValue(form.getDataIndex(CalendarConstants.COL_STATUS), newValue);
        form.refreshBySource(CalendarConstants.COL_STATUS);
        return Boolean.TRUE;
      }
    };
  }

  private void hideActionResultInput() {
    hideActionResultInput(null, null);
  }

  private void hideActionResultInput(FormView form, IsRow row) {

    if (form != null && row != null) {
      int idxStatus = form.getDataIndex(CalendarConstants.COL_STATUS);

      if (idxStatus > -1) {

        if (row.getInteger(idxStatus) == AppointmentStatus.COMPLETED.ordinal()) {
          row.setValue(idxStatus, AppointmentStatus.TENTATIVE.ordinal());
          form.refreshBySource(CalendarConstants.COL_STATUS);
        }
      }

      int idxActionResult = form.getDataIndex(CalendarConstants.COL_ACTION_RESULT);

      if (idxActionResult > -1) {
        row.setValue(idxActionResult, (String) null);
        form.refreshBySource(CalendarConstants.COL_ACTION_RESULT);
      }
    }

    if (actionResult != null) {
      if (registerResult == null) {
        StyleUtils.setDisplay(actionResult, Display.NONE);
      } else if (!registerResult.isChecked()) {
        StyleUtils.setDisplay(actionResult, Display.NONE);
      }
    }

    if (actionResultLabel != null) {
      if (registerResult == null) {
        StyleUtils.setDisplay(actionResultLabel, Display.NONE);
      } else if (!registerResult.isChecked()) {
        StyleUtils.setDisplay(actionResultLabel, Display.NONE);
      }
    }

    registerResultActive = false;
  }

  private boolean isValidData(FormView form, IsRow row) {
    boolean valid = true;
    Long startDate = null;
    Long endDate = null;
    int idxStartDate = form.getDataIndex(CalendarConstants.COL_START_DATE_TIME);
    int idxEndDate = form.getDataIndex(CalendarConstants.COL_END_DATE_TIME);

    if (idxStartDate > -1) {
      startDate = row.getLong(idxStartDate);
    }

    if (idxEndDate > -1) {
      endDate = row.getLong(idxEndDate);
    }

    valid &= validateDates(startDate, endDate, form);
    valid &= isValidResult(form, row);
    valid &= isValidResultByState(form, row);
    return valid;
  }

  private boolean isValidResult(FormView form, IsRow row) {
    if (registerResultActive) {
      if (form == null || row == null) {
        return false;
      }

      int idxActionResult = form.getDataIndex(CalendarConstants.COL_ACTION_RESULT);

      if (idxActionResult < 0) {
        form.notifySevere(Localized.getConstants().calActionResult(),
            Localized.getConstants().valueRequired());

        form.focus(CalendarConstants.COL_ACTION_RESULT);
        return false;
      } else if (BeeUtils.isEmpty(row.getString(idxActionResult))) {
        form.notifySevere(Localized.getConstants().calActionResult(),
            Localized.getConstants().valueRequired());

        form.focus(CalendarConstants.COL_ACTION_RESULT);
        return false;
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  private static boolean isValidResultByState(FormView form, IsRow row) {

    if (form == null || row == null) {
      return false;
    }

    int idxStatus = form.getDataIndex(CalendarConstants.COL_STATUS);
    if (idxStatus > -1) {
      int currStatus = BeeUtils.unbox(row.getInteger(idxStatus));
      if (currStatus == AppointmentStatus.COMPLETED.ordinal()) {
        int idxActionResult = form.getDataIndex(CalendarConstants.COL_ACTION_RESULT);
        if (idxActionResult < 0) {
          form.notifySevere(Localized.getConstants().calActionResult(),
              Localized.getConstants().valueRequired());

          form.focus(CalendarConstants.COL_ACTION_RESULT);
          return false;
        } else if (BeeUtils.isEmpty(row.getString(idxActionResult))) {
          form.notifySevere(Localized.getConstants().calActionResult(),
              Localized.getConstants().valueRequired());

          form.focus(CalendarConstants.COL_ACTION_RESULT);
          return false;
        } else {
          return true;
        }
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  private void setPlanedDurationColumnIndex(int index) {
    planedDurationColumnIndex = index;
  }

  private void showActionResultInput() {
    showActionResultInput(null, null);
  }

  private void showActionResultInput(FormView form, IsRow row) {

    if (form != null && row != null) {
      int idxStart = form.getDataIndex(CalendarConstants.COL_START_DATE_TIME);
      int idxEnd = form.getDataIndex(CalendarConstants.COL_END_DATE_TIME);
      int idxStatus = form.getDataIndex(CalendarConstants.COL_STATUS);

      if (idxStart > -1 && idxEnd > -1) {

        if (BeeUtils.isEmpty(row.getString(idxEnd))) {
          row.setValue(idxEnd, row.getValue(idxStart));
          form.refreshBySource(CalendarConstants.COL_END_DATE_TIME);
        }
      }
      if (idxStatus > -1) {
        row.setValue(idxStatus, AppointmentStatus.COMPLETED.ordinal());

        // if (statusWidget != null) {
        // statusWidget.setEnabled(false);
        // }

        form.refreshBySource(CalendarConstants.COL_STATUS);
      }
    }

    if (actionResult != null) {
      StyleUtils.setDisplay(actionResult, Display.INLINE_BLOCK);
    }

    if (actionResultLabel != null) {
      StyleUtils.setDisplay(actionResultLabel, Display.BLOCK);
    }

    registerResultActive = true;
  }

  private static boolean validateDates(Long from, Long to,
      final FormView form) {
    long now = System.currentTimeMillis();

    if (from == null && to == null) {

      if (form != null) {
        form.notifySevere(Localized.getConstants().calEnterPlannedStartTime());
      }
      return false;
    }

    if (from == null && to != null) {
      if (to.longValue() >= now) {
        return true;
      }
    }

    if (from != null && to == null) {
      if (from.longValue() <= now) {
        return true;
      }
    }

    if (from != null && to != null) {
      if (from.longValue() <= to.longValue()) {
        return true;
      } else {
        if (form != null) {
          form.notifySevere(
              Localized.getConstants().crmFinishDateMustBeGreaterThanStart());

        }
        return false;
      }
    }

    return true;
  }

}
