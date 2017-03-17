package com.butent.bee.client.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Place;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.event.logical.DataRequestEvent;
import com.butent.bee.client.event.logical.MutationEvent;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.ui.WidgetCreationCallback;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.view.edit.HasEditState;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.ExtWidget;
import com.butent.bee.client.view.grid.GridFormEvent;
import com.butent.bee.client.view.grid.GridUtils;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.navigation.ScrollPager;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements design content for a grid container component.
 */

public class GridContainerImpl extends Split implements GridContainerView,
    HasSearch, ActiveRowChangeEvent.Handler, GridFormEvent.Handler, HasEditState,
    RenderingEvent.Handler, MutationEvent.Handler {

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "GridContainer";

  private static final String STYLE_HAS_DATA = STYLE_NAME + "-has-data";
  private static final String STYLE_NO_DATA = STYLE_NAME + "-no-data";

  private static final String STYLE_HAS_ACTIVE_ROW = STYLE_NAME + "-has-active-row";
  private static final String STYLE_NO_ACTIVE_ROW = STYLE_NAME + "-no-active-row";

  private static final String STYLE_SCROLLABLE = STYLE_NAME + "-scrollable";

  private static final Set<Action> HEADER_ACTIONS =
      EnumSet.of(Action.REFRESH, Action.FILTER, Action.REMOVE_FILTER, Action.ADD, Action.DELETE,
          Action.MENU, Action.CLOSE);

  private final String supplierKey;

  private Presenter viewPresenter;

  private String footerId;
  private String headerId;
  private String scrollerId;

  private final int scrollerWidth = DomUtils.getScrollBarWidth() + 1;

  private boolean hasPaging;
  private boolean hasSearch;

  private Evaluator rowMessage;

  private boolean editing;
  private boolean enabled = true;

  private final List<ExtWidget> extWidgets = new ArrayList<>();
  private WidgetCreationCallback extCreation;

  private IsRow lastRow;
  private boolean lastEnabled;

  private boolean resizeSuspended;

  public GridContainerImpl(String gridName, String supplierKey) {
    super(-1);

    addStyleName(STYLE_NAME);
    addStyleName(STYLE_NO_ACTIVE_ROW);

    if (!BeeUtils.isEmpty(gridName)) {
      addStyleName(BeeConst.CSS_CLASS_PREFIX + "grid-" + gridName.trim());
    }
    if (!BeeUtils.isEmpty(supplierKey) && !BeeUtils.same(gridName, supplierKey)
        && !BeeUtils.same(ViewFactory.SupplierKind.GRID.getKey(gridName), supplierKey)) {
      addStyleName(BeeConst.CSS_CLASS_PREFIX + "grid-" + supplierKey.trim());
    }

    this.supplierKey = supplierKey;
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    ReadyEvent.maybeDelegate(this);
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public void bind() {
    bindDisplay(getGridView().getGrid());

    getGridView().addGridFormHandler(this);
  }

  @Override
  public void bindDisplay(CellGrid display) {
    display.addActiveRowChangeHandler(this);
    display.addRenderingHandler(this);

    if (getRowMessage() != null && hasHeader()) {
      display.addMutationHandler(this);
    }

    if (hasFooter()) {
      display.addSelectionCountChangeHandler(getFooter());
    }
  }

  @Override
  public void create(GridDescription gridDescription, GridView gridView, int rowCount,
      Filter userFilter, GridInterceptor gridInterceptor, Collection<UiOption> uiOptions,
      GridFactory.GridOptions gridOptions) {

    if (!BeeUtils.isEmpty(uiOptions)) {
      addStyleName(UiOption.getStyleName(uiOptions));
    }

    Set<Action> enabledActions = GridUtils.getEnabledActions(gridDescription, gridInterceptor);
    Set<Action> disabledActions = GridUtils.getDisabledActions(gridDescription, gridInterceptor);

    setHasPaging(GridUtils.hasPaging(gridDescription, uiOptions, gridOptions));
    setHasSearch(UiOption.hasSearch(uiOptions) && !disabledActions.contains(Action.FILTER));

    boolean hasData = !BeeUtils.isEmpty(gridDescription.getViewName());
    boolean readOnly = BeeUtils.isTrue(gridDescription.isReadOnly())
        || hasData && !Data.isViewEditable(gridDescription.getViewName());

    HeaderView header;
    if (gridDescription.hasGridHeader()) {
      String caption = (gridInterceptor == null) ? null : gridInterceptor.getCaption();
      if (caption == null) {
        caption = (gridOptions == null) ? null : gridOptions.getCaption();
      }
      if (caption == null) {
        caption = gridDescription.getCaption();
      }

      if (!enabledActions.isEmpty()) {
        enabledActions.retainAll(HEADER_ACTIONS);
      }

      Set<Action> hiddenActions = new HashSet<>();

      if (hasSearch()) {
        enabledActions.add(Action.FILTER);

        if (!disabledActions.contains(Action.REMOVE_FILTER)) {
          enabledActions.add(Action.REMOVE_FILTER);
          if (userFilter == null) {
            hiddenActions.add(Action.REMOVE_FILTER);
          }
        }
      }

      int min = BeeUtils.unbox(gridDescription.getMinNumberOfRows());
      if (min > 0 && rowCount <= min) {
        disabledActions.add(Action.DELETE);
      }
      int max = BeeUtils.unbox(gridDescription.getMaxNumberOfRows());
      if (max > 0 && rowCount >= max) {
        disabledActions.add(Action.ADD);
      }

      if (!disabledActions.contains(Action.MENU)) {
        enabledActions.add(Action.MENU);
      }

      if (disabledActions.contains(Action.AUTO_FIT)) {
        header = new HeaderImpl();

      } else {
        FaLabel autoFit = new FaLabel(Action.AUTO_FIT.getIcon(), BeeConst.CSS_CLASS_PREFIX
            + Action.AUTO_FIT.getStyleSuffix());

        StyleUtils.enableAnimation(Action.AUTO_FIT, autoFit);
        autoFit.setTitle(Action.AUTO_FIT.getCaption());

        autoFit.addClickHandler(event -> getGridView().getGrid().autoFit(
            !EventUtils.hasModifierKey(event.getNativeEvent())));

        header = new HeaderImpl(autoFit);
      }

      header.create(caption, hasData, readOnly, gridDescription.getViewName(), uiOptions,
          enabledActions, disabledActions, hiddenActions);

    } else {
      header = null;
    }

    FooterView footer;
    ScrollPager scroller;

    if (hasPaging()) {
      footer = new FooterImpl();
      footer.create(rowCount, hasPaging(), true, false);
    } else {
      footer = null;
    }

    if (hasPaging()) {
      scroller = new ScrollPager();
    } else {
      scroller = null;
    }

    getExtWidgets().clear();
    if (gridDescription.hasWidgets()) {
      if (getExtCreation() == null) {
        setExtCreation(new WidgetCreationCallback());
      }

      for (String xml : gridDescription.getWidgets()) {
        ExtWidget extWidget = ExtWidget.create(xml, gridDescription.getViewName(),
            gridView.getDataColumns(), getExtCreation(), gridInterceptor);

        if (extWidget != null) {
          if (getExtCreation().getLastWidgetDescription() != null) {
            String widgetName = getExtCreation().getLastWidgetDescription().getWidgetName();

            if (!BeeUtils.isEmpty(widgetName)) {
              String key = BeeUtils.join(BeeConst.STRING_MINUS,
                  BeeUtils.notEmpty(getSupplierKey(), gridDescription.getName()),
                  BeeKeeper.getUser().getUserId(), widgetName, UiConstants.ATTR_SIZE);
              extWidget.setStorageKey(key);

              Integer size = BeeKeeper.getStorage().getInteger(key);
              if (BeeUtils.isPositive(size)) {
                extWidget.setSize(size);
              }
            }
          }

          getExtWidgets().add(extWidget);
        }
      }
    }

    addExtWidgets(ExtWidget.Component.HEADER);
    if (header != null) {
      addNorth(header, header.getHeight());
      setHeaderId(header.getWidgetId());
    }

    addExtWidgets(ExtWidget.Component.FOOTER);
    if (footer != null) {
      addSouth(footer, footer.getHeight());
      setFooterId(footer.getWidgetId());
    }

    addExtWidgets(null);
    if (scroller != null) {
      addEast(scroller, getScrollerWidth());
      setScrollerId(scroller.getWidgetId());

      addStyleName(STYLE_SCROLLABLE);
      sinkEvents(Event.ONMOUSEWHEEL);
    }

    add(gridView);

    if (gridDescription.getRowMessage() != null) {
      setRowMessage(Evaluator.create(gridDescription.getRowMessage(), null,
          gridView.getDataColumns()));
    }

    if (getExtCreation() != null) {
      getExtCreation().addBinding(gridDescription.getName(), getId(), gridDescription.getParent());
      getExtCreation().bind(this, getId());
    }
  }

  @Override
  public String getCaption() {
    if (isEditing()) {
      FormView activeForm = getGridView().getActiveForm();
      Presenter formPresenter = (activeForm == null) ? null : activeForm.getViewPresenter();
      String formCaption = (formPresenter == null) ? null : formPresenter.getRowMessageOrCaption();

      if (!BeeUtils.isEmpty(formCaption)) {
        return formCaption;
      }
    }

    return hasHeader() ? getHeader().getCaption() : null;
  }

  @Override
  public FooterView getFooter() {
    if (BeeUtils.isEmpty(getFooterId())) {
      return null;
    }
    for (Widget widget : getChildren()) {
      if (widget instanceof FooterView
          && BeeUtils.same(widget.getElement().getId(), getFooterId())) {
        return (FooterView) widget;
      }
    }
    return null;
  }

  @Override
  public GridView getGridView() {
    if (getCenter() instanceof GridView) {
      return (GridView) getCenter();
    } else {
      return null;
    }
  }

  @Override
  public HeaderView getHeader() {
    if (BeeUtils.isEmpty(getHeaderId())) {
      return null;
    }
    for (Widget widget : getChildren()) {
      if (widget instanceof HeaderView
          && BeeUtils.same(widget.getElement().getId(), getHeaderId())) {
        return (HeaderView) widget;
      }
    }
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "grid-container";
  }

  @Override
  public Collection<PagerView> getPagers() {
    if (hasPaging()) {
      return ViewHelper.getPagers(this);
    } else {
      return new HashSet<>();
    }
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public Collection<SearchView> getSearchers() {
    if (hasSearch()) {
      return ViewHelper.getSearchers(this);
    } else {
      return new HashSet<>();
    }
  }

  @Override
  public String getSupplierKey() {
    return supplierKey;
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public boolean hasSearch() {
    return hasSearch;
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
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    IsRow rowValue = event.getRowValue();
    GridView gridView = getGridView();

    boolean rowEnabled;
    if (rowValue == null) {
      rowEnabled = false;
    } else {
      rowEnabled = !gridView.isReadOnly() && isEnabled() && gridView.isEnabled()
          && gridView.isRowEditable(rowValue, null);
    }

    if (DataUtils.sameIdAndVersion(rowValue, getLastRow()) && rowEnabled == wasLastEnabled()) {
      return;
    }

    if ((rowValue == null) != (getLastRow() == null)) {
      setStyleName(STYLE_HAS_ACTIVE_ROW, rowValue != null);
      setStyleName(STYLE_NO_ACTIVE_ROW, rowValue == null);
    }

    maybeRefreshRowMessage(rowValue);

    if (gridView.getGridInterceptor() != null) {
      gridView.getGridInterceptor().onActiveRowChange(event);
    }

    String eventSource = BeeUtils.notEmpty(getViewPresenter().getEventSource(), getId());
    BeeKeeper.getBus().fireEventFromSource(new ParentRowEvent(gridView.getViewName(), rowValue,
        rowEnabled), eventSource);

    setLastRow(rowValue);
    setLastEnabled(rowEnabled);
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);
    if (isEditing() || !isEnabled()) {
      return;
    }

    if (event.getTypeInt() == Event.ONMOUSEWHEEL) {
      int y = event.getMouseWheelVelocityY();

      HasDataTable display = getGridView().getGrid();
      ScrollPager scroller = getScroller();

      if (y == 0 || display == null || scroller == null) {
        return;
      }
      if (display.isEditing()) {
        return;
      }

      EventTarget target = event.getEventTarget();
      if (target != null && scroller.getElement().isOrHasChild(Node.as(target))) {
        return;
      }

      event.preventDefault();
      if (EventUtils.isInputElement(target)) {
        return;
      }

      int rc = display.getRowCount();
      int start = display.getPageStart();
      int length = display.getPageSize();

      if (length > 0 && rc > length) {
        int p = -1;
        if (y > 0 && start + length < rc) {
          p = start + 1;
        } else if (y < 0 && start > 0) {
          p = start - 1;
        }

        if (p >= 0) {
          display.setPageStart(p, true, true, NavigationOrigin.MOUSE);
        }
      }
    }
  }

  @Override
  public void onGridForm(GridFormEvent event) {
    if (event.isOpening() || event.isClosing()) {
      setEditing(event.isOpening());

      if (!event.isPopup()) {
        showChildren(event.isClosing());

        if (!getGridView().isChild()) {
          BeeKeeper.getScreen().onWidgetChange(this);
        }
      }
    }
  }

  @Override
  public boolean onHistory(Place place, boolean forward) {
    return getGridView().onHistory(place, forward);
  }

  @Override
  public void onMutation(MutationEvent event) {
    maybeRefreshRowMessage(getGridView().getGrid().getActiveRow());
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;

    if (getGridView().getGrid().getElement().isOrHasChild(source)) {
      ok = true;

    } else if (getId().equals(source.getId())) {
      int width = source.getClientWidth();
      int height = source.getClientHeight();

      Element content = getGridView().getGrid().getElement();
      int delta = content.getScrollWidth() - content.getClientWidth();
      if (delta > 0) {
        width += delta;
      }
      delta = content.getScrollHeight() - content.getClientHeight();
      if (delta > 0) {
        height += delta;
      }

      StyleUtils.setSize(target, width, height);
      ok = true;

    } else if (hasHeader() && getHeader().asWidget().getElement().isOrHasChild(source)) {
      ok = getHeader().onPrint(source, target);

    } else if (hasFooter() && getFooter().asWidget().getElement().isOrHasChild(source)) {
      ok = getFooter().onPrint(source, target);

    } else if (hasScroller() && getScroller().asWidget().getElement().isOrHasChild(source)) {
      ok = getScroller().onPrint(source, target);

    } else {
      ok = true;
    }
    return ok;
  }

  @Override
  public void onRender(RenderingEvent event) {
    if (event != null && event.isAfter()) {
      boolean empty = getGridView().isEmpty();

      setStyleName(STYLE_HAS_DATA, !empty);
      setStyleName(STYLE_NO_DATA, empty);

      boolean hasActiveRow = !empty && getGridView().getActiveRow() != null;

      setStyleName(STYLE_HAS_ACTIVE_ROW, hasActiveRow);
      setStyleName(STYLE_NO_ACTIVE_ROW, !hasActiveRow);
    }
  }

  @Override
  public void onResize() {
    if (isAttached() && !isResizeSuspended()) {
      super.onResize();
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    GridView gridView = getGridView();
    return gridView != null && gridView.reactsTo(action);
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled != isEnabled()) {
      this.enabled = enabled;

      UiHelper.enableChildren(this, enabled);
      setStyleName(StyleUtils.NAME_DISABLED, !enabled);
    }
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
    for (Widget widget : getChildren()) {
      if (widget instanceof View && ((View) widget).getViewPresenter() == null) {
        ((View) widget).setViewPresenter(viewPresenter);
      }
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    Scheduler.get().scheduleDeferred(() -> {
      if (getGridView().getGridInterceptor() != null) {
        getGridView().getGridInterceptor().onLoad(getGridView());
      }

      CellGrid grid = getGridView().getGrid();

      if (hasPaging()) {
        Collection<PagerView> pagers = getPagers();
        if (pagers != null) {
          for (PagerView pager : pagers) {
            pager.start(grid);
          }
        }

        int ps = estimatePageSize();
        grid.setPageSize(ps, true);

        int ds = grid.getDataSize();
        if (ps > 0 && ps < ds) {
          grid.getRowData().subList(ps, ds).clear();
          grid.refresh();
        } else if (ps > 0 && ps > ds && ds < grid.getRowCount()) {
          DataRequestEvent.fire(grid, NavigationOrigin.SYSTEM);
        } else {
          grid.refresh();
        }

      } else {
        grid.refresh();
      }
    });
  }

  @Override
  protected void onSplitterMove(Splitter splitter, int by) {
    super.onSplitterMove(splitter, by);

    if (!getExtWidgets().isEmpty()) {
      Widget target = splitter.getTarget();
      int size = getWidgetSize(target);

      if (size > 0) {
        for (ExtWidget ew : getExtWidgets()) {
          if (DomUtils.sameId(ew.getWidget().asWidget(), target)
              && !BeeUtils.isEmpty(ew.getStorageKey())) {

            ew.setSize(size);
            BeeKeeper.getStorage().set(ew.getStorageKey(), size);

            break;
          }
        }
      }
    }
  }

  @Override
  protected void onUnload() {
    if (getViewPresenter() != null) {
      getViewPresenter().onViewUnload();
    }
    super.onUnload();
  }

  private void addExtWidgets(ExtWidget.Component before) {
    if (getExtWidgets().isEmpty()) {
      return;
    }

    boolean head = ExtWidget.Component.HEADER.equals(before);
    boolean foot = ExtWidget.Component.FOOTER.equals(before);

    boolean ok;
    for (ExtWidget extWidget : getExtWidgets()) {
      if (extWidget.precedesHeader()) {
        ok = head;
      } else if (extWidget.precedesFooter()) {
        ok = foot;
      } else {
        ok = !head && !foot;
      }

      if (ok) {
        add(extWidget.getWidget(), extWidget.getDirection(), extWidget.getSize(),
            extWidget.getSplSize());
      }
    }
  }

  private int estimatePageSize() {
    if (hasPaging()) {
      int w = 0;
      int h = 0;

      Element el = getElement();

      while (el != null && (w <= 0 || h <= 0)) {
        w = el.getClientWidth();
        h = el.getClientHeight();

        el = el.getParentElement();
      }

      return estimatePageSize(getGridView(), w, h);
    }
    return BeeConst.UNDEF;
  }

  private int estimatePageSize(GridView content, int containerWidth, int containerHeight) {
    if (content != null && containerHeight > 0) {
      int w = containerWidth;
      int h = containerHeight;

      if (hasHeader()) {
        h -= getHeader().getHeight();
      }
      if (hasFooter()) {
        h -= getFooter().getHeight();
      }
      if (hasScroller()) {
        w -= getScrollerWidth();
      }

      for (ExtWidget extWidget : getExtWidgets()) {
        if (extWidget.getDirection().isHorizontal()) {
          w -= extWidget.getTotalSize();
        } else {
          h -= extWidget.getTotalSize();
        }
      }

      return content.estimatePageSize(w, h);
    }
    return BeeConst.UNDEF;
  }

  private WidgetCreationCallback getExtCreation() {
    return extCreation;
  }

  private List<ExtWidget> getExtWidgets() {
    return extWidgets;
  }

  private String getFooterId() {
    return footerId;
  }

  private String getHeaderId() {
    return headerId;
  }

  private IsRow getLastRow() {
    return lastRow;
  }

  private Evaluator getRowMessage() {
    return rowMessage;
  }

  private ScrollPager getScroller() {
    if (BeeUtils.isEmpty(getScrollerId())) {
      return null;
    }
    for (Widget widget : getChildren()) {
      if (widget instanceof ScrollPager
          && BeeUtils.same(widget.getElement().getId(), getScrollerId())) {
        return (ScrollPager) widget;
      }
    }
    return null;
  }

  private String getScrollerId() {
    return scrollerId;
  }

  private int getScrollerWidth() {
    return scrollerWidth;
  }

  private boolean hasFooter() {
    return !BeeUtils.isEmpty(getFooterId());
  }

  private boolean hasHeader() {
    return !BeeUtils.isEmpty(getHeaderId());
  }

  private boolean hasPaging() {
    return hasPaging;
  }

  private boolean hasScroller() {
    return !BeeUtils.isEmpty(getScrollerId());
  }

  private boolean isResizeSuspended() {
    return resizeSuspended;
  }

  private void maybeRefreshRowMessage(IsRow row) {
    if (getRowMessage() != null && hasHeader()) {
      getHeader().showRowMessage(getRowMessage(), row);
    }
  }

  private void setExtCreation(WidgetCreationCallback extCreation) {
    this.extCreation = extCreation;
  }

  private void setFooterId(String footerId) {
    this.footerId = footerId;
  }

  private void setHasPaging(boolean hasPaging) {
    this.hasPaging = hasPaging;
  }

  private void setHasSearch(boolean hasSearch) {
    this.hasSearch = hasSearch;
  }

  private void setHeaderId(String headerId) {
    this.headerId = headerId;
  }

  private void setLastEnabled(boolean lastEnabled) {
    this.lastEnabled = lastEnabled;
  }

  private void setLastRow(IsRow lastRow) {
    this.lastRow = lastRow;
  }

  private void setResizeSuspended(boolean resizeSuspended) {
    this.resizeSuspended = resizeSuspended;
  }

  private void setRowMessage(Evaluator rowMessage) {
    this.rowMessage = rowMessage;
  }

  private void setScrollerId(String scrollerId) {
    this.scrollerId = scrollerId;
  }

  private void showChildren(boolean show) {
    setResizeSuspended(true);

    if (hasHeader()) {
      HeaderView header = getHeader();
      if (header != null) {
        setWidgetSize(header.asWidget(), show ? header.getHeight() : 0);
      }
    }
    if (hasFooter()) {
      FooterView footer = getFooter();
      if (footer != null) {
        setWidgetSize(footer.asWidget(), show ? footer.getHeight() : 0);
      }
    }
    if (hasScroller()) {
      ScrollPager scroller = getScroller();
      if (scroller != null) {
        setWidgetSize(scroller, show ? getScrollerWidth() : 0);
      }
    }

    for (ExtWidget extWidget : getExtWidgets()) {
      if (extWidget.isHidable()) {
        setWidgetSize(extWidget.getWidget().asWidget(), show ? extWidget.getSize() : 0);
      }
    }

    setResizeSuspended(false);
  }

  private boolean wasLastEnabled() {
    return lastEnabled;
  }
}
