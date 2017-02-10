package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.VIEW_PROJECTS;

import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.calendar.CalendarConstants;

public class ProjectAppointmentsGrid extends AbstractGridInterceptor {

  private IsRow projectRow;

  @Override
  public GridInterceptor getInstance() {
    return new ProjectAppointmentsGrid();
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    event.consume();

    CalendarKeeper.openAppointment(Appointment.create(event.getRowValue()), null,
        CalendarConstants.DEFAULT_EDIT_APPOINTMENT_FORM);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    if (DataUtils.isNewRow(projectRow) && getGridView() != null) {
      getGridView().ensureRelId(result -> {
        FormView parentForm = ViewHelper.getForm(getGridView().asWidget());
        if (parentForm != null && parentForm.getActiveRow() != null) {
          CalendarKeeper.createAppointment(null, null, null,
              null, null, null, parentForm.getActiveRow(),
              CalendarConstants.DEFAULT_EDIT_APPOINTMENT_FORM);
        }
      });
    } else {
      CalendarKeeper.createAppointment(null, null, null,
          null, null, null, projectRow, CalendarConstants.DEFAULT_NEW_APPOINTMENT_FORM);
    }

    return false;
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (event.getRow() != null && event.getViewName().equals(VIEW_PROJECTS)) {
      projectRow = event.getRow();
    }
  }
}