package com.butent.bee.client.datepicker;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.Global;
import com.butent.bee.client.datepicker.DatePicker.CssClasses;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;

class MonthSelector extends Component {

  private final class Navigation implements Scheduler.ScheduledCommand {
    private final int months;

    private Navigation(int months) {
      super();
      this.months = months;
    }

    @Override
    public void execute() {
      addMonths(months);
      getDatePicker().setFocus(true);
    }
  }

  private final Image prevYear;
  private final Image prevMonth;
  private final Image nextMonth;
  private final Image nextYear;

  private final CustomDiv monthName;

  private final Horizontal table;

  MonthSelector(CssClasses cssClasses) {
    String styleDisabled = cssClasses.monthNavigationDisabled();

    this.prevYear = new Image(Global.getImages().rewind(), new Navigation(-12), styleDisabled);
    this.prevMonth = new Image(Global.getImages().previous(), new Navigation(-1), styleDisabled);
    this.nextMonth = new Image(Global.getImages().next(), new Navigation(1), styleDisabled);
    this.nextYear = new Image(Global.getImages().forward(), new Navigation(12), styleDisabled);

    EventUtils.preventClickDebouncer(prevYear);
    EventUtils.preventClickDebouncer(prevMonth);
    EventUtils.preventClickDebouncer(nextMonth);
    EventUtils.preventClickDebouncer(nextYear);

    this.monthName = new CustomDiv();

    this.table = new Horizontal();
    table.add(prevYear);
    table.add(prevMonth);

    table.add(monthName);

    table.add(nextMonth);
    table.add(nextYear);

    for (int i = 0; i < table.getWidgetCount(); i++) {
      Element cell = table.getCell(i);

      if (i == 2) {
        cell.addClassName(cssClasses.month());
      } else {
        cell.addClassName(cssClasses.monthNavigation());
      }
    }

    table.addStyleName(cssClasses.monthSelector());
    initWidget(table);
  }

  @Override
  protected void refresh() {
    YearMonth current = getModel().getCurrentMonth();
    monthName.setHtml(Format.renderYearMonth(current));

    refresh(prevYear, current.previousYear());
    refresh(prevMonth, current.previousMonth());

    refresh(nextMonth, current.nextMonth());
    refresh(nextYear, current.nextYear());
  }

  @Override
  protected void setUp() {
  }

  private boolean isEnabled(YearMonth ym) {
    JustDate minDate = getDatePicker().getMinDate();
    if (minDate != null && TimeUtils.isMore(minDate, ym.getLast())) {
      return false;
    }

    JustDate maxDate = getDatePicker().getMaxDate();
    if (maxDate != null && TimeUtils.isLess(maxDate, ym)) {
      return false;
    }
    return true;
  }

  private void refresh(Image widget, YearMonth ym) {
    boolean enabled = isEnabled(ym);
    widget.setEnabled(enabled);

    if (enabled) {
      widget.setTitle(Format.renderYearMonth(ym));
    } else {
      widget.setTitle(BeeConst.STRING_EMPTY);
    }
  }
}
