package com.butent.bee.client.layout;

import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler;
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
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Set;

public class TabbedPages extends Flow implements
    HasBeforeSelectionHandlers<Pair<Integer, TabbedPages.SelectionOrigin>>,
    HasSelectionHandlers<Pair<Integer, TabbedPages.SelectionOrigin>> {

  public enum SelectionOrigin {
    CLICK, INSERT, REMOVE, INIT, SCRIPT
  }

  private static final class Deck extends Complex {

    private String visibleId;
    private final Set<String> pendingResize = Sets.newHashSet();

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

  private final class Tab extends Simple implements HasClickHandlers {

    private Tab(Widget child) {
      setWidget(child);
      setStyleName(getStylePrefix() + "tab");
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return addDomHandler(handler, ClickEvent.getType());
    }

    @Override
    public String getIdPrefix() {
      return "tab";
    }

    private void setSelected(boolean selected) {
      setStyleName(getStylePrefix() + "tabSelected", selected);
    }
  }

  private static final String DEFAULT_STYLE_PREFIX = "bee-TabbedPages-";
  private static final String CONTENT_STYLE_SUFFIX = "content";

  private final String stylePrefix;
  private final Orientation orientation;

  private final Flow tabBar = new Flow();
  private final Deck deckPanel = new Deck();

  private int selectedIndex = BeeConst.UNDEF;

  private ElementSize tabBarSize;

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

  public void add(Widget content, String text) {
    add(content, new Label(text));
  }

  public void add(Widget content, Widget tab) {
    insertPage(content, new Tab(tab));
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

  public void insert(Widget content, String text, int beforeIndex) {
    insert(content, createTabWidget(text), beforeIndex);
  }

  public void insert(Widget content, Widget tab, int beforeIndex) {
    insertPage(content, new Tab(tab), beforeIndex);
  }

  public boolean isIndex(int index) {
    return BeeUtils.betweenExclusive(index, 0, getPageCount());
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

  protected void saveLayout() {
    setTabBarSize(isAttached() ? ElementSize.forOffset(tabBar) : null);
  }

  private void checkIndex(int index) {
    Assert.betweenExclusive(index, 0, getPageCount(), "page index out of bounds");
  }

  private static Widget createTabWidget(String text) {
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

  private void setSelectedIndex(int selectedIndex) {
    this.selectedIndex = selectedIndex;
  }

  private void setTabBarSize(ElementSize tabBarSize) {
    this.tabBarSize = tabBarSize;
  }
}
