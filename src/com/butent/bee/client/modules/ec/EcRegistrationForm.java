package com.butent.bee.client.modules.ec;

import com.google.common.collect.Maps;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.modules.administration.AdministrationUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Map;

class EcRegistrationForm extends AbstractFormInterceptor {

  private Button registerCommand;
  private Button blockCommand;

  EcRegistrationForm() {
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    refreshCommands(row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new EcRegistrationForm();
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
      }
    });
  }

  private void onCreateUser() {
    String email = BeeUtils.trim(getStringValue(COL_REGISTRATION_EMAIL));
    if (BeeUtils.isEmpty(email)) {
      notifyRequired(Localized.getConstants().email());
      return;
    }

    String firstName = getStringValue(COL_REGISTRATION_FIRST_NAME);
    if (BeeUtils.isEmpty(firstName)) {
      notifyRequired(Localized.getConstants().ecClientFirstName());
      return;
    }

    String companyName = getStringValue(COL_REGISTRATION_COMPANY_NAME);
    if (BeeUtils.isEmpty(companyName)) {
      companyName = BeeUtils.joinWords(firstName, getStringValue(COL_REGISTRATION_LAST_NAME));
    }

    final EcClientType type = EnumUtils.getEnumByIndex(EcClientType.class,
        getIntegerValue(COL_REGISTRATION_TYPE));
    if (type == null) {
      notifyRequired(Localized.getConstants().ecClientType());
      return;
    }

    final Long branch = getLongValue(COL_REGISTRATION_BRANCH);
    if (!DataUtils.isId(branch)) {
      notifyRequired(Localized.getConstants().branch());
      return;
    }

    final String personCode = getStringValue(COL_REGISTRATION_PERSON_CODE);
    final String activity = getStringValue(COL_REGISTRATION_ACTIVITY);

    Map<String, String> userFields = Maps.newHashMap();
    userFields.put(ClassifierConstants.COL_EMAIL, email);
    userFields.put(ClassifierConstants.COL_FIRST_NAME, firstName.trim());
    userFields.put(ClassifierConstants.ALS_COMPANY_NAME, companyName.trim());

    putUserField(userFields, COL_REGISTRATION_COMPANY_CODE, ClassifierConstants.ALS_COMPANY_CODE);
    putUserField(userFields, COL_REGISTRATION_VAT_CODE, ClassifierConstants.COL_COMPANY_VAT_CODE);

    putUserField(userFields, COL_REGISTRATION_LAST_NAME, ClassifierConstants.COL_LAST_NAME);

    putUserField(userFields, COL_REGISTRATION_ADDRESS, ClassifierConstants.COL_ADDRESS);
    putUserField(userFields, COL_REGISTRATION_CITY, ClassifierConstants.COL_CITY);
    putUserField(userFields, COL_REGISTRATION_COUNTRY, ClassifierConstants.COL_COUNTRY);

    putUserField(userFields, COL_REGISTRATION_PHONE, ClassifierConstants.COL_PHONE);
    putUserField(userFields, COL_REGISTRATION_POST_INDEX, ClassifierConstants.COL_POST_INDEX);

    String login = BeeUtils.notEmpty(BeeUtils.getPrefix(email, BeeConst.CHAR_AT), email);
    final String password = BeeUtils.left(login, 1);

    final Integer locale = getIntegerValue(COL_REGISTRATION_LANGUAGE);

    String caption = Localized.getConstants().ecRegistrationCommandCreate();
    AdministrationUtils.createUser(caption, login, password, UserInterface.E_COMMERCE, userFields,
        getFormView(), new IdCallback() {
          @Override
          public void onSuccess(Long result) {
            if (getFormView().isInteractive()) {
              getHeaderView().clearCommandPanel();
            }

            ParameterList params = EcKeeper.createArgs(SVC_CREATE_CLIENT);
            params.addQueryItem(EcConstants.VAR_MAIL, 1);

            params.addDataItem(COL_CLIENT_USER, result);
            params.addDataItem(COL_CLIENT_TYPE, type.ordinal());
            params.addDataItem(COL_CLIENT_PRIMARY_BRANCH, branch);

            params.addNotEmptyData(COL_CLIENT_PERSON_CODE, personCode);
            params.addNotEmptyData(COL_CLIENT_ACTIVITY, activity);

            params.addDataItem(AdministrationConstants.COL_PASSWORD, password);
            params.addNotNullData(AdministrationConstants.COL_USER_LOCALE, locale);

            BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                EcKeeper.dispatchMessages(response);

                if (response.hasResponse(BeeRow.class)) {
                  DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_CLIENTS);
                  RowEditor.open(VIEW_CLIENTS, BeeRow.restore(response.getResponseAsString()),
                      Opener.MODAL);
                }
              }
            });
          }
        });
  }

  private void putUserField(Map<String, String> parameters, String source, String destination) {
    String value = getStringValue(source);
    if (!BeeUtils.isEmpty(value)) {
      parameters.put(destination, value.trim());
    }
  }

  private void refreshCommands(IsRow row) {
    HeaderView header = getHeaderView();
    if (header == null) {
      return;
    }

    if (header.hasCommands()) {
      header.clearCommandPanel();
    }

    if (DataUtils.hasId(row)) {
      if (Data.isViewEditable(AdministrationConstants.VIEW_USERS)) {
        if (this.registerCommand == null) {
          this.registerCommand =
              new Button(Localized.getConstants().ecRegistrationCommandCreate(),
                  new ClickHandler() {
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
}
