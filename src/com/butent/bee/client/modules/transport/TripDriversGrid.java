package com.butent.bee.client.modules.transport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public class TripDriversGrid extends AbstractGridInterceptor implements ClickHandler {

  private final FaLabel main = new FaLabel(FontAwesome.CHECK);

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    main.setTitle(Localized.getConstants().setAsPrimary());
    main.addClickHandler(this);
    presenter.getHeader().addCommandItem(main);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public void afterInsertRow(IsRow result) {
    setMain(result, false);
    super.afterInsertRow(result);
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    IsRow row = getGridView().getActiveRow();

    if (row == null) {
      getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    setMain(row, true);
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    main.setVisible(DataUtils.isId(event.getRowId()));
    super.onParentRow(event);
  }

  private void setMain(IsRow row, boolean forced) {
    Filter flt = Filter.compareId(row.getLong(getGridView().getDataIndex(COL_TRIP)));

    if (!forced) {
      flt = Filter.and(flt, Filter.isNull(COL_MAIN_DRIVER));
    }
    Queries.update(TBL_TRIPS, flt, COL_MAIN_DRIVER, BeeUtils.toString(row.getId()),
        new Queries.IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            if (BeeUtils.isPositive(result)) {
              getGridPresenter().refresh(true);
            }
          }
        });
  }
}
