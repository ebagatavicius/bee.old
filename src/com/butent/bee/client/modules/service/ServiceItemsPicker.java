package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.orders.ReservationItemsPicker;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

public class ServiceItemsPicker extends ReservationItemsPicker {

  private CheckBox filterServices;

  public ServiceItemsPicker(Module module) {
    super(module);
  }

  @Override
  protected void addAdditionalFilter(ParameterList params) {
    if (BeeUtils.isTrue(filterServices.getValue())) {
      params.addDataItem(ClassifierConstants.COL_ITEM_IS_SERVICE,
          BeeUtils.toString(filterServices.getValue()));
    }
    setIsOrder(!filterServices.getValue());
  }

  @Override
  public void addAdditionalSearchWidget(Flow panel) {
    filterServices = new CheckBox();
    filterServices.setText(Localized.dictionary().services());
    filterServices.addStyleName(STYLE_SEARCH_REMAINDER);
    panel.add(filterServices);
  }

  @Override
  public Long getWarehouseFrom(IsRow row) {
    int warehouseIdx = Data
        .getColumnIndex(TBL_SERVICE_MAINTENANCE, ClassifierConstants.COL_WAREHOUSE);
    if (row == null || BeeConst.isUndef(warehouseIdx)) {
      return null;
    }

    return row.getLong(warehouseIdx);
  }

  public boolean isCheckedFilterService() {
    return filterServices.isChecked();
  }

  @Override
  public boolean setIsOrder(IsRow row) {
    return true;
  }
}