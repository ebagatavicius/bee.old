package com.butent.bee.client.modules.administration;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

class UserForm extends AbstractFormInterceptor implements ClickHandler {
  @Override
  public void afterRefresh(FormView form, IsRow row) {
    getHeaderView().clearCommandPanel();

    if (DataUtils.isNewRow(row) || Objects.equals(BeeKeeper.getUser().getUserId(), row.getId())
        || BeeKeeper.getUser().isAdministrator()) {
      getHeaderView().addCommandItem(new Button(Localized.getConstants().changePassword(), this));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new UserForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    changePassword();
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    if (BeeUtils.isEmpty(getStringValue(COL_PASSWORD))) {
      event.consume();
      changePassword();
    }
  }

  private void changePassword() {
    PasswordService.changePassword(getFormView());
  }
}