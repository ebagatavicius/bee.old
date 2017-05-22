package com.butent.bee.client.modules.classifiers;

import com.google.common.base.Splitter;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ClassifierSelector implements SelectorEvent.Handler {

  private static void filterDepartmentPositions(SelectorEvent event, DataView dataView) {
    Long department = ViewHelper.getParentRowId(dataView.asWidget(), VIEW_DEPARTMENTS);

    Filter filter;
    if (DataUtils.isId(department)) {
      filter = Filter.in(Data.getIdColumn(VIEW_POSITIONS), VIEW_DEPARTMENT_POSITIONS, COL_POSITION,
          Filter.equals(COL_DEPARTMENT, department));
    } else {
      filter = null;
    }

    event.getSelector().setAdditionalFilter(filter, filter != null);
  }

  private static Long getOptionsValue(String columnName, SelectorEvent event) {
    DataView dataView = ViewHelper.getDataView(event.getSelector());
    if (dataView == null) {
      return null;
    }

    IsRow row = dataView.getActiveRow();
    if (row == null) {
      return null;
    }

    int index = Data.getColumnIndex(dataView.getViewName(), columnName);
    if (index < 0) {
      return null;
    }

    return row.getLong(index);
  }

  private static void handleCities(SelectorEvent event) {
    if (!event.isChangePending()) {
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

    if (!Objects.equals(targetInfo.getColumnType(targetColumn), ValueType.LONG)) {
      String countryName = source.getString(sourceInfo.getColumnIndex(ALS_COUNTRY_NAME));

      if (dataView.isFlushable()) {
        target.setValue(targetIndex, countryName);
      } else {
        target.preliminaryUpdate(targetIndex, countryName);
      }
      dataView.refreshBySource(targetColumn);
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

  private static void handleEmails(SelectorEvent event) {
    if (event.isNewRow() && !BeeUtils.isEmpty(event.getDefValue())) {
      Data.setValue(TBL_EMAILS, event.getNewRow(), COL_EMAIL_ADDRESS, event.getDefValue());
      event.setDefValue(null);
    }
  }

  private static void onNewCompanyPerson(SelectorEvent event) {
    String columnName = event.getSelector().getOptions();

    if (!BeeUtils.isEmpty(columnName)) {
      Long company = getOptionsValue(columnName, event);

      if (DataUtils.isId(company)) {
        DataInfo targetInfo = Data.getDataInfo(event.getRelatedViewName());
        IsRow targetRow = event.getNewRow();

        DataView sourceView = ViewHelper.getDataView(event.getSelector());

        if (targetInfo != null && targetRow != null && sourceView != null) {
          DataInfo sourceInfo = Data.getDataInfo(sourceView.getViewName());
          IsRow sourceRow = sourceView.getActiveRow();

          Data.setValue(event.getRelatedViewName(), targetRow, COL_COMPANY, company);
          RelationUtils.updateRow(targetInfo, COL_COMPANY, targetRow, sourceInfo, sourceRow, false);
        }
      }
    }

    final String defValue = event.getDefValue();

    if (!BeeUtils.isEmpty(defValue)) {
      event.setDefValue(null);

      event.setOnOpenNewRow(formView -> {
        Widget widget = formView.getWidgetBySource(COL_PERSON);

        if (widget instanceof DataSelector) {
          final DataSelector personSelector = (DataSelector) widget;

          Scheduler.get().scheduleDeferred(() -> {
            personSelector.setFocus(true);
            personSelector.setDisplayValue(defValue);
            personSelector.startEdit(null, DataSelector.ASK_ORACLE, null, null);
          });
        }
      });
    }
  }

  private static void onNewPerson(SelectorEvent event) {
    if (!BeeUtils.isEmpty(event.getDefValue())) {
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

  private static void onOpenCompanyPersons(SelectorEvent event) {
    String columnName = event.getSelector().getOptions();

    if (!BeeUtils.isEmpty(columnName)) {
      Long company = getOptionsValue(columnName, event);

      Filter filter;
      if (company == null) {
        filter = null;
      } else {
        filter = Filter.equals(COL_COMPANY, company);
      }

      event.getSelector().setAdditionalFilter(filter, filter != null);
    }
  }

  private static void onOpenPersons(SelectorEvent event) {
    String companySelectorName = event.getSelector().getOptions();
    if (BeeUtils.isEmpty(companySelectorName)) {
      return;
    }

    FormView form = ViewHelper.getForm(event.getSelector());
    if (form == null) {
      return;
    }

    Widget companySelector = form.getWidgetByName(companySelectorName);
    if (!(companySelector instanceof DataSelector)) {
      return;
    }

    List<Long> companyIds;
    if (companySelector instanceof MultiSelector) {
      companyIds = ((MultiSelector) companySelector).getIds();
    } else {
      String companyValue = ((DataSelector) companySelector).getValue();
      companyIds = DataUtils.parseIdList(companyValue);
    }

    Filter filter;
    if (BeeUtils.isEmpty(companyIds)) {
      filter = null;
    } else {
      filter = Filter.in(Data.getIdColumn(VIEW_PERSONS), VIEW_COMPANY_PERSONS, COL_PERSON,
          Filter.any(COL_COMPANY, companyIds));
    }

    event.getSelector().setAdditionalFilter(filter, filter != null);
  }

  ClassifierSelector() {
    super();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    String viewName = event.getRelatedViewName();

    if (viewName != null) {
      switch (viewName) {
        case TBL_EMAILS:
          handleEmails(event);
          break;

        case VIEW_CITIES:
          handleCities(event);
          break;

        case VIEW_PERSONS:
          if (event.isOpened()) {
            onOpenPersons(event);
          } else if (event.isNewRow()) {
            onNewPerson(event);
          }
          break;

        case VIEW_COMPANY_PERSONS:
          if (event.isOpened()) {
            onOpenCompanyPersons(event);
          } else if (event.isNewRow()) {
            onNewCompanyPerson(event);
          }
          break;

        case VIEW_POSITIONS:
          if (event.isOpened()) {
            DataView dataView = ViewHelper.getDataView(event.getSelector());
            if (dataView != null && VIEW_DEPARTMENT_EMPLOYEES.equals(dataView.getViewName())) {
              filterDepartmentPositions(event, dataView);
            }
          }
          break;
      }
    }
  }
}
