package com.butent.bee.client.view.search;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallbackWithId;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.communication.RpcParameter;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFilterSupplier implements HasViewName, HasOptions,
    NotificationListener {

  protected enum SupplierAction implements HasCaption {
    ALL(Localized.dictionary().filterAll()),
    CLEAR(Localized.dictionary().clear()),
    COMMIT(Localized.dictionary().doFilter()),
    CANCEL(Localized.dictionary().cancel());

    private final String caption;

    SupplierAction(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    private String getStyleSuffix() {
      return name().toLowerCase();
    }
  }

  protected static final String NULL_VALUE_LABEL = Localized.dictionary().filterNullLabel();
  protected static final String NOT_NULL_VALUE_LABEL =
      Localized.dictionary().filterNotNullLabel();

  protected static final String DEFAULT_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX
      + "FilterSupplier-";

  private static final String BIN_SIZE_CELL_STYLE_SUFFIX = "binSizeCell";

  private final String viewName;

  private final BeeColumn column;
  private final String columnLabel;

  private String options;

  private boolean filterChanged;

  private Popup dialog;

  private Filter effectiveFilter;
  private boolean filtersOnCommit;

  private String counterId;
  private int counterValue;

  private final List<Integer> selectedItems = new ArrayList<>();

  private String displayId;

  public AbstractFilterSupplier(String viewName, BeeColumn column, String label, String options) {
    this.viewName = viewName;

    this.column = column;
    this.columnLabel = BeeUtils.notEmpty(label,
        (column == null) ? null : Localized.getLabel(column));

    this.options = options;
  }

  @Override
  public void clearNotifications() {
    getNotificationDelegate().clearNotifications();
  }

  public void ensureData() {
  }

  public String getComponentLabel(String ownerLabel) {
    return isEmpty() ? null : BeeUtils.joinWords(ownerLabel, getLabel());
  }

  public Filter getFilter() {
    return parse(getFilterValue());
  }

  public abstract FilterValue getFilterValue();

  public abstract String getLabel();

  @Override
  public String getOptions() {
    return options;
  }

  public String getTitle() {
    if (Global.isDebug()) {
      Filter filter = getFilter();
      return (filter == null) ? null : filter.toString();
    } else {
      return getLabel();
    }
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public boolean hasNotifications() {
    return getNotificationDelegate().hasNotifications();
  }

  public boolean isEmpty() {
    return getFilterValue() == null;
  }

  @Override
  public void notifyInfo(String... messages) {
    getNotificationDelegate().notifyInfo(messages);
  }

  @Override
  public void notifySevere(String... messages) {
    getNotificationDelegate().notifySevere(messages);
  }

  @Override
  public void notifyWarning(String... messages) {
    getNotificationDelegate().notifyWarning(messages);
  }

  public abstract void onRequest(Element target, Scheduler.ScheduledCommand onChange);

  public abstract Filter parse(FilterValue input);

  public boolean retainInput() {
    return AutocompleteProvider.retainValues(getAutocompletableWidgets());
  }

  public void setEffectiveFilter(Filter effectiveFilter) {
    this.effectiveFilter = effectiveFilter;
  }

  public abstract void setFilterValue(FilterValue filterValue);

  public void setFiltersOnCommit(boolean filtersOnCommit) {
    this.filtersOnCommit = filtersOnCommit;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  protected void addBinSize(HtmlTable display, int row, int col, String text) {
    display.setHtml(row, col, text);
    display.getCellFormatter().addStyleName(row, col,
        getStylePrefix() + BIN_SIZE_CELL_STYLE_SUFFIX);
  }

  protected void clearDisplay() {
    HtmlTable display = getDisplayAsTable();
    if (display == null) {
      return;
    }

    for (int row : getSelectedItems()) {
      display.getRowFormatter().removeStyleName(row, getStyleSelected());
    }
  }

  protected void clearSelection() {
    selectedItems.clear();
  }

  protected void closeDialog() {
    if (getDialog() != null) {
      getDialog().close();
      setDialog(null);
    }
  }

  protected void countSelection() {
    setCounter(selectedItems.size());
  }

  protected HtmlTable createDisplay(boolean addSelectionHandler) {
    final HtmlTable display = new HtmlTable(getDisplayStyle());

    if (addSelectionHandler) {
      Binder.addClickHandler(display, event -> {
        Element target = EventUtils.getEventTargetElement(event);
        TableRowElement rowElement = DomUtils.getParentRow(target, true);

        if (rowElement != null && display.getElement().isOrHasChild(rowElement)) {
          int rc = display.getRowCount();
          if (EventUtils.hasModifierKey(event.getNativeEvent()) && rc > 1) {
            for (int i = 0; i < rc; i++) {
              invertSelection(i, display.getRow(i));
            }

          } else {
            invertSelection(rowElement.getRowIndex(), rowElement);
          }

          countSelection();
        }
      });
    }

    setDisplayId(display.getId());
    return display;
  }

  protected boolean deselect(Integer item) {
    return selectedItems.remove(item);
  }

  protected void doAction(SupplierAction action) {
    switch (action) {
      case ALL:
        doAll();
        break;

      case CLEAR:
        doClear();
        break;

      case COMMIT:
        doCommit();
        break;

      case CANCEL:
        doCancel();
        break;
    }
  }

  protected void doAll() {
  }

  protected void doCancel() {
    closeDialog();
  }

  protected void doClear() {
    clearSelection();
    setCounter(0);
  }

  protected void doCommit() {
  }

  protected List<SupplierAction> getActions() {
    return new ArrayList<>();
  }

  protected List<? extends IdentifiableWidget> getAutocompletableWidgets() {
    return new ArrayList<>();
  }

  protected BeeColumn getColumn() {
    return column;
  }

  protected String getColumnId() {
    return (getColumn() == null) ? null : getColumn().getId();
  }

  protected String getColumnLabel() {
    return columnLabel;
  }

  protected ValueType getColumnType() {
    return (getColumn() == null) ? null : getColumn().getType();
  }

  protected Widget getCommandWidgets(boolean addCounter) {
    Flow panel = new Flow();
    panel.addStyleName(getStylePrefix() + "commandPanel");

    for (SupplierAction action : getActions()) {
      Button actionWidget = new Button(action.getCaption());
      actionWidget.addStyleName(getStylePrefix() + action.getStyleSuffix());

      actionWidget.addClickHandler(event -> doAction(action));

      panel.add(actionWidget);
    }

    if (addCounter) {
      Label counter = new Label();
      counter.addStyleName(getStylePrefix() + "counter");

      panel.add(counter);
      setCounterId(counter.getId());
    }

    return panel;
  }

  protected Widget getDialogChild(String id) {
    return DomUtils.getChildQuietly(getDialog(), id);
  }

  protected String getDialogStyle() {
    return DEFAULT_STYLE_PREFIX + "dialog";
  }

  protected HtmlTable getDisplayAsTable() {
    Widget widget = getDialogChild(getDisplayId());
    if (widget instanceof HtmlTable) {
      return (HtmlTable) widget;
    } else {
      return null;
    }
  }

  protected String getDisplayId() {
    return displayId;
  }

  protected String getDisplayStyle() {
    return DEFAULT_STYLE_PREFIX + "display";
  }

  protected Filter getEffectiveFilter() {
    return effectiveFilter;
  }

  protected Filter getEmptinessFilter(String columnId, Boolean emptiness) {
    if (emptiness == null) {
      return null;
    } else if (emptiness) {
      return Filter.isNull(columnId);
    } else {
      return Filter.notNull(columnId);
    }
  }

  protected String getEmptinessLabel(Boolean emptiness) {
    if (emptiness == null) {
      return null;
    } else if (emptiness) {
      return NULL_VALUE_LABEL;
    } else {
      return NOT_NULL_VALUE_LABEL;
    }
  }

  protected void getHistogram(final RpcCallback<SimpleRowSet> callback) {
    List<Property> props = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, getViewName());
    if (getEffectiveFilter() != null) {
      PropertyUtils.addProperties(props, Service.VAR_VIEW_WHERE, getEffectiveFilter().serialize());
    }
    String columns = NameUtils.join(getHistogramColumns());
    PropertyUtils.addProperties(props, Service.VAR_VIEW_COLUMNS, columns);

    List<String> order = getHistogramOrder();
    if (!BeeUtils.isEmpty(order)) {
      PropertyUtils.addProperties(props, Service.VAR_VIEW_ORDER, NameUtils.join(order));
    }

    if (!BeeUtils.isEmpty(getOptions())) {
      PropertyUtils.addProperties(props, Service.VAR_OPTIONS, getOptions());
    }

    ParameterList params = new ParameterList(Service.HISTOGRAM, RpcParameter.Section.DATA, props);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallbackWithId() {
      @Override
      public void onResponse(ResponseObject response) {
        if (Queries.checkResponse(Service.HISTOGRAM, getRpcId(), getViewName(), response,
            SimpleRowSet.class, callback)) {

          SimpleRowSet rs = SimpleRowSet.restore((String) response.getResponse());
          callback.onSuccess(rs);
        }
      }
    });
  }

  protected List<String> getHistogramColumns() {
    return Lists.newArrayList(getColumnId());
  }

  protected List<String> getHistogramOrder() {
    return Lists.newArrayList(getColumnId());
  }

  protected NotificationListener getNotificationDelegate() {
    return BeeKeeper.getScreen();
  }

  protected List<Integer> getSelectedItems() {
    return selectedItems;
  }

  protected String getStylePrefix() {
    return DEFAULT_STYLE_PREFIX;
  }

  protected boolean hasEmptiness() {
    return BeeUtils.containsSame(getOptions(), BeeConst.EMPTY);
  }

  protected boolean hasValue(FilterValue filterValue) {
    return filterValue != null && filterValue.hasValue();
  }

  protected void invertSelection(int index, Element element) {
    boolean wasSelected = isSelected(index);

    if (wasSelected) {
      element.removeClassName(getStyleSelected());
      deselect(index);
    } else {
      element.addClassName(getStyleSelected());
      select(index);
    }
  }

  protected boolean isColumnNullable() {
    return getColumn() != null && getColumn().isNullable();
  }

  protected boolean isSelected(Integer item) {
    return selectedItems.contains(item);
  }

  protected boolean isSelectionEmpty() {
    return selectedItems.isEmpty();
  }

  protected String messageAllEmpty(String count) {
    return Localized.dictionary().allValuesEmpty(getColumnLabel(), count);
  }

  protected String messageOneValue(String value, String count) {
    return Localized.dictionary().allValuesIdentical(getColumnLabel(), value, count);
  }

  protected void onDialogCancel() {
  }

  protected void openDialog(Element target, Widget widget, OpenEvent.Handler onOpen,
      final Scheduler.ScheduledCommand onChange) {

    Popup popup = new Popup(OutsideClick.CLOSE, getDialogStyle());
    DomUtils.preventSelection(popup);

    popup.setWidget(widget);
    popup.setHideOnEscape(true);

    popup.addCloseHandler(event -> {
      if (event.actionCancel()) {
        onDialogCancel();
      } else if (filterChanged()) {
        onChange.execute();
      }
    });

    Widget commitCommand = UiHelper.getChildByStyleName(widget,
        getStylePrefix() + SupplierAction.COMMIT.getStyleSuffix());

    if (commitCommand instanceof HasHtml) {
      ((HasHtml) commitCommand).setText(filtersOnCommit()
          ? Localized.dictionary().doFilter() : Localized.dictionary().actionSelect());

      popup.setOnSave(event -> doAction(SupplierAction.COMMIT));
    }

    setDialog(popup);
    setFilterChanged(false);

    if (onOpen != null) {
      popup.addOpenHandler(onOpen);
    }
    popup.showOnTop(target);
  }

  protected void select(Integer item) {
    selectedItems.add(item);
  }

  protected void selectRow(HtmlTable display, int row) {
    if (!isSelected(row)) {
      select(row);
      display.getRowFormatter().addStyleName(row, getStyleSelected());
    }
  }

  protected void setCounter(int count) {
    if (count != getCounterValue()) {
      setCounterValue(count);

      if (!BeeUtils.isEmpty(getCounterId()) && getDialog() != null) {
        Widget counter = DomUtils.getChildQuietly(getDialog(), getCounterId());
        if (counter != null) {
          String text = (count > 0) ? BeeUtils.toString(count) : BeeConst.STRING_EMPTY;
          counter.getElement().setInnerText(text);
        }
      }
    }
  }

  protected void setDisplayId(String displayId) {
    this.displayId = displayId;
  }

  protected void update(boolean valueChanged) {
    setFilterChanged(valueChanged);
    closeDialog();
  }

  protected Widget wrapDisplay(HtmlTable display, boolean addCounter) {
    Flow container = new Flow();
    container.addStyleName(getStylePrefix() + "container");

    Flow panel = new Flow();
    panel.addStyleName(getStylePrefix() + "panel");
    panel.add(display);

    container.add(panel);
    container.add(getCommandWidgets(addCounter));

    return container;
  }

  private boolean filterChanged() {
    return filterChanged;
  }

  private boolean filtersOnCommit() {
    return filtersOnCommit;
  }

  private String getCounterId() {
    return counterId;
  }

  private int getCounterValue() {
    return counterValue;
  }

  private Popup getDialog() {
    return dialog;
  }

  private String getStyleSelected() {
    return getStylePrefix() + "selected";
  }

  private void setCounterId(String counterId) {
    this.counterId = counterId;
  }

  private void setCounterValue(int counterValue) {
    this.counterValue = counterValue;
  }

  private void setDialog(Popup dialog) {
    this.dialog = dialog;
  }

  private void setFilterChanged(boolean filterChanged) {
    this.filterChanged = filterChanged;
  }
}
