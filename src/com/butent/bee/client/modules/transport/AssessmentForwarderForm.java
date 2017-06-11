package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AssessmentForwarderForm extends PrintFormInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, VAR_INCOME) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          FormView form = ViewHelper.getForm(getGridView());

          if (form != null) {
            event.getSelector().setAdditionalFilter(Filter.equals(COL_CARGO,
                form.getLongValue(COL_CARGO)));
          }
        }
      });
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentForwarderForm();
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, Long> companies = new HashMap<>();
    companies.put(COL_CUSTOMER, BeeKeeper.getUser().getCompany());
    companies.put(COL_FORWARDER, getLongValue(COL_FORWARDER));

    super.getReportParameters(defaultParameters ->
        ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
          IsRow row = getActiveRow();
          FormView form = getFormView();

          defaultParameters.putAll(companiesInfo);
          defaultParameters.put(COL_ORDER_ID, BeeUtils.toString(row.getId()));

          TransportUtils.getCargoPlaces(Filter.equals(COL_CARGO_TRIP,
              getActiveRow().getLong(form.getDataIndex(COL_CARGO_TRIP))), (loading, unloading) -> {
            for (BeeRowSet places : new BeeRowSet[] {loading, unloading}) {

              BeeRowSet current = TransportUtils.copyCargoPlaces(places);

              defaultParameters.put(places.getViewName(), current.serialize());
            }
            parametersConsumer.accept(defaultParameters);
          });
        }));
  }
}
