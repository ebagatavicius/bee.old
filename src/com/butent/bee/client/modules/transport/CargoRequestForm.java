package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.Editor;
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
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.AssessmentStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.CargoRequestStatus;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

class CargoRequestForm extends AbstractFormInterceptor {

  private Button activateCommand;
  private Button templateCommand;

  private FileCollector collector;

  CargoRequestForm() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
    if (widget instanceof FileCollector) {
      this.collector = (FileCollector) widget;
      this.collector.bindDnd(getFormView());
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    if (getCollector() != null && !getCollector().isEmpty()) {
      SelfServiceUtils.sendFiles(result.getId(), getCollector().getFiles());
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    CargoRequestStatus status;
    UserInterface fromInterface;
    if (DataUtils.hasId(row)) {
      status = EnumUtils.getEnumByIndex(CargoRequestStatus.class,
          Data.getInteger(getViewName(), row, COL_CARGO_REQUEST_STATUS));
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
    return new CargoRequestForm();
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    Widget tWidget = form.getWidgetByName("Template");
    if (tWidget instanceof Editor) {
      ((Editor) tWidget).clearValue();
    }

    if (getCollector() != null) {
      getCollector().clear();
    }

    SelfServiceUtils.setDefaultExpeditionType(form, newRow, COL_CARGO_REQUEST_EXPEDITION);
    SelfServiceUtils.setDefaultShippingTerm(form, newRow, COL_CARGO_SHIPPING_TERM);

    super.onStartNewRow(form, oldRow, newRow);
  }

  private FileCollector getCollector() {
    return collector;
  }

  private void onCreateOrder(final String viewName,
      final Long departmentId, final UserInterface fromUI) {
    Global.confirm(Localized.getConstants().trConfirmCreateNewOrder(), new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        String company = getStringValue(ALS_REQUEST_CUSTOMER_COMPANY);
        if (!DataUtils.isId(BeeUtils.toLongOrNull(company))) {
          return;
        }

        List<String> colNames = Lists.newArrayList(COL_CUSTOMER);
        List<String> values = Lists.newArrayList(company);

        String manager = getStringValue(COL_CARGO_REQUEST_MANAGER);
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
            SelfServiceUtils.updateStatus(getFormView(), COL_CARGO_REQUEST_STATUS, status);
            refreshCommands(status, fromUI);

            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName);
          }
        });
      }
    });
  }

  private void onSaveAsTemplate() {
    DataInfo tInfo = Data.getDataInfo(VIEW_CARGO_REQUEST_TEMPLATES);
    if (tInfo == null) {
      return;
    }

    IsRow rRow = getActiveRow();
    if (rRow == null) {
      return;
    }
    BeeRow tRow = RowFactory.createEmptyRow(tInfo, false);

    List<BeeColumn> rColumns = getFormView().getDataColumns();
    for (int tIndex = 0; tIndex < tInfo.getColumnCount(); tIndex++) {
      int rIndex = DataUtils.getColumnIndex(tInfo.getColumns().get(tIndex).getId(), rColumns);
      if (rIndex >= 0) {
        String value = rRow.getString(rIndex);
        if (!BeeUtils.isEmpty(value)) {
          tRow.setValue(tIndex, value);
        }
      }
    }

    RowFactory.createRow(tInfo, tRow, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_CARGO_REQUEST_TEMPLATES);
      }
    });
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

      if (status != null && Data.isViewEditable(VIEW_CARGO_REQUEST_TEMPLATES)) {
        if (this.templateCommand == null) {
          this.templateCommand =
              new Button(Localized.getConstants().trCommandSaveRequestAsTemplate(),
                  new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                      onSaveAsTemplate();
                    }
                  });
        }
        header.addCommandItem(this.templateCommand);
      }
    }
  }
}
