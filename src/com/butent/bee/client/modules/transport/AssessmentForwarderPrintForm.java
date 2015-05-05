package com.butent.bee.client.modules.transport;

import com.google.gwt.user.client.ui.HasWidgets;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

public class AssessmentForwarderPrintForm extends AssessmentPrintForm {

  @Override
  public void beforeRefresh(final FormView form, IsRow row) {
    Queries.getRowSet(TBL_CARGO_HANDLING, null, Filter.equals(COL_FORWARDER, row.getId()),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            for (String prefix : new String[] {VAR_LOADING, VAR_UNLOADING}) {
              HasWidgets dates = (HasWidgets) form.getWidgetByName(prefix + COL_PLACE_DATE);
              HasWidgets places = (HasWidgets) form.getWidgetByName(prefix + COL_PLACE_ADDRESS);

              if (dates != null) {
                dates.clear();
              }
              if (places != null) {
                places.clear();
              }
              for (int i = 0; i < result.getNumberOfRows(); i++) {
                String ordinal = result.getNumberOfRows() > 1 ? (i + 1) + "." : null;

                if (dates != null) {
                  DateTime date = result.getDateTime(i, prefix + COL_PLACE_DATE);
                  String txt = result.getString(i, prefix + COL_PLACE_NOTE);

                  if (date != null) {
                    txt = BeeUtils.joinWords(date.toCompactString(), txt);
                  }
                  if (!BeeUtils.isEmpty(txt)) {
                    Label lbl = new Label(BeeUtils.joinWords(ordinal, txt));
                    lbl.addStyleName(form.getFormName() + "-" + prefix + COL_PLACE_DATE);
                    dates.add(lbl);
                  }
                }
                String txt = BeeUtils.joinItems(result.getString(i, prefix + COL_PLACE_COMPANY),
                    result.getString(i, prefix + COL_PLACE_ADDRESS),
                    result.getString(i, prefix + COL_PLACE_POST_INDEX),
                    result.getString(i, prefix + COL_PLACE_CITY + "Name"),
                    result.getString(i, prefix + COL_PLACE_COUNTRY + "Name"),
                    result.getString(i, prefix + COL_PLACE_CONTACT));

                if (!BeeUtils.isEmpty(txt)) {
                  Label lbl = new Label(BeeUtils.joinWords(ordinal, txt));
                  lbl.addStyleName(form.getFormName() + "-" + prefix + COL_PLACE_ADDRESS);
                  places.add(lbl);
                }
              }
            }
          }
        });
    super.beforeRefresh(form, row);
  }
}
