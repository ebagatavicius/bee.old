package com.butent.bee.client.modules.classifiers;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;

class CompanyPersonForm extends AbstractFormInterceptor {
  private static final String QR_FLOW_PANEL = "qrFlowPanel";

  @Override
  public FormInterceptor getInstance() {
    return new CompanyPersonForm();
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (!DataUtils.isNewRow(row)) {
      createQrButton(form, row);
    }
    super.afterRefresh(form, row);
  }

  public static void createQrButton(final FormView form, final IsRow row) {
    FlowPanel qrFlowPanel = (FlowPanel) Assert.notNull(form.getWidgetByName(QR_FLOW_PANEL));
    qrFlowPanel.clear();
    FaLabel qrCodeLabel = new FaLabel(FontAwesome.QRCODE);
    qrCodeLabel.setTitle(Localized.getConstants().qrCode());
    qrCodeLabel.addStyleName("bee-FontSize-x-large");
    qrFlowPanel.add(qrCodeLabel);
    qrCodeLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        ClassifierKeeper.generateQrCode(form, row);
      }
    });
  }
}