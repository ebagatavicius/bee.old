package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.Disclosure;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Link;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.data.*;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.orders.OrdersConstants;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ServiceMaintenanceForm extends MaintenanceStateChangeInterceptor
    implements SelectorEvent.Handler, RowUpdateEvent.Handler, DataChangeEvent.Handler {

  private static final BeeLogger logger = LogUtils.getLogger(ServiceMaintenanceForm.class);

  private static final String FA_LABEL_SUFFIX = "Add";
  private static final String WIDGET_ADDRESS_NAME = "AddressLabel";
  private static final String WIDGET_MAINTENANCE_COMMENTS = "MaintenanceComments";
  private static final String WIDGET_PANEL_NAME = "Panel";
  private static final String WIDGET_OTHER_INFO = "OtherInfo";
  private static final String WIDGET_SEARCH_ALL_DEVICES = "SearchingAllDevices";

  private static final String STYLE_PROGRESS_CONTAINER =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressContainer";
  private static final String STYLE_PROGRESS_BAR =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressBar";
  private static final int REPORT_DATA_CONSUMPTIONS_COUNT = 3;

  private DataSelector deviceSelector;
  private final MaintenanceEventsHandler eventsHandler = new MaintenanceEventsHandler();
  private Flow maintenanceComments;
  private Disclosure otherInfo;
  private String reasonComment;
  private final Collection<HandlerRegistration> registry = new ArrayList<>();
  private InputBoolean searchAllDevices;
  private FlowPanel warrantyMaintenancePanel;
  private Set<DataSelector> disableEditWidgets = new HashSet<>();

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), COL_WAREHOUSE)) {
      editableWidget.addCellValidationHandler(event -> {
        CellValidation cv = event.getCellValidation();
        final String newValue = cv.getNewValue();
        String oldValue = cv.getOldValue();

        if (!Objects.equals(newValue, oldValue) && oldValue != null
            && DataUtils.hasId(getActiveRow())) {

          Global.confirm(Localized.dictionary().ordAskChangeWarehouse() + " "
              + Localized.dictionary().saveChanges(), () -> {
            if (DataUtils.isId(newValue)) {
              Filter filter = Filter.equals(COL_SERVICE_MAINTENANCE, getActiveRowId());
              Queries.update(TBL_SERVICE_ITEMS, filter, OrdersConstants.COL_RESERVED_REMAINDER,
                  new NumberValue(BeeConst.DOUBLE_ZERO), result -> {
                    getActiveRow().setValue(getDataIndex(COL_WAREHOUSE), newValue);
                    update();
                  });
            }
          });
          return false;
        } else if (!Objects.equals(newValue, oldValue) && DataUtils.hasId(getActiveRow())) {
          getActiveRow().setValue(getDataIndex(COL_WAREHOUSE), newValue);
          update();
          return false;
        }
        return true;
      });

    } else if (BeeUtils.same(editableWidget.getColumnId(), COL_REPAIRER)
        && widget instanceof DataSelector) {
      ServiceHelper.setRepairerFilter(widget);
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector && BeeUtils.inList(name, COL_TYPE, COL_SERVICE_OBJECT,
        COL_WARRANTY_MAINTENANCE, COL_CONTACT, COL_WARRANTY_TYPE, COL_COMPANY)) {
      ((DataSelector) widget).addSelectorHandler(this);

      if (BeeUtils.same(name, COL_SERVICE_OBJECT)) {
        deviceSelector = (DataSelector) widget;
      }

      if (BeeUtils.inList(name, COL_SERVICE_OBJECT, COL_CONTACT, COL_COMPANY)) {
        disableEditWidgets.add((DataSelector) widget);
      }

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

    } else if (BeeUtils.same(name, TBL_SERVICE_ITEMS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new ServiceItemsGrid());

    } else if (BeeUtils.same(name, WIDGET_SEARCH_ALL_DEVICES) && widget instanceof InputBoolean) {
      searchAllDevices = (InputBoolean) widget;
      searchAllDevices.addValueChangeHandler(event -> updateDeviceFilter());
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    super.afterInsertRow(result, forced);

    updateServiceObject(result.getId(), null);

    fillDataByStateProcessSettings(result, null, null);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    drawComments(row);

    updateStateDataSelector(false);

    updateWarrantyMaintenanceWidget(DataUtils
        .isId(row.getLong(getDataIndex(COL_WARRANTY_MAINTENANCE))));

    if (row.isTrue(getDataIndex(COL_ADDRESS_REQUIRED))) {
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

    boolean isMaintenanceActive = BeeUtils.isEmpty(row.getString(getDataIndex(COL_ENDING_DATE)));
    ServiceHelper.setGridEnabled(form, TBL_SERVICE_ITEMS, isMaintenanceActive);
    ServiceHelper.setGridEnabled(form, TBL_MAINTENANCE_PAYROLL, !isMaintenanceActive);

    if (searchAllDevices != null) {
      boolean searchAll = BeeUtils.unbox(Global
          .getParameterBoolean(PRM_FILTER_ALL_DEVICES));
      searchAllDevices.setChecked(searchAll);
    }
    updateDeviceFilter();

    if (!DataUtils.isNewRow(getActiveRow())) {
      ServiceUtils.getStateProcessRowSet(row, processRowSet -> {
        if (!DataUtils.isEmpty(processRowSet)) {
          IsRow stateProcessRow = processRowSet.getRow(0);
          disableEditWidgets.forEach(widget ->
              widget.setEnabled(!stateProcessRow
                  .isTrue(processRowSet.getColumnIndex(COL_PROHIBIT_EDIT))));
        }
      });
    }

    ServiceUtils.setClientValuesForRevert(getActiveRow());
    ServiceUtils.setObjectValuesForRevert(getActiveRow());

    getHeaderView().clearCommandPanel();
    String maintenanceUrl = Global.getParameterText(PRM_EXTERNAL_MAINTENANCE_URL);

    if (!BeeUtils.isEmpty(maintenanceUrl) && DataUtils.isId(row.getId())) {
      String link = BeeUtils.join(BeeConst.STRING_EMPTY,
          Global.getParameterText(PRM_EXTERNAL_MAINTENANCE_URL), row.getId());
      getHeaderView().addCommandItem(new Link(link, link));
    }
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    super.afterUpdateRow(result);

    updateServiceObject(result.getId(),
            result.getLong(getDataIndex(COL_SERVICE_OBJECT)));

    createClientChangeComment(result);
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
    ParameterList params = ServiceKeeper.createArgs(SVC_GET_ITEMS_INFO);
    params.addDataItem(COL_SERVICE_MAINTENANCE, getActiveRowId());

    BeeKeeper.getRpc().makePostRequest(params, response -> {
      if (!response.isEmpty() && !response.hasErrors()) {
        BeeRowSet items = BeeRowSet.restore((String) response.getResponse());
        dataConsumer.accept(new BeeRowSet[] {items});
      }
    });
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
        Queries.getRow(VIEW_DEPARTMENTS, departmentId, departmentRow -> {
          for (String column : Arrays.asList(COL_PHONE, COL_FAX, COL_EMAIL, COL_ADDRESS,
              COL_POST_INDEX, ALS_CITY_NAME, COL_WEBSITE)) {
            defaultParameters.put(COL_DEPARTMENT + column,
                departmentRow.getString(Data.getColumnIndex(VIEW_DEPARTMENTS, column)));
          }
          action.run();
        });
      } else {
        action.run();
      }

      Filter commentsFilter = Filter.and(Filter.equals(COL_SERVICE_MAINTENANCE, getActiveRowId()),
          Filter.notNull(COL_SHOW_CUSTOMER));
      Queries.getRowSet(TBL_MAINTENANCE_COMMENTS, null, commentsFilter, comments -> {
        defaultParameters.put(TBL_MAINTENANCE_COMMENTS, comments.serialize());
        action.run();
      });
    });
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    super.onClose(messages, oldRow, newRow);
    EventUtils.clearRegistry(registry);

    if (otherInfo != null) {
      otherInfo.setOpen(true);
    }
    clearReason();
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (event.hasView(VIEW_COMPANY_PERSONS)
        && getActiveRow().isTrue(getDataIndex(COL_COMPANY_TYPE_PERSON))) {
      Long companyId = getActiveRow().getLong(getDataIndex(COL_COMPANY));
      Queries.getRow(VIEW_COMPANY_PERSONS, Filter.equals(COL_COMPANY, companyId), null,
          companyPersonRow -> {
            if (companyPersonRow != null && DataUtils.isId(companyPersonRow.getId())) {
              DataInfo targetDataInfo = Data.getDataInfo(getViewName());
              DataInfo sourceDataInfo = Data.getDataInfo(VIEW_COMPANY_PERSONS);

              getActiveRow().setValue(targetDataInfo.getColumnIndex(COL_CONTACT),
                  companyPersonRow.getId());
              RelationUtils.maybeUpdateColumn(targetDataInfo, ALS_CONTACT_PHONE, getActiveRow(),
                  sourceDataInfo, COL_PHONE, companyPersonRow);
              RelationUtils.maybeUpdateColumn(targetDataInfo, ALS_CONTACT_FIRST_NAME,
                  getActiveRow(), sourceDataInfo, COL_FIRST_NAME, companyPersonRow);
              RelationUtils.maybeUpdateColumn(targetDataInfo, ALS_CONTACT_LAST_NAME,
                  getActiveRow(), sourceDataInfo, COL_LAST_NAME, companyPersonRow);
              RelationUtils.maybeUpdateColumn(targetDataInfo, ALS_CONTACT_EMAIL, getActiveRow(),
                  sourceDataInfo, COL_EMAIL, companyPersonRow);
              RelationUtils.maybeUpdateColumn(targetDataInfo, ALS_CONTACT_ADDRESS, getActiveRow(),
                  sourceDataInfo, COL_ADDRESS, companyPersonRow);

              getFormView().refreshBySource(COL_CONTACT);
              getFormView().refreshBySource(ALS_CONTACT_PHONE);
              getFormView().refreshBySource(ALS_CONTACT_EMAIL);
              getFormView().refreshBySource(ALS_CONTACT_ADDRESS);
            }
          });
    }
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isNewRow() && BeeUtils.in(event.getRelatedViewName(), VIEW_COMPANY_PERSONS,
        VIEW_SERVICE_OBJECTS)) {
      final String defValue = event.getDefValue();
      event.setDefValue(null);

      event.setOnOpenNewRow(formView -> {
        if (event.hasRelatedView(VIEW_COMPANY_PERSONS)
            && getActiveRow().isTrue(getDataIndex(COL_ADDRESS_REQUIRED))) {
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
        }
        Widget selectorWidget = formView.getWidgetBySource(event
            .hasRelatedView(VIEW_COMPANY_PERSONS) ? COL_PERSON : COL_CATEGORY);

        if (selectorWidget instanceof DataSelector) {
          final DataSelector personSelector = (DataSelector) selectorWidget;

          Scheduler.get().scheduleDeferred(() -> {
            personSelector.setFocus(true);
            personSelector.setDisplayValue(defValue);
            personSelector.startEdit(null, DataSelector.ASK_ORACLE, null, null);
          });
        }
      });
    } else if (event.isChanged()) {
      switch (event.getRelatedViewName()) {
        case VIEW_MAINTENANCE_TYPES:
          updateStateDataSelector(true);

          boolean addressRequired = event.getRelatedRow()
              .isTrue(Data.getColumnIndex(event.getRelatedViewName(), COL_ADDRESS_REQUIRED));
          updateContactAddressLabel(addressRequired);
          break;

        case VIEW_COMPANY_PERSONS:
          if (BeeUtils.isEmpty(getActiveRow().getString(getDataIndex(COL_COMPANY)))) {
            ServiceUtils.fillCompanyValues(getActiveRow(), event.getRelatedRow(),
                event.getRelatedViewName(), COL_COMPANY, ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME);
            getFormView().refreshBySource(COL_COMPANY);
          }
          break;

        case VIEW_SERVICE_OBJECTS:
          ServiceUtils.onClientOrObjectUpdate(event, getFormView(), comment ->
            reasonComment = comment);
          break;

        case TBL_WARRANTY_TYPES:
          getActiveRow().setValue(getDataIndex(COL_WARRANTY_VALID_TO),
              ServiceUtils.calculateWarrantyDate(event));
          getFormView().refreshBySource(COL_WARRANTY_VALID_TO);
          break;

        case VIEW_COMPANIES:
          updateDeviceFilter();
          ServiceUtils.onClientOrObjectUpdate(event, getFormView(),  comment ->
              reasonComment = comment);
          break;
      }
    }
  }

  @Override
  public void onLoad(FormView form) {
    super.onLoad(form);
    registry.add(BeeKeeper.getBus().registerRowUpdateHandler(this, false));
    registry.add(BeeKeeper.getBus().registerDataChangeHandler(this, false));
    clearReason();
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
          Queries.getRow(getViewName(), row.getId(), rowResult -> {
            form.refresh();
            RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), rowResult);
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
        fillDataByStateProcessSettings(event.getNewRow(), event.getOldRow(), event);
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
    BeeKeeper.getRpc().makePostRequest(params, response -> {
      if (!response.isEmpty() && !response.hasErrors()) {
        Map<String, String> columnsData = Codec
            .deserializeLinkedHashMap(response.getResponseAsString());
        columnsData.forEach((column, value) -> newRow.setValue(getDataIndex(column), value));
      }
      form.removeStyleName(STYLE_PROGRESS_CONTAINER);
      form.removeStyleName(STYLE_PROGRESS_BAR);

      getFormView().refresh(false, false);
    });
  }

  @Override
  public void onUnload(FormView form) {
    super.onUnload(form);
    EventUtils.clearRegistry(registry);
    clearReason();
  }

  @Override
  public boolean saveOnPrintNewRow() {
    return true;
  }

  private void clearReason() {
    reasonComment = null;
  }

  private void createClientChangeComment(IsRow maintenanceRow) {
    IsRow oldRow = getFormView().getOldRow();
    String oldCompanyId = oldRow.getString(getDataIndex(COL_COMPANY));
    String newCompanyId = maintenanceRow.getString(getDataIndex(COL_COMPANY));

    if (!BeeUtils.isEmpty(oldCompanyId) && !BeeUtils.same(oldCompanyId, newCompanyId)) {
      List<BeeColumn> columns = Data.getColumns(TBL_MAINTENANCE_COMMENTS, Lists.newArrayList(
          COL_SERVICE_MAINTENANCE, COL_EVENT_NOTE, COL_COMMENT));
      List<String> values = Lists.newArrayList(BeeUtils.toString(maintenanceRow.getId()),
          Localized.dictionary().svcChangedClient(), reasonComment);
      Queries.insert(TBL_MAINTENANCE_COMMENTS, columns, values);
      clearReason();
    }
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

    Queries.insert(TBL_MAINTENANCE_COMMENTS, columns, values, null, ServiceUtils::informClient);
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

  private void fillDataByStateProcessSettings(IsRow row, IsRow oldRow, SaveChangesEvent event) {
    ServiceUtils.getStateProcessRowSet(row, processRowSet -> {
      if (!DataUtils.isEmpty(processRowSet)) {
        IsRow stateProcessRow = processRowSet.getRow(0);
        String oldValue;

        if (oldRow != null) {
          oldValue = oldRow.getString(getDataIndex(COL_ENDING_DATE));
        } else {
          oldValue = null;
        }

        if (BeeUtils.toBoolean(stateProcessRow.getString(processRowSet
            .getColumnIndex(COL_FINITE)))) {
          Consumer<String> changeStateConsumer = canChangeStateErrorMsg -> {
            if (BeeUtils.isEmpty(canChangeStateErrorMsg)) {
              Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(),
                  COL_ENDING_DATE, oldValue, BeeUtils.toString(System.currentTimeMillis()),
                  ModificationEvent.Kind.UPDATE_ROW);
            } else {
              event.consume();
              getFormView().notifySevere(canChangeStateErrorMsg);
            }
          };
          boolean isItemsRequired = stateProcessRow.isTrue(processRowSet
              .getColumnIndex(COL_FINITE));
          ServiceUtils.checkCanChangeState(true, isItemsRequired, changeStateConsumer,
              getFormView());

        } else if (!BeeUtils.isEmpty(oldValue)) {
          Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(), COL_ENDING_DATE,
              oldValue, null, ModificationEvent.Kind.UPDATE_ROW);
        }

        createStateChangeComment(row, stateProcessRow);

      } else {
        createStateChangeComment(row, null);
      }
    });
  }

  private boolean isValidData(FormView form, IsRow row) {
    boolean addressRequired = row.isTrue(getDataIndex(COL_ADDRESS_REQUIRED));

    if (addressRequired) {
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

  private void update() {
    FormView form = getFormView();
    BeeRowSet rowSet =
        DataUtils.getUpdated(form.getViewName(), form.getDataColumns(), form.getOldRow(),
            getActiveRow(), form.getChildrenForUpdate());

    Queries.updateRow(rowSet, result -> {
      RowUpdateEvent.fire(BeeKeeper.getBus(), form.getViewName(), result);
      form.refresh();
    });
  }

  private void updateContactAddressLabel(boolean addressRequired) {
    Widget addressLabel = getFormView().getWidgetByName(WIDGET_ADDRESS_NAME);

    if (addressLabel != null) {
      addressLabel.setStyleName(StyleUtils.NAME_REQUIRED, addressRequired);
    }
  }

  private void updateDeviceFilter() {
    if (deviceSelector != null) {
      Long maintenanceCompany = getActiveRow().getLong(getDataIndex(COL_COMPANY));
      boolean searchAll = searchAllDevices != null && searchAllDevices.isChecked();
      Filter deviceFilter = searchAll || !DataUtils.isId(maintenanceCompany) ? null
          : Filter.equals(COL_SERVICE_CUSTOMER, maintenanceCompany);
      deviceSelector.setAdditionalFilter(deviceFilter);
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

    BeeKeeper.getRpc().makePostRequest(params, response -> {
      if (!response.isEmpty() && !response.hasErrors()) {
        Queries.getRow(VIEW_SERVICE_OBJECTS, response.getResponseAsLong(), result -> {
          if (result != null) {
            RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_SERVICE_OBJECTS, result);
          }
        });
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