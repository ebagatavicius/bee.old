package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public class ReportConstantItem extends ReportNumericItem {

  private InputNumber expressionWidget;

  public ReportConstantItem(Number constant, String caption) {
    super(BeeUtils.randomString(10), caption);
    setExpression(constant != null ? constant.toString() : "0");
  }

  @Override
  public void deserialize(String data) {
  }

  @Override
  public ReportValue evaluate(SimpleRow row) {
    return ReportValue.of(getExpression());
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
  public Widget getExpressionWidget(Report report) {
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
  public String getOptionsCaption() {
    return null;
  }

  @Override
  public InputSpinner getOptionsWidget() {
    return null;
  }

  @Override
  public String saveOptions() {
    if (BeeUtils.isEmpty(expressionWidget.getValue())) {
      return Localized.getConstants().valueRequired();
    }
    setExpression(expressionWidget.getValue());
    return null;
  }

  @Override
  public String serialize() {
    return serialize(null);
  }
}
