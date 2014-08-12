package com.butent.bee.client.modules.calendar.view;

import com.google.common.collect.Range;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.modules.calendar.CalendarFormat;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.RangeMap;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;

public class DayViewHeader extends Horizontal {

  private static final int DATE_CELL_INDEX = 0;
  private static final int DAY_PANEL_INDEX = 1;

  private static final RangeMap<Integer, DateTimeFormat> labelFormats;

  static {
    labelFormats = RangeMap.create();

    labelFormats.put(Range.lessThan(35), DateTimeFormat.getFormat("d"));
    labelFormats.put(Range.closedOpen(35, 45),
        DateTimeFormat.getFormat("M" + TimeUtils.DATE_FIELD_SEPARATOR + "d"));
    labelFormats.put(Range.closedOpen(45, 60),
        DateTimeFormat.getFormat("MM" + TimeUtils.DATE_FIELD_SEPARATOR + "dd"));
    labelFormats.put(Range.closedOpen(60, 120),
        DateTimeFormat.getFormat("EEE, MM" + TimeUtils.DATE_FIELD_SEPARATOR + "dd"));
    labelFormats.put(Range.closedOpen(120, 150),
        DateTimeFormat.getFormat("EEEE, MM" + TimeUtils.DATE_FIELD_SEPARATOR + "dd"));
    labelFormats.put(Range.atLeast(150), DateTimeFormat.getFormat("EEEE, MMMM d"));
  }

  public DayViewHeader() {
    super();
    addStyleName(CalendarStyleManager.CALENDAR_HEADER);

    Label dateLabel = new Label();
    add(dateLabel);
    addStyleToCell(dateLabel, CalendarStyleManager.DATE_CELL);

    Flow dayPanel = new Flow();
    dayPanel.addStyleName(CalendarStyleManager.DAY_CELL_CONTAINER);
    add(dayPanel);

    CustomDiv filler = new CustomDiv();
    add(filler);
    setCellWidth(filler, DomUtils.getScrollBarWidth());
  }

  public void setDays(JustDate date, int days) {
    Widget dayPanel = getWidget(DAY_PANEL_INDEX);
    ((HasWidgets) dayPanel).clear();

    int dayWidthPct = 100 / days;
    int columnWidthPx = CalendarUtils.getColumnWidth(dayPanel, days);

    DateTimeFormat format = labelFormats.get(columnWidthPx);

    JustDate tmp = JustDate.copyOf(date);
    for (int i = 0; i < days; i++) {
      Label dayLabel = new Label(format == null ? tmp.toString() : format.format(tmp));
      dayLabel.addStyleName(CalendarStyleManager.DAY_CELL);

      StyleUtils.setLeft(dayLabel, dayWidthPct * i, CssUnit.PCT);
      StyleUtils.setWidth(dayLabel, dayWidthPct, CssUnit.PCT);

      if (TimeUtils.isToday(tmp)) {
        dayLabel.addStyleName(CalendarStyleManager.TODAY);
      }
      if (TimeUtils.isWeekend(tmp)) {
        dayLabel.addStyleName(CalendarStyleManager.DAY_CELL_WEEKEND);
      }

      ((HasWidgets) dayPanel).add(dayLabel);
      TimeUtils.moveOneDayForward(tmp);
    }
  }

  public void setYear(JustDate date) {
    getWidget(DATE_CELL_INDEX).getElement().setInnerHTML(CalendarFormat.formatWeekOfYear(date));
  }
}
