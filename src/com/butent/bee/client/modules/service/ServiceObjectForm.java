package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.Autocomplete;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.AutocompleteEvent;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServiceObjectForm extends MaintenanceExpanderForm implements ClickHandler,
    RowActionEvent.Handler, SelectorEvent.Handler, RowUpdateEvent.Handler, DataChangeEvent.Handler {

  private final class AutocompleteFilter implements AutocompleteEvent.Handler {

    private final String source;
    private final String criterion;

    private AutocompleteFilter(String source, String criterion) {
      this.source = source;
      this.criterion = criterion;
    }

    @Override
    public void onDataSelector(AutocompleteEvent event) {
      if (event.getState() == State.OPEN) {
        CompoundFilter flt = Filter.and();

        for (String name : new String[] {COL_SERVICE_CATEGORY, COL_SERVICE_OBJECT}) {
          Long id = getLongValue(name);

          if (DataUtils.isId(id)) {
            if (BeeUtils.same(name, COL_SERVICE_CATEGORY)) {
              flt.add(Filter.isEqual(name, Value.getValue(id)));
            } else {
              flt.add(Filter.isNotEqual(name, Value.getValue(id)));
            }
          }
        }

        if (BeeUtils.isEmpty(source)) {
          flt.add(Filter.isNull(COL_SERVICE_CRITERIA_GROUP_NAME));

          if (!BeeUtils.isEmpty(criterion)) {
            flt.add(Filter.isEqual(COL_SERVICE_CRITERION_NAME, Value.getValue(criterion)));
          }

        } else if (!BeeUtils.same(source, COL_SERVICE_CRITERIA_GROUP_NAME)) {
          if (groupsGrid != null) {
            flt.add(Filter.isEqual(COL_SERVICE_CRITERIA_GROUP_NAME,
                groupsGrid.getPresenter().getActiveRow().getValue(groupsGrid.getPresenter()
                    .getGridView().getDataIndex(COL_SERVICE_CRITERIA_GROUP_NAME))));
          }

          if (BeeUtils.same(source, COL_SERVICE_CRITERION_VALUE) && criteriaGrid != null) {
            flt.add(Filter.isEqual(COL_SERVICE_CRITERION_NAME,
                criteriaGrid.getPresenter().getActiveRow().getValue(criteriaGrid.getPresenter()
                    .getGridView().getDataIndex(COL_SERVICE_CRITERION_NAME))));
          }
        }

        event.getSelector().setAdditionalFilter(flt);
      }
    }
  }

  private final GridInterceptor childInterceptor = new AbstractGridInterceptor() {
    @Override
    public void afterCreateEditor(String source, Editor editor, boolean embedded) {
      if (editor instanceof Autocomplete) {
        ((Autocomplete) editor).addAutocompleteHandler(new AutocompleteFilter(source, null));
      }
    }

    @Override
    public GridInterceptor getInstance() {
      return null;
    }
  };

  private HasWidgets criteriaPanel;

  private final Map<String, String> criteriaHistory = new LinkedHashMap<>();
  private final Map<String, Editor> criteriaEditors = new LinkedHashMap<>();

  private Long criteriaGroupId;
  private final Map<String, Long> criteriaIds = new HashMap<>();

  private ChildGrid groupsGrid;
  private ChildGrid criteriaGrid;

  private final List<HandlerRegistration> registry = new ArrayList<>();

  ServiceObjectForm() {
    super();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
    super.afterCreateWidget(name, widget, callback);

    if (BeeUtils.same(name, "MainCriteriaEditor")) {
      widget.asWidget().addDomHandler(this, ClickEvent.getType());

    } else if (widget instanceof HasWidgets && BeeUtils.same(name, "MainCriteriaContainer")) {
      criteriaPanel = (HasWidgets) widget;

    } else if (widget instanceof ChildGrid) {
      ChildGrid grid = (ChildGrid) widget;

      if (BeeUtils.same(name, VIEW_SERVICE_CRITERIA_GROUPS)) {
        groupsGrid = grid;
        grid.setGridInterceptor(childInterceptor);

      } else if (BeeUtils.same(name, VIEW_SERVICE_CRITERIA)) {
        criteriaGrid = grid;
        grid.setGridInterceptor(childInterceptor);

      } else if (BeeUtils.same(name, TBL_SERVICE_MAINTENANCE)) {
        grid.setGridInterceptor(new AbstractGridInterceptor() {
          @Override
          public GridInterceptor getInstance() {
            return null;
          }

          @Override
          public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
            FormView parentForm = ViewHelper.getForm(presenter.getMainView());
            if (parentForm != null && parentForm.getActiveRow() != null) {
              presenter.getGridView().ensureRelId(result -> {
                DataInfo objectDataInfo = Data.getDataInfo(parentForm.getViewName());
                DataInfo maintenanceDataInfo = Data.getDataInfo(getViewName());
                IsRow objectRow = parentForm.getActiveRow();

                BeeRow newRow = RowFactory.createEmptyRow(maintenanceDataInfo, true);
                newRow.setValue(maintenanceDataInfo.getColumnIndex(COL_SERVICE_OBJECT), result);
                newRow.setValue(maintenanceDataInfo.getColumnIndex(ALS_SERVICE_CATEGORY_NAME),
                    objectRow.getString(objectDataInfo.getColumnIndex(ALS_SERVICE_CATEGORY_NAME)));
                newRow.setValue(maintenanceDataInfo.getColumnIndex(COL_MODEL),
                    objectRow.getString(objectDataInfo.getColumnIndex(COL_MODEL)));
                newRow.setValue(maintenanceDataInfo.getColumnIndex(COL_SERIAL_NO),
                    objectRow.getString(objectDataInfo.getColumnIndex(COL_SERIAL_NO)));
                newRow.setValue(maintenanceDataInfo.getColumnIndex(COL_ARTICLE_NO),
                    objectRow.getString(objectDataInfo.getColumnIndex(COL_ARTICLE_NO)));

                ServiceUtils.fillContactValues(newRow, objectRow);
                ServiceUtils.fillCompanyValues(newRow, objectRow, parentForm.getViewName(),
                    COL_SERVICE_CUSTOMER, ALS_SERVICE_CUSTOMER_NAME, ALS_CUSTOMER_TYPE_NAME);
                ServiceUtils.fillContractorAndManufacturerValues(newRow, objectRow);

                RowFactory.createRow(maintenanceDataInfo, newRow, Modality.ENABLED);
              });
              return false;
            } else {
              return true;
            }
          }
        });
      }
    } else if (widget instanceof DataSelector
        && BeeUtils.in(name, COL_SERVICE_CUSTOMER, ALS_CONTACT_PERSON)) {
      ((DataSelector) widget).addSelectorHandler(this);
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    if (!forced) {
      save(result);
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    ServiceHelper.setGridEnabled(form, TBL_SERVICE_ITEMS, false);
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    save(result);

    ParameterList params = ServiceKeeper.createArgs(SVC_UPDATE_SERVICE_MAINTENANCE_OBJECT);
    params.addDataItem(COL_SERVICE_OBJECT, result.getId());
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.isEmpty() && !response.hasErrors()) {
          Queries.getRow(TBL_SERVICE_MAINTENANCE, response.getResponseAsLong(), new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              if (result != null) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), TBL_SERVICE_MAINTENANCE, result);
              }
            }
          });
        }
      }
    });
  }

  @Override
  public FormInterceptor getInstance() {
    return new ServiceObjectForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    Dictionary loc = Localized.dictionary();

    Global.inputCollection(loc.mainCriteria(), loc.name(), true, criteriaEditors.keySet(),
        new Consumer<Collection<String>>() {
          @Override
          public void accept(Collection<String> collection) {
            Map<String, Editor> oldCriteria = new HashMap<>(criteriaEditors);
            criteriaEditors.clear();

            for (String crit : collection) {
              Editor input = oldCriteria.get(crit);

              if (input == null) {
                input = createAutocomplete(VIEW_SERVICE_DISTINCT_VALUES,
                    COL_SERVICE_CRITERION_VALUE, crit);
              }
              criteriaEditors.put(crit, input);
            }
            render();
          }
        },
        new Function<String, Editor>() {
          @Override
          public Editor apply(String value) {
            Editor editor = createAutocomplete(VIEW_SERVICE_DISTINCT_CRITERIA,
                COL_SERVICE_CRITERION_NAME, null);
            editor.setValue(value);
            return editor;
          }
        });
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    if (save(null)) {
      if (messages.size() == 1) {
        String msg = BeeUtils.joinItems(messages.get(0), Localized.dictionary().mainCriteria());
        messages.clear();
        messages.add(msg);

      } else {
        messages.add(BeeUtils.joinWords(Localized.dictionary().changedValues(),
            Localized.dictionary().mainCriteria()));
      }
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (event.hasView(VIEW_COMPANY_PERSONS)
        && BeeUtils.isTrue(getActiveRow().getBoolean(getDataIndex(COL_COMPANY_TYPE_PERSON)))) {
      Long companyId = getActiveRow().getLong(getDataIndex(COL_SERVICE_CUSTOMER));
      Queries.getRow(VIEW_COMPANY_PERSONS, Filter.equals(COL_COMPANY, companyId), null,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow companyPersonRow) {
              if (companyPersonRow != null && DataUtils.isId(companyPersonRow.getId())) {
                DataInfo targetDataInfo = Data.getDataInfo(getViewName());
                DataInfo sourceDataInfo = Data.getDataInfo(VIEW_COMPANY_PERSONS);

                getActiveRow().setValue(targetDataInfo.getColumnIndex(ALS_CONTACT_PERSON),
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

                getFormView().refreshBySource(ALS_CONTACT_PERSON);
              }
            }
          });
    }
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isChanged()
        && BeeUtils.same(event.getRelatedViewName(), VIEW_COMPANIES)) {
      getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_CONTACT_PERSON));
      getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_CONTACT_FIRST_NAME));
      getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_CONTACT_LAST_NAME));
      getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_CONTACT_PHONE));
      getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_CONTACT_ADDRESS));
      getActiveRow().clearCell(Data.getColumnIndex(getViewName(), ALS_CONTACT_EMAIL));
      getFormView().refreshBySource(ALS_CONTACT_PERSON);

    } else if (event.isChanged()
        && BeeUtils.same(event.getRelatedViewName(), VIEW_COMPANY_PERSONS)) {
      Long objectCustomerId = getActiveRow().getLong(getDataIndex(COL_SERVICE_CUSTOMER));

      if (event.getRelatedRow() != null && !DataUtils.isId(objectCustomerId)) {
        Long company = event.getRelatedRow().getLong(Data
            .getColumnIndex(event.getRelatedViewName(), COL_COMPANY));

        if (DataUtils.isId(company)) {
          getActiveRow().setValue(getDataIndex(COL_SERVICE_CUSTOMER), company);
          RelationUtils.updateRow(Data.getDataInfo(getViewName()), COL_SERVICE_CUSTOMER,
              getActiveRow(), Data.getDataInfo(event.getRelatedViewName()),
              event.getRelatedRow(), false);
          getFormView().refreshBySource(COL_SERVICE_CUSTOMER);
        }
      }
    }
  }

  @Override
  public void onLoad(FormView form) {
    EventUtils.clearRegistry(registry);
    registry.add(BeeKeeper.getBus().registerRowActionHandler(this));
    registry.add(BeeKeeper.getBus().registerRowUpdateHandler(this, false));
    registry.add(BeeKeeper.getBus().registerDataChangeHandler(this, false));
  }

  @Override
  public void onRowAction(RowActionEvent event) {
    if (event != null && event.isCreateRow() && event.hasRow()
        && event.hasAnyView(TaskConstants.VIEW_TASKS, TaskConstants.VIEW_RECURRING_TASKS)
        && DomUtils.isOrHasChild(getFormView().asWidget(), event.getOptions())
        && getActiveRow() != null) {

      String address = getStringValue(COL_SERVICE_ADDRESS);

      if (!BeeUtils.isEmpty(address)) {
        Data.setValue(event.getViewName(), event.getRow(), TaskConstants.COL_SUMMARY, address);
      }

      Long customer = getLongValue(COL_SERVICE_CUSTOMER);

      if (DataUtils.isId(customer)) {
        RelationUtils.copyWithDescendants(
            Data.getDataInfo(getViewName()), COL_SERVICE_CUSTOMER, getActiveRow(),
            Data.getDataInfo(event.getViewName()), COL_COMPANY, event.getRow());
      }
    }
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

    if (event.hasView(getViewName()) && Objects.equals(event.getRowId(), getActiveRowId())) {
      form.updateRow(event.getRow(), true);
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    if (event.isEmpty()) {
      save(getActiveRow());
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    requery(row);
    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    requery(newRow);
  }

  @Override
  public void onUnload(FormView form) {
    EventUtils.clearRegistry(registry);
  }

  private Autocomplete createAutocomplete(String viewName, String column, String value) {
    Autocomplete input = Autocomplete.create(Relation.create(viewName,
        Lists.newArrayList(column)), true);

    input.addAutocompleteHandler(new AutocompleteFilter(null, value));
    return input;
  }

  private void render() {
    if (criteriaPanel == null) {
      return;
    }
    criteriaPanel.clear();

    if (criteriaEditors.size() > 0) {
      int h = 0;
      FlowPanel labelContainer = new FlowPanel();
      labelContainer.getElement().getStyle().setMarginRight(5, Unit.PX);
      criteriaPanel.add(labelContainer);

      FlowPanel inputContainer = new FlowPanel();
      inputContainer.addStyleName(StyleUtils.NAME_FLEXIBLE);
      criteriaPanel.add(inputContainer);

      for (Entry<String, Editor> entry : criteriaEditors.entrySet()) {
        Label label = new Label(entry.getKey());
        StyleUtils.setTextAlign(label.getElement(), TextAlign.RIGHT);
        SimplePanel labelDiv = new SimplePanel(label);
        labelContainer.add(labelDiv);

        Widget editor = entry.getValue().asWidget();
        StyleUtils.fullWidth(editor);
        SimplePanel editorDiv = new SimplePanel(editor);
        inputContainer.add(editorDiv);

        if (!BeeUtils.isPositive(h)) {
          h = BeeUtils.max(labelDiv.getElement().getClientHeight(),
              editorDiv.getElement().getClientHeight());

          if (!BeeUtils.isPositive(h)) {
            h = 20;
          }
          h += 5;
        }

        StyleUtils.setHeight(labelDiv, h);
        StyleUtils.setHeight(editorDiv, h);
      }
    }
  }

  private void requery(IsRow row) {
    criteriaHistory.clear();
    criteriaEditors.clear();
    criteriaIds.clear();
    criteriaGroupId = null;

    render();

    Long objId = row.getId();
    if (!DataUtils.isId(objId)) {
      return;
    }

    Filter filter = Filter.and(Filter.equals(COL_SERVICE_OBJECT, objId),
        Filter.isNull(COL_SERVICE_CRITERIA_GROUP_NAME));

    Queries.getRowSet(VIEW_SERVICE_OBJECT_CRITERIA, null, filter, new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.getNumberOfRows() > 0) {
          criteriaGroupId = result.getRow(0).getId();

          for (BeeRow crit : result) {
            String name = DataUtils.getString(result, crit, COL_SERVICE_CRITERION_NAME);

            if (!BeeUtils.isEmpty(name)) {
              String value = DataUtils.getString(result, crit, COL_SERVICE_CRITERION_VALUE);

              Autocomplete box = createAutocomplete(VIEW_SERVICE_DISTINCT_VALUES,
                  COL_SERVICE_CRITERION_VALUE, name);

              box.setValue(value);

              criteriaHistory.put(name, value);
              criteriaEditors.put(name, box);

              criteriaIds.put(name, DataUtils.getLong(result, crit, "ID"));
            }
          }

          render();
        }
      }
    });
  }

  private boolean save(final IsRow row) {
    final Map<String, String> newValues = new LinkedHashMap<>();
    Map<Long, String> changedValues = new HashMap<>();

    CompoundFilter flt = Filter.or();
    final Holder<Integer> holder = Holder.of(0);

    for (String crit : criteriaEditors.keySet()) {
      String value = criteriaEditors.get(crit).getValue();
      value = BeeUtils.isEmpty(value) ? null : value;
      Long id = criteriaIds.get(crit);

      if (!criteriaHistory.containsKey(crit) || !Objects.equals(value, criteriaHistory.get(crit))) {
        if (DataUtils.isId(id)) {
          changedValues.put(id, value);
        } else {
          newValues.put(crit, value);
        }
        holder.set(holder.get() + 1);
      }
    }

    for (String crit : criteriaIds.keySet()) {
      if (!criteriaEditors.containsKey(crit)) {
        flt.add(Filter.compareId(criteriaIds.get(crit)));
      }
    }
    if (!flt.isEmpty()) {
      holder.set(holder.get() + 1);
    }

    if (row == null) {
      return BeeUtils.isPositive(holder.get());
    }

    final ScheduledCommand scheduler = new ScheduledCommand() {
      @Override
      public void execute() {
        holder.set(holder.get() - 1);

        if (!BeeUtils.isPositive(holder.get())) {
          Queries.getRow(getViewName(), row.getId(), new RowUpdateCallback(getViewName()) {
            @Override
            public void onSuccess(BeeRow result) {
              super.onSuccess(result);

              GridView gridView = getGridView();
              if (gridView != null) {
                gridView.getGrid().refresh();
              }
            }
          });
        }
      }
    };

    if (!BeeUtils.isEmpty(newValues)) {
      final Consumer<Long> consumer = new Consumer<Long>() {
        @Override
        public void accept(Long id) {
          List<BeeColumn> columns = Data.getColumns(VIEW_SERVICE_CRITERIA,
              Lists.newArrayList(COL_SERVICE_CRITERIA_GROUP,
                  COL_SERVICE_CRITERION_NAME, COL_SERVICE_CRITERION_VALUE));

          for (Entry<String, String> entry : newValues.entrySet()) {
            List<String> values = Lists.newArrayList(BeeUtils.toString(id),
                entry.getKey(), entry.getValue());

            Queries.insert(VIEW_SERVICE_CRITERIA, columns, values, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                scheduler.execute();
              }
            });
          }
        }
      };

      if (!DataUtils.isId(criteriaGroupId)) {
        Queries.insert(VIEW_SERVICE_CRITERIA_GROUPS,
            Data.getColumns(VIEW_SERVICE_CRITERIA_GROUPS, Lists.newArrayList(COL_SERVICE_OBJECT)),
            Lists.newArrayList(BeeUtils.toString(row.getId())), null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                consumer.accept(result.getId());
              }
            });
      } else {
        consumer.accept(criteriaGroupId);
      }
    }

    if (!BeeUtils.isEmpty(changedValues)) {
      for (Entry<Long, String> entry : changedValues.entrySet()) {
        Queries.update(VIEW_SERVICE_CRITERIA, Filter.compareId(entry.getKey()),
            COL_SERVICE_CRITERION_VALUE, new TextValue(entry.getValue()), new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                scheduler.execute();
              }
            });
      }
    }

    if (!flt.isEmpty()) {
      Queries.delete(VIEW_SERVICE_CRITERIA, flt, new IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          scheduler.execute();
        }
      });
    }

    return true;
  }
}
