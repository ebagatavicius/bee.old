package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

class ProjectStageForm extends AbstractFormInterceptor {

  private InputText wActualTasksDuration;
  private InputText wExpectedTasksDuration;
  private BeeRowSet timeUnits;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    switch (name) {
      case COL_ACTUAL_TASKS_DURATION:
        if (widget instanceof InputText) {
          wActualTasksDuration = (InputText) widget;
        }
        break;
      case COL_EXPECTED_TASKS_DURATION:
        if (widget instanceof InputText) {
          wExpectedTasksDuration = (InputText) widget;
        }
      break;
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (!BeeUtils.isEmpty(row.getProperty(PROP_TIME_UNTIS))) {
      String prop = row.getProperty(PROP_TIME_UNTIS);
      BeeRowSet unitsRows = BeeRowSet.maybeRestore(prop);
      setTimeUnits(unitsRows);
    }

    if (getTimeUnits() != null) {
      showComputedTimes(form, row);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new ProjectStageForm();
  }

  private BeeRowSet getTimeUnits() {
    return timeUnits;
  }

  private void setTimeUnits(BeeRowSet timeUnits) {
    this.timeUnits = timeUnits;
  }

  private void showComputedTimes(FormView form, IsRow row) {
    if (form == null) {
      return;
    }

    if (row == null) {
      return;
    }

    int idxExpTD = form.getDataIndex(COL_EXPECTED_TASKS_DURATION);
    int idxActTD = form.getDataIndex(COL_ACTUAL_TASKS_DURATION);
    int idxUnit = form.getDataIndex(COL_PROJECT_TIME_UNIT);

    double factor = BeeConst.DOUBLE_ONE;
    String unitName = BeeConst.STRING_EMPTY;

    if (!BeeConst.isUndef(idxUnit) && getTimeUnits() != null) {
      long idValue = row.getLong(idxUnit);
      BeeRow unitRow = getTimeUnits().getRowById(idValue);

      if (unitRow != null) {
        String prop = unitRow.getProperty(PROP_REAL_FACTOR);

        if (!BeeUtils.isEmpty(prop) && BeeUtils.isDouble(prop)) {
          factor = BeeUtils.toDouble(prop);
        }

        int idxName = getTimeUnits().getColumnIndex(ClassifierConstants.COL_UNIT_NAME);

        if (!BeeConst.isUndef(idxName)) {
          unitName = unitRow.getString(idxName);
        }
      }
    }

    if (wExpectedTasksDuration != null && !BeeConst.isUndef(idxExpTD)) {
      long value = BeeUtils.unbox(row.getLong(idxExpTD));
      wExpectedTasksDuration.setValue(BeeConst.STRING_EMPTY);

      if (factor == BeeConst.DOUBLE_ONE) {
        wExpectedTasksDuration.setText(TimeUtils.renderMinutes(BeeUtils.toInt(value
            / TimeUtils.MILLIS_PER_MINUTE), true));
      } else {
        long factorMls = BeeUtils.toLong(factor * TimeUtils.MILLIS_PER_HOUR);

        int calcValue = BeeUtils.toInt(value / factorMls);
        long decValue = value % factorMls;

        wExpectedTasksDuration.setText(BeeUtils.joinWords(calcValue, unitName, decValue != 0
            ? TimeUtils
                .renderMinutes(
                    BeeUtils.toInt(decValue
                        / TimeUtils.MILLIS_PER_MINUTE), true) : BeeConst.STRING_EMPTY));
      }
    }

    if (wActualTasksDuration != null && !BeeConst.isUndef(idxActTD)) {
      long value = BeeUtils.unbox(row.getLong(idxActTD));
      wActualTasksDuration.setValue(BeeConst.STRING_EMPTY);

      if (factor == BeeConst.DOUBLE_ONE) {
        wActualTasksDuration.setText(TimeUtils.renderMinutes(BeeUtils.toInt(value
            / TimeUtils.MILLIS_PER_MINUTE), true));
      } else {
        long factorMls = BeeUtils.toLong(factor * TimeUtils.MILLIS_PER_HOUR);

        int calcValue = BeeUtils.toInt(value / factorMls);
        long decValue = value % factorMls;

        wActualTasksDuration.setText(BeeUtils.joinWords(calcValue, unitName, decValue != 0
            ? TimeUtils
                .renderMinutes(
                    BeeUtils.toInt(decValue
                        / TimeUtils.MILLIS_PER_MINUTE), true) : BeeConst.STRING_EMPTY));
      }
    }
  }
}
