package com.butent.bee.client.modules.calendar;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.HasDataProvider;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

class SelectorHandler implements SelectorEvent.Handler {

  private boolean companyHandlerEnabled;
  private boolean vehicleHandlerEnabled;

  SelectorHandler() {
    super();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (BeeUtils.same(event.getRelatedViewName(), VIEW_EXTENDED_PROPERTIES)) {
      handleExtendedProperties(event);

    } else if (BeeUtils.same(event.getRelatedViewName(), ClassifierConstants.VIEW_COMPANIES)) {
      if (isCompanyHandlerEnabled()) {
        handleCompany(event);
      }

    } else if (BeeUtils.same(event.getRelatedViewName(), TransportConstants.VIEW_VEHICLES)) {
      if (isVehicleHandlerEnabled()) {
        handleVehicle(event);
      }
    }
  }

  boolean isCompanyHandlerEnabled() {
    return companyHandlerEnabled;
  }

  boolean isVehicleHandlerEnabled() {
    return vehicleHandlerEnabled;
  }

  void setCompanyHandlerEnabled(boolean companyHandlerEnabled) {
    this.companyHandlerEnabled = companyHandlerEnabled;
  }

  void setVehicleHandlerEnabled(boolean vehicleHandlerEnabled) {
    this.vehicleHandlerEnabled = vehicleHandlerEnabled;
  }

  private static void chooseVehicle(final DataView dataView, final BeeRowSet rowSet,
      String companyName, final IsRow owner) {

    List<String> options = new ArrayList<>();

    int numberIndex = rowSet.getColumnIndex(TransportConstants.COL_NUMBER);
    int brandIndex = rowSet.getColumnIndex(COL_VEHICLE_BRAND_NAME);
    int modelIndex = rowSet.getColumnIndex(TransportConstants.COL_MODEL_NAME);

    for (IsRow row : rowSet.getRows()) {
      options.add(BeeUtils.joinWords(row.getString(numberIndex),
          row.getString(brandIndex), row.getString(modelIndex)));
    }
    options.add(Localized.dictionary().actionNew1());

    Global.choice(Localized.dictionary().calSelectVehicle(), companyName, options,
        new ChoiceCallback() {
          @Override
          public void onSuccess(int value) {
            if (value < rowSet.getNumberOfRows()) {
              RelationUtils.updateRow(CalendarKeeper.getAppointmentViewInfo(), COL_VEHICLE,
                  dataView.getActiveRow(), Data.getDataInfo(TransportConstants.VIEW_VEHICLES),
                  rowSet.getRow(value), true);
              dataView.refresh(false, true);
            } else {
              createVehicle(owner, dataView);
            }
          }
        });
  }

