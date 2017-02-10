package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.ALS_COMPANY_TYPE_NAME;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Map;

public final class ServiceUtils {

  private static DataInfo maintenanceDataInfo = Data.getDataInfo(TBL_SERVICE_MAINTENANCE);
  private static DataInfo objectDataInfo = Data.getDataInfo(VIEW_SERVICE_OBJECTS);

  public static void fillContactValues(IsRow maintenanceRow, IsRow objectRow) {
    if (objectRow != null) {
      Long contactPerson = objectRow.getLong(objectDataInfo.getColumnIndex(ALS_CONTACT_PERSON));

      if (DataUtils.isId(contactPerson)) {
        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(COL_CONTACT), contactPerson);

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_PHONE),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_PHONE)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_FIRST_NAME),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_FIRST_NAME)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_LAST_NAME),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_LAST_NAME)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_EMAIL),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_EMAIL)));

        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_ADDRESS),
            objectRow.getString(objectDataInfo.getColumnIndex(ALS_CONTACT_ADDRESS)));

        return;
      }
    }
    clearContactValue(maintenanceRow);
  }

  public static void clearContactValue(IsRow maintenanceRow) {
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(COL_CONTACT));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_PHONE));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_FIRST_NAME));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_LAST_NAME));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_EMAIL));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_CONTACT_ADDRESS));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(COL_COMPANY));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_COMPANY_NAME));
    maintenanceRow.clearCell(maintenanceDataInfo.getColumnIndex(ALS_COMPANY_TYPE_NAME));
  }

  public static void fillCompanyColumns(IsRow maintenanceRow, IsRow dataRow, String dataViewName,
      String companyColumnAlias, String companyTypeColumnAlias) {
    maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_COMPANY_NAME),
        dataRow.getString(Data.getColumnIndex(dataViewName, companyColumnAlias)));
    maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_COMPANY_TYPE_NAME),
        dataRow.getString(Data.getColumnIndex(dataViewName, companyTypeColumnAlias)));
  }

  public static void fillCompanyValues(IsRow maintenanceRow, IsRow dataRow, String dataViewName,
      String companyColumnName, String companyColumnAlias, String companyTypeColumnAlias) {
    if (dataRow != null) {
      Long company = dataRow.getLong(Data.getColumnIndex(dataViewName, companyColumnName));

      if (DataUtils.isId(company)) {
        maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(COL_COMPANY), company);
        fillCompanyColumns(maintenanceRow, dataRow, dataViewName, companyColumnAlias,
            companyTypeColumnAlias);
        return;
      }
    }
    clearContactValue(maintenanceRow);
  }

  public static void fillContractorAndManufacturerValues(IsRow maintenanceRow, IsRow objectRow) {
    maintenanceRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_MANUFACTURER_NAME),
        objectRow.getString(objectDataInfo.getColumnIndex(ALS_MANUFACTURER_NAME)));

    maintenanceRow.setValue(
        maintenanceDataInfo.getColumnIndex(ALS_SERVICE_CONTRACTOR_NAME),
        objectRow.getString(objectDataInfo.getColumnIndex(ALS_SERVICE_CONTRACTOR_NAME)));
  }

  public static Filter getStateFilter(Long stateId, Long maintenanceTypeId) {
    return getStateFilter(stateId, maintenanceTypeId, null);
  }

  public static Filter getStateFilter(Long stateId, Long typeId, Filter stateFilter) {
    Filter roleFilter = Filter.in(AdministrationConstants.COL_ROLE,
        AdministrationConstants.VIEW_USER_ROLES, AdministrationConstants.COL_ROLE,
        Filter.equals(AdministrationConstants.COL_USER, BeeKeeper.getUser().getUserId()));

    return Filter.and(
        DataUtils.isId(stateId) ? Filter.equals(COL_MAINTENANCE_STATE, stateId) : null,
        DataUtils.isId(typeId) ? Filter.equals(COL_MAINTENANCE_TYPE, typeId) : null,
        roleFilter, stateFilter);
  }

  private ServiceUtils() {
  }

  public static void informClient(BeeRow commentRow) {
    int informColIndex = Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS, COL_CUSTOMER_SENT);

    if (BeeUtils.isTrue(commentRow.getBoolean(informColIndex))) {
      ParameterList params = ServiceKeeper.createArgs(SVC_INFORM_CUSTOMER);
      params.addDataItem(COL_COMMENT, commentRow.getId());

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasErrors()) {
            Global.showError(Arrays.asList(response.getErrors()));
          }

          if (!response.isEmpty()) {
            for (Map.Entry<String, String> entry : Codec.deserializeHashMap(response
                .getResponseAsString()).entrySet()) {
              commentRow.setValue(Data.getColumnIndex(TBL_MAINTENANCE_COMMENTS, entry.getKey()),
                  entry.getValue());
            }

            RowUpdateEvent.fire(BeeKeeper.getBus(), TBL_MAINTENANCE_COMMENTS, commentRow);
          }
        }
      });
    }
  }
}