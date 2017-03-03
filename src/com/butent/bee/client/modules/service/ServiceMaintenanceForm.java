package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.Disclosure;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.*;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COMPANY_TYPE_NAME;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ServiceMaintenanceForm extends MaintenanceStateChangeInterceptor
    implements SelectorEvent.Handler, RowUpdateEvent.Handler {

  private static final BeeLogger logger = LogUtils.getLogger(ServiceMaintenanceForm.class);

  private static final String FA_LABEL_SUFFIX = "Add";
  private static final String WIDGET_ADDRESS_NAME = "AddressLabel";
  private static final String WIDGET_MAINTENANCE_COMMENTS = "MaintenanceComments";
  private static final String WIDGET_PANEL_NAME = "Panel";
  private static final String WIDGET_OTHER_INFO = "OtherInfo";

  private static final String STYLE_PROGRESS_CONTAINER =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressContainer";
  private static final String STYLE_PROGRESS_BAR =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressBar";
  private static final int REPORT_DATA_CONSUMPTIONS_COUNT = 2;

  private final MaintenanceEventsHandler eventsHandler = new MaintenanceEventsHandler();
  private Flow maintenanceComments;
  private Disclosure otherInfo;
  private final Collection<HandlerRegistration> registry = new ArrayList<>();
  private FlowPanel warrantyMaintenancePanel;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector && BeeUtils.inList(name, COL_TYPE, COL_SERVICE_OBJECT,
        COL_WARRANTY_MAINTENANCE, COL_CONTACT)) {
      ((DataSelector) widget).addSelectorHandler(this);

    } else if (widget instanceof Flow && BeeUtils.same(name, WIDGET_MAINTENANCE_COMMENTS)) {
      maintenanceComments = (Flow) widget;
      maintenanceComments.clear();

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, VIEW_SERVICE_DATES)) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
          FormView parentForm = ViewHelper.getForm(gridView.getViewPresenter().getMainView());

          if (parentForm != null
                  && BeeUtils.same(parentForm.getViewName(), TBL_SERVICE_MAINTENANCE)) {
            event.getColumns().add(Data.getColumn(getViewName(), COL_SERVICE_OBJECT));
            event.getValues().add(parentForm.getActiveRow()
                .getString(Data.getColumnIndex(parentForm.getViewName(), COL_SERVICE_OBJECT)));
          }

          super.onReadyForInsert(gridView, event);
        }
      });

    } else if (widget instanceof FaLabel && BeeUtils.inList(name,
        COL_SERVICE_OBJECT + FA_LABEL_SUFFIX, COL_CONTACT + FA_LABEL_SUFFIX)) {
      ((FaLabel) widget).addClickHandler(event -> {
        String viewName = BeeUtils.same(name, COL_SERVICE_OBJECT + FA_LABEL_SUFFIX)
            ? TBL_SERVICE_OBJECTS : TBL_COMPANY_PERSONS;
        Widget selectorWidget = null;

        if (BeeUtils.same(viewName, TBL_COMPANY_PERSONS)) {
          selectorWidget = getFormView().getWidgetBySource(COL_CONTACT);

        } else if (BeeUtils.same(viewName, TBL_SERVICE_OBJECTS)) {
          selectorWidget = getFormView().getWidgetBySource(COL_SERVICE_OBJECT);
        }

        if (selectorWidget instanceof DataSelector) {
          RowFactory.createRelatedRow((DataSelector) selectorWidget, null);
        }
      });

    } else if (widget instanceof Disclosure && BeeUtils.same(name, WIDGET_OTHER_INFO)) {
      otherInfo = (Disclosure) widget;

    } else if (widget instanceof InputBoolean && BeeUtils.same(name, COL_WARRANTY)) {
      final InputBoolean warranty = (InputBoolean) widget;
      warranty.addValueChangeHandler(event -> {
        boolean isWarranty = BeeUtils.toBoolean(event.getValue());

        if (warrantyMaintenancePanel != null) {
          warrantyMaintenancePanel.setVisible(isWarranty);
        }

        if (!isWarranty) {
          getActiveRow().clearCell(getDataIndex(COL_WARRANTY_MAINTENANCE));

          Widget warrantySelector = getFormView().getWidgetByName(COL_WARRANTY_MAINTENANCE, false);
          if (warrantySelector instanceof DataSelector) {
            ((DataSelector) warrantySelector).clearDisplay();
          }
        }
      });

    } else if (widget instanceof DataSelector
        && BeeUtils.same(name, AdministrationConstants.COL_DEPARTMENT)) {
      Filter departmentFilter =
          Filter.in(Data.getIdColumn(AdministrationConstants.VIEW_DEPARTMENTS),
              VIEW_DEPARTMENT_EMPLOYEES, COL_DEPARTMENT, Filter.equals(COL_COMPANY_PERSON,
                  BeeKeeper.getUser().getUserData().getCompanyPerson()));
      ((DataSelector) widget).setAdditionalFilter(departmentFilter);

    } else if (BeeUtils.same(name, COL_WARRANTY_MAINTENANCE + WIDGET_PANEL_NAME)
        && widget instanceof FlowPanel) {
      warrantyMaintenancePanel = (FlowPanel) widget;
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    super.afterInsertRow(result, forced);

    updateServiceObject(result.getId(), null);

    fillDataByStateProcessSettings(result, null);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    drawComments(row);

    updateStateDataSelector(false);

    updateWarrantyMaintenanceWidget(DataUtils
        .isId(row.getLong(getDataIndex(COL_WARRANTY_MAINTENANCE))));

    if (BeeUtils.isTrue(row.getBoolean(getDataIndex(COL_ADDRESS_REQUIRED)))) {
      updateContactAddressLabel(true);
    }

    Widget typeWidget = form.getWidgetBySource(COL_TYPE);

    if (typeWidget instanceof DataSelector) {
      ((DataSelector) typeWidget).setEnabled(DataUtils.isNewRow(row));
    }

    Widget warrantyPanel = getFormView().getWidgetByName(COL_WARRANTY + WIDGET_PANEL_NAME, false);

    if (warrantyPanel != null) {
      warrantyPanel.setVisible(!DataUtils.isNewRow(row));
    }
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    super.afterUpdateRow(result);

    updateServiceObject(result.getId(),
            result.getLong(getDataIndex(COL_SERVICE_OBJECT)));
  }

  @Override
  public FormInterceptor getInstance() {
    return new ServiceMaintenanceForm();
  }

  public Flow getMaintenanceComments() {
    return maintenanceComments;
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    super.getReportData(dataConsumer);
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    super.getReportParameters(defaultParameters -> {
      Widget widget = getFormView().getWidgetBySource(COL_EQUIPMENT);

      if (widget instanceof MultiSelector) {
        MultiSelector selector = (MultiSelector) widget;
        List<Long> ids = DataUtils.parseIdList(selector.getValue());

        if (!ids.isEmpty()) {
          List<String> labels = new ArrayList<>();
          for (Long id : ids) {
            labels.add(selector.getRowLabel(id));
          }
          defaultParameters.put(COL_EQUIPMENT, BeeUtils.joinItems(labels));
        }
      }
      defaultParameters.put(PRM_EXTERNAL_MAINTENANCE_URL,
          Global.getParameterText(PRM_EXTERNAL_MAINTENANCE_URL));

      Latch latch = new Latch(REPORT_DATA_CONSUMPTIONS_COUNT);

      Runnable action = () -> {
        latch.decrement();
        if (latch.isOpen()) {
          parametersConsumer.accept(defaultParameters);
        }
      };

      Map<String, Long> companies = new HashMap<>();
      String creatorCompanyAlias = COL_CREATOR + COL_COMPANY;
      companies.put(creatorCompanyAlias, getLongValue(creatorCompanyAlias));

      ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
        defaultParameters.putAll(companiesInfo);
        action.run();
      });

      Long departmentId = getActiveRow().getLong(getDataIndex(COL_DEPARTMENT));

      if (DataUtils.isId(departmentId)) {
        Queries.getRow(VIEW_DEPARTMENTS, departmentId, new RowCallback() {
          @Override
          public void onSuccess(BeeRow departmentRow) {
            for (String column : Arrays.asList(COL_PHONE, COL_FAX, COL_EMAIL, COL_ADDRESS,
                COL_POST_INDEX, ALS_CITY_NAME, COL_WEBSITE)) {
              defaultParameters.put(COL_DEPARTMENT + column,
                  departmentRow.getString(Data.getColumnIndex(VIEW_DEPARTMENTS, column)));
            }
            action.run();
          }
        });
      } else {
        action.run();
      }
    });
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    super.onClose(messages, oldRow, newRow);
    EventUtils.clearRegistry(registry);

    if (otherInfo != null) {
      otherInfo.setOpen(true);
    }
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isNewRow()
        && BeeUtils.inList(event.getRelatedViewName(), VIEW_COMPANY_PERSONS, TBL_SERVICE_OBJECTS)) {

      event.setDefValue(null);

      if (BeeUtils.same(event.getRelatedViewName(), VIEW_COMPANY_PERSONS)
          && BeeUtils.isTrue(getActiveRow().getBoolean(getDataIndex(COL_ADDRESS_REQUIRED)))) {
        event.setOnOpenNewRow(formView -> {
          Widget widget = formView.getWidgetBySource(COL_ADDRESS);

          if (widget instanceof InputText) {
            formView.getWidgetByName(COL_ADDRESS).addStyleName(StyleUtils.NAME_REQUIRED);

            formView.addCellValidationHandler(COL_ADDRESS, validationEvent -> {

              if (BeeUtils.isEmpty(validationEvent.getNewValue())) {
                formView.notifySevere(Localized.dictionary()
                    .fieldRequired(Localized.dictionary().address()));
                return false;
              }
              return true;
            });
          }
        });
      }
    } else if (event.isChanged()) {
      switch (event.getRelatedViewName()) {
        case VIEW_MAINTENANCE_TYPES:
          updateStateDataSelector(true);

          Boolean addressRequired = event.getRelatedRow()
              .getBoolean(Data.getColumnIndex(event.getRelatedViewName(), COL_ADDRESS_REQUIRED));
          updateContactAddressLabel(BeeUtils.isTrue(addressRequired));
          break;

        case VIEW_COMPANY_PERSONS:
          ServiceUtils.fillCompanyValues(getActiveRow(), event.getRelatedRow(),
              event.getRelatedViewName(), COL_COMPANY, ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME);
          getFormView().refreshBySource(COL_COMPANY);
          break;

        case VIEW_SERVICE_OBJECTS:
          ServiceUtils.fillContactValues(getActiveRow(), event.getRelatedRow());
          getFormView().refreshBySource(COL_CONTACT);
          getFormView().refreshBySource(ALS_CONTACT_PHONE);
          getFormView().refreshBySource(ALS_CONTACT_EMAIL);
          getFormView().refreshBySource(ALS_CONTACT_ADDRESS);

          ServiceUtils.fillContractorAndManufacturerValues(getActiveRow(), event.getRelatedRow());
          getFormView().refreshBySource(ALS_MANUFACTURER_NAME);
          getFormView().refreshBySource(ALS_SERVICE_CONTRACTOR_NAME);

          ServiceUtils.fillCompanyValues(getActiveRow(), event.getRelatedRow(),
              event.getRelatedViewName(), COL_SERVICE_CUSTOMER, ALS_SERVICE_CUSTOMER_NAME,
              ALS_CUSTOMER_TYPE_NAME);
          getFormView().refreshBySource(COL_COMPANY);
          break;
      }
    }
  }

  @Override
  public void onLoad(FormView form) {
    super.onLoad(form);
    registry.add(BeeKeeper.getBus().registerRowUpdateHandler(this, false));
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    event.setConsumed(!isValidData(getFormView(), getActiveRow()));
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (getFormView() == null) {
      return;
    }

    if (getActiveRow() == null) {
      return;
    }

    FormView form = getFormView();
    IsRow row = getActiveRow();

    if (BeeUtils.in(event.getViewName(), VIEW_SERVICE_OBJECTS, VIEW_COMPANY_PERSONS,
        VIEW_COMPANIES)) {
      String updatedViewName = event.getViewName();
      Long relId = event.getRowId();
      String updatedIdColumn = null;
      Long updatedId = null;
      Long oldId = null;

      switch (updatedViewName) {
        case VIEW_SERVICE_OBJECTS:
          updatedIdColumn = COL_SERVICE_OBJECT;
          break;
        case VIEW_COMPANY_PERSONS:
          updatedIdColumn = COL_CONTACT;
          break;
        case VIEW_COMPANIES:
          updatedIdColumn = COL_COMPANY;
          break;
      }

      if (Data.containsColumn(form.getViewName(), updatedIdColumn)) {
        updatedId = row.getLong(form.getDataIndex(updatedIdColumn));

        if (form.getOldRow() != null) {
          oldId = form.getOldRow().getLong(form.getDataIndex(updatedIdColumn));
        }
      }

      if (DataUtils.isId(relId) && DataUtils.isId(updatedId) && updatedId.equals(relId)) {
        IsRow eventRow = event.getRow();

        if (event.hasView(VIEW_COMPANY_PERSONS)) {
          Long companyId = row.getLong(form.getDataIndex(COL_COMPANY));
          Long updatedCompanyId = event.getRow()
              .getLong(Data.getColumnIndex(VIEW_COMPANY_PERSONS, COL_COMPANY));

          if (!companyId.equals(updatedCompanyId)) {

            if (DataUtils.isNewRow(row)) {
              updateNewServiceMaintenanceData(row, form, updatedViewName, eventRow);
              form.refresh();

            } else {
              Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(), COL_COMPANY,
                  BeeUtils.toString(companyId), BeeUtils.toString(updatedCompanyId),
                  ModificationEvent.Kind.UPDATE_ROW);
            }
            return;
          }
        }

        if (DataUtils.isNewRow(row) || !updatedId.equals(oldId)) {
          updateNewServiceMaintenanceData(row, form, updatedViewName, eventRow);
          form.refresh();

        } else {
          Queries.getRow(getViewName(), row.getId(), new RowCallback() {

            @Override
            public void onSuccess(BeeRow rowResult) {
              form.refresh();
              RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), rowResult);
            }
          });
        }
      }
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    if (isValidData(getFormView(), getActiveRow())) {

      if (event.getColumns().contains(Data.getColumn(getViewName(),
          AdministrationConstants.COL_STATE))) {
        fillDataByStateProcessSettings(event.getNewRow(), event.getOldRow());
      }

    } else {
      event.consume();
    }
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    super.onStartNewRow(form, oldRow, newRow);

    if (otherInfo != null) {
      otherInfo.setOpen(true);
    }

    form.addStyleName(STYLE_PROGRESS_CONTAINER);
    form.addStyleName(STYLE_PROGRESS_BAR);

    ParameterList params = ServiceKeeper.createArgs(SVC_GET_MAINTENANCE_NEW_ROW_VALUES);
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.isEmpty() && !response.hasErrors()) {
          Map<String, String> columnsData = Codec
              .deserializeLinkedHashMap(response.getResponseAsString());
          columnsData.forEach((column, value) -> newRow.setValue(getDataIndex(column), value));
        }
        form.removeStyleName(STYLE_PROGRESS_CONTAINER);
        form.removeStyleName(STYLE_PROGRESS_BAR);

        getFormView().refresh(false, false);
      }
    });
  }

  @Override
  public void onUnload(FormView form) {
    super.onUnload(form);
    EventUtils.clearRegistry(registry);
  }

  private void createStateChangeComment(IsRow row, IsRow stateProcessRow) {
    List<BeeColumn> columns = Data.getColumns(TBL_MAINTENANCE_COMMENTS,
        Lists.newArrayList(COL_SERVICE_MAINTENANCE, COL_EVENT_NOTE, COL_MAINTENANCE_STATE,
            COL_STATE_COMMENT));
    List<String> values = Lists.newArrayList(Long.toString(row.getId()),
        row.getString(getDataIndex(ALS_STATE_NAME)),
        row.getString(getDataIndex(AdministrationConstants.COL_STATE)),
        BeeUtils.toString(Boolean.TRUE));

    if (stateProcessRow != null) {
      String notifyValue = stateProcessRow.
          getString(Data.getColumnIndex(TBL_STATE_PROCESS, COL_NOTIFY_CUSTOMER));

      if (BeeUtils.toBoolean(notifyValue)) {
        columns.add(Data.getColumn(TBL_MAINTENANCE_COMMENTS, COL_CUSTOMER_SENT));
        values.add(notifyValue);
      }

      String showCustomerValue = stateProcessRow.
          getString(Data.getColumnIndex(TBL_STATE_PROCESS, COL_SHOW_CUSTOMER));

      if (BeeUtils.toBoolean(showCustomerValue)) {
        columns.add(Data.getColumn(TBL_MAINTENANCE_COMMENTS, COL_SHOW_CUSTOMER));
        values.add(showCustomerValue);
      }

      String commentValue = stateProcessRow.getString(Data.getColumnIndex(TBL_STATE_PROCESS,
          COL_MESSAGE));

      if (!BeeUtils.isEmpty(commentValue)) {
        columns.add(Data.getColumn(TBL_MAINTENANCE_COMMENTS, COL_COMMENT));
        values.add(commentValue);
      }
    }

    Queries.insert(TBL_MAINTENANCE_COMMENTS, columns, values, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow commentRow) {
        ServiceUtils.informClient(commentRow);
      }
    });
  }

  private void drawComments(IsRow row) {
    final Flow comments = getMaintenanceComments();

    if (comments == null) {
      logger.warning("Widget of project comments not found");
      return;
    }

    if (eventsHandler == null) {
      logger.warning("Events handler not initialized");
      return;
    }

    comments.clear();

    if (!DataUtils.isId(row.getId())) {
      return;
    }

    eventsHandler.create(comments, row.getId());
    eventsHandler.setMaintenanceRow(row);
  }

  private void fillDataByStateProcessSettings(IsRow row, IsRow oldRow) {
    Long stateId = row.getLong(getDataIndex(AdministrationConstants.COL_STATE));
    Long maintenanceTypeId = row.getLong(getDataIndex(COL_TYPE));

    Queries.getRowSet(TBL_STATE_PROCESS, null,
        ServiceUtils.getStateFilter(stateId, maintenanceTypeId), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (!DataUtils.isEmpty(result)) {
          IsRow stateProcessRow = result.getRow(0);
          String oldValue;

          if (oldRow != null) {
            oldValue = oldRow.getString(getDataIndex(COL_ENDING_DATE));
          } else {
            oldValue = null;
          }

          if (BeeUtils.toBoolean(stateProcessRow.getString(result.getColumnIndex(COL_FINITE)))) {
            Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(), COL_ENDING_DATE,
                oldValue, BeeUtils.toString(System.currentTimeMillis()),
                ModificationEvent.Kind.UPDATE_ROW);

          } else if (!BeeUtils.isEmpty(oldValue)) {
            Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(), COL_ENDING_DATE,
                oldValue, null, ModificationEvent.Kind.UPDATE_ROW);
          }

          createStateChangeComment(row, stateProcessRow);

        } else {
          createStateChangeComment(row, null);
        }
      }
    });
  }

  private boolean isValidData(FormView form, IsRow row) {
    String phone = row.getString(getDataIndex(ALS_CONTACT_PHONE));

    if (BeeUtils.isEmpty(phone)) {
      form.notifySevere(Localized.dictionary().fieldRequired(Localized.dictionary().phone()));
      return false;
    }

    Boolean addressRequired = row.getBoolean(getDataIndex(COL_ADDRESS_REQUIRED));

    if (BeeUtils.isTrue(addressRequired)) {
      String address = row.getString(getDataIndex(ALS_CONTACT_ADDRESS));

      if (BeeUtils.isEmpty(address)) {
        form.notifySevere(Localized.dictionary().fieldRequired(Localized.dictionary().address()));
        return false;
      }
    }

    Long serviceMaintenanceId = row.getLong(getDataIndex(COL_WARRANTY_MAINTENANCE));
    Widget warrantyWidget = getFormView().getWidgetByName(COL_WARRANTY, false);
    if (warrantyWidget instanceof HasCheckedness && ((HasCheckedness) warrantyWidget).isChecked()
        && !DataUtils.isId(serviceMaintenanceId)) {
      form.notifySevere(Localized.dictionary()
          .fieldRequired(Localized.dictionary().svcWarrantyMaintenance()));
      return false;
    }

    return true;
  }

  private void updateContactAddressLabel(boolean addressRequired) {
    Widget addressLabel = getFormView().getWidgetByName(WIDGET_ADDRESS_NAME);

    if (addressLabel != null) {
      addressLabel.setStyleName(StyleUtils.NAME_REQUIRED, addressRequired);
    }
  }

  private static void updateNewServiceMaintenanceData(IsRow row, FormView form, String viewName,
      IsRow eventRow) {
    switch (viewName) {
      case VIEW_SERVICE_OBJECTS:
        row.setValue(form.getDataIndex(COL_SERVICE_OBJECT), eventRow.getId());
        row.setValue(form.getDataIndex(ALS_SERVICE_CATEGORY_NAME),
            eventRow.getValue(Data.getColumnIndex(viewName, ALS_SERVICE_CATEGORY_NAME)));
        row.setValue(form.getDataIndex(ALS_MANUFACTURER_NAME),
            eventRow.getValue(Data.getColumnIndex(viewName, ALS_MANUFACTURER_NAME)));
        row.setValue(form.getDataIndex(COL_MODEL),
            eventRow.getValue(Data.getColumnIndex(viewName, COL_MODEL)));
        row.setValue(form.getDataIndex(COL_SERIAL_NO),
            eventRow.getValue(Data.getColumnIndex(viewName, COL_SERIAL_NO)));
        row.setValue(form.getDataIndex(COL_ARTICLE_NO),
            eventRow.getValue(Data.getColumnIndex(viewName, COL_ARTICLE_NO)));
        row.setValue(form.getDataIndex(ALS_SERVICE_CONTRACTOR_NAME),
            eventRow.getValue(Data.getColumnIndex(viewName, ALS_SERVICE_CONTRACTOR_NAME)));
        ServiceUtils.fillContactValues(row, eventRow);
        break;

      case VIEW_COMPANY_PERSONS:
        row.setValue(form.getDataIndex(COL_CONTACT), eventRow.getId());
        row.setValue(form.getDataIndex(ALS_CONTACT_PHONE),
            eventRow.getValue(Data.getColumnIndex(viewName, COL_PHONE)));
        row.setValue(form.getDataIndex(ALS_CONTACT_EMAIL),
            eventRow.getValue(Data.getColumnIndex(viewName, COL_EMAIL)));
        row.setValue(form.getDataIndex(ALS_CONTACT_ADDRESS),
            eventRow.getValue(Data.getColumnIndex(viewName, COL_ADDRESS)));
        ServiceUtils.fillCompanyValues(row, eventRow, viewName, COL_COMPANY,
            ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME);
        break;

      case VIEW_COMPANIES:
        ServiceUtils.fillCompanyColumns(row, eventRow, viewName,
            COL_COMPANY_NAME, ALS_COMPANY_TYPE_NAME);
        break;
    }
  }

  private static void updateServiceObject(long maintenanceId, Long objectId) {
    ParameterList params = ServiceKeeper.createArgs(SVC_UPDATE_SERVICE_MAINTENANCE_OBJECT);
    params.addDataItem(COL_SERVICE_MAINTENANCE, maintenanceId);

    if (DataUtils.isId(objectId)) {
      params.addDataItem(COL_SERVICE_OBJECT, objectId);
    }

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.isEmpty() && !response.hasErrors()) {
          Queries.getRow(VIEW_SERVICE_OBJECTS, response.getResponseAsLong(), new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              if (result != null) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_SERVICE_OBJECTS, result);
              }
            }
          });
        }
      }
    });
  }

  private void updateStateDataSelector(boolean clearValue) {
    Widget stateWidget = getFormView().getWidgetByName(AdministrationConstants.COL_STATE);

    if (stateWidget instanceof DataSelector) {
      Filter stateFilter = DataUtils.isNewRow(getActiveRow())
          ? Filter.isPositive(COL_INITIAL) : Filter.isNull(COL_INITIAL);
      Long maintenanceTypeId = getActiveRow().getLong(getDataIndex(COL_TYPE));

      ((DataSelector) stateWidget).setAdditionalFilter(
          Filter.in(Data.getIdColumn(VIEW_MAINTENANCE_STATES), TBL_STATE_PROCESS,
              COL_MAINTENANCE_STATE,
              ServiceUtils.getStateFilter(null, maintenanceTypeId, stateFilter)));

      if (clearValue) {
        ((DataSelector) stateWidget).clearValue();
        getFormView().getActiveRow().clearCell(getDataIndex(AdministrationConstants.COL_STATE));
        getFormView().getActiveRow().clearCell(getDataIndex(ALS_STATE_NAME));
        getFormView().refreshBySource(AdministrationConstants.COL_STATE);
      }
    }
  }

  private void updateWarrantyMaintenanceWidget(boolean mandatory) {
    if (warrantyMaintenancePanel != null) {
      warrantyMaintenancePanel.setVisible(mandatory);
    }

    Widget warrantyWidget = getFormView().getWidgetByName(COL_WARRANTY, false);
    if (warrantyWidget instanceof HasCheckedness) {
      ((HasCheckedness) warrantyWidget).setChecked(mandatory);
    }
  }
}