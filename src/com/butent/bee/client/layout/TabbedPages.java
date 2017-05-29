package com.butent.bee.client.layout;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.style.ComputedStyles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowConsumer;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TabbedPages extends Flow implements
    HasBeforeSelectionHandlers<Pair<Integer, TabbedPages.SelectionOrigin>>,
    HasSelectionHandlers<Pair<Integer, TabbedPages.SelectionOrigin>>,
    RowConsumer {

  public enum SelectionOrigin {
    CLICK, INSERT, REMOVE, INIT, SCRIPT
  }

  private static final class Deck extends Complex {

    private String visibleId;
    private final Set<String> pendingResize = new HashSet<>();

    private Deck() {
      super(Position.RELATIVE);
    }

    @Override
    public String getIdPrefix() {
      return "deck";
    }

    @Override
    public void insert(Widget w, int beforeIndex) {
      DomUtils.ensureId(w, "dc");
      StyleUtils.occupy(w);
      VisibilityChangeEvent.hideAndFire(w);

      if (w instanceof RequiresResize) {
        String id = DomUtils.getId(w);

        if (!BeeUtils.isEmpty(id)) {
          pendingResize.add(id);
        }
      }

      super.insert(w, beforeIndex);
    }

    @Override
    public void onResize() {
      pendingResize.clear();

      for (int i = 0; i < getWidgetCount(); i++) {
        resize(i);
      }
    }

    @Override
    public boolean remove(Widget w) {
      boolean removed = super.remove(w);

      if (removed) {
        String id = DomUtils.getId(w);
        if (BeeUtils.same(id, getVisibleId())) {
          setVisibleId(null);
        }
        pendingResize.remove(id);
      }

      return removed;
    }

    private String getVisibleId() {
      return visibleId;
    }

    private Widget getVisibleWidget() {
      return BeeUtils.isEmpty(getVisibleId()) ? null : DomUtils.getChildById(this, getVisibleId());
    }

    private void resize(int index) {
      Widget widget = getWidget(index);

      if (widget instanceof RequiresResize) {
        String id = DomUtils.getId(widget);

        if (!BeeUtils.isEmpty(id)) {
          if (BeeUtils.same(id, getVisibleId())) {
            ((RequiresResize) widget).onResize();
          } else {
            pendingResize.add(id);
          }
        }
      }
    }

    private void setVisibleId(String visibleId) {
      this.visibleId = visibleId;
    }

    private void showWidget(int index) {
      Widget widget = getWidget(index);
      if (DomUtils.idEquals(widget, getVisibleId())) {
        return;
      }

      if (!BeeUtils.isEmpty(getVisibleId())) {
        VisibilityChangeEvent.hideAndFire(getVisibleWidget());
      }
      VisibilityChangeEvent.showAndFire(widget);

      String id = DomUtils.getId(widget);
      setVisibleId(id);

      if (pendingResize.remove(id)) {
        ((RequiresResize) widget).onResize();
      }
    }
  }

  private final class Tab extends Simple implements HasClickHandlers, HasEnabled,
      SummaryChangeEvent.Handler {

    private final CustomDiv summaryWidget;
    private final Map<String, Value> summaryValues = new LinkedHashMap<>();

    private final Evaluator rowPredicate;
    private boolean enabled = true;

    private SummaryChangeEvent.Renderer summaryRenderer;

    private Tab(Widget child, Evaluator rowPredicate) {
      this(child, null, null, rowPredicate);
    }

    private Tab(Widget child, CustomDiv summaryWidget,
        Collection<HasSummaryChangeHandlers> summarySources, Evaluator rowPredicate) {

      setWidget(child);
      addStyleName(getStylePrefix() + TAB_STYLE_SUFFIX);

      this.summaryWidget = summaryWidget;

      if (summaryWidget != null && !BeeUtils.isEmpty(summarySources)) {
        for (HasSummaryChangeHandlers summarySource : summarySources) {
          if (summarySource != null && !BeeUtils.isEmpty(summarySource.getId())) {
            summaryValues.put(summarySource.getId(), summarySource.getSummary());
            summarySource.addSummaryChangeHandler(Tab.this);
          }
        }
      }

      this.rowPredicate = rowPredicate;
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return addDomHandler(handler, ClickEvent.getType());
    }

    @Override
    public String getIdPrefix() {
      return "tab";
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void onSummaryChange(SummaryChangeEvent event) {
      Value oldValue = summaryValues.get(event.getSourceId());

      if (summaryRenderer != null || !Objects.equals(event.getValue(), oldValue)) {
        summaryValues.put(event.getSourceId(), event.getValue());

        String html = (summaryRenderer == null)
            ? SummaryChangeEvent.renderSummary(summaryValues.values())
            : summaryRenderer.apply(summaryValues);

        summaryWidget.setHtml(html);
      }
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      setStyleName(getStylePrefix() + "tabDisabled", !enabled);
    }

    private void setSelected(boolean selected) {
      setStyleName(getStylePrefix() + "tabSelected", selected);
    }

    private void setSummaryRenderer(SummaryChangeEvent.Renderer summaryRenderer) {
      this.summaryRenderer = summaryRenderer;
    }

    private boolean onDataChange(IsRow row) {
      if (row != null && rowPredicate != null) {
        rowPredicate.update(row);
        boolean ok = BeeUtils.toBoolean(rowPredicate.evaluate());

        if (isEnabled() != ok) {
          setEnabled(ok);
          return true;
        }
      }

      return false;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(TabbedPages.class);

  public static final String DEFAULT_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "TabbedPages-";
  public static final String TAB_STYLE_SUFFIX = "tab";
  private static final String CONTENT_STYLE_SUFFIX = "content";

  private static final String RESIZER_CONTENT_TYPE = "tabbed_pages";

  private final String stylePrefix;
  private final Orientation orientation;

  private final Flow tabBar = new Flow();
  private final Deck deckPanel = new Deck();

  private int selectedIndex = BeeConst.UNDEF;

  private ElementSize tabBarSize;

  private boolean resizable;
  private boolean resizerInitialized;

  public TabbedPages() {
    this(DEFAULT_STYLE_PREFIX);
  }

  public TabbedPages(String stylePrefix) {
    this(stylePrefix, Orientation.HORIZONTAL);
  }

  public TabbedPages(String stylePrefix, Orientation orientation) {
    super();
    this.stylePrefix = Assert.notEmpty(stylePrefix);
    this.orientation = Assert.notNull(orientation);

    tabBar.addStyleName(stylePrefix + "tabPanel");
    super.add(tabBar);

    deckPanel.addStyleName(stylePrefix + "contentPanel");
    super.add(deckPanel);

    addStyleName(stylePrefix + "container");
    addStyleName(stylePrefix + orientation.getCaption());
    DomUtils.createId(this, getIdPrefix());
  }

  @Override
  public void accept(IsRow row) {
    if (dependsOnData()) {
      saveLayout();
      boolean changed = false;

      for (int i = 0; i < getPageCount(); i++) {
        changed |= getTab(i).onDataChange(row);
      }

      if (changed) {
        if (isIndex(getSelectedIndex()) && !isPageEnabled(getSelectedIndex())) {
          int page = nextEnabledPage(getSelectedIndex());

          if (isIndex(page)) {
            selectPage(page, SelectionOrigin.SCRIPT);
          } else {
            setSelectedIndex(BeeConst.UNDEF);
          }
        }

        checkLayout();
      }
    }
  }

  @Override
  public void add(Widget w) {
    Assert.untouchable(getClass().getName() + ": cannot add widget without tab");
  }

  public IdentifiableWidget add(Widget content, String text, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, Evaluator rowPredicate) {

    return add(content, createCaption(text), summary, summarySources, rowPredicate);
  }

  public IdentifiableWidget add(Widget content, Widget caption, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, Evaluator rowPredicate) {

    Tab tab = createTab(caption, summary, summarySources, rowPredicate);
    insertPage(content, tab);

    return tab;
  }

  @Override
  public HandlerRegistration addBeforeSelectionHandler(
      BeforeSelectionHandler<Pair<Integer, SelectionOrigin>> handler) {
    return addHandler(handler, BeforeSelectionEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionHandler(
      SelectionHandler<Pair<Integer, SelectionOrigin>> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  public void disablePage(int index) {
    setPageEnabled(index, false);
  }

  public void enablePage(int index) {
    setPageEnabled(index, true);
  }

  public int getContentIndex(String id) {
    for (int i = 0; i < getPageCount(); i++) {
      if (DomUtils.idEquals(getContentWidget(i), id)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  public int getContentIndex(Widget content) {
    return deckPanel.getWidgetIndex(content);
  }

  public Widget getContentWidget(int index) {
    return deckPanel.getWidget(index);
  }

  @Override
  public String getIdPrefix() {
    return "tabbed";
  }

  public int getPageCount() {
    return deckPanel.getWidgetCount();
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public Widget getSelectedWidget() {
    return (getSelectedIndex() >= 0) ? getContentWidget(getSelectedIndex()) : null;
  }

  public int getTabIndexByDataKey(String key) {
    if (!BeeUtils.isEmpty(key)) {
      for (int i = 0; i < getPageCount(); i++) {
        if (BeeUtils.same(DomUtils.getDataKey(getTab(i).getElement()), key)) {
          return i;
        }
      }
    }

    return BeeConst.UNDEF;
  }

  public Widget getTabWidget(int index) {
    return checkIndex(index) ? getTab(index).getWidget() : null;
  }

  public void insert(Widget content, String text, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, Evaluator rowPredicate,
      int beforeIndex) {

    insert(content, createCaption(text), summary, summarySources, rowPredicate, beforeIndex);
  }

  public void insert(Widget content, Widget caption, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, Evaluator rowPredicate,
      int beforeIndex) {

    insertPage(content, createTab(caption, summary, summarySources, rowPredicate), beforeIndex);
  }

  public boolean isIndex(int index) {
    return BeeUtils.betweenExclusive(index, 0, getPageCount());
  }

  public boolean isResizable() {
    return resizable;
  }

  public void removePage(int index) {
    if (checkIndex(index)) {
      saveLayout();

      tabBar.remove(index);
      deckPanel.remove(index);

      if (index == getSelectedIndex()) {
        setSelectedIndex(BeeConst.UNDEF);

        int page = nextEnabledPage(index);
        if (isIndex(page)) {
          selectPage(page, SelectionOrigin.REMOVE);
        }

      } else if (index < getSelectedIndex()) {
        setSelectedIndex(getSelectedIndex() - 1);
      }

      checkLayout();
    }
  }

  public void resizePage(int index) {
    if (checkIndex(index)) {
      deckPanel.resize(index);
    }
  }

  public boolean selectPage(int index, SelectionOrigin origin) {
    if (!checkIndex(index)) {
      return false;
    }
    if (index == getSelectedIndex()) {
      return true;
    }

    if (!isPageEnabled(index)) {
      return false;
    }

    Pair<Integer, SelectionOrigin> data = Pair.of(index, origin);

    BeforeSelectionEvent<Pair<Integer, SelectionOrigin>> event =
        BeforeSelectionEvent.fire(this, data);
    if ((event != null) && event.isCanceled()) {
      return false;
    }

    if (!BeeConst.isUndef(getSelectedIndex())) {
      getTab(getSelectedIndex()).setSelected(false);
    }

    deckPanel.showWidget(index);
    getTab(index).setSelected(true);

    setSelectedIndex(index);

    SelectionEvent.fire(this, data);
    return true;
  }

  public void setResizable(boolean resizable) {
    this.resizable = resizable;

    if (resizable && !resizerInitialized && isAttached()) {
      maybeInitResizer();
    }
  }

  public void setSummaryRenderer(int index, SummaryChangeEvent.Renderer summaryRenderer) {
    if (checkIndex(index)) {
      getTab(index).setSummaryRenderer(summaryRenderer);
    }
  }

  public void setSummaryRenderer(String tabKey, SummaryChangeEvent.Renderer summaryRenderer) {
    setSummaryRenderer(getTabIndexByDataKey(tabKey), summaryRenderer);
  }

  public void setTabStyle(int index, String style, boolean add) {
    if (checkIndex(index)) {
      getTab(index).setStyleName(style, add);
    }
  }

  protected void checkLayout() {
    if (!isAttached() || getTabBarSize() == null) {
      return;
    }

    Scheduler.get().scheduleDeferred(() -> {
      boolean changed = false;

      if (getTabBarSize() != null) {
        switch (orientation) {
          case HORIZONTAL:
            changed = !getTabBarSize().sameHeight(tabBar);
            break;
          case VERTICAL:
            changed = !getTabBarSize().sameWidth(tabBar);
            break;
        }
        setTabBarSize(null);
      }

      if (changed) {
        deckPanel.onResize();
      }
    });
  }

  protected String getStylePrefix() {
    return stylePrefix;
  }

  protected Flow getTabBar() {
    return tabBar;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (resizable && !resizerInitialized) {
      maybeInitResizer();
    }
  }

  protected void saveLayout() {
    setTabBarSize(isAttached() ? ElementSize.forOffset(tabBar) : null);
  }

  private boolean checkIndex(int index) {
    if (BeeUtils.betweenExclusive(index, 0, getPageCount())) {
      return true;
    } else {
      logger.severe(NameUtils.getName(this), getId(),
          "page index", index, "page count", getPageCount());
      return false;
    }
  }

  private Tab createTab(Widget caption, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, Evaluator rowPredicate) {

    Tab tab;

    if (BeeUtils.isEmpty(summary) && BeeUtils.isEmpty(summarySources)) {
      if (caption != null) {
        caption.addStyleName(getStylePrefix() + "tabSingleton");
      }
      tab = new Tab(caption, rowPredicate);

    } else {
      Flow wrapper = new Flow(getStylePrefix() + "tabWrapper");

      if (caption != null) {
        caption.addStyleName(getStylePrefix() + "tabCaption");
        wrapper.add(caption);
      }

      CustomDiv summaryWidget = new CustomDiv(getStylePrefix() + "tabSummary");
      if (!BeeUtils.isEmpty(summary)) {
        summaryWidget.setHtml(summary);
      }

      wrapper.add(summaryWidget);

      tab = new Tab(wrapper, summaryWidget, summarySources, rowPredicate);
    }

    return tab;
  }

  private static Widget createCaption(String text) {
    Label widget = new Label(text);
    UiHelper.makePotentiallyBold(widget.getElement(), text);

    return widget;
  }

  private boolean dependsOnData() {
    for (int i = 0; i < getPageCount(); i++) {
      if (getTab(i).rowPredicate != null) {
        return true;
      }
    }
    return false;
  }

  private Tab getTab(int index) {
    return (Tab) tabBar.getWidget(index);
  }

  private ElementSize getTabBarSize() {
    return tabBarSize;
  }

  private void insertPage(Widget content, Tab tab) {
    insertPage(content, tab, getPageCount());
  }

  private void insertPage(Widget content, Tab tab, int before) {
    Assert.notNull(content, "page content is null");
    Assert.notNull(tab, "page tab is null");
    Assert.betweenInclusive(before, 0, getPageCount(), "insert page: beforeIndex out of bounds");

    saveLayout();

    tabBar.insert(tab, before);

    final String tabId = tab.getId();

    tab.addClickHandler(event -> {
      for (int i = 0; i < getPageCount(); i++) {
        if (getTab(i).getId().equals(tabId)) {
          selectPage(i, SelectionOrigin.CLICK);
          break;
        }
      }
    });

    deckPanel.insert(content, before);
    content.addStyleName(stylePrefix + CONTENT_STYLE_SUFFIX);

    if (BeeConst.isUndef(getSelectedIndex())) {
      selectPage(0, SelectionOrigin.INIT);
    } else if (getSelectedIndex() >= before) {
      setSelectedIndex(getSelectedIndex() + 1);
    }

    checkLayout();
  }

  private boolean isPageEnabled(int index) {
    return getTab(index).isEnabled();
  }

  private void maybeInitResizer() {
    Widget target = getParent();
    Element offsetParent = getElement().getOffsetParent();

    if (target instanceof DndTarget && target.getElement().equals(offsetParent)) {
      DndHelper.makeSource(tabBar, RESIZER_CONTENT_TYPE, getId(), getStylePrefix() + "drag");

      DndHelper.makeTarget((DndTarget) target, Collections.singleton(RESIZER_CONTENT_TYPE), null,
          input -> getId().equals(input),
          (t, u) -> {
            int dy = t.getNativeEvent().getClientY() - DndHelper.getStartY();

            int top = getElement().getOffsetTop();
            int height = getElement().getClientHeight();

            int margin = ComputedStyles.getPixels(getElement(), StyleUtils.STYLE_MARGIN_TOP);
            int minHeight = ComputedStyles.getPixels(getElement(), StyleUtils.STYLE_MIN_HEIGHT);

            dy += margin;
            if (minHeight > 0 && height - dy < minHeight) {
              dy = height - minHeight;
            }

            if (dy != 0 && top + dy >= 0 && height > dy) {
              Style style = getElement().getStyle();

              if (margin != 0) {
                StyleUtils.setProperty(style, StyleUtils.STYLE_MARGIN_TOP,
                    BeeConst.DOUBLE_ZERO, CssUnit.PX);
              }

              StyleUtils.setTop(style, top + dy);
              StyleUtils.setHeight(style, height - dy);

              StyleUtils.makeAbsolute(style);
              addStyleName(getStylePrefix() + "resized");

              Scheduler.get().scheduleDeferred(deckPanel::onResize);
            }
          });

      this.resizerInitialized = true;

    } else {
      logger.warning(NameUtils.getName(this), getId(), "not resizable, parent",
          NameUtils.getName(target), target.getElement().getId());
    }
  }

  private int nextEnabledPage(int from) {
    if (getPageCount() > 0) {
      if (from < getPageCount()) {
        for (int i = Math.max(from, 0); i < getPageCount(); i++) {
          if (isPageEnabled(i)) {
            return i;
          }
        }
      }

      if (from > 0) {
        for (int i = Math.min(from, getPageCount()) - 1; i >= 0; i--) {
          if (isPageEnabled(i)) {
            return i;
          }
        }
      }
    }

    return BeeConst.UNDEF;
  }

  private void setPageEnabled(int index, boolean enabled) {
    if (checkIndex(index)) {
      Tab tab = getTab(index);
      if (tab.isEnabled() == enabled) {
        return;
      }

      saveLayout();
      tab.setEnabled(enabled);

      if (!enabled && index == getSelectedIndex()) {
        int page = nextEnabledPage(index);

        if (isIndex(page)) {
          selectPage(page, SelectionOrigin.SCRIPT);
        } else {
          tab.setSelected(false);
          setSelectedIndex(BeeConst.UNDEF);
        }
      }

      checkLayout();
    }
  }

  private void setSelectedIndex(int selectedIndex) {
    this.selectedIndex = selectedIndex;
  }

  private void setTabBarSize(ElementSize tabBarSize) {
    this.tabBarSize = tabBarSize;
  }
}
