package com.butent.bee.client.modules.transport;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

public class AssessmentForwarderPrintForm extends AbstractFormInterceptor {

  @Override
  public void beforeRefresh(final FormView form, IsRow row) {
    for (String name : new String[] {COL_CUSTOMER, COL_FORWARDER}) {
      Widget widget = form.getWidgetByName(name);

      if (widget != null) {
        ClassifierUtils.getCompanyInfo(row.getLong(form.getDataIndex(name)), widget);
      }
    }
    TransportUtils.getCargoPlaces(Filter.equals(COL_CARGO_TRIP,
        row.getLong(form.getDataIndex(COL_CARGO_TRIP))), (loading, unloading) -> {

      for (BeeRowSet places : new BeeRowSet[] {loading, unloading}) {
        String prefix = BeeUtils.removePrefix(places.getViewName(), COL_CARGO);
        HasWidgets datesWidget = (HasWidgets) form.getWidgetByName(prefix + COL_PLACE_DATE);
        HasWidgets placesWidget = (HasWidgets) form.getWidgetByName(prefix + COL_PLACE_ADDRESS);

        if (datesWidget != null) {
          datesWidget.clear();
        }
        if (placesWidget != null) {
          placesWidget.clear();
        }
        for (int i = 0; i < places.getNumberOfRows(); i++) {
          String ordinal = places.getString(i, ClassifierConstants.COL_ITEM_ORDINAL);

          if (datesWidget != null) {
            DateTime date = places.getDateTime(i, COL_PLACE_DATE);
            String txt = places.getString(i, COL_PLACE_NOTE);

            if (date != null) {
              txt = BeeUtils.joinWords(Format.renderDateTime(date), txt);
            }
            if (!BeeUtils.isEmpty(txt)) {
              Label lbl = new Label(BeeUtils.join(". ", ordinal, txt));
              lbl.addStyleName(form.getFormName() + "-" + COL_PLACE_DATE);
              datesWidget.add(lbl);
            }
          }
          String txt = BeeUtils.joinItems(places.getString(i, COL_PLACE_COMPANY),
              places.getString(i, COL_PLACE_ADDRESS),
              places.getString(i, COL_PLACE_POST_INDEX),
              places.getString(i, COL_PLACE_CITY + "Name"),
              places.getString(i, COL_PLACE_COUNTRY + "Name"),
              places.getString(i, COL_PLACE_CONTACT));

          if (placesWidget != null && !BeeUtils.isEmpty(txt)) {
            Label lbl = new Label(BeeUtils.join(". ", ordinal, txt));
            lbl.addStyleName(form.getFormName() + "-" + COL_PLACE_ADDRESS);
            placesWidget.add(lbl);
          }
        }
      }
    });
    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return null;
  }
}
