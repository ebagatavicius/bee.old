package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.time.DateTime;

public class CarServiceEvent extends Appointment {

  public CarServiceEvent(IsRow row, Long separatedAttendee) {
    super(row, separatedAttendee);
  }

  @Override
  public boolean handlesCopyAction(DateTime newStart, DateTime newEnd) {
    Queries.getRow(TBL_SERVICE_EVENTS, getEventId(), new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        DataInfo dataInfo = Data.getDataInfo(TBL_SERVICE_EVENTS);
        result.setId(DataUtils.NEW_ROW_ID);
        result.setVersion(DataUtils.NEW_ROW_VERSION);
        result.setValue(dataInfo.getColumnIndex(COL_START_DATE_TIME), newStart);
        result.setValue(dataInfo.getColumnIndex(COL_END_DATE_TIME), newEnd);
        RowFactory.setDefaults(result, dataInfo);
        RowFactory.createRow(dataInfo, result, null);
      }
    });
    return true;
  }

  @Override
  public boolean handlesOpenAction() {
    RowEditor.open(TBL_SERVICE_EVENTS, getEventId(), Opener.MODAL);
    return true;
  }

  public Long getEventId() {
    return Data.getLong(VIEW_APPOINTMENTS, getRow(), COL_SERVICE_EVENT);
  }
}
