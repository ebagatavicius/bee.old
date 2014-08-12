package com.butent.bee.client.modules.calendar.layout;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Composite;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.calendar.ItemWidget;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

public class ItemPanel extends Composite {

  public ItemPanel() {
    Simple scrollArea = new Simple();
    scrollArea.addStyleName(CalendarStyleManager.SCROLL_AREA);

    Timeline timeline = new Timeline();
    timeline.addStyleName(CalendarStyleManager.TIME_STRIP);

    ItemGrid grid = new ItemGrid();
    grid.addStyleName(CalendarStyleManager.APPOINTMENT_GRID);

    Flow layout = new Flow();
    layout.addStyleName(CalendarStyleManager.APPOINTMENT_PANEL);

    layout.add(timeline);
    layout.add(grid);

    scrollArea.setWidget(layout);
    initWidget(scrollArea);
  }

  public void build(int columnCount, CalendarSettings settings) {
    build(columnCount, settings, BeeConst.UNDEF, BeeConst.UNDEF);
  }

  public void build(int columnCount, CalendarSettings settings,
      int todayStartColumn, int todayEndColumn) {
    getTimeline().build(settings);
    getGrid().build(columnCount, settings, todayStartColumn, todayEndColumn);
  }

  public void doScroll(CalendarSettings settings, Collection<ItemWidget> widgets) {
    int oldPos = getScrollArea().getElement().getScrollTop();
    int newPos = CalendarUtils.getStartPixels(settings, widgets);

    if (oldPos != newPos) {
      getScrollArea().getElement().setScrollTop(newPos);
    }
  }

  public int getColumnIndex(int x, int columnCount) {
    int left = getGrid().getAbsoluteLeft();
    int relativeX = x - left;

    int index = relativeX / CalendarUtils.getColumnWidth(getGrid(), columnCount);
    return BeeUtils.clamp(index, 0, columnCount - 1);
  }

  public DateTime getCoordinatesDate(int x, int y, CalendarSettings settings,
      JustDate date, int days) {
    int top = getScrollArea().getAbsoluteTop();
    int scrollTop = getScrollArea().getElement().getScrollTop();

    int relativeY = y - top + scrollTop;

    DateTime result = date.getDateTime();

    int day = getColumnIndex(x, days);
    if (day > 0) {
      result.setDom(result.getDom() + day);
    }

    int minutes = CalendarUtils.getMinutes(relativeY, settings);
    if (minutes > 0) {
      result.setMinute(minutes);
    }
    return result;
  }

  public ItemGrid getGrid() {
    return (ItemGrid) getLayoutPanel().getWidget(1);
  }

  public Simple getScrollArea() {
    return (Simple) getWidget();
  }

  public Timeline getTimeline() {
    return (Timeline) getLayoutPanel().getWidget(0);
  }

  public boolean isGrid(Element element) {
    return getGrid().getElement().isOrHasChild(element);
  }

  public void onClock(CalendarSettings settings) {
    getTimeline().onClock(settings);
    getGrid().onClock(settings);
  }

  private Flow getLayoutPanel() {
    return (Flow) getScrollArea().getWidget();
  }
}
