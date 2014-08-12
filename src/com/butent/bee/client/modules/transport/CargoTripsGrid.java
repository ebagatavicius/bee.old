package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

class CargoTripsGrid extends AbstractGridInterceptor {

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    final GridView gridView = presenter.getGridView();

    gridView.ensureRelId(new IdCallback() {
      @Override
      public void onSuccess(Long cargoId) {
        int tripIndex = gridView.getDataIndex(COL_TRIP);
        CompoundFilter tripFilter = Filter.and();

        for (IsRow row : gridView.getGrid().getRowData()) {
          tripFilter.add(Filter.compareId(Operator.NE, row.getLong(tripIndex)));
        }
        TripSelector.select(new String[] {BeeUtils.toString(cargoId)}, tripFilter,
            gridView.getElement());
      }
    });
    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new CargoTripsGrid();
  }

  @Override
  public String getRowCaption(IsRow row, boolean edit) {
    return Localized.getConstants().trCargoActualPlaces();
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (!BeeUtils.inListSame(event.getColumnId(), "Loading", "Unloading", COL_TRIP_PERCENT)) {
      event.consume();
    }
    super.onEditStart(event);
  }
}