package com.butent.bee.client.modules.ec;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class ArticleCodesGridInterceptor extends AbstractGridInterceptor {

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnName, COL_TCD_CODE_NR)) {
      editableColumn.addCellValidationHandler(new CellValidateEvent.Handler() {

        @Override
        public Boolean validateCell(CellValidateEvent event) {
          if (event.isCellValidation() && event.isPostValidation()) {
            CellValidation cv = event.getCellValidation();
            IsRow row = cv.getRow();

            String value = EcUtils.normalizeCode(cv.getNewValue());

            if (event.isNewRow()) {
              row.setValue(Data.getColumnIndex(VIEW_ARTICLE_CODES, COL_TCD_SEARCH_NR), value);
            } else {
              BeeColumn searchCol = new BeeColumn(ValueType.TEXT, COL_TCD_SEARCH_NR);
              List<BeeColumn> cols = Lists.newArrayList(cv.getColumn(), searchCol);

              List<String> oldValues = Lists.newArrayList(cv.getOldValue(),
                  row.getString(Data.getColumnIndex(VIEW_ARTICLE_CODES, COL_TCD_SEARCH_NR)));

              List<String> newValues = Lists.newArrayList(cv.getNewValue(), value);

              Queries.update(VIEW_ARTICLE_CODES, row.getId(), row.getVersion(), cols, oldValues,
                  newValues, null, new RowUpdateCallback(VIEW_ARTICLE_CODES));

              return null;
            }
          }

          return true;
        }
      });
    }

    return true;
  }
  
  @Override
  public GridInterceptor getInstance() {
    return new ArticleCodesGridInterceptor();
  }
}
