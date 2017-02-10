package com.butent.bee.shared.data;

import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RelationUtils {

  public static int clearRelatedValues(DataInfo dataInfo, String colName, IsRow row) {
    int result = 0;
    if (dataInfo == null || BeeUtils.isEmpty(colName) || row == null) {
      return result;
    }

    Collection<ViewColumn> descendants = dataInfo.getDescendants(colName, false);
    if (descendants.isEmpty()) {
      return result;
    }

    for (ViewColumn vc : descendants) {
      int index = dataInfo.getColumnIndex(vc.getName());
      if (!BeeConst.isUndef(index)) {
        row.clearCell(index);
        result++;
      }
    }
    return result;
  }

  public static Collection<String> copyWithDescendants(DataInfo sourceInfo, String sourceColumn,
      IsRow sourceRow, DataInfo targetInfo, String targetColumn, IsRow targetRow) {

    List<String> result = new ArrayList<>();
    if (BeeUtils.anyNull(sourceInfo, sourceRow, targetInfo, targetRow)
        || BeeUtils.anyEmpty(sourceColumn, targetColumn)) {
      return result;
    }

    int sourceIndex = sourceInfo.getColumnIndex(sourceColumn);
    if (sourceIndex < 0) {
      logger.warning(sourceInfo.getViewName(), sourceColumn, "column not found");
      return result;
    }

    int targetIndex = targetInfo.getColumnIndex(targetColumn);
    if (targetIndex < 0) {
      logger.warning(targetInfo.getViewName(), targetColumn, "column not found");
      return result;
    }

    Long sourceValue = sourceRow.getLong(sourceIndex);
    Long targetValue = targetRow.getLong(targetIndex);

    if (Objects.equals(sourceValue, targetValue)) {
      return result;
    }

    boolean clear = sourceValue == null;

    if (clear) {
      targetRow.clearCell(targetIndex);
    } else {
      targetRow.setValue(targetIndex, sourceValue);
    }
    result.add(targetColumn);

    Collection<ViewColumn> targetDescendants = targetInfo.getDescendants(targetColumn, false);
    if (targetDescendants.isEmpty()) {
      logger.warning(targetInfo.getViewName(), targetColumn, "no descendants found");
      return result;
    }

    if (clear) {
      for (ViewColumn tc : targetDescendants) {
        int index = targetInfo.getColumnIndex(tc.getName());
        if (index >= 0 && !targetRow.isNull(index)) {
          targetRow.clearCell(index);
          result.add(tc.getName());
        }
      }
      return result;
    }

    int sourceLevel = Math.max(sourceInfo.getViewColumnLevel(sourceColumn), 0);
    int targetLevel = Math.max(targetInfo.getViewColumnLevel(targetColumn), 0);

    for (ViewColumn tc : targetDescendants) {
      targetIndex = targetInfo.getColumnIndex(tc.getName());
      if (BeeConst.isUndef(targetIndex)) {
        logger.warning(targetInfo.getViewName(), tc.getName(), "column not found");
        continue;
      }

      sourceIndex = sourceInfo.getColumnIndexBySource(tc.getTable(), tc.getField(),
          sourceLevel + tc.getLevel() - targetLevel);
      if (sourceIndex >= 0) {
        String value = sourceRow.getString(sourceIndex);
        if (!BeeUtils.equalsTrimRight(targetRow.getString(targetIndex), value)) {
          targetRow.setValue(targetIndex, value);
          result.add(tc.getName());
        }
      }
    }

    return result;
  }

  public static List<String> getRenderColumns(DataInfo dataInfo, String colName) {
    List<String> result = new ArrayList<>();
    if (dataInfo == null || BeeUtils.isEmpty(colName)) {
      return result;
    }

    Collection<ViewColumn> descendants = dataInfo.getDescendants(colName, false);

    if (!descendants.isEmpty()) {
      List<Integer> columnIndexes = new ArrayList<>();
      for (ViewColumn vc : descendants) {
        if (BeeUtils.isEmpty(vc.getRelation())) {
          int index = dataInfo.getColumnIndex(vc.getName());
          if (!BeeConst.isUndef(index)) {
            columnIndexes.add(index);
          }
        }
      }

      if (columnIndexes.size() > 1) {
        Collections.sort(columnIndexes);
      }
      for (int index : columnIndexes) {
        result.add(dataInfo.getColumnId(index));
      }
    }
    return result;
  }

  public static int setDefaults(DataInfo dataInfo, IsRow row, Collection<String> colNames,
      List<BeeColumn> columns, UserData userData) {

    int result = 0;
    if (row == null || BeeUtils.isEmpty(colNames) || BeeUtils.isEmpty(columns)) {
      return result;
    }

    for (String colName : colNames) {
      BeeColumn column = DataUtils.getColumn(colName, columns);
      if (column == null || !column.hasDefaults()) {
        continue;
      }

      if (Defaults.DefaultExpression.CURRENT_USER.equals(column.getDefaults().getA())) {
        result += setUserFields(dataInfo, row, colName, userData);
      }

      if (Defaults.DefaultExpression.MAIN_CURRENCY.equals(column.getDefaults().getA())
          && !BeeUtils.isEmpty(ClientDefaults.getCurrencyName())) {
        if (!BeeUtils.isEmpty(setCurrencyName(dataInfo, row, colName,
            ClientDefaults.getCurrencyName()))) {
          result++;
        }
      }
    }
    return result;
  }

  public static int setRelatedValues(DataInfo dataInfo, String colName, IsRow targetRow,
      IsRow sourceRow) {

    int result = 0;
    if (dataInfo == null || BeeUtils.isEmpty(colName) || targetRow == null
        || sourceRow == null) {
      return result;
    }

    Collection<ViewColumn> descendants = dataInfo.getDescendants(colName, false);
    if (descendants.isEmpty()) {
      return result;
    }

    for (ViewColumn vc : descendants) {
      int index = dataInfo.getColumnIndex(vc.getName());
      if (!BeeConst.isUndef(index)) {
        targetRow.setValue(index, sourceRow.getString(index));
        result++;
      }
    }
    return result;
  }

  public static int setUserFields(DataInfo dataInfo, IsRow row, String userColumn,
      UserData userData) {

    int result = 0;
    if (dataInfo == null || row == null || BeeUtils.isEmpty(userColumn) || userData == null) {
      return result;
    }

    Collection<ViewColumn> descendants = dataInfo.getDescendants(userColumn, false);
    if (descendants.isEmpty()) {
      return result;
    }

    for (ViewColumn vc : descendants) {
      int index = dataInfo.getColumnIndex(vc.getName());
      if (BeeConst.isUndef(index)) {
        continue;
      }

      if (BeeUtils.same(vc.getField(), ClassifierConstants.COL_FIRST_NAME)) {
        row.setValue(index, userData.getFirstName());
        result++;
      } else if (BeeUtils.same(vc.getField(), ClassifierConstants.COL_LAST_NAME)) {
        row.setValue(index, userData.getLastName());
        result++;
      } else if (BeeUtils.same(vc.getField(), ClassifierConstants.COL_COMPANY_NAME)) {
        row.setValue(index, userData.getCompanyName());
        result++;
      } else if (BeeUtils.same(vc.getField(), ClassifierConstants.COL_COMPANY_PERSON)) {
        row.setValue(index, userData.getCompanyPerson());
        result++;
      } else if (BeeUtils.same(vc.getField(), ClassifierConstants.COL_PERSON)) {
        row.setValue(index, userData.getPerson());
        result++;
      } else if (BeeUtils.same(vc.getField(), ClassifierConstants.COL_COMPANY)) {
        row.setValue(index, userData.getCompany());
        result++;
      }
    }
    return result;
  }

  public static Collection<String> maybeUpdateCurrency(DataInfo dataInfo, IsRow row,
      String currencyColumn, boolean set) {

    Collection<String> result = new HashSet<>();
    if (dataInfo == null || row == null || BeeUtils.isEmpty(currencyColumn)) {
      return result;
    }

    Long currency;
    String currencyName;

    if (set) {
      currency = ClientDefaults.getCurrency();
      currencyName = ClientDefaults.getCurrencyName();
    } else {
      currency = null;
      currencyName = null;
    }

    int index = dataInfo.getColumnIndex(currencyColumn);
    if (BeeConst.isUndef(index) || (row.isNull(index) == (currency == null))) {
      return result;
    }

    row.setValue(index, currency);
    result.add(currencyColumn);

    String nameColumn = setCurrencyName(dataInfo, row, currencyColumn, currencyName);
    if (!BeeUtils.isEmpty(nameColumn)) {
      result.add(nameColumn);
    }

    return result;
  }

  /**
   * Setting related values from sourceRow to targetRow using view data information. The targetRow
   * has index of field to targetColumn where targetInfo view data information has references of
   * relations of data source systems like as RDBMS. Depending of relations of data sources
   * using targetInfo and sourceInfo data view information mappings, sets to targetRow values
   * from sourceRow.
   *
   * @param targetInfo targetRow data information about target view.
   * @param targetColumn column of target row, where sets related values from sourceRow.
   * @param targetRow set of values where affected by related sourceRow values.
   * @param sourceInfo sourceRow data information about source view.
   * @param sourceRow set of values where contains related data for set to sourceRow.
   * @param updateRelationColumn if source row is main (first level) data set, using this
   *                             parameter, targetColumn value will be replaced own source ID
   *                             value using sourceRow.getId().
   * @return set of targetRow columns, where was effected from source row.
   */
  public static Collection<String> updateRow(DataInfo targetInfo, String targetColumn,
      IsRow targetRow, DataInfo sourceInfo, IsRow sourceRow, boolean updateRelationColumn) {

    Set<String> result = new HashSet<>();
    if (targetInfo == null || sourceInfo == null || BeeUtils.isEmpty(targetColumn)
        || targetRow == null) {
      return result;
    }

    boolean clear = sourceRow == null;

    if (updateRelationColumn) {
      int index = targetInfo.getColumnIndex(targetColumn);
      if (BeeConst.isUndef(index)) {
        logger.warning(targetInfo.getViewName(), targetColumn, "column not found");
      } else {
        if (clear) {
          targetRow.clearCell(index);
        } else {
          targetRow.setValue(index, sourceRow.getId());
        }
        result.add(targetColumn);
      }
    }

    Collection<ViewColumn> targetColumns = targetInfo.getDescendants(targetColumn, false);
    if (targetColumns.isEmpty()) {
      return result;
    }

    int tcLevel = Math.max(targetInfo.getViewColumnLevel(targetColumn), 0);

    for (ViewColumn tc : targetColumns) {
      int targetIndex = targetInfo.getColumnIndex(tc.getName());
      if (BeeConst.isUndef(targetIndex)) {
        logger.warning(targetInfo.getViewName(), tc.getName(), "column not found");
        continue;
      }

      if (clear) {
        if (!targetRow.isNull(targetIndex)) {
          targetRow.clearCell(targetIndex);
          result.add(tc.getName());
        }

      } else {
        int sourceIndex = sourceInfo.getColumnIndexBySource(tc.getTable(), tc.getField(),
            tc.getLevel() + tcLevel - 1);
        if (!BeeConst.isUndef(sourceIndex)
            && !BeeUtils.equalsTrimRight(targetRow.getString(targetIndex),
                sourceRow.getString(sourceIndex))) {
          targetRow.setValue(targetIndex, sourceRow.getString(sourceIndex));
          result.add(tc.getName());
        }
      }
    }
    return result;
  }

  private static String setCurrencyName(DataInfo dataInfo, IsRow row, String currencyColumn,
      String currencyName) {

    if (dataInfo == null || row == null || BeeUtils.isEmpty(currencyColumn)) {
      return null;
    }

    Collection<ViewColumn> descendants = dataInfo.getDescendants(currencyColumn, false);
    if (descendants.isEmpty()) {
      return null;
    }

    for (ViewColumn vc : descendants) {
      if (BeeUtils.same(vc.getField(), AdministrationConstants.COL_CURRENCY_NAME)) {
        int index = dataInfo.getColumnIndex(vc.getName());
        if (!BeeConst.isUndef(index)) {
          row.setValue(index, currencyName);
          return vc.getName();
        }
      }
    }
    return null;
  }

  private static final BeeLogger logger = LogUtils.getLogger(RelationUtils.class);

  private RelationUtils() {
  }
}
