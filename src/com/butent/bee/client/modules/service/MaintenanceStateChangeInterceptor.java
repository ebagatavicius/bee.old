package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.*;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class MaintenanceStateChangeInterceptor extends MaintenanceExpanderForm {

  private static final String DEFAULT_QUANTITY = "1";

  @Override
  public void onEditEnd(EditEndEvent event, Object source) {
    super.onEditEnd(event, source);

    if (!DataUtils.isNewRow(event.getRowValue())
        && BeeUtils.same(event.getColumnId(), AdministrationConstants.COL_STATE)
        && !BeeUtils.same(event.getNewValue(), event.getOldValue())) {

      Long stateId = BeeUtils.toLong(event.getNewValue());
      Long maintenanceTypeId = event.getRowValue().getLong(getDataIndex(COL_TYPE));

      Queries.getRowSet(TBL_STATE_PROCESS, null,
          ServiceUtils.getStateFilter(stateId, maintenanceTypeId), new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet stateProcessRowSet) {

          if (!DataUtils.isEmpty(stateProcessRowSet)) {
            IsRow stateProcessRow = stateProcessRowSet.getRow(0);
            Boolean comment = stateProcessRow.getBoolean(stateProcessRowSet
                .getColumnIndex(COL_COMMENT));

            if (BeeUtils.isTrue(comment)) {
              registerReason(getActiveRow(), stateProcessRow, result -> {

                if (result == null) {
                  return;
                }

                if (result.getA()) {
                  List<String> columns = Lists.newArrayList(event.getColumnId());
                  List<String> oldValues = Lists.newArrayList(event.getOldValue());
                  List<String> newValues = Lists.newArrayList(event.getNewValue());
                  IsRow oldRow = getFormView().getOldRow();

                  Map<String, String> maintenanceValues = result.getB();
                  maintenanceValues.forEach((column, value) -> {
                    if (!BeeUtils.isEmpty(value)) {
                      columns.add(column);
                      int columnIndex = getDataIndex(column);
                      oldValues.add(oldRow.getString(columnIndex));
                      newValues.add(value);
                    }
                  }
                  );

                  Boolean isFinalState = stateProcessRow.getBoolean(Data
                      .getColumnIndex(TBL_STATE_PROCESS, COL_FINITE));
                  int endingDateIndex = getDataIndex(COL_ENDING_DATE);
                  columns.add(COL_ENDING_DATE);
                  oldValues.add(oldRow.getString(endingDateIndex));

                  if (BeeUtils.isTrue(isFinalState)) {
                    newValues.add(BeeUtils.toString(System.currentTimeMillis()));
                  } else {
                    newValues.add(null);
                  }

                  Queries.update(getViewName(), oldRow.getId(), oldRow.getVersion(),
                      Data.getColumns(getViewName(), columns), oldValues, newValues,
                      null, new RowCallback() {

                        @Override
                        public void onSuccess(BeeRow maintenanceRow) {
                          RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), maintenanceRow);
                        }
                      });

                } else {
                  int stateIdIndex = getDataIndex(AdministrationConstants.COL_STATE);
                  int stateNameIndex = getDataIndex(ALS_STATE_NAME);

                  getActiveRow().setValue(stateIdIndex,
                      getFormView().getOldRow().getString(stateIdIndex));
                  getActiveRow().setValue(stateNameIndex,
                      getFormView().getOldRow().getString(stateNameIndex));
                  getFormView().refreshBySource(AdministrationConstants.COL_STATE);
                }
              });
            }
          }
        }
      });
    }
  }

  private static void registerReason(IsRow maintenanceRow, IsRow stateProcessRow,
      final Callback<Pair<Boolean, Map<String, String>>> success) {
    DataInfo data = Data.getDataInfo(TBL_MAINTENANCE_COMMENTS);
    BeeRow emptyRow = RowFactory.createEmptyRow(data, false);

    RowFactory.createRow(FORM_MAINTENANCE_STATE_COMMENT, null, data, emptyRow,
        Modality.ENABLED, null,
        new MaintenanceCommentForm(maintenanceRow, stateProcessRow), null, new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            if (success != null) {
              createMaintenanceItem(maintenanceRow, result);

              Map<String, String> maintenanceValues = new HashMap<>();
              maintenanceValues.put(COL_WARRANTY_VALID_TO,
                  result.getProperty(COL_WARRANTY_VALID_TO));
              maintenanceValues.put(COL_WARRANTY_TYPE, result.getProperty(COL_WARRANTY_TYPE));
              success.onSuccess(Pair.of(true, maintenanceValues));
            }
          }

          @Override
          public void onCancel() {
            if (success != null) {
              success.onSuccess(Pair.of(false, null));
            }
          }

          @Override
          public void onFailure(String... reason) {
            if (success != null) {
              success.onFailure(reason);
              success.onSuccess(Pair.of(false, null));
            }
          }
        });
  }

  private static void createMaintenanceItem(IsRow serviceMaintenanceRow, BeeRow commentRow) {
    Long itemId = commentRow.getLong(Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS,
        COL_MAINTENANCE_ITEM));

    if (DataUtils.isId(itemId)) {
      List<String> columns = Lists.newArrayList(COL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT,
          COL_MAINTENANCE_DATE, COL_MAINTENANCE_ITEM, "Quantity",
          ClassifierConstants.COL_ITEM_PRICE, ClassifierConstants.COL_ITEM_CURRENCY);

      DateTime currentTime = new DateTime(System.currentTimeMillis());
      currentTime.setSecond(0);
      currentTime.setMillis(0);

      List<String> values = Lists.newArrayList(BeeUtils.toString(serviceMaintenanceRow.getId()),
          serviceMaintenanceRow.getString(Data.getColumnIndex(TBL_SERVICE_MAINTENANCE,
              COL_SERVICE_OBJECT)),
          BeeUtils.toString(currentTime.getTime()),
          BeeUtils.toString(itemId), DEFAULT_QUANTITY,
          commentRow.getString(Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS,
              ClassifierConstants.COL_ITEM_PRICE)),
          commentRow.getString(Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS,
              ClassifierConstants.COL_ITEM_CURRENCY)));

      Queries.insert(VIEW_MAINTENANCE, Data.getColumns(VIEW_MAINTENANCE, columns), values, null,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_MAINTENANCE);
            }
          });
    }
  }
}