package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.COL_STAGE_START_DATE;

import com.butent.bee.client.Global;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public class ProjectDateForm extends AbstractFormInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {
    if (widget instanceof InputDateTime
        && BeeUtils.same(COL_STAGE_START_DATE, name)) {

      ((InputDateTime) widget).addEditStopHandler(event -> {
        if (event.isChanged()) {
          InputDateTime dateWidget = (InputDateTime) getWidgetByName(COL_STAGE_START_DATE);
          setTimeToDateTimeInput(Global.getParameterTime(TimeUtils.isToday(dateWidget.getDateTime())
              ? TaskConstants.PRM_END_OF_WORK_DAY : TaskConstants.PRM_START_OF_WORK_DAY));
        }
      });
    }
  }

  private void setTimeToDateTimeInput(Long time) {
    if (Objects.nonNull(time)) {
      InputDateTime dateWidget = (InputDateTime) getWidgetByName(COL_STAGE_START_DATE);
      DateTime dateTime = TimeUtils.toDateTimeOrNull(time);
      if (dateTime != null) {
        int hour = dateTime.getUtcHour();
        int minute = dateTime.getUtcMinute();
        DateTime value = dateWidget.getDateTime();
        value.setHour(hour);
        value.setMinute(minute);
        dateWidget.setDateTime(value);
      }
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new ProjectDateForm();
  }
}
