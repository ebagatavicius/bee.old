package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;

import java.util.List;

final class DefectBuilder {

  private static final String STYLE_PREFIX = ServiceKeeper.STYLE_PREFIX + "defect-builder-";

  static void start(IdentifiableWidget sourceWidget) {
    if (sourceWidget == null) {
      return;
    }

    final FormView form = UiHelper.getForm(sourceWidget.asWidget());
    if (form == null || !form.isEnabled() || !VIEW_SERVICE_OBJECTS.equals(form.getViewName())) {
      return;
    }

    final long objId = form.getActiveRowId();
    if (!DataUtils.isId(objId)) {
      return;
    }

    Filter filter = Filter.and(Filter.equals(COL_SERVICE_OBJECT, objId),
        Filter.isNull(COL_MAINTENANCE_DEFECT));

    Queries.getRowSet(VIEW_MAINTENANCE, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          form.notifyInfo(Localized.getConstants().noData());

        } else {
          ServiceHelper.selectMaintenanceItems(objId, result,
              Localized.getConstants().svcNewDefect(), Localized.getConstants().actionCreate(),
              STYLE_PREFIX, new BiConsumer<Long, BeeRowSet>() {
                @Override
                public void accept(Long t, BeeRowSet u) {
                  buildHeader(t, u);
                }
              });
        }
      }
    });
  }

  private static void buildHeader(long objId, final BeeRowSet items) {
    Queries.getRow(VIEW_SERVICE_OBJECTS, objId, new RowCallback() {
      @Override
      public void onSuccess(BeeRow objRow) {
        DataInfo dfInfo = Data.getDataInfo(VIEW_SERVICE_DEFECTS);
        BeeRow dfRow = RowFactory.createEmptyRow(dfInfo, true);
        
        dfRow.setValue(dfInfo.getColumnIndex(COL_SERVICE_OBJECT), objRow.getId());

        Long customer = Data.getLong(VIEW_SERVICE_OBJECTS, objRow, COL_SERVICE_CUSTOMER);
        if (DataUtils.isId(customer)) {
          dfRow.setValue(dfInfo.getColumnIndex(COL_SERVICE_CUSTOMER), customer);
          dfRow.setValue(dfInfo.getColumnIndex(ALS_SERVICE_CUSTOMER_NAME),
              Data.getString(VIEW_SERVICE_OBJECTS, objRow, ALS_SERVICE_CUSTOMER_NAME));
        }

        UserData userData = BeeKeeper.getUser().getUserData();
        if (userData != null) {
          dfRow.setValue(dfInfo.getColumnIndex(COL_DEFECT_MANAGER), userData.getUserId());
          RelationUtils.setUserFields(dfInfo, dfRow, COL_DEFECT_MANAGER, userData);

          dfRow.setValue(dfInfo.getColumnIndex(COL_DEFECT_SUPPLIER), userData.getCompany());
          dfRow.setValue(dfInfo.getColumnIndex(ALS_DEFECT_SUPPLIER_NAME),
              userData.getCompanyName());
        }

        for (BeeRow row : items) {
          Long currency = row.getLong(items.getColumnIndex(AdministrationConstants.COL_CURRENCY));

          if (DataUtils.isId(currency)) {
            dfRow.setValue(dfInfo.getColumnIndex(AdministrationConstants.COL_CURRENCY), currency);
            dfRow.setValue(dfInfo.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME),
                row.getString(items.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME)));
            break;
          }
        }

        RowFactory.createRow("NewServiceDefect", null, dfInfo, dfRow, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            ParameterList params = ServiceKeeper.createArgs(SVC_CREATE_DEFECT_ITEMS);

            final long dfId = result.getId();
            params.addQueryItem(COL_DEFECT, dfId);

            Long currency = Data.getLong(VIEW_SERVICE_DEFECTS, result,
                AdministrationConstants.COL_CURRENCY);
            if (DataUtils.isId(currency)) {
              params.addQueryItem(AdministrationConstants.COL_CURRENCY, currency);
            }

            List<Long> ids = Lists.newArrayList();
            for (IsRow item : items) {
              ids.add(item.getId());
            }

            params.addDataItem(VIEW_MAINTENANCE, DataUtils.buildIdList(ids));

            BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                response.notify(BeeKeeper.getScreen());

                if (!response.hasErrors()) {
                  DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_MAINTENANCE);
                  DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_SERVICE_OBJECTS);

                  RowEditor.open(VIEW_SERVICE_DEFECTS, dfId, Opener.MODAL);
                }
              }
            });
          }
        });
      }
    });
  }

  private DefectBuilder() {
  }
}
