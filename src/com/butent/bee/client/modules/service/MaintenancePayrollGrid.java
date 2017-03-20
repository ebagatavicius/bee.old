package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.service.ServiceUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;

public class MaintenancePayrollGrid extends AbstractGridInterceptor {

  @Override
  public void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
      boolean rowMode) {
    if (BeeUtils.same(column.getId(), COL_PAYROLL_CONFIRMED)) {
      Queries.update(getViewName(), Filter.compareId(result.getId()),
          Arrays.asList(COL_PAYROLL_CONFIRMED_USER, COL_PAYROLL_CONFIRMATION_DATE),
          Arrays.asList(BeeUtils.toString(BeeKeeper.getUser().getUserId()),
              BeeUtils.toString(System.currentTimeMillis())), new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer updatedResult) {
              Queries.getRow(getViewName(), result.getId(), new RowCallback() {
                @Override
                public void onSuccess(BeeRow updatedRow) {
                  if (result != null) {
                    RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), updatedRow);
                  }
                }
              });
            }
          });
    } else if (BeeUtils.in(column.getId(), COL_PAYROLL_SALARY, COL_PAYROLL_TARIFF)) {
      Double basicAmount = getActiveRow().getDouble(getDataIndex(COL_PAYROLL_BASIC_AMOUNT));
      String updateColumn = BeeUtils.same(column.getId(), COL_PAYROLL_SALARY)
          ? COL_PAYROLL_TARIFF : COL_PAYROLL_SALARY;

      if (basicAmount != null) {
        Double columnValue = BeeUtils.same(column.getId(), COL_PAYROLL_SALARY)
            ? ServiceUtils.calculateTariff(BeeUtils.toDouble(newValue), basicAmount)
            : ServiceUtils.calculateSalary(BeeUtils.toDouble(newValue), basicAmount);

        Queries.updateCellAndFire(getViewName(), result.getId(), result.getVersion(), updateColumn,
            getActiveRow().getString(getDataIndex(updateColumn)), BeeUtils.toString(columnValue));
      }
    }
    super.afterUpdateCell(column, oldValue, newValue, result, rowMode);
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    if (canDeleteAndEdit(activeRow)) {
      return DeleteMode.SINGLE;
    } else {
      getGridView().notifySevere(Localized.dictionary().rowIsNotRemovable());
      return DeleteMode.CANCEL;
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new MaintenancePayrollGrid();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return canDeleteAndEdit(row);
  }

  private boolean canDeleteAndEdit(IsRow row) {
    int index = Data.getColumnIndex(getViewName(), COL_PAYROLL_CONFIRMED);
    return !(BeeUtils.isPositive(index) && BeeUtils.isTrue(row.getBoolean(index)));
  }
}
