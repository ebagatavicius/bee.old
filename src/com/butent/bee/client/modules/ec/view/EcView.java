package com.butent.bee.client.modules.ec.view;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.ec.EcConstants.COL_CONFIG_CONTACTS_HTML;
import static com.butent.bee.shared.modules.ec.EcConstants.COL_CONFIG_CONTACTS_URL;
import static com.butent.bee.shared.modules.ec.EcConstants.COL_CONFIG_TOD_HTML;
import static com.butent.bee.shared.modules.ec.EcConstants.COL_CONFIG_TOD_URL;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants;

public abstract class EcView extends Flow {

  public static EcView create(String service) {
    EcView ecView = null;

    if (EcConstants.SVC_FINANCIAL_INFORMATION.equals(service)) {
      ecView = new FinancialInformation();

    } else if (EcConstants.SVC_SHOW_TERMS_OF_DELIVERY.equals(service)) {
      ecView = new HtmlViewer(Localized.getConstants().ecTermsOfDelivery(), COL_CONFIG_TOD_URL,
          COL_CONFIG_TOD_HTML);
    } else if (EcConstants.SVC_SHOW_CONTACTS.equals(service)) {
      ecView = new HtmlViewer(Localized.getConstants().ecContacts(), COL_CONFIG_CONTACTS_URL,
          COL_CONFIG_CONTACTS_HTML);

    } else if (EcConstants.SVC_SEARCH_BY_ITEM_CODE.equals(service)) {
      ecView = new SearchByItem(service, Localized.getConstants().ecItemCode());
    } else if (EcConstants.SVC_SEARCH_BY_OE_NUMBER.equals(service)) {
      ecView = new SearchByItem(service, Localized.getConstants().ecItemOeNumber());

    } else if (EcConstants.SVC_SEARCH_BY_CAR.equals(service)) {
      ecView = new SearchByCar();
    } else if (EcConstants.SVC_SEARCH_BY_BRAND.equals(service)) {
      ecView = new SearchByBrand();

    } else if (EcConstants.SVC_GENERAL_ITEMS.equals(service)) {
      ecView = new SearchByGroup(false);
    } else if (EcConstants.SVC_BIKE_ITEMS.equals(service)) {
      ecView = new SearchByGroup(true);
    }

    if (ecView != null) {
      ecView.createUi();
    }
    return ecView;
  }

  protected EcView() {
    super(EcStyles.name("View"));
    EcStyles.add(this, getPrimaryStyle());
  }

  protected abstract void createUi();

  protected abstract String getPrimaryStyle();

  @Override
  protected void onLoad() {
    super.onLoad();
    UiHelper.focus(this);
  }

  protected Widget renderNoData(String key) {
    Label label = new Label(Localized.getMessages().dataNotAvailable(key));
    EcStyles.add(label, "noData");
    return label;
  }
}
