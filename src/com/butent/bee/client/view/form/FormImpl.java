package com.butent.bee.client.view.form;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dialog.TabulationHandler;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.event.logical.ActiveWidgetChangeEvent;
import com.butent.bee.client.event.logical.DataRequestEvent;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.client.event.logical.SelectionCountChangeEvent;
import com.butent.bee.client.event.logical.SortEvent;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HandlesRendering;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesValueChange;
import com.butent.bee.client.ui.HasRowChildren;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetCreationCallback;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.utils.EvalHelper;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.validation.CellValidateEvent.Handler;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.validation.ValidationOrigin;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.AddEndEvent;
import com.butent.bee.client.view.add.AddStartEvent;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormImpl extends Absolute implements FormView, PreviewHandler, TabulationHandler {

  private final class CreationCallback extends WidgetCreationCallback {

    private CreationCallback() {
      super();
    }

    @Override
    public void onSuccess(WidgetDescription result, IdentifiableWidget widget) {
      if (result == null) {
        onFailure("widget description is null");
        return;
      }

      final String id = result.getWidgetId();
      final FormWidget type = result.getWidgetType();
      if (type == null) {
        onFailure("widget type is null", id);
        return;
      }

      if (result.isDisablable() && !BeeUtils.isEmpty(id)) {
        getDisablableWidgets().add(id);
      }

      final String source = result.getSource();
      final int index;

      if (!BeeUtils.isEmpty(source) && hasData()) {
        index = getDataIndex(source);
        if (index < 0) {
          onFailure("widget id:", id, "source:", source, "not found");
        }
      } else {
        index = BeeConst.UNDEF;
      }

      CellSource cellSource;
      if (index >= 0) {
        cellSource = CellSource.forColumn(getDataColumns().get(index), index);
      } else if (!BeeUtils.isEmpty(result.getRowProperty()) && widget instanceof HasValueType) {
        cellSource = CellSource.forProperty(result.getRowProperty(),
            ((HasValueType) widget).getValueType());
      } else {
        cellSource = null;
      }

      DisplayWidget displayWidget = null;

      if (type.isDisplay() || widget instanceof HandlesRendering) {
        AbstractCellRenderer renderer = null;
        if (getFormInterceptor() != null) {
          renderer = getFormInterceptor().getRenderer(result);
        }

        if (renderer == null) {
          renderer = RendererFactory.getRenderer(result.getRendererDescription(),
              result.getRender(), result.getRenderTokens(), result.getEnumKey(),
              NameUtils.toList(result.getRenderColumns()), getDataColumns(), cellSource,
              result.getRelation());
        }

        if (widget instanceof HandlesRendering) {
          ((HandlesRendering) widget).setRenderer(renderer);
          renderer = null;
        }

        if (type.isDisplay()) {
          displayWidget = new DisplayWidget(index, renderer, result);
          getDisplayWidgets().add(displayWidget);
        }
      }

      if (type.isEditable()) {
        EditableWidget editableWidget = new EditableWidget(getDataColumns(), index,
            result, displayWidget);
        getEditableWidgets().add(editableWidget);

        result.setNullable(editableWidget.isNullable());
        result.setHasDefaults(editableWidget.hasDefaults());

        if (getFormInterceptor() != null) {
          getFormInterceptor().afterCreateEditableWidget(editableWidget, widget);
        }
      }

      super.onSuccess(result, widget);
    }
  }

  private final class DataObserver implements HandlesAllDataEvents {

    private final Collection<com.google.web.bindery.event.shared.HandlerRegistration> registry;

    private DataObserver() {
      this.registry = BeeKeeper.getBus().registerDataHandler(this, false);
    }

    @Override
    public void onCellUpdate(CellUpdateEvent event) {
      if (isEventRelevant(event) && isRowRelevant(event.getRowId())) {
        FormImpl.this.onCellUpdate(event);
      }
    }

    @Override
    public void onDataChange(DataChangeEvent event) {
      if (isEventRelevant(event) && event.hasCancel()) {
        cancelForm();
      }
    }

    @Override
    public void onMultiDelete(MultiDeleteEvent event) {
      if (isEventRelevant(event)) {
        for (RowInfo rowInfo : event.getRows()) {
          if (isRowRelevant(rowInfo.getId())) {
            cancelForm();
            break;
          }
        }
      }
    }

    @Override
    public void onRowDelete(RowDeleteEvent event) {
      if (isEventRelevant(event) && isRowRelevant(event.getRowId())) {
        cancelForm();
      }
    }

    @Override
    public void onRowInsert(RowInsertEvent event) {
    }

    @Override
    public void onRowUpdate(RowUpdateEvent event) {
      if (isEventRelevant(event) && isRowRelevant(event.getRowId())) {
        FormImpl.this.onRowUpdate(event);
      }
    }

    private void cancelForm() {
      FormImpl.this.getViewPresenter().handleAction(Action.CANCEL);
    }

    private boolean isEventRelevant(DataEvent event) {
      return event.hasView(FormImpl.this.getViewName()) && !FormImpl.this.isClosed()
          && DataUtils.isId(FormImpl.this.getActiveRowId());
    }

    private boolean isRowRelevant(long rowId) {
      return FormImpl.this.getActiveRowId() == rowId;
    }

    private void stop() {
      for (com.google.web.bindery.event.shared.HandlerRegistration entry : registry) {
        if (entry != null) {
          entry.removeHandler();
        }
      }
    }
  }

  private static final class TabEntry implements Comparable<TabEntry> {
    private final int tabIndex;
    private final int order;
    private final String widgetId;

    private TabEntry(int tabIndex, int order, String widgetId) {
      this.tabIndex = tabIndex;
      this.order = order;
      this.widgetId = widgetId;
    }

    @Override
    public int compareTo(TabEntry o) {
      Assert.notNull(o);

      int res = Integer.valueOf(getTabIndex()).compareTo(o.getTabIndex());
      if (res == BeeConst.COMPARE_EQUAL) {
        res = Integer.valueOf(getOrder()).compareTo(o.getOrder());
      }
      return res;
    }

    private int getOrder() {
      return order;
    }

    private int getTabIndex() {
      return tabIndex;
    }

    private String getWidgetId() {
      return widgetId;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(FormImpl.class);

  private static final String STYLE_FORM = StyleUtils.CLASS_NAME_PREFIX + "Form";
  private static final String STYLE_FORM_DISABLED = StyleUtils.CLASS_NAME_PREFIX + "Form-"
      + StyleUtils.NAME_DISABLED;
  private static final String STYLE_WIDGET_DISABLED = StyleUtils.CLASS_NAME_PREFIX
      + StyleUtils.NAME_DISABLED;

  private static final String NEW_ROW_CAPTION = "Create New";

  private final String formName;

  private Presenter viewPresenter;

  private IdentifiableWidget rootWidget;

  private Evaluator rowEditable;
  private Evaluator rowValidation;

  private final Notification notification = new Notification();

  private boolean enabled = true;

  private boolean hasData;
  private String viewName;
  private List<BeeColumn> dataColumns;

  private boolean editing;
  private boolean adding;

  private int pageStart;
  private int rowCount = BeeConst.UNDEF;

  private IsRow activeRow;
  private IsRow oldRow;

  private IsRow rowBuffer;
  private JavaScriptObject rowJso;

  private boolean readOnly;

  private String caption;
  private boolean showRowId;

  private boolean printFooter;
  private boolean printHeader;

  private FormInterceptor formInterceptor;

  private final CreationCallback creationCallback = new CreationCallback();
  private final List<String> disablableWidgets = new ArrayList<>();

  private final Set<DisplayWidget> displayWidgets = new HashSet<>();
  private final List<EditableWidget> editableWidgets = new ArrayList<>();

  private final List<TabEntry> tabOrder = new ArrayList<>();

  private String previewId;

  private int activeEditableIndex = BeeConst.UNDEF;

  private Dimensions dimensions;

  private State state;

  private DataObserver dataObserver;

  private String options;
  private final Map<String, String> properties = new HashMap<>();

  public FormImpl(String formName) {
    super(Position.RELATIVE, Overflow.AUTO);
    this.formName = formName;

    if (!BeeUtils.isEmpty(formName)) {
      addStyleName(StyleUtils.CLASS_NAME_PREFIX + "form-" + formName.trim());
    }
  }

  @Override
  public HandlerRegistration addActiveRowChangeHandler(ActiveRowChangeEvent.Handler handler) {
    return addHandler(handler, ActiveRowChangeEvent.getType());
  }

  @Override
  public HandlerRegistration addAddEndHandler(AddEndEvent.Handler handler) {
    return addHandler(handler, AddEndEvent.getType());
  }

  @Override
  public HandlerRegistration addAddStartHandler(AddStartEvent.Handler handler) {
    return addHandler(handler, AddStartEvent.getType());
  }

  @Override
  public HandlerRegistration addCellValidationHandler(String columnId, Handler handler) {
    Assert.notEmpty(columnId);
    EditableWidget editableWidget = getEditableWidgetBySource(columnId, true);

    if (editableWidget == null) {
      return null;
    } else {
      return editableWidget.addCellValidationHandler(handler);
    }
  }

  @Override
  public HandlerRegistration addDataRequestHandler(DataRequestEvent.Handler handler) {
    return addHandler(handler, DataRequestEvent.getType());
  }

  @Override
  public HandlerRegistration addReadyForInsertHandler(ReadyForInsertEvent.Handler handler) {
    return addHandler(handler, ReadyForInsertEvent.getType());
  }

  @Override
  public HandlerRegistration addReadyForUpdateHandler(ReadyForUpdateEvent.Handler handler) {
    return addHandler(handler, ReadyForUpdateEvent.getType());
  }

  @Override
  public HandlerRegistration addSaveChangesHandler(SaveChangesEvent.Handler handler) {
    return addHandler(handler, SaveChangesEvent.getType());
  }

  @Override
  public HandlerRegistration addScopeChangeHandler(ScopeChangeEvent.Handler handler) {
    return addHandler(handler, ScopeChangeEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionCountChangeHandler(
      SelectionCountChangeEvent.Handler handler) {
    return null;
  }

  @Override
  public HandlerRegistration addSortHandler(SortEvent.Handler handler) {
    return null;
  }

  @Override
  public void applyOptions(String opt) {
  }

  @Override
  public boolean checkOnClose(NativePreviewEvent event) {
    if (isChildEditing()) {
      return false;
    } else {
      if (event != null) {
        event.cancel();
      }
      return checkForUpdate(false);
    }
  }

  @Override
  public boolean checkOnSave(NativePreviewEvent event) {
    if (isChildEditing()) {
      return false;
    } else {
      if (event != null) {
        event.cancel();
      }
      return checkForUpdate(false);
    }
  }

  @Override
  public void clearNotifications() {
    if (getNotification() != null) {
      getNotification().clear();
    }
  }

  @Override
  public void create(FormDescription formDescription, String view, List<BeeColumn> dataCols,
      boolean addStyle, FormInterceptor interceptor) {
    Assert.notNull(formDescription);

    setViewName(BeeUtils.notEmpty(view, formDescription.getViewName()));
    setDataColumns(dataCols);
    setHasData(!BeeUtils.isEmpty(dataCols));

    setFormInterceptor(interceptor);
    if (interceptor != null) {
      interceptor.setFormView(this);
    }

    if (hasData()) {
      Calculation calc = formDescription.getRowEditable();
      if (calc != null) {
        setRowEditable(Evaluator.create(calc, null, dataCols));
      }

      calc = formDescription.getRowValidation();
      if (calc != null) {
        setRowValidation(Evaluator.create(calc, null, dataCols));
      }
    }

    setReadOnly(formDescription.isReadOnly());

    setCaption(formDescription.getCaption());
    setShowRowId(formDescription.showRowId());

    setPrintHeader(formDescription.printHeader());
    setPrintFooter(formDescription.printFooter());

    setDimensions(formDescription.getDimensions());

    setOptions(formDescription.getOptions());
    setProperties(formDescription.getProperties());

    IdentifiableWidget root = FormFactory.createForm(formDescription, getViewName(), dataCols,
        creationCallback, interceptor);
    if (root == null) {
      return;
    }

    if (addStyle) {
      StyleUtils.makeAbsolute(root.asWidget());
      root.asWidget().addStyleName(STYLE_FORM);
    }
    setRootWidget(root);

    add(root);
    add(getNotification());

    creationCallback.bind(this, getId());

    if (interceptor != null) {
      interceptor.afterCreate(this);
    }
  }

  @Override
  public void editRow(IsRow rowValue, Scheduler.ScheduledCommand focusCommand) {
    IsRow row = DataUtils.cloneRow(rowValue);

    boolean upd;
    if (getFormInterceptor() != null) {
      upd = getFormInterceptor().onStartEdit(this, row, focusCommand);
    } else {
      upd = true;
    }

    if (upd) {
      updateRow(row, true);
      if (focusCommand != null) {
        focusCommand.execute();
      }
    }

    if (hasData() && DataUtils.hasId(row)) {
      Global.getNewsAggregator().onAccess(getViewName(), row.getId());
    }
  }

  @Override
  public void finishNewRow(IsRow rowValue) {
    fireEvent(new AddEndEvent(false));

    if (rowValue != null) {
      setActiveRow(rowValue);
    } else {
      setActiveRow(getRowBuffer());
    }

    refreshData(true, true);

    if (rowValue != null) {
      int rc = getRowCount();
      setPageStart(rc, false, false, NavigationOrigin.SYSTEM);
      setRowCount(rc + 1, false);
      fireScopeChange();
    }

    setAdding(false);
  }

  @Override
  public boolean focus(String source) {
    if (BeeUtils.isEmpty(source)) {
      return false;
    }

    Widget widget = getWidgetBySource(source);
    return UiHelper.focus(widget);
  }

  @Override
  public IsRow getActiveRow() {
    return activeRow;
  }

  @Override
  public long getActiveRowId() {
    return (getActiveRow() == null) ? BeeConst.LONG_UNDEF : getActiveRow().getId();
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public Collection<RowChildren> getChildrenForInsert() {
    Collection<RowChildren> result = new ArrayList<>();

    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.getEditor() instanceof HasRowChildren) {
        RowChildren children = ((HasRowChildren) editableWidget.getEditor()).getChildrenForInsert();
        if (children != null) {
          result.add(children);
        }
      }
    }

    return result;
  }

  @Override
  public Collection<RowChildren> getChildrenForUpdate() {
    Collection<RowChildren> result = new ArrayList<>();

    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.getEditor() instanceof HasRowChildren) {
        RowChildren children = ((HasRowChildren) editableWidget.getEditor()).getChildrenForUpdate();
        if (children != null) {
          result.add(children);
        }
      }
    }

    return result;
  }

  @Override
  public List<BeeColumn> getDataColumns() {
    return dataColumns;
  }

  @Override
  public int getDataIndex(String source) {
    return DataUtils.getColumnIndex(source, getDataColumns());
  }

  @Override
  public DateTime getDateTimeValue(String source) {
    int index = getDataIndex(source);
    if (getActiveRow() != null && index >= 0) {
      return getActiveRow().getDateTime(index);
    } else {
      return null;
    }
  }

  @Override
  public JustDate getDateValue(String source) {
    int index = getDataIndex(source);
    if (getActiveRow() != null && index >= 0) {
      return getActiveRow().getDate(index);
    } else {
      return null;
    }
  }

  @Override
  public HasDataTable getDisplay() {
    return this;
  }

  @Override
  public Double getDoubleValue(String source) {
    int index = getDataIndex(source);
    if (getActiveRow() != null && index >= 0) {
      return getActiveRow().getDouble(index);
    } else {
      return null;
    }
  }

  @Override
  public List<EditableWidget> getEditableWidgets() {
    return editableWidgets;
  }

  @Override
  public FormInterceptor getFormInterceptor() {
    return formInterceptor;
  }

  @Override
  public String getFormName() {
    return formName;
  }

  @Override
  public CssUnit getHeightUnit() {
    return (getDimensions() == null) ? null : getDimensions().getHeightUnit();
  }

  @Override
  public Double getHeightValue() {
    return (getDimensions() == null) ? null : getDimensions().getHeightValue();
  }

  @Override
  public Integer getIntegerValue(String source) {
    int index = getDataIndex(source);
    if (getActiveRow() != null && index >= 0) {
      return getActiveRow().getInteger(index);
    } else {
      return null;
    }
  }

  @Override
  public Long getLongValue(String source) {
    int index = getDataIndex(source);
    if (getActiveRow() != null && index >= 0) {
      return getActiveRow().getLong(index);
    } else {
      return null;
    }
  }

  @Override
  public IsRow getOldRow() {
    return oldRow;
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public int getPageSize() {
    return 1;
  }

  @Override
  public int getPageStart() {
    return pageStart;
  }

  @Override
  public Element getPrintElement() {
    return getRootWidget().asWidget().getElement();
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public String getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public IdentifiableWidget getRootWidget() {
    return rootWidget;
  }

  @Override
  public int getRowCount() {
    return rowCount;
  }

  @Override
  public List<? extends IsRow> getRowData() {
    List<IsRow> data = new ArrayList<>();
    if (getActiveRow() != null) {
      data.add(getActiveRow());
    }
    return data;
  }

  @Override
  public JavaScriptObject getRowJso() {
    if (!hasData() || getActiveRow() == null) {
      return null;
    }

    if (rowJso == null) {
      setRowJso(EvalHelper.createJso(getDataColumns()));
    }
    EvalHelper.toJso(getDataColumns(), getActiveRow(), rowJso);

    return rowJso;
  }

  @Override
  public Order getSortOrder() {
    return null;
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public String getStringValue(String source) {
    int index = getDataIndex(source);
    if (getActiveRow() != null && index >= 0) {
      return getActiveRow().getString(index);
    } else {
      return null;
    }
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public Widget getWidgetByName(String name) {
    Assert.notEmpty(name);
    String id = creationCallback.getWidgetIdByName(name);

    Widget widget = getWidgetById(id);
    if (widget == null) {
      logger.warning("widget not found:", name);
    }

    return widget;
  }

  @Override
  public Widget getWidgetBySource(String source) {
    Assert.notEmpty(source);

    EditableWidget editableWidget = getEditableWidgetBySource(source, false);
    if (editableWidget != null) {
      return getWidgetById(editableWidget.getWidgetId());
    }

    DisplayWidget displayWidget = getDisplayWidgetBySource(source, false);
    return (displayWidget == null) ? null : getWidgetById(displayWidget.getWidgetId());
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public CssUnit getWidthUnit() {
    return (getDimensions() == null) ? null : getDimensions().getWidthUnit();
  }

  @Override
  public Double getWidthValue() {
    return (getDimensions() == null) ? null : getDimensions().getWidthValue();
  }

  @Override
  public boolean handlesTabulation() {
    return true;
  }

  @Override
  public boolean hasNotifications() {
    return getNotification() != null && getNotification().isActive();
  }

  @Override
  public boolean isEditing() {
    return editing;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isFlushable() {
    return isAdding() || isEditing();
  }

  @Override
  public boolean isInteractive() {
    return isAttached() && !isClosed() && DomUtils.isVisible(getElement());
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public boolean isRowEditable(IsRow rowValue, boolean warn) {
    if (rowValue == null) {
      return false;
    }

    boolean ok = rowValue.isEditable();

    if (ok && getFormInterceptor() != null) {
      ok = getFormInterceptor().isRowEditable(rowValue);
    }

    if (ok && getRowEditable() != null) {
      getRowEditable().update(rowValue);
      ok = BeeUtils.toBoolean(getRowEditable().evaluate());
    }

    if (!ok && warn) {
      notifyWarning(Localized.getConstants().rowIsReadOnly());
    }
    return ok;
  }

  @Override
  public boolean isRowEnabled(IsRow rowValue) {
    return !isReadOnly() && isEnabled() && isRowEditable(rowValue, false);
  }

  @Override
  public void notifyInfo(String... messages) {
    showNote(LogLevel.INFO, messages);
  }

  @Override
  public void notifySevere(String... messages) {
    showNote(LogLevel.ERROR, messages);
  }

  @Override
  public void notifyWarning(String... messages) {
    showNote(LogLevel.WARNING, messages);
  }

  @Override
  public void observeData() {
    if (getDataObserver() == null) {
      setDataObserver(new DataObserver());
    }
  }

  @Override
  public boolean observesData() {
    return getDataObserver() != null;
  }

  @Override
  public void onActiveWidgetChange(ActiveWidgetChangeEvent event) {
    if (event.isActive()) {
      for (int i = 0; i < getEditableWidgets().size(); i++) {
        if (BeeUtils.same(event.getWidgetId(), getEditableWidgets().get(i).getWidgetId())) {
          setActiveEditableIndex(i);
          break;
        }
      }
    } else {
      setActiveEditableIndex(BeeConst.UNDEF);
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    Assert.notNull(event);

    IsRow rowValue = getActiveRow();
    long rowId = event.getRowId();
    if (rowValue == null || rowValue.getId() != rowId) {
      return;
    }

    event.applyTo(rowValue);
    if (getOldRow() != null && getOldRow().getId() == rowId) {
      event.applyTo(getOldRow());
    }
    String source = event.getSourceName();

    boolean wasRowEnabled = isRowEnabled(rowValue);

    Set<String> refreshed = new HashSet<>();
    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.hasSource(source)) {
        editableWidget.refresh(rowValue);
        refreshed.add(editableWidget.getWidgetId());
      }
    }

    if (!isReadOnly() && isEnabled()) {
      boolean rowEnabled = isRowEditable(rowValue, false);

      for (EditableWidget editableWidget : getEditableWidgets()) {
        if (editableWidget.isReadOnly() || !isWidgetDisablable(editableWidget.getWidgetId())) {
          continue;
        }
        Editor editor = editableWidget.getEditor();
        if (editor == null) {
          continue;
        }

        boolean editable = rowEnabled && editableWidget.isEditable(rowValue);
        if (editable != editor.isEnabled()) {
          editor.setEnabled(editable);
          editor.asWidget().setStyleName(STYLE_WIDGET_DISABLED, !editable);
        }
      }

      if (rowEnabled != wasRowEnabled) {
        Set<String> editableIds = new HashSet<>();
        for (EditableWidget editableWidget : getEditableWidgets()) {
          editableIds.add(editableWidget.getWidgetId());
        }

        for (String id : getDisablableWidgets()) {
          if (!editableIds.contains(id)) {
            Widget widget = getWidgetById(id);
            if (widget instanceof HasEnabled && rowEnabled != ((HasEnabled) widget).isEnabled()) {
              ((HasEnabled) widget).setEnabled(rowEnabled);
              widget.setStyleName(STYLE_WIDGET_DISABLED, !rowEnabled);
            }
          }
        }

        refreshChildWidgets(rowValue);
      }
    }

    refreshDisplayWidgets(refreshed);
  }

  @Override
  public void onClose(final CloseCallback closeCallback) {
    Assert.notNull(closeCallback);
    if (!hasData() || getOldRow() == null || getActiveRow() == null
        || BeeUtils.isEmpty(getViewName())) {
      closeCallback.onClose();
      return;
    }

    boolean isNew = DataUtils.isNewRow(getActiveRow());

    List<String> messages = new ArrayList<>();
    List<String> updatedLabels = new ArrayList<>();

    final BeeRowSet rowSet =
        DataUtils.getUpdated(getViewName(), getDataColumns(), getOldRow(), getActiveRow(), null);
    if (!DataUtils.isEmpty(rowSet)) {
      updatedLabels.addAll(Localized.getLabels(rowSet.getColumns()));
    }

    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.getEditor() instanceof HandlesValueChange
          && ((HandlesValueChange) editableWidget.getEditor()).isValueChanged()) {

        String label = ((HandlesValueChange) editableWidget.getEditor()).getLabel();
        if (!BeeUtils.isEmpty(label) && !updatedLabels.contains(label)) {
          updatedLabels.add(label);
        }
      }
    }

    if (!updatedLabels.isEmpty()) {
      String msg = isNew ? Localized.getConstants().newValues()
          : Localized.getConstants().changedValues();
      messages.add(msg + BeeConst.STRING_SPACE
          + BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, updatedLabels));
    }

    if (getFormInterceptor() != null) {
      getFormInterceptor().onClose(messages, getOldRow(), getActiveRow());
    }

    if (messages.isEmpty()) {
      closeCallback.onClose();
      return;
    }

    messages.add(isNew ? Localized.getConstants().createNewRow()
        : Localized.getConstants().saveChanges());

    DecisionCallback callback = new DecisionCallback() {
      @Override
      public void onCancel() {
        if (!DataUtils.isEmpty(rowSet)) {
          for (BeeColumn column : rowSet.getColumns()) {
            if (focus(column.getId())) {
              return;
            }
          }
        }
      }

      @Override
      public void onConfirm() {
        closeCallback.onSave();
      }

      @Override
      public void onDeny() {
        closeCallback.onClose();
      }
    };

    String cap = (getViewPresenter() == null) ? getCaption() : getViewPresenter().getCaption();
    Global.decide(cap, messages, callback, DialogConstants.DECISION_YES);
  }

  @Override
  public void onEditEnd(EditEndEvent event, EditEndEvent.HasEditEndHandler source) {
    Assert.notNull(event);

    IsRow rowValue = getActiveRow();
    IsColumn column = event.getColumn();

    Integer keyCode = event.getKeyCode();
    String widgetId = event.getWidgetId();
    boolean hasModifiers = event.hasModifiers();

    String newValue = event.getNewValue();

    if (column == null) {
      if (source instanceof EditableWidget && ((EditableWidget) source).hasRowProperty()) {
        String propertyName = ((EditableWidget) source).getRowPropertyName();
        String oldValue = rowValue.getProperty(propertyName);

        if (!BeeUtils.equalsTrim(oldValue, newValue)) {
          logger.debug(propertyName, "old:", oldValue, "new:", newValue);
          rowValue.setProperty(propertyName, newValue);
        }
      }

      navigate(keyCode, hasModifiers, widgetId);
      return;
    }

    int index = getDataIndex(column.getId());
    String oldValue = rowValue.getString(index);

    if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
      logger.debug(column.getId(), "old:", oldValue, "new:", newValue);

      if (isFlushable()) {
        rowValue.setValue(index, newValue);

        Set<String> refreshed = new HashSet<>();

        if (event.hasRelation() && source instanceof EditableWidget) {
          Collection<String> updatedColumns =
              ((EditableWidget) source).maybeUpdateRelation(getViewName(), rowValue, false);

          refreshed.addAll(refreshEditableWidget(index));

          if (!BeeUtils.isEmpty(updatedColumns)) {
            for (String uc : updatedColumns) {
              if (!column.getId().equals(uc)) {
                refreshed.addAll(refreshEditableWidget(getDataIndex(uc)));
              }
            }
          }

        } else if (event.isRowMode()) {
          refreshed.addAll(refreshEditableWidgets());
        }

        refreshDisplayWidgets(refreshed);

      } else {
        fireUpdate(rowValue, column, oldValue, newValue, event.isRowMode());
      }
    }

    navigate(keyCode, hasModifiers, widgetId);
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    if (isInteractive()) {
      String type = event.getNativeEvent().getType();

      if (EventUtils.isClick(type)) {
        if (!BeeUtils.isEmpty(getPreviewId())) {
          setPreviewId(null);
          event.cancel();
        }

      } else if (EventUtils.isMouseDown(type)) {
        if (!BeeConst.isUndef(getActiveEditableIndex())) {
          EditableWidget editableWidget = getEditableWidgets().get(getActiveEditableIndex());

          if (!editableWidget.getEditor().isOrHasPartner(targetNode)) {
            if (!editableWidget.checkForUpdate(true)) {
              setPreviewId(editableWidget.getWidgetId());
              event.cancel();
            }
          }
        }
      }
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (getRootWidget().getId().equals(source.getId())) {
      ElementSize.copyScroll(source, target);
    }
    return true;
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    Assert.notNull(event);
    IsRow newRow = event.getRow();

    if (DataUtils.sameId(getActiveRow(), newRow)) {
      setActiveRow(newRow);
      refreshData(false, false);
    }
  }

  @Override
  public void prepareForInsert() {
    if (!validate(this, true)) {
      return;
    }

    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (int i = 0; i < getDataColumns().size(); i++) {
      String value = getActiveRow().getString(i);
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      BeeColumn column = getDataColumns().get(i);
      if (column.isEditable()) {
        columns.add(column);
        values.add(value);
      }
    }

    if (columns.isEmpty()) {
      notifySevere("New Row", "all columns cannot be empty");
      return;
    }

    AutocompleteProvider.retainValues(this);

    RowCallback callback = new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        notifySevere(reason);
        finishNewRow(null);
      }

      @Override
      public void onSuccess(BeeRow result) {
        if (getFormInterceptor() != null) {
          getFormInterceptor().afterInsertRow(result, false);
        }
        finishNewRow(result);
      }
    };

    ReadyForInsertEvent event = new ReadyForInsertEvent(columns, values, getChildrenForInsert(),
        callback, getId());
    if (getFormInterceptor() != null) {
      getFormInterceptor().onReadyForInsert(this, event);
      if (event.isConsumed()) {
        return;
      }
    }

    fireEvent(event);
  }

  @Override
  public void preserveActiveRow(List<? extends IsRow> values) {
  }

  @Override
  public boolean printFooter() {
    return printFooter;
  }

  @Override
  public boolean printHeader() {
    return printHeader;
  }

  @Override
  public void refresh() {
    refresh(true, false);
  }

  @Override
  public void refresh(boolean refreshChildren, boolean focus) {
    refreshData(refreshChildren, focus);
  }

  @Override
  public int refreshBySource(String source) {
    Assert.notEmpty(source);
    Set<String> refreshed = new HashSet<>();

    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.hasSource(source)) {
        String id = editableWidget.getWidgetId();
        Widget widget = DomUtils.getChildQuietly(this, id);

        if (widget != null) {
          editableWidget.refresh(getActiveRow());
          refreshed.add(id);
        }
      }
    }

    for (DisplayWidget displayWidget : getDisplayWidgets()) {
      if (displayWidget.hasSource(source) && !refreshed.contains(displayWidget.getWidgetId())) {
        String id = displayWidget.getWidgetId();
        Widget widget = DomUtils.getChildQuietly(this, id);

        if (widget != null) {
          displayWidget.refresh(widget, getActiveRow());
          refreshed.add(id);
        }
      }
    }

    return refreshed.size();
  }

  @Override
  public void refreshChildWidgets(IsRow rowValue) {
    BeeKeeper.getBus().fireEventFromSource(new ParentRowEvent(getViewName(), rowValue,
        isRowEnabled(rowValue)), getId());
  }

  @Override
  public boolean removeRowById(long rowId) {
    if (getActiveRow() != null && getActiveRow().getId() == rowId) {
      setActiveRow(null);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void reset() {
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;

    for (String id : getDisablableWidgets()) {
      Widget widget = getWidgetById(id);
      if (widget instanceof HasEnabled) {
        ((HasEnabled) widget).setEnabled(enabled);
        widget.setStyleName(STYLE_WIDGET_DISABLED, !enabled);
      }
    }

    getRootWidget().asWidget().setStyleName(STYLE_FORM_DISABLED, !enabled);
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
  }

  @Override
  public void setHeightUnit(CssUnit heightUnit) {
    if (getDimensions() != null) {
      getDimensions().setHeightUnit(heightUnit);
    }
  }

  @Override
  public void setHeightValue(Double heightValue) {
    if (getDimensions() != null) {
      getDimensions().setHeightValue(heightValue);
    }
  }

  @Override
  public void setPageSize(int size, boolean fireScopeChange) {
  }

  @Override
  public void setPageStart(int start, boolean fireScopeChange, boolean fireDataRequest,
      NavigationOrigin origin) {
    Assert.nonNegative(start);
    if (start == getPageStart()) {
      return;
    }

    this.pageStart = start;

    if (fireScopeChange) {
      fireScopeChange();
    }
    if (fireDataRequest) {
      fireDataRequest(origin);
    }
  }

  @Override
  public void setRowCount(int count, boolean fireScopeChange) {
    Assert.nonNegative(count);
    if (count == getRowCount()) {
      return;
    }

    this.rowCount = count;

    if (getPageStart() >= count) {
      setPageStart(Math.max(count - 1, 0), true, false, NavigationOrigin.SYSTEM);
    } else if (fireScopeChange) {
      fireScopeChange();
    }
  }

  @Override
  public void setRowData(List<? extends IsRow> values, boolean refresh) {
    if (BeeUtils.isEmpty(values)) {
      setActiveRow(null);
    } else {
      setActiveRow(values.get(0));
    }

    if (refresh) {
      refresh(true, false);
    }
  }

  @Override
  public void setState(State state) {
    this.state = state;

    if (State.OPEN.equals(state)) {
      Previewer.ensureRegistered(this);
    } else if (State.CLOSED.equals(state)) {
      Previewer.ensureUnregistered(this);
    }
  }

  @Override
  public void setViewPresenter(Presenter presenter) {
    this.viewPresenter = presenter;
  }

  @Override
  public void setWidthUnit(CssUnit widthUnit) {
    if (getDimensions() != null) {
      getDimensions().setWidthUnit(widthUnit);
    }
  }

  @Override
  public void setWidthValue(Double widthValue) {
    if (getDimensions() != null) {
      getDimensions().setWidthValue(widthValue);
    }
  }

  @Override
  public void start(Integer count) {
    if (!getTabOrder().isEmpty()) {
      getTabOrder().clear();
    }

    for (EditableWidget editableWidget : getEditableWidgets()) {
      editableWidget.bind(this);

      if (editableWidget.isFocusable() && editableWidget.getEditor() != null) {
        int tabIndex = editableWidget.getEditor().getTabIndex();
        if (tabIndex >= 0) {
          getTabOrder().add(new TabEntry(tabIndex, getTabOrder().size(),
              editableWidget.getWidgetId()));
        }
      }
    }

    if (getTabOrder().size() > 1) {
      Collections.sort(getTabOrder());
    }

    if (count != null) {
      setRowCount(count, true);
      if (count > 0) {
        fireDataRequest(NavigationOrigin.SYSTEM);
      } else {
        setActiveRow(null);
      }
    }

    if (getFormInterceptor() != null) {
      getFormInterceptor().onStart(this);
    }
  }

  @Override
  public void startNewRow(boolean copy) {
    setAdding(true);
    fireEvent(new AddStartEvent(NEW_ROW_CAPTION, false));

    IsRow row = getActiveRow();
    setRowBuffer(row);
    if (row == null) {
      row = DataUtils.createEmptyRow(getDataColumns().size());
    }
    IsRow newRow = DataUtils.createEmptyRow(getDataColumns().size());

    if (getActiveRow() != null && copy) {
      for (int i = 0; i < getDataColumns().size(); i++) {
        if (!row.isNull(i)) {
          newRow.setValue(i, row.getString(i));
        }
      }
    }

    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.hasCarry() && editableWidget.hasColumn()) {
        String carry = editableWidget.getCarryValue(row);
        if (!BeeUtils.isEmpty(carry)) {
          newRow.setValue(editableWidget.getDataIndex(), carry);
        }
      }
    }

    if (getFormInterceptor() != null) {
      getFormInterceptor().onStartNewRow(this, row, newRow);
    }

    setActiveRow(newRow);
    refreshData(true, true);
  }

  @Override
  public void updateCell(String columnId, String newValue) {
    Assert.notEmpty(columnId);

    IsRow rowValue = getActiveRow();
    if (rowValue == null) {
      notifySevere("update cell:", columnId, newValue, "form has no data");
      return;
    }

    int index = getDataIndex(columnId);
    if (BeeConst.isUndef(index)) {
      notifySevere("update cell:", columnId, newValue, "column not found");
      return;
    }

    String oldValue = rowValue.getString(index);

    if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
      if (isFlushable()) {
        rowValue.setValue(index, newValue);
        Set<String> refreshed = refreshEditableWidget(index);
        refreshDisplayWidgets(refreshed);
      } else {
        BeeColumn column = getDataColumns().get(index);
        fireUpdate(rowValue, column, oldValue, newValue, column.isForeign());
      }
    }
  }

  @Override
  public void updateRow(IsRow rowValue, boolean refreshChildren) {
    setActiveRow(rowValue);
    render(refreshChildren);
  }

  @Override
  public boolean validate(NotificationListener notificationListener, boolean focusOnError) {
    boolean ok = true;

    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (!editableWidget.validate(ValidationOrigin.FORM)) {
        if (focusOnError && editableWidget.getEditor() != null) {
          editableWidget.getEditor().setFocus(true);
        }

        ok = false;
        break;
      }
    }

    if (ok && getActiveRow() != null) {
      ok = ValidationHelper.validateRow(getActiveRow(), getRowValidation(), notificationListener);
    }
    return ok;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    Previewer.ensureRegistered(this);

    if (getFormInterceptor() != null) {
      getFormInterceptor().onLoad(this);
    }
  }

  @Override
  protected void onUnload() {
    if (getFormInterceptor() != null) {
      getFormInterceptor().onUnload(this);
    }

    Previewer.ensureUnregistered(this);
    if (getDataObserver() != null) {
      getDataObserver().stop();
    }

    super.onUnload();
  }

  private boolean checkForUpdate(boolean normalize) {
    if (BeeConst.isUndef(getActiveEditableIndex())) {
      return true;
    }

    EditableWidget editableWidget = getEditableWidgets().get(getActiveEditableIndex());
    if (editableWidget == null) {
      return true;
    } else {
      return editableWidget.checkForUpdate(normalize);
    }
  }

  private void fireDataRequest(NavigationOrigin origin) {
    fireEvent(new DataRequestEvent(origin));
  }

  private void fireScopeChange() {
    fireEvent(new ScopeChangeEvent(getPageStart(), getPageSize(), getRowCount()));
  }

  private void fireUpdate(IsRow rowValue, final IsColumn column, String oldValue,
      final String newValue, boolean rowMode) {
    fireEvent(new ReadyForUpdateEvent(rowValue, column, oldValue, newValue, rowMode,
        new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            notifySevere(reason);
          }

          @Override
          public void onSuccess(BeeRow result) {
          }
        }));
  }

  private void focus(int index, boolean forward, boolean cycle) {
    if (!BeeUtils.isIndex(getTabOrder(), index)) {
      return;
    }
    if (hasData() && !isRowEnabled(getActiveRow())) {
      return;
    }

    Widget widget = getWidgetById(getTabOrder().get(index).getWidgetId());
    if (UiHelper.focus(widget)) {
      return;
    }

    int size = getTabOrder().size();
    if (size <= 1) {
      return;
    }

    int i;
    boolean md;

    if (forward) {
      if (index < size - 1) {
        i = index + 1;
        md = true;
      } else {
        i = 0;
        md = false;
      }
    } else {
      if (index > 0) {
        i = index - 1;
        md = true;
      } else {
        i = size - 1;
        md = false;
      }
    }

    if (cycle || md) {
      focus(i, forward, cycle && md);
    }
  }

  private int getActiveEditableIndex() {
    return activeEditableIndex;
  }

  private DataObserver getDataObserver() {
    return dataObserver;
  }

  private Dimensions getDimensions() {
    return dimensions;
  }

  private List<String> getDisablableWidgets() {
    return disablableWidgets;
  }

  private DisplayWidget getDisplayWidgetBySource(String source, boolean warn) {
    for (DisplayWidget displayWidget : getDisplayWidgets()) {
      if (displayWidget.hasSource(source)) {
        return displayWidget;
      }
    }

    if (warn) {
      logger.warning("display widget not found:", source);
    }
    return null;
  }

  private Set<DisplayWidget> getDisplayWidgets() {
    return displayWidgets;
  }

  private EditableWidget getEditableWidgetBySource(String source, boolean warn) {
    for (EditableWidget editableWidget : getEditableWidgets()) {
      if (editableWidget.hasSource(source)) {
        return editableWidget;
      }
    }

    if (warn) {
      logger.warning("editable widget not found:", source);
    }
    return null;
  }

  private Notification getNotification() {
    return notification;
  }

  private String getPreviewId() {
    return previewId;
  }

  private IsRow getRowBuffer() {
    return rowBuffer;
  }

  private Evaluator getRowEditable() {
    return rowEditable;
  }

  private Evaluator getRowValidation() {
    return rowValidation;
  }

  private List<TabEntry> getTabOrder() {
    return tabOrder;
  }

  private Widget getWidgetById(String id) {
    return DomUtils.getChildQuietly(this, id);
  }

  private boolean hasData() {
    return hasData;
  }

  private boolean isAdding() {
    return adding;
  }

  private boolean isChildEditing() {
    for (GridView gridView : ViewHelper.getGrids(getRootWidget().asWidget())) {
      if (gridView.getGrid().isEditing()) {
        return true;
      }
    }
    return false;
  }

  private boolean isClosed() {
    return State.CLOSED.equals(getState());
  }

  private boolean isReadOnly() {
    return readOnly;
  }

  private boolean isWidgetDisablable(String widgetId) {
    return getDisablableWidgets().contains(widgetId);
  }

  private void navigate(Integer keyCode, boolean hasModifiers, String widgetId) {
    if (keyCode != null && !BeeUtils.isEmpty(widgetId) && getTabOrder().size() > 1 && !isClosed()) {
      switch (BeeUtils.unbox(keyCode)) {
        case KeyCodes.KEY_ENTER:
        case KeyCodes.KEY_DOWN:
          navigate(widgetId, true);
          break;

        case KeyCodes.KEY_TAB:
          navigate(widgetId, !hasModifiers);
          break;

        case KeyCodes.KEY_UP:
          navigate(widgetId, false);
          break;
      }
    }
  }

  private void navigate(String widgetId, boolean forward) {
    if (BeeUtils.isEmpty(widgetId)) {
      return;
    }
    int cnt = getTabOrder().size();
    if (cnt <= 1) {
      return;
    }

    int idx = BeeConst.UNDEF;
    for (int i = 0; i < cnt; i++) {
      if (BeeUtils.same(getTabOrder().get(i).getWidgetId(), widgetId)) {
        idx = i;
        break;
      }
    }
    if (BeeConst.isUndef(idx)) {
      return;
    }

    boolean cycle;
    if (forward) {
      if (idx < cnt - 1) {
        idx++;
        cycle = true;
      } else {
        idx = 0;
        cycle = false;
      }
    } else {
      if (idx > 0) {
        idx--;
        cycle = true;
      } else {
        idx = cnt - 1;
        cycle = false;
      }
    }

    focus(idx, forward, cycle);
  }

  private void refreshData(boolean refreshChildren, boolean focus) {
    render(refreshChildren);

    if (focus) {
      focus(0, true, false);
    }
  }

  private void refreshDisplayWidgets(Set<String> skip) {
    for (DisplayWidget displayWidget : getDisplayWidgets()) {
      String id = displayWidget.getWidgetId();
      if (skip != null && skip.contains(id)) {
        continue;
      }

      Widget widget = DomUtils.getChildQuietly(this, id);
      if (widget == null) {
        logger.warning("refresh display:", id, "widget not found");
      } else {
        displayWidget.refresh(widget, getActiveRow());
      }
    }
  }

  private Set<String> refreshEditableWidget(int dataIndex) {
    Set<String> refreshed = new HashSet<>();

    if (!BeeConst.isUndef(dataIndex)) {
      for (EditableWidget editableWidget : getEditableWidgets()) {
        if (editableWidget.getDataIndex() == dataIndex) {
          editableWidget.refresh(getActiveRow());
          refreshed.add(editableWidget.getWidgetId());
        }
      }
    }
    return refreshed;
  }

  private Set<String> refreshEditableWidgets() {
    Set<String> refreshed = new HashSet<>();

    boolean rowEnabled = isRowEnabled(getActiveRow());
    boolean isNew = DataUtils.isNewRow(getActiveRow());

    for (EditableWidget editableWidget : getEditableWidgets()) {
      Editor editor = editableWidget.getEditor();
      if (editor == null) {
        continue;
      }

      boolean editable;

      if (getActiveRow() == null) {
        editable = false;

      } else {
        editable = rowEnabled;

        if (editable && editableWidget.isReadOnly()) {
          if (isNew && editableWidget.hasColumn()) {
            BeeColumn column = editableWidget.getDataColumn();
            editable = column.isEditable() && !column.isNullable() && !column.hasDefaults();
          } else {
            editable = false;
          }
        }

        if (editable) {
          editable = editableWidget.isEditable(getActiveRow());
        }
      }

      editableWidget.refresh(getActiveRow());
      if (editable != editor.isEnabled() && isWidgetDisablable(editableWidget.getWidgetId())) {
        editor.setEnabled(editable);
        editor.asWidget().setStyleName(STYLE_WIDGET_DISABLED, !editable);
      }

      refreshed.add(editableWidget.getWidgetId());
    }
    return refreshed;
  }

  private void render(boolean refreshChildren) {
    if (getFormInterceptor() != null) {
      getFormInterceptor().beforeRefresh(this, getActiveRow());
    }

    Set<String> refreshed = refreshEditableWidgets();
    refreshDisplayWidgets(refreshed);

    if (refreshChildren) {
      refreshChildWidgets(getActiveRow());
    }

    fireEvent(new ActiveRowChangeEvent(getActiveRow()));

    if (getFormInterceptor() != null) {
      getFormInterceptor().afterRefresh(this, getActiveRow());
    }

    if (showRowId() && getViewPresenter() != null) {
      long rowId = (getActiveRow() == null) ? BeeConst.UNDEF : getActiveRow().getId();
      String message = DataUtils.isId(rowId) ? BeeUtils.bracket(rowId) : BeeConst.STRING_EMPTY;

      getViewPresenter().getHeader().setMessage(message);
    }
  }

  private void setActiveEditableIndex(int activeEditableIndex) {
    this.activeEditableIndex = activeEditableIndex;
  }

  private void setActiveRow(IsRow activeRow) {
    if (getFormInterceptor() != null) {
      getFormInterceptor().onSetActiveRow(activeRow);
    }
    setOldRow((activeRow == null) ? null : DataUtils.cloneRow(activeRow));
    this.activeRow = activeRow;
  }

  private void setAdding(boolean adding) {
    this.adding = adding;
  }

  private void setCaption(String caption) {
    this.caption = caption;
  }

  private void setDataColumns(List<BeeColumn> dataColumns) {
    this.dataColumns = dataColumns;
  }

  private void setDataObserver(DataObserver dataObserver) {
    this.dataObserver = dataObserver;
  }

  private void setDimensions(Dimensions dimensions) {
    this.dimensions = dimensions;
  }

  private void setFormInterceptor(FormInterceptor formInterceptor) {
    this.formInterceptor = formInterceptor;
  }

  private void setHasData(boolean hasData) {
    this.hasData = hasData;
  }

  private void setOldRow(IsRow oldRow) {
    this.oldRow = oldRow;
  }

  private void setOptions(String options) {
    this.options = options;
  }

  private void setPreviewId(String previewId) {
    this.previewId = previewId;
  }

  private void setPrintFooter(boolean printFooter) {
    this.printFooter = printFooter;
  }

  private void setPrintHeader(boolean printHeader) {
    this.printHeader = printHeader;
  }

  private void setProperties(Map<String, String> properties) {
    BeeUtils.overwrite(this.properties, properties);
  }

  private void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  private void setRootWidget(IdentifiableWidget rootWidget) {
    this.rootWidget = rootWidget;
  }

  private void setRowBuffer(IsRow rowBuffer) {
    this.rowBuffer = rowBuffer;
  }

  private void setRowEditable(Evaluator rowEditable) {
    this.rowEditable = rowEditable;
  }

  private void setRowJso(JavaScriptObject rowJso) {
    this.rowJso = rowJso;
  }

  private void setRowValidation(Evaluator rowValidation) {
    this.rowValidation = rowValidation;
  }

  private void setShowRowId(boolean showRowId) {
    this.showRowId = showRowId;
  }

  private void setViewName(String viewName) {
    this.viewName = viewName;
  }

  private void showNote(LogLevel level, String... messages) {
    StyleUtils.setZIndex(getNotification(), StyleUtils.getZIndex(getRootWidget().asWidget()) + 1);
    getNotification().show(level, messages);
  }

  private boolean showRowId() {
    return showRowId;
  }
}
