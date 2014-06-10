package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;

class ServiceObjectWrapper {

  private static final String idColumn = Data.getIdColumn(VIEW_SERVICE_OBJECTS);

  private static final String categoryLabel = Data.getColumnLabel(VIEW_SERVICE_OBJECTS,
      COL_SERVICE_CATEGORY);
  private static final String addressLabel = Data.getColumnLabel(VIEW_SERVICE_OBJECTS,
      COL_SERVICE_ADDRESS);
  private static final String customerLabel = Data.getColumnLabel(VIEW_SERVICE_OBJECTS,
      COL_SERVICE_CUSTOMER);
  private static final String contractorLabel = Data.getColumnLabel(VIEW_SERVICE_OBJECTS,
      COL_SERVICE_CONTRACTOR);

  private final Long id;

  private final String address;

  private final String title;

  ServiceObjectWrapper(SimpleRow row) {
    this.id = row.getLong(idColumn);

    this.address = row.getValue(COL_SERVICE_ADDRESS);

    this.title = TimeBoardHelper.buildTitle(
        categoryLabel, row.getValue(ALS_SERVICE_CATEGORY_NAME),
        addressLabel, row.getValue(COL_SERVICE_ADDRESS),
        customerLabel, row.getValue(ALS_SERVICE_CUSTOMER_NAME),
        contractorLabel, row.getValue(ALS_SERVICE_CONTRACTOR_NAME));
  }

  Long getId() {
    return id;
  }

  String getAddress() {
    return address;
  }

  String getTitle() {
    return title;
  }
}
