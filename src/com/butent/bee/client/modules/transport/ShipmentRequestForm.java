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
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.modules.administration.AdministrationUtils;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.AssessmentStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.CargoRequestStatus;
import com.butent.bee.shared.ui.UserInterface;
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
    UserInterface fromInterface;
    if (DataUtils.hasId(row)) {
      status = EnumUtils.getEnumByIndex(CargoRequestStatus.class,
          Data.getInteger(getViewName(), row, COL_QUERY_STATUS));
      fromInterface = UserInterface.normalize(
          EnumUtils.getEnumByIndex(UserInterface.class, Data.getInteger(getViewName(), row,
              AdministrationConstants.COL_USER_INTERFACE)));
    } else {
      status = null;
      fromInterface = UserInterface.normalize(null);
    }

    refreshCommands(status, fromInterface);
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

  private void onCreateOrder(final String viewName,
      final Long departmentId, final UserInterface fromUI) {
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

            if (DataUtils.isId(departmentId) && !BeeUtils.isEmpty(viewName)) {
              colNames.add(AdministrationConstants.COL_DEPARTMENT);
              values.add(BeeUtils.toString(departmentId));
            }

            switch (viewName) {
              case VIEW_ASSESSMENTS:
                colNames.add(COL_STATUS);
                values.add(BeeUtils.toString(AssessmentStatus.NEW.ordinal()));

                colNames.add(ALS_LOADING_DATE);
                values.add(getStringValue(ALS_LOADING_DATE));

                colNames.add(ALS_UNLOADING_DATE);
                values.add(getStringValue(ALS_UNLOADING_DATE));

                colNames.add(ALS_LOADING_COMPANY);
                values.add(getStringValue(ALS_LOADING_COMPANY));

                colNames.add(ALS_UNLOADING_COMPANY);
                values.add(getStringValue(ALS_UNLOADING_COMPANY));

                colNames.add(ALS_LOADING_CONTACT);
                values.add(getStringValue(ALS_LOADING_CONTACT));

                colNames.add(ALS_UNLOADING_CONTACT);
                values.add(getStringValue(ALS_UNLOADING_CONTACT));

                colNames.add(ALS_LOADING_ADDRESS);
                values.add(getStringValue(ALS_LOADING_ADDRESS));

                colNames.add(ALS_UNLOADING_ADDRESS);
                values.add(getStringValue(ALS_UNLOADING_ADDRESS));

                colNames.add(ALS_LOADING_POST_INDEX);
                values.add(getStringValue(ALS_LOADING_POST_INDEX));

                colNames.add(ALS_UNLOADING_POST_INDEX);
                values.add(getStringValue(ALS_UNLOADING_POST_INDEX));

                colNames.add(COL_QUERY_LOADING_CITY);
                values.add(getStringValue(COL_QUERY_LOADING_CITY));

                colNames.add(COL_QUERY_UNLOADING_CITY);
                values.add(getStringValue(COL_QUERY_UNLOADING_CITY));

                colNames.add(COL_CARGO_WEIGHT);
                values
                    .add(Data.clamp(viewName, COL_CARGO_WEIGHT, getStringValue(COL_CARGO_WEIGHT)));
                break;

              default:
                break;
            }

            List<BeeColumn> columns = Data.getColumns(viewName, colNames);

            Queries.insert(viewName, columns, values, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                RowEditor.open(viewName, result, Opener.MODAL);

                switch (viewName) {
                  case VIEW_ORDERS:
                    getActiveRow().setValue(getDataIndex(COL_ORDER), result.getId());
                    break;
                  case VIEW_ASSESSMENTS:
                    getActiveRow().setValue(getDataIndex(COL_ORDER),
                        Data.getLong(viewName, result, COL_ORDER));
                    break;

                  default:
                    break;
                }

                CargoRequestStatus status = CargoRequestStatus.ACTIVE;
                updateStatus(status);
                refreshCommands(status, fromUI);

                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName);

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

  private void refreshCommands(CargoRequestStatus status, final UserInterface fromUI) {
    HeaderView header = getHeaderView();
    if (header == null) {
      return;
    }

    if (header.hasCommands()) {
      header.clearCommandPanel();
    }

    if (status == CargoRequestStatus.NEW) {
      if (Data.isViewEditable(VIEW_ORDERS)
          && (UserInterface.SELF_SERVICE.equals(fromUI) || fromUI == null)) {
        if (this.activateCommand == null) {
          this.activateCommand =
              new Button(Localized.getConstants().trCommandCreateNewOrder(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  onCreateOrder(VIEW_ORDERS, null, fromUI);
                }
              });
        }
        header.addCommandItem(this.activateCommand);
      }

      if (Data.isViewEditable(VIEW_ASSESSMENTS)
          && (UserInterface.SELF_SERVICE_LOG.equals(fromUI) || fromUI == null)) {
        Button newAssessment =
            new Button(Localized.getConstants().trNewAssessment(), new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {

                if (DataUtils.isId(getLongValue(COL_QUERY_MANAGER))) {

                  Queries.getRowSet(VIEW_ASSESSMENT_EXECUTORS, Lists
                      .newArrayList(AdministrationConstants.COL_DEPARTMENT), Filter.equals(
                      AdministrationConstants.COL_USER, getLongValue(COL_QUERY_MANAGER)),
                      new RowSetCallback() {

                        @Override
                        public void onSuccess(BeeRowSet result) {
                          if (result.isEmpty()) {
                            notifyRequired(AdministrationConstants.COL_DEPARTMENT);
                            return;
                          }

                          onCreateOrder(VIEW_ASSESSMENTS, result.getLong(result
                              .getNumberOfRows() - 1, result.getNumberOfColumns() - 1), fromUI);

                        }
                      });

                } else {
                  BeeColumn col =
                      Data.getColumn(VIEW_ASSESSMENTS, TransportConstants.COL_ORDER_MANAGER);
                  if (col != null) {
                    notifyRequired(Localized.getLabel(col));
                  } else {
                    notifyRequired(COL_QUERY_MANAGER);
                  }
                }
              }
            });

        header.addCommandItem(newAssessment);
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
