package com.butent.bee.client.modules.calendar.layout;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Composite;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class AppointmentPanel extends Composite {

  public AppointmentPanel() {
    Simple scrollArea = new Simple();
    scrollArea.addStyleName(CalendarStyleManager.SCROLL_AREA);

    Timeline timeline = new Timeline();
    timeline.addStyleName(CalendarStyleManager.TIME_STRIP);

    AppointmentGrid grid = new AppointmentGrid();
    grid.addStyleName(CalendarStyleManager.APPOINTMENT_GRID);

    Flow layout = new Flow();
    layout.addStyleName(CalendarStyleManager.APPOINTMENT_PANEL);

    layout.add(timeline);
    layout.add(grid);

    scrollArea.setWidget(layout);
    initWidget(scrollArea);
  }

  public void build(int columnCount, CalendarSettings settings) {
    getTimeline().build(settings);
    getGrid().build(columnCount, settings);
  }

  public int getColumnIndex(int x, int columnCount) {
    int left = getGrid().getOverlay().getAbsoluteLeft();
    int relativeX = x - left;

    int index = relativeX / getColumnWidth(columnCount);
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
    
    int hour = relativeY / settings.getHourHeight();
    if (hour > 0) {
      result.setHour(hour);
    }
    
    int interval = (relativeY - hour * settings.getHourHeight()) / settings.getPixelsPerInterval();
    int minute = interval * TimeUtils.MINUTES_PER_HOUR / settings.getIntervalsPerHour();
    if (minute > 0) {
      result.setMinute(minute);
    }

    return result;
  }
  
  public AppointmentGrid getGrid() {
    return (AppointmentGrid) getLayoutPanel().getWidget(1);
  }

  public Simple getScrollArea() {
    return (Simple) getWidget();
  }

  public Timeline getTimeline() {
    return (Timeline) getLayoutPanel().getWidget(0);
  }
  
  public boolean isGrid(Element element) {
    return getGrid().getOverlay().getElement().equals(element);
  }

  public void scrollToHour(int hour, CalendarSettings settings) {
    if (hour >= 0) {
      getScrollArea().getElement().setScrollTop(hour *
          settings.getIntervalsPerHour() * settings.getPixelsPerInterval());
    }
  }
  
  private int getColumnWidth(int columnCount) {
    int totalWidth = getGrid().getOverlay().getOffsetWidth();
    if (columnCount <= 1) {
      return totalWidth;
    } else {
      return totalWidth * (100 / columnCount) / 100;
    }
  }
  
  private Flow getLayoutPanel() {
    return (Flow) getScrollArea().getWidget(); 
  }
}
