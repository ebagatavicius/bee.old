package com.butent.bee.client.modules.transport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.modules.administration.AdministrationUtils;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.TranspRegStatus;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashMap;
import java.util.Map;

class TransportRegistrationForm extends AbstractFormInterceptor {

  private Button registerCommand;
  private Button blockCommand;

  TransportRegistrationForm() {
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    TranspRegStatus status;
    if (DataUtils.hasId(row)) {
      status = EnumUtils.getEnumByIndex(TranspRegStatus.class,
          Data.getInteger(getViewName(), row, COL_REGISTRATION_STATUS));
    } else {
      status = null;
    }

    refreshCommands(status);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TransportRegistrationForm();
  }

  private void onBlock() {
    String host = getStringValue(COL_REGISTRATION_HOST);
    if (BeeUtils.isEmpty(host)) {
      return;
    }

    String caption = Localized.getConstants().ipBlockCommand();
    AdministrationUtils.blockHost(caption, host, getFormView(), new Callback<String>() {
      @Override
      public void onSuccess(String result) {
        if (getFormView().isInteractive()) {
          getHeaderView().clearCommandPanel();
        }
        updateStatus(TranspRegStatus.REJECTED);
      }
    });
  }

  private void onCreateUser() {
    String email = BeeUtils.trim(getStringValue(COL_REGISTRATION_EMAIL));
    if (BeeUtils.isEmpty(email)) {
      return;
    }

    String login = BeeUtils.notEmpty(BeeUtils.getPrefix(email, BeeConst.CHAR_AT), email);

    Map<String, String> parameters = new HashMap<>();
    parameters.put(ClassifierConstants.COL_EMAIL, email);

    putUserField(parameters, COL_REGISTRATION_COMPANY_NAME, ClassifierConstants.ALS_COMPANY_NAME);
    putUserField(parameters, COL_REGISTRATION_COMPANY_CODE, ClassifierConstants.ALS_COMPANY_CODE);
    putUserField(parameters, COL_REGISTRATION_VAT_CODE, ClassifierConstants.COL_COMPANY_VAT_CODE);
    putUserField(parameters, COL_REGISTRATION_EXCHANGE_CODE,
        ClassifierConstants.COL_COMPANY_EXCHANGE_CODE);

    String contact = BeeUtils.trim(getStringValue(COL_REGISTRATION_CONTACT));
    if (!BeeUtils.isEmpty(contact)) {
      int p = contact.lastIndexOf(BeeConst.CHAR_SPACE);
      if (p > 0) {
        parameters.put(ClassifierConstants.COL_FIRST_NAME, contact.substring(0, p).trim());
        parameters.put(ClassifierConstants.COL_LAST_NAME, contact.substring(p + 1).trim());
      } else {
        parameters.put(ClassifierConstants.COL_FIRST_NAME, contact);
      }
    }
    putUserField(parameters, COL_REGISTRATION_CONTACT_POSITION, ClassifierConstants.COL_POSITION);

    putUserField(parameters, COL_REGISTRATION_ADDRESS, ClassifierConstants.COL_ADDRESS);
    putUserField(parameters, COL_REGISTRATION_CITY, ClassifierConstants.COL_CITY);
    putUserField(parameters, COL_REGISTRATION_COUNTRY, ClassifierConstants.COL_COUNTRY);

    putUserField(parameters, COL_REGISTRATION_PHONE, ClassifierConstants.COL_PHONE);
    putUserField(parameters, COL_REGISTRATION_MOBILE, ClassifierConstants.COL_MOBILE);
    putUserField(parameters, COL_REGISTRATION_FAX, ClassifierConstants.COL_FAX);

    String caption = Localized.getConstants().trCommandCreateNewUser();
    AdministrationUtils.createUser(caption, login, null, UserInterface.SELF_SERVICE, parameters,
        getFormView(), new IdCallback() {
          @Override
          public void onSuccess(Long result) {
            if (getFormView().isInteractive()) {
              getHeaderView().clearCommandPanel();
            }
            updateStatus(TranspRegStatus.CONFIRMED);
          }
        });
  }

  private void putUserField(Map<String, String> parameters, String source, String destination) {
    String value = getStringValue(source);
    if (!BeeUtils.isEmpty(value)) {
      parameters.put(destination, value.trim());
    }
  }

  private void refreshCommands(TranspRegStatus status) {
    HeaderView header = getHeaderView();
    if (header == null) {
      return;
    }

    if (header.hasCommands()) {
      header.clearCommandPanel();
    }

    if (status == null) {
      return;
    }

    if (status == TranspRegStatus.NEW) {
      if (Data.isViewEditable(AdministrationConstants.VIEW_USERS)) {
        if (this.registerCommand == null) {
          this.registerCommand =
              new Button(Localized.getConstants().trCommandCreateNewUser(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  onCreateUser();
                }
              });
        }
        header.addCommandItem(this.registerCommand);
      }

      if (!BeeUtils.isEmpty(getStringValue(COL_REGISTRATION_HOST))
          && Data.isViewEditable(AdministrationConstants.VIEW_IP_FILTERS)) {
        if (this.blockCommand == null) {
          this.blockCommand =
              new Button(Localized.getConstants().ipBlockCommand(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  onBlock();
                }
              });
        }
        header.addCommandItem(this.blockCommand);
      }
    }
  }

  private void updateStatus(final TranspRegStatus status) {
    SelfServiceUtils.updateStatus(getFormView(), COL_REGISTRATION_STATUS, status);
  }
}
