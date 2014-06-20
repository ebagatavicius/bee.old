package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

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

  private static final int MAX_MAIN_CRITERIA_COUNT = 20;

  private final Long id;

  private final String address;

  private final String title;

  private final String mainCriteriaTitle;

  ServiceObjectWrapper(SimpleRow row) {
    this.id = row.getLong(idColumn);

    this.address = row.getValue(COL_SERVICE_ADDRESS);

    this.mainCriteriaTitle = BeeConst.STRING_EMPTY;

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

  String getMainCriteriaTitle() {
    return this.mainCriteriaTitle;
  }

  void appendMainCriteriaTitle(final Flow panel) {
    Filter filter = Filter.and(Filter.equals(COL_SERVICE_OBJECT, getId()),
        Filter.isNull(COL_SERVICE_CRITERIA_GROUP_NAME));

    Queries.getRowSet(VIEW_SERVICE_OBJECT_CRITERIA, null, filter, new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.getNumberOfRows() < 0) {
          return;
        }

        int i = 0;
        for (BeeRow crit : result) {
          String name = DataUtils.getString(result, crit, COL_SERVICE_CRITERION_NAME);

          if (!BeeUtils.isEmpty(name)) {
            String value = DataUtils.getString(result, crit, COL_SERVICE_CRITERION_VALUE);

            value = BeeUtils.isEmpty(value) ? Localized.getConstants().filterNullLabel() : value;

            panel.setTitle(BeeUtils.buildLines(panel.getTitle(), TimeBoardHelper.buildTitle(name,
                value)));

            i++;
            int left = result.getNumberOfRows() - MAX_MAIN_CRITERIA_COUNT;
            if ((i >= MAX_MAIN_CRITERIA_COUNT) && (left > 0)) {
              panel.setTitle(BeeUtils.buildLines(panel.getTitle(), TimeBoardHelper.buildTitle(
                  BeeConst.ELLIPSIS,
                  BeeUtils.bracket(left))));
              break;
            }
          }
        }
      }

    });
  }
}
