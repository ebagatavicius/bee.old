package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Objects;

public class ReportTextConstantItem extends ReportTextItem {

  private InputText expressionWidget;

  public ReportTextConstantItem(String constant, String caption) {
    super(BeeUtils.randomString(10), caption);
    setExpression(constant != null ? constant : BeeConst.STRING_SPACE);
  }

  @Override
  public ResultValue evaluate(SimpleRowSet.SimpleRow row, Dictionary dictionary) {
    return ResultValue.of(getExpression());
  }

  @Override
  public String getCaption() {
    String cap = super.getCaption();

    if (Objects.equals(cap, getExpression())) {
      cap = BeeConst.STRING_EMPTY;
    }
    return cap;
  }

  @Override
  public String getExpressionCaption() {
    return Localized.dictionary().constant();
  }

  @Override
  public Widget getExpressionWidget(List<ReportItem> reportItems) {
    if (expressionWidget == null) {
      expressionWidget = new InputText();
    }
    expressionWidget.setValue(getExpression());
    return expressionWidget;
  }

  @Override
  public Widget getFilterWidget() {
    return null;
  }

  @Override
  public String getFormatedCaption() {
    return BeeUtils.notEmpty(getCaption(), getExpression());
  }

  @Override
  public Widget getOptionsWidget() {
    return null;
  }

  @Override
  public String saveOptions() {
    setExpression(expressionWidget.getValue());
    return null;
  }
}
