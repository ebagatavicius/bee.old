package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.ClientDefaults;
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
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
          ServiceUtils.getStateFilter(stateId, maintenanceTypeId), stateProcessRowSet -> {

            if (!DataUtils.isEmpty(stateProcessRowSet)) {
              IsRow stateProcessRow = stateProcessRowSet.getRow(0);
              boolean comment = stateProcessRow.isTrue(stateProcessRowSet
                  .getColumnIndex(COL_COMMENT));

              boolean isFinalState = stateProcessRow.isTrue(Data
                  .getColumnIndex(TBL_STATE_PROCESS, COL_FINITE));
              boolean isItemsRequired = stateProcessRow.isTrue(Data
                  .getColumnIndex(TBL_STATE_PROCESS, COL_IS_ITEMS_REQUIRED));

              Consumer<String> changeStateConsumer = canChangeStateErrorMsg -> {
                if (!BeeUtils.isEmpty(canChangeStateErrorMsg)) {
                  getFormView().notifySevere(canChangeStateErrorMsg);
                  revertState();

                } else if (comment
                    || !BeeUtils.isEmpty(stateProcessRow.getString(Data
                    .getColumnIndex(TBL_STATE_PROCESS, COL_MAINTENANCE_ITEM)))
                    || !BeeUtils.isEmpty(stateProcessRow.getString(Data
                    .getColumnIndex(TBL_STATE_PROCESS, COL_TERM)))
                    || !BeeUtils.isEmpty(stateProcessRow.getString(Data
                    .getColumnIndex(TBL_STATE_PROCESS, COL_WARRANTY)))) {
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

                      int endingDateIndex = getDataIndex(COL_ENDING_DATE);
                      columns.add(COL_ENDING_DATE);
                      oldValues.add(oldRow.getString(endingDateIndex));

                      if (isFinalState) {
                        newValues.add(BeeUtils.toString(System.currentTimeMillis()));
                      } else {
                        newValues.add(null);
                      }

                      Queries.update(getViewName(), oldRow.getId(), oldRow.getVersion(),
                          Data.getColumns(getViewName(), columns), oldValues, newValues, null,
                          maintenanceRow -> RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(),
                              maintenanceRow));

                    } else {
                      revertState();
                    }
                  });
                }
              };
              ServiceUtils.checkCanChangeState(isFinalState, isItemsRequired, changeStateConsumer,
                  getFormView());
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
              maintenanceValues.put(COL_MAINTENANCE_URGENT,
                  result.getProperty(COL_MAINTENANCE_URGENT));
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
      String currency = commentRow.getString(Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS,
          ClassifierConstants.COL_ITEM_CURRENCY));
      List<String> columns = Lists.newArrayList(COL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT,
          COL_REPAIRER, COL_MAINTENANCE_ITEM, "Quantity",
          ClassifierConstants.COL_ITEM_PRICE, ClassifierConstants.COL_ITEM_CURRENCY);

      List<String> values = Lists.newArrayList(BeeUtils.toString(serviceMaintenanceRow.getId()),
          serviceMaintenanceRow.getString(Data.getColumnIndex(TBL_SERVICE_MAINTENANCE,
              COL_SERVICE_OBJECT)),
          BeeUtils.toString(BeeKeeper.getUser().getUserData().getCompanyPerson()),
          BeeUtils.toString(itemId), DEFAULT_QUANTITY,
          BeeUtils.toString(ServiceUtils.calculateServicePrice(commentRow, serviceMaintenanceRow)),
          BeeUtils.isEmpty(currency) ? BeeUtils.toString(ClientDefaults.getCurrency()) : currency);

      Queries.getRowSet(ClassifierConstants.TBL_ITEMS,
          Arrays.asList(ClassifierConstants.COL_ITEM_VAT, ClassifierConstants.COL_ITEM_VAT_PERCENT),
          Filter.compareId(itemId), result -> {
            if (!result.isEmpty()) {
              columns.add(ClassifierConstants.COL_ITEM_VAT);
              values.add(result.getRow(0).getString(result
                  .getColumnIndex(ClassifierConstants.COL_ITEM_VAT_PERCENT)));
              columns.add(ClassifierConstants.COL_ITEM_VAT_PERCENT);
              values.add(result.getRow(0).getString(result
                  .getColumnIndex(ClassifierConstants.COL_ITEM_VAT)));
            }

            Queries.insert(TBL_SERVICE_ITEMS, Data.getColumns(TBL_SERVICE_ITEMS, columns),
                values, null, createdServiceItem ->
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), TBL_SERVICE_ITEMS));
          });
    }
  }

  private void revertState() {
    int stateIdIndex = getDataIndex(AdministrationConstants.COL_STATE);
    int stateNameIndex = getDataIndex(ALS_STATE_NAME);

    getActiveRow().setValue(stateIdIndex,
        getFormView().getOldRow().getString(stateIdIndex));
    getActiveRow().setValue(stateNameIndex,
        getFormView().getOldRow().getString(stateNameIndex));
    getFormView().refreshBySource(AdministrationConstants.COL_STATE);
  }
}