  private static void createVehicle(IsRow owner, final DataView dataView) {
    final DataInfo vehiclesInfo = Data.getDataInfo(TransportConstants.VIEW_VEHICLES);
    BeeRow row = RowFactory.createEmptyRow(vehiclesInfo, true);

    if (owner != null) {
      RelationUtils.updateRow(vehiclesInfo, TransportConstants.COL_OWNER, row,
          Data.getDataInfo(ClassifierConstants.VIEW_COMPANIES), owner, true);
    }

    RowFactory.createRow(TransportConstants.FORM_NEW_VEHICLE,
        Localized.dictionary().trNewVehicle(), vehiclesInfo, row, Modality.ENABLED,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RelationUtils.updateRow(CalendarKeeper.getAppointmentViewInfo(), COL_VEHICLE,
                dataView.getActiveRow(), vehiclesInfo, result, true);
            dataView.refresh(false, true);
          }
        });
  }

  private static int getCompanyIndex() {
    return Data.getColumnIndex(VIEW_APPOINTMENTS, COL_COMPANY);
  }

  private static int getCompanyNameIndex() {
    return Data.getColumnIndex(VIEW_APPOINTMENTS, ALS_COMPANY_NAME);
  }

  private static void getCompanyRow(Long company, final RowCallback callback) {
    Queries.getRow(ClassifierConstants.VIEW_COMPANIES, company, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        callback.onSuccess(result);
      }
    });
  }

  private static int getVehicleOwnerIndex() {
    return Data.getColumnIndex(VIEW_APPOINTMENTS, COL_VEHICLE_OWNER);
  }

  private static void handleCompany(final SelectorEvent event) {
    if (!event.isChanged()) {
      return;
    }
    long companyId = event.getValue();
    if (!DataUtils.isId(companyId)) {
      return;
    }

    final DataView dataView = ViewHelper.getDataView(event.getSelector());
    if (dataView == null) {
      return;
    }
    if (!BeeUtils.same(dataView.getViewName(), VIEW_APPOINTMENTS)) {
      return;
    }

    IsRow row = dataView.getActiveRow();
    if (row == null) {
      return;
    }

    Long ownerId = row.getLong(getVehicleOwnerIndex());
    if (ownerId != null && ownerId == companyId) {
      return;
    }

    Queries.getRowSet(TransportConstants.VIEW_VEHICLES, null,
        Filter.equals(TransportConstants.COL_OWNER, companyId), new Queries.RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet result) {
            int rowCount = result.getNumberOfRows();
            String companyName = dataView.getActiveRow().getString(getCompanyNameIndex());

            if (rowCount <= 0) {
              createVehicle(event.getRelatedRow(), dataView);
            } else if (rowCount == 1) {
              RelationUtils.updateRow(CalendarKeeper.getAppointmentViewInfo(), COL_VEHICLE,
                  dataView.getActiveRow(), Data.getDataInfo(TransportConstants.VIEW_VEHICLES),
                  result.getRow(0), true);
              dataView.refresh(false, false);
            } else {
              chooseVehicle(dataView, result, companyName, event.getRelatedRow());
            }
          }
        });
  }

  private static void handleExtendedProperties(SelectorEvent event) {
    if (!event.isOpened()) {
      return;
    }

    GridView gridView = ViewHelper.getGrid(event.getSelector());
    if (gridView == null) {
      return;
    }
    IsRow row = gridView.getActiveRow();
    if (row == null) {
      return;
    }

    long id = row.getId();
    Filter filter = null;

    if (BeeUtils.same(gridView.getViewName(), VIEW_PROPERTY_GROUPS)) {
      if (DataUtils.isId(id)) {
        filter = Filter.equals(COL_PROPERTY_GROUP, id);
      } else {
        filter = Filter.isNull(COL_PROPERTY_GROUP);
      }

    } else if (BeeUtils.same(gridView.getViewName(), VIEW_ATTENDEE_PROPS)) {
      if (gridView.getViewPresenter() instanceof HasDataProvider) {
        Provider provider = ((HasDataProvider) gridView.getViewPresenter()).getDataProvider();

        if (provider != null) {
          int index = provider.getColumnIndex(COL_ATTENDEE_PROPERTY);
          Long exclude = DataUtils.isId(id) ? row.getLong(index) : null;
          List<Long> used = DataUtils.getDistinct(gridView.getRowData(), index, exclude);

          if (used.isEmpty()) {
            filter = provider.getImmutableFilter();
          } else {
            CompoundFilter and = Filter.and();
            and.add(provider.getImmutableFilter());

            for (Long value : used) {
              and.add(Filter.compareId(Operator.NE, value));
            }
            filter = and;
          }
        }
      }
    }

    event.getSelector().setAdditionalFilter(filter);
  }

  private static void handleVehicle(final SelectorEvent event) {
    final DataView dataView = ViewHelper.getDataView(event.getSelector());
    if (dataView == null) {
      return;
    }
    if (!BeeUtils.same(dataView.getViewName(), VIEW_APPOINTMENTS)) {
      return;
    }

    final IsRow row = dataView.getActiveRow();
    if (row == null) {
      return;
    }
    Long company = row.getLong(getCompanyIndex());

    if (event.isOpened()) {
      if (DataUtils.isId(company)) {
        Filter filter = Filter.equals(TransportConstants.COL_OWNER, company);
        event.getSelector().setAdditionalFilter(filter);
      }

    } else if (event.isChanged()) {
      Long owner = Data.getLong(event.getRelatedViewName(), event.getRelatedRow(),
          TransportConstants.COL_OWNER);

      if (DataUtils.isId(owner) && !owner.equals(company)) {
        getCompanyRow(owner, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RelationUtils.updateRow(CalendarKeeper.getAppointmentViewInfo(), COL_COMPANY, row,
                Data.getDataInfo(ClassifierConstants.VIEW_COMPANIES), result, true);
            dataView.refresh(false, false);
          }
        });
      }

    } else if (event.isNewRow()) {
      if (DataUtils.isId(company)) {
        Data.setValue(event.getRelatedViewName(), event.getNewRow(),
            TransportConstants.COL_OWNER, company);
        Data.setValue(event.getRelatedViewName(), event.getNewRow(),
            TransportConstants.COL_OWNER_NAME, row.getString(getCompanyNameIndex()));
      }
    }
  }
}
