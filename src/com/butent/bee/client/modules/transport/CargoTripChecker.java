package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Set;

public class CargoTripChecker extends AbstractGridInterceptor {

  @Override
  public DeleteMode getDeleteMode(final GridPresenter presenter, final IsRow activeRow,
      final Collection<RowInfo> selectedRows, final DeleteMode defMode) {

    ParameterList args = TransportHandler.createArgs(TransportConstants.SVC_GET_CARGO_USAGE);
    args.addDataItem("ViewName", presenter.getViewName());

    Set<Long> ids = Sets.newHashSet(activeRow.getId());

    if (!BeeUtils.isEmpty(selectedRows)) {
      for (RowInfo row : selectedRows) {
        ids.add(row.getId());
      }
    }
    args.addDataItem("IdList", Codec.beeSerialize(ids));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(presenter.getGridView());

        if (!response.hasErrors()) {
          if (BeeUtils.isPositiveInt((String) response.getResponse())) {
            Global.confirmDelete(Settings.getAppName(), Icon.ALARM,
                Lists.newArrayList(Localized.getConstants()
                    .trCargoTripThereCargosAssignedInTripsAlarm(), Localized.getConstants()
                    .continueQuestion()),
                new ConfirmationCallback() {
                  @Override
                  public void onConfirm() {
                    doDelete();
                  }
                });
          } else {
            doDelete();
          }
        }
      }

      private void doDelete() {
        if (defMode == DeleteMode.SINGLE) {
          presenter.deleteRow(activeRow, true);
        } else if (defMode == DeleteMode.MULTI) {
          presenter.deleteRows(activeRow, selectedRows);
        }
      }
    });
    return DeleteMode.CANCEL;
  }
  
  @Override
  public GridInterceptor getInstance() {
    return new CargoTripChecker();
  }
}