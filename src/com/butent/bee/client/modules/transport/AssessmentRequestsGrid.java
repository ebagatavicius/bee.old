package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AssessmentRequestsGrid extends AbstractGridInterceptor {

  @Override
  public GridInterceptor getInstance() {
    return new AssessmentRequestsGrid();
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    if (copy) {
      IsRow oldRow = presenter.getActiveRow();
      DataInfo dataInfo = Data.getDataInfo(getViewName());
      BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

      for (String col : dataInfo.getColumnNames(false)) {
        int idx = dataInfo.getColumnIndex(col);

        if (!BeeConst.isUndef(idx) && !BeeUtils.contains(Arrays.asList(COL_ASSESSMENT, COL_ORDER_NO,
            COL_CARGO, COL_STATUS, COL_DATE, COL_NUMBER, COL_ORDER_MANAGER, COL_ASSESSMENT_EXPENSES,
            COL_ASSESSMENT_LOG), col)) {
          newRow.setValue(idx, oldRow.getString(idx));
        }
      }
      newRow.setValue(getDataIndex(COL_ASSESSMENT_STATUS), AssessmentStatus.NEW.ordinal());
      newRow.setValue(getDataIndex(ALS_ORDER_STATUS), OrderStatus.REQUEST.ordinal());

      Queries.insertRow(DataUtils.createRowSetForInsert(getViewName(), getDataColumns(), newRow),
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow assessmentRow) {
              int cargoIndex = getDataIndex(COL_CARGO);
              TransportUtils.getCargoPlaces(Filter.equals(COL_CARGO, oldRow.getLong(cargoIndex)),
                  (loading, unloading) -> {
                List<BeeRowSet> placesRowSets = Arrays.asList(loading, unloading);
                Runnable onCloneChildren = new Runnable() {
                  int copiedGrids;

                  @Override
                  public void run() {
                    if (Objects.equals(placesRowSets.size(), ++copiedGrids)) {
                      RowEditor.open(getViewName(), assessmentRow.getId(), Opener.MODAL);
                    }
                  }
                };

                for (BeeRowSet placesRowSet : placesRowSets) {
                  BeeRowSet newPlaces = Data.createRowSet(placesRowSet.getViewName());
                  int cargoIdx = newPlaces.getColumnIndex(COL_CARGO);

                  for (BeeRow row : placesRowSet) {
                    BeeRow clonned = newPlaces.addEmptyRow();
                    clonned.setValues(row.getValues());
                    clonned.setValue(cargoIdx, assessmentRow.getValue(cargoIndex));
                  }

                  if (!newPlaces.isEmpty()) {
                    newPlaces = DataUtils.createRowSetForInsert(newPlaces);
                    Queries.insertRows(newPlaces, new RpcCallback<RowInfoList>() {
                      @Override
                      public void onSuccess(RowInfoList result) {
                        onCloneChildren.run();
                      }
                    });
                  } else {
                    onCloneChildren.run();
                  }
                }
              });
            }
          });
      return false;
    } else {
      return super.beforeAddRow(presenter, copy);
    }
  }
}
