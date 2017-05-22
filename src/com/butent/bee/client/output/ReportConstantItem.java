package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Objects;

public class ReportConstantItem extends ReportNumericItem {

  private InputNumber expressionWidget;

  public ReportConstantItem(Number constant, String caption) {
    super(BeeUtils.randomString(10), caption);
    setExpression(constant != null ? constant.toString() : BeeConst.STRING_ZERO);
  }

  @Override
  public void deserialize(String data) {
  }

  @Override
  public ResultValue evaluate(SimpleRow row, Dictionary dictionary) {
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
      expressionWidget = new InputNumber();
    }
    expressionWidget.setValue(getExpression());
    return expressionWidget;
  }

  @Override
  public String getFormatedCaption() {
    return BeeUtils.notEmpty(getCaption(), getExpression());
  }

  @Override
  public InputSpinner getOptionsWidget() {
    return null;
  }

  @Override
  public String saveOptions() {
    if (BeeUtils.isEmpty(expressionWidget.getValue())) {
      return Localized.dictionary().valueRequired();
    }
    setExpression(expressionWidget.getValue());
    return null;
  }

  @Override
  public String serialize() {
    return serialize(null);
  }
}
