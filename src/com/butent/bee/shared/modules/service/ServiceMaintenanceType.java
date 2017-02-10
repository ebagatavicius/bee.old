package com.butent.bee.shared.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.COL_REPAIRER;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;

public enum ServiceMaintenanceType implements HasCaption, HasWidgetSupplier {

  ALL(Localized.dictionary().svcMaintenance()) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return null;
    }
  },

  MY(Localized.dictionary().svcMyMaintenance()) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return Filter.or(Filter.isNull(COL_REPAIRER), Filter.isEqual(COL_REPAIRER, userValue));
    }
  };

  public static ServiceMaintenanceType getByPrefix(String input) {
    for (ServiceMaintenanceType type : values()) {
      if (BeeUtils.startsSame(type.name(), input)) {
        return type;
      }
    }
    return null;
  }

  private final String caption;

  ServiceMaintenanceType(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public abstract Filter getFilter(LongValue userValue);

  @Override
  public String getSupplierKey() {
    return ServiceConstants.GRID_SERVICE_MAINTENANCE + BeeConst.STRING_UNDER + name().toLowerCase();
  }
}
