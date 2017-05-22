package com.butent.bee.client.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public class BudgetHeadersGrid extends AbstractGridInterceptor {

  public BudgetHeadersGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new BudgetHeadersGrid();
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    if (copy && presenter != null && presenter.getActiveRow() != null) {
      final IsRow oldRow = presenter.getActiveRow();

      String oldName = getStringValue(COL_BUDGET_NAME);
      int maxLength = Data.getColumnPrecision(getViewName(), COL_BUDGET_NAME);

      Global.inputString(Localized.dictionary().finBudgetCopy(oldName),
          Localized.dictionary().finBudgetName(), new StringCallback() {
            @Override
            public void onSuccess(String value) {
              if (!BeeUtils.isEmpty(value)) {
                final String newName = BeeUtils.trim(value);

                Queries.getRowCount(getViewName(), Filter.equals(COL_BUDGET_NAME, newName),
                    new Queries.IntCallback() {
                      @Override
                      public void onSuccess(Integer result) {
                        if (BeeUtils.isPositive(result)) {
                          getGridView().notifyWarning(Localized.dictionary().valueExists(newName));
                        } else {
                          copyBudget(oldRow, newName);
                        }
                      }
                    });
              }
            }
          },
          null, null, maxLength, null, 30, CssUnit.EM);

      return false;

    } else {
      return super.beforeAddRow(presenter, copy);
    }
  }

  private void copyBudget(final IsRow oldRow, String name) {
    IsRow headerRow = DataUtils.cloneRow(oldRow);
    Data.setValue(getViewName(), headerRow, COL_BUDGET_NAME, name);

    int ordinalIndex = getDataIndex(COL_BUDGET_HEADER_ORDINAL);
    Integer ordinal = oldRow.getInteger(ordinalIndex);
    if (BeeUtils.isPositive(ordinal)) {
      headerRow.setValue(ordinalIndex, ordinal + 1);
    }

    BeeRowSet rowSet = DataUtils.createRowSetForInsert(getViewName(),
        getGridView().getDataColumns(), headerRow);

    Queries.insertRow(rowSet, new RowCallback() {
      @Override
      public void onSuccess(BeeRow newHeader) {
        if (DataUtils.hasId(newHeader)) {
          Queries.getRowSet(VIEW_BUDGET_ENTRIES, null,
              Filter.equals(COL_BUDGET_HEADER, oldRow.getId()), new Queries.RowSetCallback() {
                @Override
                public void onFailure(String... reason) {
                  afterCopy(newHeader);
                  super.onFailure(reason);
                }

                @Override
                public void onSuccess(BeeRowSet oldEntries) {
                  if (DataUtils.isEmpty(oldEntries)) {
                    afterCopy(newHeader);

                  } else {
                    int index = oldEntries.getColumnIndex(COL_BUDGET_HEADER);
                    long newHeaderId = newHeader.getId();

                    oldEntries.getRows().forEach(row -> row.setValue(index, newHeaderId));

                    Queries.insertRows(DataUtils.createRowSetForInsert(oldEntries),
                        new RpcCallback<RowInfoList>() {
                          @Override
                          public void onFailure(String... reason) {
                            afterCopy(newHeader);
                            super.onFailure(reason);
                          }

                          @Override
                          public void onSuccess(RowInfoList riList) {
                            afterCopy(newHeader);
                          }
                        });
                  }
                }
              });
        }
      }
    });
  }

  private void afterCopy(BeeRow newRow) {
    getGridView().getGrid().insertRow(newRow, true);
    RowInsertEvent.fire(BeeKeeper.getBus(), getViewName(), newRow, getGridView().getId());
  }
}
