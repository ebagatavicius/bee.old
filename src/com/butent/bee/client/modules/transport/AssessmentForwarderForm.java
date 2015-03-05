package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.output.PrintFormInterceptor;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;

public class AssessmentForwarderForm extends PrintFormInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, TransportConstants.TBL_CARGO_HANDLING)) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {

        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
          event.getColumns().add(DataUtils.getColumn(COL_CARGO, gridView.getDataColumns()));
          event.getValues().add(getStringValue(COL_CARGO));
          super.onReadyForInsert(gridView, event);
        }

        @Override
        public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
          FormView form = ViewHelper.getForm(AssessmentForwarderForm.this.getGridView());

          if (form != null && gridView.isEmpty()) {
            for (String prefix : new String[] {VAR_LOADING, VAR_UNLOADING}) {
              for (String col : new String[] {COL_PLACE_DATE, COL_PLACE_ADDRESS,
                  COL_PLACE_POST_INDEX, COL_PLACE_COMPANY, COL_PLACE_CONTACT,
                  COL_PLACE_CITY, "CityName", COL_PLACE_COUNTRY, "CountryName", "CountryCode"}) {

                newRow.setValue(gridView.getDataIndex(prefix + col),
                    form.getStringValue(prefix + col));
              }
            }
          }
          return super.onStartNewRow(gridView, oldRow, newRow);
        }
      });
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new AssessmentForwarderPrintForm();
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentForwarderForm();
  }

}
