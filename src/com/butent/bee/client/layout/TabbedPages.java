package com.butent.bee.client.layout;

import com.google.common.base.Predicate;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Pair;
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
    HasSelectionHandlers<Pair<Integer, TabbedPages.SelectionOrigin>> {

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

  private final class Tab extends Simple implements HasClickHandlers, SummaryChangeEvent.Handler {

    private final CustomDiv summaryWidget;
    private final Map<String, Value> summaryValues = new LinkedHashMap<>();

    private Tab(Widget child) {
      this(child, null, null);
    }

    private Tab(Widget child, CustomDiv summaryWidget,
        Collection<HasSummaryChangeHandlers> summarySources) {

      setWidget(child);
      addStyleName(getStylePrefix() + "tab");

      this.summaryWidget = summaryWidget;

      if (summaryWidget != null && !BeeUtils.isEmpty(summarySources)) {
        for (HasSummaryChangeHandlers summarySource : summarySources) {
          if (summarySource != null && !BeeUtils.isEmpty(summarySource.getId())) {
            summaryValues.put(summarySource.getId(), summarySource.getSummary());
            summarySource.addSummaryChangeHandler(Tab.this);
          }
        }
      }
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
    public void onSummaryChange(SummaryChangeEvent event) {
      Value oldValue = summaryValues.get(event.getSourceId());

      if (!Objects.equals(event.getValue(), oldValue)) {
        summaryValues.put(event.getSourceId(), event.getValue());
        summaryWidget.setHtml(SummaryChangeEvent.renderSummary(summaryValues.values()));
      }
    }

    private void setSelected(boolean selected) {
      setStyleName(getStylePrefix() + "tabSelected", selected);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(TabbedPages.class);

  private static final String DEFAULT_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "TabbedPages-";
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
  public void add(Widget w) {
    Assert.untouchable(getClass().getName() + ": cannot add widget without tab");
  }

  public IdentifiableWidget add(Widget content, String text, String summary,
      Collection<HasSummaryChangeHandlers> summarySources) {
    return add(content, createCaption(text), summary, summarySources);
  }

  public IdentifiableWidget add(Widget content, Widget caption, String summary,
      Collection<HasSummaryChangeHandlers> summarySources) {

    Tab tab = createTab(caption, summary, summarySources);
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

  public Widget getTabWidget(int index) {
    checkIndex(index);
    return getTab(index).getWidget();
  }

  public void insert(Widget content, String text, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, int beforeIndex) {
    insert(content, createCaption(text), summary, summarySources, beforeIndex);
  }

  public void insert(Widget content, Widget caption, String summary,
      Collection<HasSummaryChangeHandlers> summarySources, int beforeIndex) {
    insertPage(content, createTab(caption, summary, summarySources), beforeIndex);
  }

  public boolean isIndex(int index) {
    return BeeUtils.betweenExclusive(index, 0, getPageCount());
  }

  public boolean isResizable() {
    return resizable;
  }

  public void removePage(int index) {
    checkIndex(index);

    saveLayout();

    tabBar.remove(index);
    deckPanel.remove(index);

    if (index == getSelectedIndex()) {
      setSelectedIndex(BeeConst.UNDEF);
      if (getPageCount() > 0) {
        selectPage(Math.min(index, getPageCount() - 1), SelectionOrigin.REMOVE);
      }
    } else if (index < getSelectedIndex()) {
      setSelectedIndex(getSelectedIndex() - 1);
    }

    checkLayout();
  }

  public void resizePage(int index) {
    checkIndex(index);
    deckPanel.resize(index);
  }

  public void selectPage(int index, SelectionOrigin origin) {
    checkIndex(index);
    if (index == getSelectedIndex()) {
      return;
    }

    Pair<Integer, SelectionOrigin> data = Pair.of(index, origin);

    BeforeSelectionEvent<Pair<Integer, SelectionOrigin>> event =
        BeforeSelectionEvent.fire(this, data);
    if ((event != null) && event.isCanceled()) {
      return;
    }

    if (!BeeConst.isUndef(getSelectedIndex())) {
      getTab(getSelectedIndex()).setSelected(false);
    }

    deckPanel.showWidget(index);
    getTab(index).setSelected(true);

    setSelectedIndex(index);

    SelectionEvent.fire(this, data);
  }

  public void setResizable(boolean resizable) {
    this.resizable = resizable;

    if (resizable && !resizerInitialized && isAttached()) {
      maybeInitResizer();
    }
  }

  public void setTabStyle(int index, String style, boolean add) {
    checkIndex(index);
    getTab(index).setStyleName(style, add);
  }

  protected void checkLayout() {
    if (!isAttached() || getTabBarSize() == null) {
      return;
    }

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
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

  private void checkIndex(int index) {
    Assert.betweenExclusive(index, 0, getPageCount(), "page index out of bounds");
  }

  private Tab createTab(Widget caption, String summary,
      Collection<HasSummaryChangeHandlers> summarySources) {

    if (BeeUtils.isEmpty(summary) && BeeUtils.isEmpty(summarySources)) {
      return new Tab(caption);

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

      Tab tab = new Tab(wrapper, summaryWidget, summarySources);

      return tab;
    }
  }

  private static Widget createCaption(String text) {
    return new Label(text);
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

    tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        for (int i = 0; i < getPageCount(); i++) {
          if (getTab(i).getId().equals(tabId)) {
            selectPage(i, SelectionOrigin.CLICK);
            break;
          }
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

  private void maybeInitResizer() {
    Widget target = getParent();
    Element offsetParent = getElement().getOffsetParent();

    if (target instanceof DndTarget && target.getElement().equals(offsetParent)) {
      DndHelper.makeSource(tabBar, RESIZER_CONTENT_TYPE, getId(), getStylePrefix() + "drag");

      DndHelper.makeTarget((DndTarget) target, Collections.singleton(RESIZER_CONTENT_TYPE), null,
          new Predicate<Object>() {
            @Override
            public boolean apply(Object input) {
              return getId().equals(input);
            }
          },
          new BiConsumer<DropEvent, Object>() {
            @Override
            public void accept(DropEvent t, Object u) {
              int dy = t.getNativeEvent().getClientY() - DndHelper.getStartY();

              int top = getElement().getOffsetTop();
              int height = getOffsetHeight();

              if (dy != 0 && top + dy >= 0 && height > dy) {
                int left = getElement().getOffsetLeft();
                int width = getOffsetWidth();

                Style style = getElement().getStyle();

                StyleUtils.setTop(style, top + dy);
                StyleUtils.setHeight(style, height - dy);

                StyleUtils.setLeft(style, left);
                StyleUtils.setWidth(style, width);

                StyleUtils.makeAbsolute(style);

                addStyleName(getStylePrefix() + "resized");
              }
            }
          });

    } else {
      logger.warning(NameUtils.getName(this), getId(), "not resizable, parent",
          NameUtils.getName(target), target.getElement().getId());
    }
  }

  private void setSelectedIndex(int selectedIndex) {
    this.selectedIndex = selectedIndex;
  }

  private void setTabBarSize(ElementSize tabBarSize) {
    this.tabBarSize = tabBarSize;
  }
}
