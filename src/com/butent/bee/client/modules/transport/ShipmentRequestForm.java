package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.modules.administration.AdministrationUtils;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.CargoRequestStatus;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ShipmentRequestForm extends AbstractFormInterceptor {

  private Button activateCommand;
  private Button blockCommand;

  ShipmentRequestForm() {
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    CargoRequestStatus status;
    if (DataUtils.hasId(row)) {
      status = EnumUtils.getEnumByIndex(CargoRequestStatus.class,
          Data.getInteger(getViewName(), row, COL_QUERY_STATUS));
    } else {
      status = null;
    }

    refreshCommands(status);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ShipmentRequestForm();
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    SelfServiceUtils.setDefaultExpeditionType(form, newRow, COL_QUERY_EXPEDITION);
    SelfServiceUtils.setDefaultShippingTerm(form, newRow, COL_CARGO_SHIPPING_TERM);

    super.onStartNewRow(form, oldRow, newRow);
  }

  private void onBlock() {
    String host = getStringValue(COL_QUERY_HOST);
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
        updateStatus(CargoRequestStatus.REJECTED);
      }
    });
  }

  private void onCreateOrder() {
    Global.confirm(Localized.getConstants().trConfirmCreateNewOrder(), new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        String companyName = getStringValue(COL_QUERY_CUSTOMER_NAME);
        if (BeeUtils.isEmpty(companyName)) {
          return;
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put(COL_COMPANY_NAME, companyName);

        putField(parameters, COL_QUERY_CUSTOMER_CODE, COL_COMPANY_CODE);
        putField(parameters, COL_QUERY_CUSTOMER_VAT_CODE, COL_COMPANY_VAT_CODE);
        putField(parameters, COL_QUERY_CUSTOMER_EXCHANGE_CODE, COL_COMPANY_EXCHANGE_CODE);

        putField(parameters, COL_QUERY_CUSTOMER_EMAIL, COL_EMAIL);
        putField(parameters, COL_QUERY_CUSTOMER_ADDRESS, COL_ADDRESS);
        putField(parameters, COL_QUERY_CUSTOMER_PHONE, COL_PHONE);

        ClassifierUtils.createCompany(parameters, getFormView(), new IdCallback() {
          @Override
          public void onSuccess(Long company) {
            List<String> colNames = Lists.newArrayList(COL_CUSTOMER);
            List<String> values = Queries.asList(company);

            String manager = getStringValue(COL_QUERY_MANAGER);
            if (!BeeUtils.isEmpty(manager)) {
              colNames.add(COL_ORDER_MANAGER);
              values.add(manager);
            }

            List<BeeColumn> columns = Data.getColumns(VIEW_ORDERS, colNames);

            Queries.insert(VIEW_ORDERS, columns, values, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                getActiveRow().setValue(getDataIndex(COL_ORDER), result.getId());

                CargoRequestStatus status = CargoRequestStatus.ACTIVE;
                updateStatus(status);
                refreshCommands(status);

                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDERS);
              }
            });
          }
        });
      }
    });
  }

  private void putField(Map<String, String> parameters, String source, String destination) {
    String value = getStringValue(source);
    if (!BeeUtils.isEmpty(value)) {
      parameters.put(destination, value.trim());
    }
  }

  private void refreshCommands(CargoRequestStatus status) {
    HeaderView header = getHeaderView();
    if (header == null) {
      return;
    }

    if (header.hasCommands()) {
      header.clearCommandPanel();
    }

    if (status == CargoRequestStatus.NEW) {
      if (Data.isViewEditable(VIEW_ORDERS)) {
        if (this.activateCommand == null) {
          this.activateCommand =
              new Button(Localized.getConstants().trCommandCreateNewOrder(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  onCreateOrder();
                }
              });
        }
        header.addCommandItem(this.activateCommand);
      }

      if (!BeeUtils.isEmpty(getStringValue(COL_QUERY_HOST))
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

  private void updateStatus(CargoRequestStatus status) {
    SelfServiceUtils.updateStatus(getFormView(), COL_QUERY_STATUS, status);
  }
}
