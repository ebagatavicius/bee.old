package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class TodoListInterceptor extends AbstractGridInterceptor {

  TodoListInterceptor() {
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    GridView gridView = presenter.getGridView();

    if (gridView != null && !gridView.isReadOnly() && presenter.getHeader() != null) {
      if (presenter.getHeader().hasCommands()) {
        presenter.getHeader().clearCommandPanel();
      }

      if (BeeKeeper.getUser().canCreateData(VIEW_TASKS)) {
        Button taskCommand = new Button(Localized.getConstants().crmTodoCreateTask(),
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                IsRow row = getGridView().getActiveRow();
                if (row != null) {
                  createTask(row);
                }
              }
            });

        presenter.getHeader().addCommandItem(taskCommand);
      }

      if (BeeKeeper.getUser().isModuleVisible(ModuleAndSub.of(Module.CALENDAR))
          && BeeKeeper.getUser().canCreateData(CalendarConstants.VIEW_APPOINTMENTS)) {

        Button appointmentCommand = new Button(Localized.getConstants().crmTodoCreateAppointment(),
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                IsRow row = getGridView().getActiveRow();
                if (row != null) {
                  createAppointment(row);
                }
              }
            });

        presenter.getHeader().addCommandItem(appointmentCommand);
      }
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new TodoListInterceptor();
  }

  private void createAppointment(final IsRow item) {
    Consumer<BeeRow> initializer = new Consumer<BeeRow>() {
      @Override
      public void accept(BeeRow appointment) {
        DataInfo srcInfo = Data.getDataInfo(getViewName());
        DataInfo dstInfo = Data.getDataInfo(CalendarConstants.VIEW_APPOINTMENTS);
        if (srcInfo == null || dstInfo == null) {
          return;
        }

        Map<String, String> colNames = new HashMap<>();

        colNames.put(COL_SUMMARY, CalendarConstants.COL_SUMMARY);
        colNames.put(COL_DESCRIPTION, CalendarConstants.COL_DESCRIPTION);
        colNames.put(COL_START_TIME, CalendarConstants.COL_START_DATE_TIME);
        colNames.put(COL_FINISH_TIME, CalendarConstants.COL_END_DATE_TIME);

        for (Map.Entry<String, String> entry : colNames.entrySet()) {
          int srcIndex = srcInfo.getColumnIndex(entry.getKey());
          String value = BeeConst.isUndef(srcIndex) ? null : item.getString(srcIndex);

          if (!BeeUtils.isEmpty(value)) {
            int dstIndex = dstInfo.getColumnIndex(entry.getValue());
            if (!BeeConst.isUndef(dstIndex)) {
              appointment.setValue(dstIndex, value);
            }
          }
        }

        Long company = item.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_COMPANY));
        if (DataUtils.isId(company)) {
          RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_COMPANY, item,
              dstInfo, ClassifierConstants.COL_COMPANY, appointment);
        }

        Long contact = item.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_CONTACT));
        if (DataUtils.isId(contact)) {
          RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_CONTACT, item,
              dstInfo, ClassifierConstants.COL_COMPANY_PERSON, appointment);
        }
      }
    };

    RowCallback callback = new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        Queries.deleteRowAndFire(getViewName(), item.getId());
      }
    };

    CalendarKeeper.createAppointment(initializer,
        item.getString(getDataIndex(COL_EXPECTED_DURATION)), callback);
  }

  private void createTask(final IsRow item) {
    DataInfo srcInfo = Data.getDataInfo(getViewName());
    DataInfo dstInfo = Data.getDataInfo(VIEW_TASKS);
    if (srcInfo == null || dstInfo == null) {
      return;
    }

    BeeRow task = RowFactory.createEmptyRow(dstInfo, true);

    Set<String> colNames = Sets.newHashSet(COL_SUMMARY, COL_DESCRIPTION,
        COL_START_TIME, COL_FINISH_TIME, COL_EXPECTED_DURATION);
    for (String colName : colNames) {
      int srcIndex = srcInfo.getColumnIndex(colName);
      String value = BeeConst.isUndef(srcIndex) ? null : item.getString(srcIndex);

      if (!BeeUtils.isEmpty(value)) {
        int dstIndex = dstInfo.getColumnIndex(colName);
        if (!BeeConst.isUndef(dstIndex)) {
          task.setValue(dstIndex, value);
        }
      }
    }

    Long company = item.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_COMPANY));
    if (DataUtils.isId(company)) {
      RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_COMPANY, item,
          dstInfo, ClassifierConstants.COL_COMPANY, task);
    }

    Long contact = item.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_CONTACT));
    if (DataUtils.isId(contact)) {
      RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_CONTACT, item,
          dstInfo, ClassifierConstants.COL_CONTACT, task);
    }

    RowFactory.createRow(dstInfo, task, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        Queries.deleteRowAndFire(getViewName(), item.getId());
      }
    });
  }
}
