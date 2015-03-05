package com.butent.bee.client.modules.calendar.dnd;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.DropEvent;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndSource;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.calendar.CalendarPanel;
import com.butent.bee.client.modules.calendar.CalendarView;
import com.butent.bee.client.modules.calendar.CalendarWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TodoMoveController {

  private static final String CONTENT_TYPE = "CalendarTodo";

  public TodoMoveController(final DndSource todoContainer, final CalendarPanel calendarPanel) {

    todoContainer.addDragStartHandler(new DragStartHandler() {
      @Override
      public void onDragStart(DragStartEvent event) {
        Element cell = EventUtils.getEventTargetElement(event);
        String rowIndex = DomUtils.getDataRow(cell);

        if (CellGrid.isBodyRow(rowIndex)) {
          EventUtils.allowMove(event);
          EventUtils.setDndData(event, rowIndex);

          DndHelper.fillContent(CONTENT_TYPE, null, null, rowIndex);
        }
      }
    });

    todoContainer.addDragEndHandler(new DragEndHandler() {
      @Override
      public void onDragEnd(DragEndEvent event) {
        DndHelper.reset();
      }
    });

    DndHelper.makeTarget(calendarPanel, Collections.singleton(CONTENT_TYPE), null,
        DndHelper.ALWAYS_TARGET,
        new BiConsumer<DropEvent, Object>() {
          @Override
          public void accept(DropEvent t, Object u) {
            GridView grid = ViewHelper.getChildGrid(todoContainer.asWidget(), GRID_CALENDAR_TODO);
            Integer rowIndex = (u instanceof String) ? BeeUtils.toIntOrNull((String) u) : null;

            Element target = EventUtils.getEventTargetElement(t);
            CalendarView calendarView = calendarPanel.getCalendarView();

            if (grid != null && BeeUtils.isNonNegative(rowIndex)
                && target != null && calendarView != null) {

              IsRow row = BeeUtils.getQuietly(grid.getRowData(), rowIndex);
              CalendarWidget container = calendarView.getCalendarWidget();

              if (row != null && container != null && container.getElement().isOrHasChild(target)) {
                int x = t.getNativeEvent().getClientX();
                int y = t.getNativeEvent().getClientY();

                Pair<DateTime, Long> pair = calendarView.resolveCoordinates(x, y);
                if (pair != null) {
                  onDrop(grid.getViewName(), row, calendarPanel.getCalendarId(),
                      pair.getA(), pair.getB());
                }
              }
            }
          }
        });
  }

  private static void onDrop(final String srcViewName, final IsRow srcRow,
      Long calendarId, final DateTime dt, Long attendee) {

    final DataInfo srcInfo = Data.getDataInfo(srcViewName);
    final String duration =
        srcRow.getString(srcInfo.getColumnIndex(TaskConstants.COL_EXPECTED_DURATION));

    Consumer<BeeRow> initializer = new Consumer<BeeRow>() {
      @Override
      public void accept(BeeRow appointment) {
        DataInfo dstInfo = Data.getDataInfo(VIEW_APPOINTMENTS);

        Map<String, String> colNames = new HashMap<>();

        colNames.put(TaskConstants.COL_SUMMARY, COL_SUMMARY);
        colNames.put(TaskConstants.COL_DESCRIPTION, COL_DESCRIPTION);

        if (dt == null) {
          colNames.put(TaskConstants.COL_START_TIME, COL_START_DATE_TIME);
        }
        if (dt == null || BeeUtils.isEmpty(duration)) {
          colNames.put(TaskConstants.COL_FINISH_TIME, COL_END_DATE_TIME);
        }

        for (Map.Entry<String, String> entry : colNames.entrySet()) {
          int srcIndex = srcInfo.getColumnIndex(entry.getKey());
          String value = BeeConst.isUndef(srcIndex) ? null : srcRow.getString(srcIndex);

          if (!BeeUtils.isEmpty(value)) {
            int dstIndex = dstInfo.getColumnIndex(entry.getValue());
            if (!BeeConst.isUndef(dstIndex)) {
              appointment.setValue(dstIndex, value);
            }
          }
        }

        Long company = srcRow.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_COMPANY));
        if (DataUtils.isId(company)) {
          RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_COMPANY, srcRow,
              dstInfo, ClassifierConstants.COL_COMPANY, appointment);
        }

        Long contact = srcRow.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_CONTACT));
        if (DataUtils.isId(contact)) {
          RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_CONTACT, srcRow,
              dstInfo, ClassifierConstants.COL_COMPANY_PERSON, appointment);
        }
      }
    };

    RowCallback callback = new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        Queries.deleteRowAndFire(srcViewName, srcRow.getId());
      }
    };

    CalendarKeeper.createAppointment(calendarId, dt, duration, attendee, initializer, callback);
  }
}
