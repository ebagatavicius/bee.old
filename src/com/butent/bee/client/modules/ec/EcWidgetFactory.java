package com.butent.bee.client.modules.ec;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.CustomSpan;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.utils.BeeUtils;

public final class EcWidgetFactory {

  private static final String STYLE_FIELD_CONTAINER = "-container";
  private static final String STYLE_FIELD_LABEL = "-label";

  public static Widget createStockWidget(double stock) {
    InlineLabel widget = new InlineLabel();
    widget.getElement().setInnerText(EcKeeper.formatStock(stock));

    DomUtils.setDataProperty(widget.getElement(), EcConstants.DATA_ATTRIBUTE_STOCK, stock);
    EcStyles.markStock(widget.getElement());

    return widget;
  }

  public static Widget renderField(String label, String value, String styleName) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    }
    Assert.notEmpty(styleName);

    Flow container = new Flow(styleName + STYLE_FIELD_CONTAINER);

    if (!BeeUtils.isEmpty(label)) {
      CustomSpan labelWidget = new CustomSpan(styleName + STYLE_FIELD_LABEL);
      labelWidget.setHtml(label);
      container.add(labelWidget);
    }

    CustomSpan valueWidget = new CustomSpan(styleName);
    valueWidget.setHtml(value);
    container.add(valueWidget);

    return container;
  }

  private EcWidgetFactory() {
  }
}
