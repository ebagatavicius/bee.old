package com.butent.bee.client.modules.classifiers;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassifierSelector implements SelectorEvent.Handler {

  private final Map<String, Long> companyPersonSelectors = new HashMap<>();
  private final Map<String, String> personSelectors = new HashMap<>();

  ClassifierSelector() {
    super();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (BeeUtils.same(event.getRelatedViewName(), TBL_EMAILS)) {
      handleEmails(event);

    } else if (BeeUtils.same(event.getRelatedViewName(), VIEW_CITIES)) {
      handleCities(event);

    } else if (event.isNewRow()
        && BeeUtils.inListSame(event.getRelatedViewName(), VIEW_PERSONS, VIEW_COMPANY_PERSONS)) {
      handleNewPersons(event);

    } else if (BeeUtils.same(event.getRelatedViewName(), VIEW_COMPANY_PERSONS)) {
      if (event.isOpened() || event.isDataLoaded() || event.isUnloading()) {
        handleCompanyPersons(event);
      }

    } else if (BeeUtils.same(event.getRelatedViewName(), VIEW_PERSONS)) {
      String options = event.getSelector().getOptions();

      if (!BeeUtils.isEmpty(options)) {
        if (event.isOpened() || event.isDataLoaded() || event.isUnloading()) {
          handlePersons(event, options);
        }

      } else if (event.isOpened()) {
        DataView dataView = ViewHelper.getDataView(event.getSelector());

        if (dataView != null && VIEW_COMPANY_OBJECTS.equals(dataView.getViewName())) {
          if (TimeUtils.year() < 0) { // never
            filterPersonsByCompany(event, dataView);
          }
        }
      }

    } else if (event.isOpened() && event.hasRelatedView(VIEW_POSITIONS)) {
      DataView dataView = ViewHelper.getDataView(event.getSelector());
      if (dataView != null
          && AdministrationConstants.VIEW_DEPARTMENT_EMPLOYEES.equals(dataView.getViewName())) {
        filterDepartmentPositions(event, dataView);
      }
    }
  }

  private static void filterDepartmentPositions(SelectorEvent event, DataView dataView) {
    Long department = ViewHelper.getParentRowId(dataView.asWidget(),
        AdministrationConstants.VIEW_DEPARTMENTS);

    Filter filter;
    if (DataUtils.isId(department)) {
      filter = Filter.in(Data.getIdColumn(VIEW_POSITIONS),
          AdministrationConstants.VIEW_DEPARTMENT_POSITIONS, COL_POSITION,
          Filter.equals(AdministrationConstants.COL_DEPARTMENT, department));
    } else {
      filter = null;
    }

    event.getSelector().setAdditionalFilter(filter, true);
  }

  private static void filterPersonsByCompany(SelectorEvent event, DataView dataView) {
    Long company = ViewHelper.getParentRowId(dataView.asWidget(), VIEW_COMPANIES);

    Filter filter;
    if (DataUtils.isId(company)) {
      filter = Filter.in(Data.getIdColumn(VIEW_PERSONS), VIEW_COMPANY_PERSONS, COL_PERSON,
          Filter.equals(COL_COMPANY, company));
    } else {
      filter = Filter.isFalse();
    }

    event.getSelector().setAdditionalFilter(filter, true);
  }

  private static void handleCities(SelectorEvent event) {
    if (!event.isChanged()) {
      return;
    }

    DataInfo sourceInfo = Data.getDataInfo(event.getRelatedViewName());
    IsRow source = event.getRelatedRow();
    if (source == null) {
      return;
    }

    DataView dataView = ViewHelper.getDataView(event.getSelector());
    if (dataView == null || BeeUtils.isEmpty(dataView.getViewName())) {
      return;
    }

    DataInfo targetInfo = Data.getDataInfo(dataView.getViewName());
    IsRow target = dataView.getActiveRow();
    if (target == null) {
      return;
    }

    String targetColumn = BeeUtils.notEmpty(event.getSelector().getOptions(), COL_COUNTRY);
    int targetIndex = targetInfo.getColumnIndex(targetColumn);
    if (BeeConst.isUndef(targetIndex)) {
      return;
    }

    Long country = source.getLong(sourceInfo.getColumnIndex(COL_COUNTRY));
    if (country == null || country.equals(target.getLong(targetIndex))) {
      return;
    }

    if (dataView.isFlushable()) {
      target.setValue(targetIndex, country);
    } else {
      target.preliminaryUpdate(targetIndex, country.toString());
    }

    Collection<String> updatedColumns =
        RelationUtils.updateRow(targetInfo, targetColumn, target, sourceInfo, source, false);
    updatedColumns.add(targetColumn);

    for (String colName : updatedColumns) {
      dataView.refreshBySource(colName);
    }
  }

  private void handleCompanyPersons(SelectorEvent event) {
    String targetCompanyColumnName = event.getSelector().getOptions();
    if (BeeUtils.isEmpty(targetCompanyColumnName)) {
      return;
    }

    String selectorId = event.getSelector().getId();
    if (BeeUtils.isEmpty(selectorId)) {
      return;
    }

    if (event.isUnloading()) {
      removeCompanyPersonSelector(selectorId);
      return;
    }

    DataView dataView = ViewHelper.getDataView(event.getSelector());
    if (dataView == null) {
      removeCompanyPersonSelector(selectorId);
      return;
    }

    IsRow targetRow = dataView.getActiveRow();
    if (targetRow == null) {
      removeCompanyPersonSelector(selectorId);
      return;
    }

    int targetCompanyIndex = Data.getColumnIndex(dataView.getViewName(), targetCompanyColumnName);
    if (targetCompanyIndex < 0) {
      removeCompanyPersonSelector(selectorId);
      return;
    }

    Long company = targetRow.getLong(targetCompanyIndex);
    if (Objects.equals(company, companyPersonSelectors.get(selectorId))) {
      return;
    }

    if (event.isOpened()) {
      event.getSelector().getOracle().clearData();
      return;
    }

    if (event.isDataLoaded()) {
      if (company == null) {
        removeCompanyPersonSelector(selectorId);
        return;
      }

      companyPersonSelectors.put(selectorId, company);

      BeeRowSet rowSet = event.getSelector().getOracle().getViewData();
      if (rowSet == null || rowSet.getNumberOfRows() <= 1) {
        return;
      }

      int sourceCompanyIndex = Data.getColumnIndex(rowSet.getViewName(), COL_COMPANY);
      if (targetCompanyIndex < 0) {
        return;
      }

      List<BeeRow> companyRows = new ArrayList<>();

      for (Iterator<BeeRow> it = rowSet.getRows().iterator(); it.hasNext();) {
        BeeRow row = it.next();

        if (company.equals(row.getLong(sourceCompanyIndex))) {
          companyRows.add(row);
          it.remove();
        }
      }

      if (!companyRows.isEmpty()) {
        if (rowSet.isEmpty()) {
          rowSet.addRows(companyRows);
        } else {
          companyRows.addAll(rowSet.getRows());
          rowSet.setRows(companyRows);
        }
      }
    }
  }

  private static void handleEmails(SelectorEvent event) {
    if (event.isNewRow() && !BeeUtils.isEmpty(event.getDefValue())) {
      Data.setValue(TBL_EMAILS, event.getNewRow(), COL_EMAIL_ADDRESS, event.getDefValue());
      event.setDefValue(null);
    }
  }

  private static void handleNewPersons(SelectorEvent event) {
    if (event.isNewRow() && !BeeUtils.isEmpty(event.getDefValue())) {
      String firstName = null;
      String lastName = null;

      for (String val : Splitter.on(BeeConst.CHAR_SPACE).trimResults().omitEmptyStrings().limit(2)
          .split(event.getDefValue())) {
        if (BeeUtils.isEmpty(firstName)) {
          firstName = val;
        } else {
          lastName = val;
        }
      }

      if (!BeeUtils.isEmpty(firstName)) {
        Data.setValue(event.getRelatedViewName(), event.getNewRow(), COL_FIRST_NAME, firstName);
      }
      if (!BeeUtils.isEmpty(lastName)) {
        Data.setValue(event.getRelatedViewName(), event.getNewRow(), COL_LAST_NAME, lastName);
      }

      event.setDefValue(null);
    }
  }

  private void handlePersons(SelectorEvent event, String companySelectorName) {
    String selectorId = event.getSelector().getId();
    if (BeeUtils.isEmpty(selectorId)) {
      return;
    }

    if (event.isUnloading()) {
      removePersonSelector(selectorId);
      return;
    }

    FormView form = ViewHelper.getForm(event.getSelector());
    if (form == null) {
      removePersonSelector(selectorId);
      return;
    }

    Widget companySelector = form.getWidgetByName(companySelectorName);
    if (!(companySelector instanceof DataSelector)) {
      removePersonSelector(selectorId);
      return;
    }

    String companyValue = ((DataSelector) companySelector).getValue();

    if (Objects.equals(companyValue, personSelectors.get(selectorId))) {
      return;
    }

    if (event.isOpened()) {
      event.getSelector().getOracle().clearData();
      return;
    }

    if (event.isDataLoaded()) {
      List<Long> companyIds = DataUtils.parseIdList(companyValue);
      if (companyIds.isEmpty()) {
        removePersonSelector(selectorId);
        return;
      }

      personSelectors.put(selectorId, companyValue);

      BeeRowSet rowSet = event.getSelector().getOracle().getViewData();
      if (rowSet == null || rowSet.getNumberOfRows() <= 1) {
        return;
      }

      Multimap<Long, BeeRow> filteredRows = ArrayListMultimap.create();

      for (Iterator<BeeRow> it = rowSet.iterator(); it.hasNext();) {
        BeeRow row = it.next();

        String value = row.getProperty(PROP_COMPANY_IDS);
        if (!BeeUtils.isEmpty(value)) {
          Set<Long> values = DataUtils.parseIdSet(value);

          for (Long id : companyIds) {
            if (values.contains(id)) {
              filteredRows.put(id, row);
              it.remove();
              break;
            }
          }
        }
      }

      if (!filteredRows.isEmpty()) {
        List<BeeRow> rows = new ArrayList<>();
        for (Long id : companyIds) {
          if (filteredRows.containsKey(id)) {
            rows.addAll(filteredRows.get(id));
          }
        }

        if (rowSet.isEmpty()) {
          rowSet.addRows(rows);
        } else {
          rows.addAll(rowSet.getRows());
          rowSet.setRows(rows);
        }
      }
    }
  }

  private void removeCompanyPersonSelector(String id) {
    if (companyPersonSelectors.containsKey(id)) {
      companyPersonSelectors.remove(id);
    }
  }

  private void removePersonSelector(String id) {
    if (personSelectors.containsKey(id)) {
      personSelectors.remove(id);
    }
  }
}
