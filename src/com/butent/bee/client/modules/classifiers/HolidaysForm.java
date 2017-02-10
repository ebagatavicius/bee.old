package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Badge;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.Grego;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class HolidaysForm extends AbstractFormInterceptor implements SelectorEvent.Handler {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "co-holidays-";

  private static final String STYLE_YEAR_WIDGET = STYLE_PREFIX + "year-widget";
  private static final String STYLE_YEAR_ACTIVE = STYLE_PREFIX + "year-active";
  private static final String STYLE_YEAR_LABEL = STYLE_PREFIX + "year-label";
  private static final String STYLE_YEAR_BADGE = STYLE_PREFIX + "year-badge";

  private static final String STYLE_ADD_FIRST = STYLE_PREFIX + "add-first";
  private static final String STYLE_ADD_LAST = STYLE_PREFIX + "add-last";

  private static final String STYLE_MONTH_PANEL = STYLE_PREFIX + "month-panel";
  private static final String STYLE_MONTH_LABEL = STYLE_PREFIX + "month-label";
  private static final String STYLE_MONTH_TABLE = STYLE_PREFIX + "month-table";
  private static final String STYLE_WEEKDAY_CELL = STYLE_PREFIX + "weekday-cell";

  private static final String STYLE_DAY_CELL = STYLE_PREFIX + "day-cell";
  private static final String STYLE_WEEKEND = STYLE_PREFIX + "weekend";
  private static final String STYLE_HOLIDAY = STYLE_PREFIX + "holiday";

  private static int getYear(int day) {
    return new JustDate(day).getYear();
  }

  private final Multimap<Long, Integer> data = HashMultimap.create();

  private UnboundSelector countrySelector;
  private Flow yearPanel;
  private Flow calendarPanel;

  HolidaysForm() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof UnboundSelector) {
      countrySelector = (UnboundSelector) widget;
      countrySelector.addSelectorHandler(this);

    } else if (BeeUtils.same(name, "Years")) {
      yearPanel = (Flow) widget;

    } else if (BeeUtils.same(name, "Calendar")) {
      calendarPanel = (Flow) widget;
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new HolidaysForm();
  }

  @Override
  public void onLoad(FormView form) {
    Queries.getRowSet(VIEW_HOLIDAYS, null, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (!data.isEmpty()) {
          data.clear();
        }

        Long lastCountry = null;
        Integer lastDay = null;

        if (!DataUtils.isEmpty(result)) {
          int countryIndex = result.getColumnIndex(COL_HOLY_COUNTRY);
          int dayIndex = result.getColumnIndex(COL_HOLY_DAY);

          long version = 0;

          for (BeeRow row : result) {
            data.put(row.getLong(countryIndex), row.getInteger(dayIndex));

            if (row.getVersion() > version) {
              lastCountry = row.getLong(countryIndex);
              lastDay = row.getInteger(dayIndex);

              version = row.getVersion();
            }
          }
        }

        if (DataUtils.isId(lastCountry)) {
          init(lastCountry, getYear(lastDay));

        } else {
          Long countryId = Global.getParameterRelation(AdministrationConstants.PRM_COUNTRY);

          if (DataUtils.isId(countryId)) {
            init(countryId, TimeUtils.year());
          }
        }
      }
    });
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isChanged() && event.getRelatedRow() != null) {
      long country = event.getRelatedRow().getId();

      List<Integer> years = getYears(country);
      if (years.isEmpty()) {
        years.add(TimeUtils.year());
      }

      renderYears(country, years);
      selectYear(country, years.get(years.size() - 1));
    }
  }

  private void addFirst(Long country) {
    for (int i = 1; i < yearPanel.getWidgetCount(); i++) {
      int year = DomUtils.getDataIndexInt(yearPanel.getWidget(i).getElement());

      if (year > 0) {
        year--;

        Widget widget = yearPanel.getWidget(0);
        if (widget.getElement().hasClassName(STYLE_ADD_FIRST)) {
          widget.setTitle(BeeUtils.toString(year - 1));
        }

        yearPanel.insert(renderYear(country, year), i);
        selectYear(country, year);
        break;
      }
    }
  }

  private void addLast(Long country) {
    for (int i = yearPanel.getWidgetCount() - 1; i >= 0; i--) {
      int year = DomUtils.getDataIndexInt(yearPanel.getWidget(i).getElement());

      if (year > 0) {
        year++;

        Widget widget = yearPanel.getWidget(yearPanel.getWidgetCount() - 1);
        if (widget.getElement().hasClassName(STYLE_ADD_LAST)) {
          widget.setTitle(BeeUtils.toString(year + 1));
        }

        yearPanel.insert(renderYear(country, year), i + 1);
        selectYear(country, year);
        break;
      }
    }
  }

  private int count(Long country, int year) {
    int result = 0;

    if (country != null && data.containsKey(country)) {
      for (Integer day : data.get(country)) {
        if (year == getYear(day)) {
          result++;
        }
      }
    }

    return result;
  }

  private Widget findYearWidget(int year) {
    for (Widget widget : yearPanel) {
      if (DomUtils.getDataIndexInt(widget.getElement()) == year) {
        return widget;
      }
    }
    return null;
  }

  private List<JustDate> getDates(Long country, int year) {
    List<JustDate> dates = new ArrayList<>();

    if (country != null && data.containsKey(country)) {
      int min = TimeUtils.startOfYear(year).getDays();
      int max = TimeUtils.startOfYear(year + 1).getDays();

      for (int day : data.get(country)) {
        if (day >= min && day < max) {
          dates.add(new JustDate(day));
        }
      }
    }

    if (dates.size() > 1) {
      Collections.sort(dates);
    }

    return dates;
  }

  private List<Integer> getYears(Long country) {
    List<Integer> years = new ArrayList<>();

    int min = BeeConst.UNDEF;
    int max = BeeConst.UNDEF;

    if (country != null && data.containsKey(country)) {
      for (Integer day : data.get(country)) {
        int year = getYear(day);
        if (BeeConst.isUndef(min)) {
          min = year;
          max = year;
        } else {
          min = Math.min(min, year);
          max = Math.max(max, year);
        }
      }
    }

    if (!BeeConst.isUndef(min)) {
      for (int i = min; i <= max; i++) {
        years.add(i);
      }
    }

    return years;
  }

  private void init(Long country, Integer year) {
    if (DataUtils.isId(country)) {
      countrySelector.setValue(country, false);
    }

    if (year != null) {
      List<Integer> years = getYears(country);
      if (years.isEmpty()) {
        years.add(year);
      }

      renderYears(country, years);

      if (years.contains(year)) {
        selectYear(country, year);
      }
    }
  }

  private void onDayClick(int day) {
    Long country = countrySelector.getRelatedId();

    if (DataUtils.isId(country)) {
      Widget yearWidget = findYearWidget(getYear(day));
      Widget countWidget = UiHelper.getChildByStyleName(yearWidget, STYLE_YEAR_BADGE);

      Badge badge = (countWidget instanceof Badge) ? (Badge) countWidget : null;

      if (data.containsEntry(country, day)) {
        data.remove(country, day);

        Queries.delete(VIEW_HOLIDAYS, Filter.and(Filter.equals(COL_HOLY_COUNTRY, country),
            Filter.equals(COL_HOLY_DAY, day)), null);

        if (badge != null) {
          badge.decrement();
        }

      } else {
        data.put(country, day);

        List<BeeColumn> columns = Data.getColumns(VIEW_HOLIDAYS,
            Arrays.asList(COL_HOLY_COUNTRY, COL_HOLY_DAY));
        Queries.insert(VIEW_HOLIDAYS, columns, Queries.asList(country, day));

        if (badge != null) {
          badge.increment();
        }
      }
    }
  }

  private void renderCalendar(Long country, int year) {
    List<JustDate> dates = getDates(country, year);

    Multimap<Integer, Integer> doms = HashMultimap.create();
    for (JustDate date : dates) {
      doms.put(date.getMonth(), date.getDom());
    }

    if (!calendarPanel.isEmpty()) {
      calendarPanel.clear();
    }

    for (int i = 1; i <= 12; i++) {
      calendarPanel.add(renderMonth(year, i, doms.get(i)));
    }
  }

  private Widget renderMonth(final int year, final int month, Collection<Integer> holiDoms) {
    Flow panel = new Flow(STYLE_MONTH_PANEL);

    Label monthLabel = new Label(Format.renderMonthFullStandalone(month));
    monthLabel.addStyleName(STYLE_MONTH_LABEL);

    panel.add(monthLabel);

    HtmlTable table = new HtmlTable(STYLE_MONTH_TABLE);

    String[] wn = LocaleInfo.getCurrentLocale().getDateTimeFormatInfo().weekdaysNarrow();
    for (int i = 0; i < TimeUtils.DAYS_PER_WEEK; i++) {
      String text = (i == 6) ? wn[0] : wn[i + 1];
      table.setText(0, i, text, STYLE_WEEKDAY_CELL);
    }

    JustDate startOfMonth = new JustDate(year, month, 1);
    int dow = startOfMonth.getDow();
    int shift = dow - 1;

    String styleName;

    for (int dom = 1; dom <= Grego.monthLength(year, month); dom++) {
      int row = (dom + shift - 1) / TimeUtils.DAYS_PER_WEEK + 1;
      int col = dow - 1;

      if (holiDoms.contains(dom)) {
        styleName = STYLE_HOLIDAY;
      } else if (dow >= 6) {
        styleName = STYLE_WEEKEND;
      } else {
        styleName = STYLE_DAY_CELL;
      }

      table.setText(row, col, BeeUtils.toString(dom), styleName);

      dow++;
      if (dow > TimeUtils.DAYS_PER_WEEK) {
        dow = 1;
      }
    }

    table.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        TableCellElement cell =
            DomUtils.getParentCell(EventUtils.getEventTargetElement(event), true);
        String text = (cell == null) ? null : cell.getInnerText();

        if (BeeUtils.isDigit(text)) {
          JustDate date = new JustDate(year, month, BeeUtils.toInt(text));
          onDayClick(date.getDays());

          if (cell.hasClassName(STYLE_HOLIDAY)) {
            cell.removeClassName(STYLE_HOLIDAY);

            cell.addClassName(TimeUtils.isWeekend(date) ? STYLE_WEEKEND : STYLE_DAY_CELL);

          } else {
            cell.removeClassName(STYLE_DAY_CELL);
            cell.removeClassName(STYLE_WEEKEND);

            cell.addClassName(STYLE_HOLIDAY);
          }
        }
      }
    });

    panel.add(table);
    return panel;
  }

  private Widget renderYear(final Long country, final int year) {
    Flow panel = new Flow(STYLE_YEAR_WIDGET);

    Label label = new Label(BeeUtils.toString(year));
    label.addStyleName(STYLE_YEAR_LABEL);
    panel.add(label);

    Badge badge = new Badge(count(country, year));
    badge.addStyleName(STYLE_YEAR_BADGE);
    panel.add(badge);

    DomUtils.setDataIndex(panel.getElement(), year);

    panel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectYear(country, year);
      }
    });

    return panel;
  }

  private void renderYears(final Long country, List<Integer> years) {
    if (!yearPanel.isEmpty()) {
      yearPanel.clear();
    }

    if (!years.isEmpty()) {
      FaLabel addFirst = new FaLabel(FontAwesome.PLUS_SQUARE_O, STYLE_ADD_FIRST);
      addFirst.setTitle(BeeUtils.toString(years.get(0) - 1));

      addFirst.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          addFirst(country);
        }
      });

      yearPanel.add(addFirst);
    }

    for (int year : years) {
      yearPanel.add(renderYear(country, year));
    }

    if (!years.isEmpty()) {
      FaLabel addLast = new FaLabel(FontAwesome.PLUS_SQUARE_O, STYLE_ADD_LAST);
      addLast.setTitle(BeeUtils.toString(years.get(years.size() - 1) + 1));

      addLast.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          addLast(country);
        }
      });

      yearPanel.add(addLast);
    }
  }

  private void selectYear(Long country, int year) {
    for (Widget widget : yearPanel) {
      if (widget.getElement().hasClassName(STYLE_YEAR_ACTIVE)) {
        widget.removeStyleName(STYLE_YEAR_ACTIVE);
      }
    }

    Widget yearWidget = findYearWidget(year);
    if (yearWidget != null) {
      yearWidget.addStyleName(STYLE_YEAR_ACTIVE);
    }

    renderCalendar(country, year);
  }
}
