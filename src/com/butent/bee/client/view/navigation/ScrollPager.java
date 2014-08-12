package com.butent.bee.client.view.navigation;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.client.layout.Scroll;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.style.StyleUtils.ScrollBars;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.utils.BeeUtils;

public class ScrollPager extends AbstractPager implements RequiresResize {

  private static final int MAX_HEIGHT = 10000;

  private int lastPos;
  private long lastHeight;

  private boolean isUpdating;

  public ScrollPager() {
    Widget widget = new CustomDiv();
    StyleUtils.setWidth(widget, 0);
    StyleUtils.setHeight(widget, 0);

    Scroll scroll = new Scroll(widget);
    initWidget(scroll);

    StyleUtils.hideScroll(scroll, ScrollBars.HORIZONTAL);
    StyleUtils.alwaysScroll(scroll, ScrollBars.VERTICAL);

    scroll.addScrollHandler(new ScrollHandler() {
      @Override
      public void onScroll(ScrollEvent event) {
        if (isUpdating) {
          isUpdating = false;
          return;
        }
        if (!isEnabled()) {
          return;
        }

        int pos = getPosition();
        int maxPos = getMaxPosition();
        int height = getWidgetHeight();
        if (pos < 0 || pos == lastPos || height >= maxPos || pos > maxPos - height) {
          return;
        }

        int pageSize = getPageSize();
        int rowCount = getRowCount();
        if (pageSize <= 0 || pageSize >= rowCount) {
          return;
        }

        int start = pos * (rowCount - pageSize) / (maxPos - height);
        start = BeeUtils.clamp(start, 0, rowCount - pageSize);

        if (start != getPageStart()) {
          setPageStart(start);
        }
        lastPos = pos;
      }
    });
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    return false;
  }

  @Override
  public void onResize() {
    updateHeight();
  }

  @Override
  public void onScopeChange(ScopeChangeEvent event) {
    if (event.getOrigin() != getNavigationOrigin()) {
      updateHeight();
      isUpdating = updatePosition();
    }
  }

  @Override
  protected NavigationOrigin getNavigationOrigin() {
    return NavigationOrigin.SCROLLER;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    ReadyEvent.fire(this);
  }

  private static long calculateHeight(int pageSize, int rowCount, int widgetHeight) {
    if (pageSize <= 0 || rowCount < pageSize || widgetHeight <= 0) {
      return 0;
    } else {
      return (long) widgetHeight * rowCount / pageSize;
    }
  }

  private Widget getInnerWidget() {
    Scroll outer = getOuterWidget();
    if (outer == null) {
      return null;
    }
    return outer.getWidget();
  }

  private int getMaxPosition() {
    if (lastHeight < MAX_HEIGHT) {
      return (int) lastHeight;
    } else {
      return MAX_HEIGHT;
    }
  }

  private Scroll getOuterWidget() {
    Widget outer = getWidget();
    if (outer == null) {
      return null;
    }
    return (Scroll) outer;
  }

  private int getPosition() {
    Scroll outer = getOuterWidget();
    if (outer == null) {
      return BeeConst.UNDEF;
    }
    return outer.getVerticalScrollPosition();
  }

  private int getWidgetHeight() {
    return getElement().getClientHeight();
  }

  private void setPosition(int position) {
    Scroll outer = getOuterWidget();
    if (outer != null && position >= 0) {
      outer.setVerticalScrollPosition(position);
    }
  }

  private boolean updateHeight() {
    long h = calculateHeight(getPageSize(), getRowCount(), getWidgetHeight());

    if (h >= 0 && h != lastHeight) {
      lastHeight = h;
      int z = (h < MAX_HEIGHT) ? (int) h : MAX_HEIGHT;
      StyleUtils.setHeight(getInnerWidget(), z);
      return true;
    } else {
      return false;
    }
  }

  private boolean updatePosition() {
    int pageStart = getPageStart();
    int pageSize = getPageSize();
    int rowCount = getRowCount();

    int maxPos = getMaxPosition();
    int height = getWidgetHeight();

    int pos = 0;
    if (rowCount > 0 && pageSize > 0 && rowCount > pageSize) {
      pos = pageStart * (maxPos - height) / (rowCount - pageSize);
    }

    if (pos >= 0 && pos <= maxPos - height && pos != getPosition()) {
      setPosition(pos);
      lastPos = pos;
      return true;
    } else {
      return false;
    }
  }
}
