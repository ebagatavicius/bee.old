package com.butent.bee.client.datepicker;

import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;

import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DatePicker extends Composite implements HasValue<JustDate>, HasKeyDownHandlers {

  public static class CssClasses {
    private final String widgetStyleName;
    private final String baseStyleName;

    public CssClasses(String widgetStyleName, String baseStyleName) {
      this.widgetStyleName = widgetStyleName;
      this.baseStyleName = baseStyleName;
    }

    public String datePicker() {
      return getWidgetStyleName();
    }

    public String day() {
      return wrap("day");
    }

    public String day(String dayModifier) {
      return day() + "Is" + dayModifier;
    }

    public String dayIsActive() {
      return day("Active");
    }

    public String dayIsDisabled() {
      return day("Disabled");
    }

    public String dayIsFiller() {
      return day("Filler");
    }

    public String dayIsToday() {
      return day("Today");
    }

    public String dayIsValue() {
      return day("Value");
    }

    public String dayIsWeekend() {
      return day("Weekend");
    }

    public String days() {
      return wrap("days");
    }

    public String daysLabel() {
      return wrap("daysLabel");
    }

    public String getBaseStyleName() {
      return baseStyleName;
    }

    public String getWidgetStyleName() {
      return widgetStyleName;
    }

    public String month() {
      return wrap("month");
    }

    public String monthNavigation() {
      return wrap("monthNavigation");
    }

    public String monthNavigationDisabled() {
      return monthNavigation() + "-disabled";
    }

    public String monthSelector() {
      return wrap("monthSelector");
    }

    public String weekdayLabel() {
      return wrap("weekdayLabel");
    }

    public String weekendLabel() {
      return wrap("weekendLabel");
    }

    protected String wrap(String style) {
      return baseStyleName + style;
    }
  }

  private static final class DateStyler {
    private final Map<JustDate, String> styles = new HashMap<>();

    private DateStyler() {
    }

    public String getStyleName(JustDate date) {
      return styles.get(date);
    }

    public void setStyleName(JustDate date, String styleName, boolean add) {
      String current = styles.get(date);

      if (add) {
        if (current == null) {
          styles.put(date, styleName.trim());
        } else {
          styles.put(date, StyleUtils.addClassName(current, styleName));
        }

      } else if (current != null) {
        String newValue = StyleUtils.removeClassName(current, styleName);
        if (BeeUtils.isEmpty(newValue)) {
          styles.remove(date);
        } else {
          styles.put(date, newValue);
        }
      }
    }
  }

  private static final CssClasses DEFAULT_CSS_CLASSES =
      new CssClasses(BeeConst.CSS_CLASS_PREFIX + "DatePicker",
          BeeConst.CSS_CLASS_PREFIX + "DatePicker-");

  private final DateStyler dateStyler = new DateStyler();

  private final CssClasses cssClasses;

  private final MonthSelector monthSelector;
  private final MonthView view;
  private final Model model;

  private JustDate value;

  private final JustDate minDate;
  private final JustDate maxDate;

  public DatePicker(JustDate date, JustDate minDate, JustDate maxDate) {
    this(date, minDate, maxDate, DEFAULT_CSS_CLASSES);
  }

  public DatePicker(JustDate date, JustDate minDate, JustDate maxDate, CssClasses cssClasses) {
    this(date, minDate, maxDate, cssClasses, new MonthSelector(cssClasses),
        new MonthView(cssClasses), new Model(date));
  }

  public DatePicker(JustDate date, JustDate minDate, JustDate maxDate, CssClasses cssClasses,
      MonthSelector monthSelector, MonthView view, Model model) {

    Assert.notNull(date);
    Assert.notNull(cssClasses);
    Assert.notNull(monthSelector);
    Assert.notNull(view);
    Assert.notNull(model);

    this.minDate = (minDate == null) ? null : TimeUtils.min(date, minDate);
    this.maxDate = (maxDate == null) ? null : TimeUtils.max(date, maxDate);

    this.cssClasses = cssClasses;
    this.monthSelector = monthSelector;
    this.view = view;
    this.model = model;

    monthSelector.setDatePicker(this);
    view.setDatePicker(this);

    view.setUp();
    monthSelector.setUp();
    init();

    addStyleToDate(cssClasses.dayIsToday(), new JustDate());
    setDate(date);
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return view.addKeyDownHandler(handler);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<JustDate> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public JustDate getValue() {
    return JustDate.copyOf(value);
  }

  public void setDate(JustDate newValue) {
    setDate(newValue, false);
  }

  public void setDate(JustDate newValue, boolean fireEvents) {
    Assert.notNull(newValue);

    if (!newValue.equals(value)) {
      setValue(newValue, fireEvents);
      setCurrentMonth(YearMonth.of(newValue));
    }
  }

  public void setFocus(boolean focus) {
    view.setFocus(focus);
  }

  @Override
  public void setValue(JustDate newValue) {
    setValue(newValue, false);
  }

  @Override
  public void setValue(JustDate newValue, boolean fireEvents) {
    if (!Objects.equals(value, newValue)) {
      JustDate oldValue = value;
      if (oldValue != null) {
        removeStyleFromDate(getCssClasses().dayIsValue(), oldValue);
      }

      value = JustDate.copyOf(newValue);
      if (value != null) {
        addStyleToDate(getCssClasses().dayIsValue(), value);
      }
    }

    if (fireEvents) {
      ValueChangeEvent.fire(this, newValue);
    }
  }

  CssClasses getCssClasses() {
    return cssClasses;
  }

  YearMonth getCurrentMonth() {
    return getModel().getCurrentMonth();
  }

  JustDate getMaxDate() {
    return maxDate;
  }

  JustDate getMinDate() {
    return minDate;
  }

  Model getModel() {
    return model;
  }

  String getStyleOfDate(JustDate date) {
    return dateStyler.getStyleName(date);
  }

  boolean isDateEnabled(JustDate date) {
    if (getMinDate() != null && TimeUtils.isLess(date, getMinDate())) {
      return false;
    }

    if (getMaxDate() != null && TimeUtils.isMore(date, getMaxDate())) {
      return false;
    }
    return true;
  }

  boolean isDateVisible(JustDate date) {
    return TimeUtils.isBetweenInclusiveRequired(date, getFirstDate(), getLastDate());
  }

  void refreshAll() {
    getView().refresh();
    getMonthSelector().refresh();
  }

  void setCurrentMonth(YearMonth ym) {
    getModel().setCurrentMonth(ym);
    refreshAll();
  }

  private void addStyleToDate(String styleName, JustDate date) {
    dateStyler.setStyleName(date, styleName, true);
    if (isDateVisible(date)) {
      getView().addStyleToDate(styleName, date);
    }
  }

  private JustDate getFirstDate() {
    return getView().getFirstDate();
  }

  private JustDate getLastDate() {
    return getView().getLastDate();
  }

  private MonthSelector getMonthSelector() {
    return monthSelector;
  }

  private MonthView getView() {
    return view;
  }

  private void removeStyleFromDate(String styleName, JustDate date) {
    dateStyler.setStyleName(date, styleName, false);
    if (isDateVisible(date)) {
      getView().removeStyleFromDate(styleName, date);
    }
  }

  private void init() {
    Vertical panel = new Vertical();
    initWidget(panel);

    setStyleName(panel.getElement(), getCssClasses().datePicker());

    panel.add(getMonthSelector());
    panel.add(getView());
  }
}
