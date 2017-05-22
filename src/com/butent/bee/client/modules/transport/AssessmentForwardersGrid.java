package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;

import java.util.List;
import java.util.Set;

public class AssessmentForwardersGrid extends ParentRowRefreshGrid {
  private static final String SERVICE_CARGO = "ServiceCargo";
  private static final String SUPPLIER = "Supplier";
  private static final String FORWARDER = "Forwarder";

  @Override
  public GridInterceptor getInstance() {
    return new AssessmentForwardersGrid();
  }

  @Override
  public boolean previewModify(Set<Long> rowIds) {
    if (super.previewModify(rowIds)) {
      Data.refreshLocal(TBL_CARGO_EXPENSES);
      return true;
    }
    return false;
  }

  @Override
  public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
    super.onReadyForInsert(gridView, event);

    List<BeeColumn> columns = event.getColumns();
    List<String> values = event.getValues();

    String cargo = ViewHelper.getForm(gridView).getStringValue(COL_CARGO);

    columns.add(DataUtils.getColumn(COL_CARGO, gridView.getDataColumns()));
    values.add(cargo);
    columns.add(DataUtils.getColumn(SERVICE_CARGO, gridView.getDataColumns()));
    values.add(cargo);

    if (DataUtils.getColumn(SUPPLIER, columns) == null) {
      columns.add(DataUtils.getColumn(SUPPLIER, gridView.getDataColumns()));
      values.add(values.get(DataUtils.getColumnIndex(FORWARDER, columns)));
    }
  }
}
