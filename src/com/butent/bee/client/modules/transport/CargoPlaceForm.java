package com.butent.bee.client.modules.transport;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.UserInterface;

import java.util.Objects;

public class CargoPlaceForm extends AbstractFormInterceptor {

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (!Objects.equals(BeeKeeper.getScreen().getUserInterface(), UserInterface.SELF_SERVICE)) {
      Widget dateQst = form.getWidgetByName(VAR_DATE_QST);

      if (dateQst != null) {
        dateQst.setVisible(false);
      }
    }

    super.beforeRefresh(form, row);
  }

  @Override
  public String getCaption() {
    if (Objects.equals(getViewName(), TBL_CARGO_LOADING)) {
      return Localized.dictionary().cargoLoadingPlace();
    } else if (Objects.equals(getViewName(), TBL_CARGO_UNLOADING)) {
      return Localized.dictionary().cargoUnloadingPlace();
    } else {
      return super.getCaption();
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new CargoPlaceForm();
  }
}
