package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.Filter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AssessmentTransportationForm extends PrintFormInterceptor {

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentTransportationForm();
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    Queries.getRowSet(VIEW_TRIP_CARGO, null, Filter.equals(COL_TRIP, getActiveRowId()),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            dataConsumer.accept(new BeeRowSet[] {result});
          }
        });
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, Long> companies = new HashMap<>();
    companies.put(COL_CUSTOMER, BeeKeeper.getUser().getCompany());
    companies.put(COL_FORWARDER, getLongValue(COL_FORWARDER));

    super.getReportParameters(defaultParameters ->
        ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
          defaultParameters.putAll(companiesInfo);
          parametersConsumer.accept(defaultParameters);
        }));
  }
}