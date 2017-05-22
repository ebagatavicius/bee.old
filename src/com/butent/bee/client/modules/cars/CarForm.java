package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.VIEW_TRADE_DOCUMENTS;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.modules.trade.TradeDocumentsGrid;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;

import java.util.Objects;

public class CarForm extends SpecificationForm {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (Objects.equals(name, TBL_SERVICE_ORDERS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
          DataInfo orderInfo = Data.getDataInfo(gridView.getViewName());
          DataInfo carInfo = Data.getDataInfo(CarForm.this.getViewName());
          IsRow carRow = CarForm.this.getActiveRow();

          RelationUtils.updateRow(orderInfo, COL_CAR, newRow, carInfo, carRow, true);

          RelationUtils.copyWithDescendants(carInfo, COL_OWNER, carRow, orderInfo, COL_CUSTOMER,
              newRow);

          return super.onStartNewRow(gridView, oldRow, newRow, copy);
        }
      });
    }
    if (Objects.equals(name, VIEW_TRADE_DOCUMENTS) && widget instanceof GridPanel) {
      ((GridPanel) widget).setGridInterceptor(new TradeDocumentsGrid().setFilterSupplier(() ->
          Filter.custom(FILTER_CAR_DOCUMENTS, getActiveRowId())));
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CarForm();
  }
}
