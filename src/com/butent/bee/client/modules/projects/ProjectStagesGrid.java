package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.List;

class ProjectStagesGrid extends AbstractGridInterceptor {

  private static final Dictionary LC = Localized.dictionary();

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {

    Provider provider = presenter.getDataProvider();

    int idxTaskCount = provider.getColumnIndex(ALS_TASK_COUNT);
    int idxOwner = provider.getColumnIndex(ProjectConstants.COL_PROJECT_OWNER);
    int idxStatus = provider.getColumnIndex(ProjectConstants.ALS_PROJECT_STATUS);

    if (BeeUtils.unbox(activeRow.getLong(idxOwner)) == BeeKeeper.getUser().getUserId()) {
      if (BeeUtils.unbox(activeRow.getLong(idxTaskCount)) > 0) {
        presenter.getGridView().notifySevere(LC.prjStageHasTasks());
        return GridInterceptor.DeleteMode.CANCEL;
      } else {
        return GridInterceptor.DeleteMode.SINGLE;
      }

    } else {
      presenter.getGridView().notifySevere(
          LC.project()
              + " "
              + EnumUtils.getEnumByIndex(ProjectStatus.class, activeRow.getInteger(idxStatus))
                  .getCaption());
      return GridInterceptor.DeleteMode.CANCEL;
    }
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if ((BeeUtils.same(columnName, NAME_ACTUAL_TASKS_DURATION)
        || BeeUtils.same(columnName, NAME_EXPECTED_TASKS_DURATION))
        && column instanceof HasCellRenderer) {

      ((HasCellRenderer) column).setRenderer(getDurationRenderer(dataColumns, columnName));

      return true;
    }

    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ProjectStagesGrid();
  }

  private static AbstractCellRenderer getDurationRenderer(
      final List<? extends IsColumn> dataColumns, final String calcColName) {
    return new AbstractCellRenderer(null) {

      @Override
      public String render(IsRow row) {
        BeeRowSet unitsRows = null;

        if (dataColumns == null) {
          return BeeConst.STRING_EMPTY;
        }

        if (row == null) {
          return BeeConst.STRING_EMPTY;
        }

        if (!BeeUtils.isEmpty(row.getProperty(PROP_TIME_UNITS))) {
          String prop = row.getProperty(PROP_TIME_UNITS);
          unitsRows = BeeRowSet.maybeRestore(prop);
        }

        String colName = null;
        switch (calcColName) {
          case NAME_ACTUAL_TASKS_DURATION:
            colName = COL_ACTUAL_TASKS_DURATION;
            break;

          case NAME_EXPECTED_TASKS_DURATION:
            colName = COL_EXPECTED_TASKS_DURATION;
            break;
        }

        if (BeeUtils.isEmpty(colName)) {
          return BeeConst.STRING_EMPTY;
        }

        int idxTD = DataUtils.getColumnIndex(colName, dataColumns);
        int idxUnit = DataUtils.getColumnIndex(COL_PROJECT_TIME_UNIT, dataColumns);

        double factor = BeeConst.DOUBLE_ONE;
        String unitName = BeeConst.STRING_EMPTY;

        if (!BeeConst.isUndef(idxUnit)) {
          long idValue = BeeUtils.unbox(row.getLong(idxUnit));
          BeeRow unitRow = unitsRows != null ? unitsRows.getRowById(idValue) : null;

          if (unitRow != null) {
            String prop = unitRow.getProperty(PROP_REAL_FACTOR);

            if (!BeeUtils.isEmpty(prop) && BeeUtils.isDouble(prop)) {
              factor = BeeUtils.toDouble(prop);
            }

            int idxName = unitsRows.getColumnIndex(ClassifierConstants.COL_UNIT_NAME);

            if (!BeeConst.isUndef(idxName)) {
              unitName = unitRow.getString(idxName);
            }
          }
        }

        if (!BeeConst.isUndef(idxTD)) {
          long value = BeeUtils.unbox(row.getLong(idxTD));
          String result = BeeConst.STRING_EMPTY;

          if (factor == BeeConst.DOUBLE_ONE) {
            result = TimeUtils.renderMinutes(BeeUtils.toInt(value
                / TimeUtils.MILLIS_PER_MINUTE), true);
          } else {
            long factorMls = BeeUtils.toLong(factor * TimeUtils.MILLIS_PER_HOUR);

            int calcValue = BeeUtils.toInt(value / factorMls);
            long decValue = value % factorMls;

            result = BeeUtils.joinWords(calcValue, unitName, decValue != 0
                ? TimeUtils
                    .renderMinutes(
                        BeeUtils.toInt(decValue
                            / TimeUtils.MILLIS_PER_MINUTE), true) : BeeConst.STRING_EMPTY);
          }

          final int idxExpD = DataUtils.getColumnIndex(COL_EXPECTED_DURATION, dataColumns);
          long expDMls =
              BeeUtils.unbox(row.getLong(idxExpD))
                  * BeeUtils.toLong(factor * TimeUtils.MILLIS_PER_HOUR);

          if (value > expDMls) {
            return "<span class=\"bee-prj-stage-GridFieldOverSized\">" + result + "</span>";
          } else {
            return "<span class=\"bee-prj-stage-GridFieldNotOverSized\">" + result + "</span>";
          }
        }
        return BeeConst.STRING_EMPTY;
      }
    };
  }
}
