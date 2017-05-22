package com.butent.bee.client.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.modules.finance.FinanceKeeper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.finance.analysis.AnalysisResults;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalysisResultsGrid extends AbstractGridInterceptor {

  public AnalysisResultsGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new AnalysisResultsGrid();
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if ("Open".equals(columnName)) {
      column.getCell().addClickHandler(event -> {
        IsRow row = AbstractCell.getEventRow(event);

        if (DataUtils.hasId(row)) {
          open(row.getId());
        }
      });
    }

    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  private void open(long id) {
    ParameterList parameters = FinanceKeeper.createArgs(SVC_GET_ANALYSIS_RESULTS);
    parameters.addQueryItem(Service.VAR_ID, id);

    BeeKeeper.getRpc().makeRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getGridView());

        if (response.hasResponse()) {
          Map<String, String> map = Codec.deserializeHashMap(response.getResponseAsString());

          String caption = map.get(COL_ANALYSIS_RESULT_CAPTION);
          String results = map.get(COL_ANALYSIS_RESULTS);

          if (results == null) {
            getGridView().notifySevere(Localized.dictionary().noData());

          } else {
            Set<Action> enabledActions = new HashSet<>();
            enabledActions.add(Action.EXPORT);
            enabledActions.add(Action.PRINT);

            AnalysisResults analysisResults = AnalysisResults.restore(results);
            AnalysisViewer viewer = new AnalysisViewer(analysisResults, enabledActions);

            if (!BeeUtils.isEmpty(caption)
                && !BeeUtils.containsSame(viewer.getCaption(), caption)) {

              HeaderView header = viewer.getHeader();
              if (header != null) {
                if (BeeUtils.isEmpty(header.getCaption())) {
                  header.setCaption(caption);
                } else {
                  header.setMessage(0, caption, null);
                }
              }
            }

            BeeKeeper.getScreen().showInNewPlace(viewer);
          }
        }
      }
    });
  }
}
