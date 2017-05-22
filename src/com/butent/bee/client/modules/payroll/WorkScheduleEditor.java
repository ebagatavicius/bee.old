package com.butent.bee.client.modules.payroll;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;

import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputTimeOfDay;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.time.Grego;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class WorkScheduleEditor extends AbstractFormInterceptor {

  private final GridInterceptor gridInterceptor = new AbstractGridInterceptor() {

    @Override
    public DeleteMode beforeDeleteRow(GridPresenter presenter, IsRow row) {

      Long employee = row.getLong(getGridView().getDataIndex(COL_EMPLOYEE));
      YearMonth day = YearMonth.of(row.getDate(getGridView().getDataIndex(COL_WORK_SCHEDULE_DATE)));

      for (int i = 0; i < lockData.getNumberOfRows(); i++) {
        YearMonth lock = new YearMonth(lockData.getDate(i, COL_WOKR_SCHEDULE_LOCK));
        if (Objects.equals(employee, lockData.getLong(i, COL_EMPLOYEE)) && lock.equals(day)) {
          getGridView().notifySevere(Localized.dictionary().rowIsReadOnly());
          return DeleteMode.CANCEL;
        }
      }
      return super.beforeDeleteRow(presenter, row);
    }

    @Override
    public void afterDeleteRow(long rowId) {
      dayRefresher.run();
    }

    @Override
    public void afterInsertRow(IsRow result) {
      dayRefresher.run();
    }

    @Override
    public void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
        boolean rowMode) {
      dayRefresher.run();
    }

    @Override
    public void afterUpdateRow(IsRow result) {
      dayRefresher.run();
    }

    @Override
    public String getCaption() {
      return Format.renderDateFull(date);
    }

    @Override
    public void beforeRender(GridView gridView, RenderingEvent event) {

      if (getWorkScheduleKind(WorkScheduleEditor.this.getFormView()) == WorkScheduleKind.ACTUAL) {
        gridView.getGrid().removeColumn(COL_TIME_RANGE_CODE);
        gridView.getGrid().removeColumn("TimeFrom");
        gridView.getGrid().removeColumn("TimeUntil");
      }

      super.beforeRender(gridView, event);
    }

    @Override
    public GridInterceptor getInstance() {
      return this;
    }
  };

  private static final String STYLE_PREFIX = PayrollKeeper.STYLE_PREFIX + "ws-editor-";

  private static final String STYLE_CALENDAR_PANEL = STYLE_PREFIX + "calendar-panel";
  private static final String STYLE_CALENDAR_LABEL = STYLE_PREFIX + "calendar-label";
  private static final String STYLE_CALENDAR_TABLE = STYLE_PREFIX + "calendar-table";
  private static final String STYLE_WEEKDAY_CELL = STYLE_PREFIX + "calendar-cell";

  private static final String STYLE_DAY_CELL = STYLE_PREFIX + "day-cell";
  private static final String STYLE_WEEKEND = STYLE_PREFIX + "weekend";
  private static final String STYLE_HOLIDAY = STYLE_PREFIX + "holiday";
  private static final String STYLE_DAY_SELECTED = STYLE_PREFIX + "day-selected";

  private final JustDate date;
  private final Set<Integer> holidays;

  private final Runnable dayRefresher;
  private final BeeRowSet lockData;

  private DataSelector timeCardCodeSelector;
  private InputTimeOfDay durationInput;

  private String calendarId;

  WorkScheduleEditor(JustDate date, Set<Integer> holidays, Runnable dayRefresher,
                     BeeRowSet lockData) {
    this.date = date;

    if (holidays == null) {
      this.holidays = new HashSet<>();
    } else {
      this.holidays = holidays;
    }

    this.dayRefresher = dayRefresher;
    this.lockData = lockData;
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    switch (editableWidget.getColumnId()) {
      case COL_TIME_CARD_CODE:
        if (widget instanceof DataSelector) {
          timeCardCodeSelector = (DataSelector) widget;
          timeCardCodeSelector.addEditStopHandler(event -> onTimeCardSelectorEdit());
        }
        break;
      case COL_WORK_SCHEDULE_DURATION:
        if (widget instanceof  InputTimeOfDay) {
          durationInput = (InputTimeOfDay) widget;
          durationInput.addValueChangeHandler(event -> onDurationInputValueChange());
          durationInput.addEditStopHandler(event -> onDurationInputValueChange());
        }
        break;
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if ("Calendar".equals(name) && widget instanceof Flow && date != null) {
      renderCalendar((Flow) widget);

    } else if (widget instanceof GridPanel && dayRefresher != null) {
      ((GridPanel) widget).setGridInterceptor(gridInterceptor);
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    initTimeCardCodeSelectorFilter();
    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new WorkScheduleEditor(date, holidays, dayRefresher, lockData);
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    boolean tr = event.containsColumn(COL_TIME_RANGE_CODE);
    boolean tc = event.containsColumn(COL_TIME_CARD_CODE);

    boolean fr = event.containsColumn(COL_WORK_SCHEDULE_FROM);
    boolean to = event.containsColumn(COL_WORK_SCHEDULE_UNTIL);

    boolean du = event.containsColumn(COL_WORK_SCHEDULE_DURATION);
    WorkScheduleKind kind = getWorkScheduleKind(getFormView());

    boolean ok;
    switch (kind) {
      case PLANNED:
        ok = tr && !tc && !fr && !to && !du
          || !tr && tc && !fr && !to && !du
          || !tr && !tc && fr && (to || du)
          || !tr && !tc && !fr && !to && du;
        break;
      case ACTUAL:
        ok = !tr && (tc || du) && !fr && !to;
        break;
      default:
          ok = false;
    }

    if (!ok) {
      event.consume();

      String message;
      if (tr || tc || fr || to || du) {
        message = Localized.dictionary().error();
      } else {
        message = Localized.dictionary().allValuesCannotBeEmpty();
      }

      event.getCallback().onFailure(message);

    } else {
      Set<JustDate> dates = getSelectedDates();

      if (dates.size() == 1 && !dates.contains(date)) {
        String value = BeeUtils.toString(BeeUtils.peek(dates).getDays());
        event.update(COL_WORK_SCHEDULE_DATE, value);

      } else if (dates.size() > 1) {
        event.consume();

        BeeRowSet rowSet = new BeeRowSet(VIEW_WORK_SCHEDULE, event.getColumns());
        int dateIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_DATE);

        for (JustDate d : dates) {
          BeeRow row = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
              event.getValues());
          row.setValue(dateIndex, d);

          rowSet.addRow(row);
        }

        Queries.insertRows(rowSet, new RpcCallback<RowInfoList>() {
          @Override
          public void onSuccess(RowInfoList result) {
            event.getCallback().onSuccess(null);
          }
        });
      }
    }
  }

  private static WorkScheduleKind getWorkScheduleKind(FormView form) {
    if (form == null) {
      return null;
    }
    return EnumUtils.getEnumByIndex(WorkScheduleKind.class,
      form.getIntegerValue(COL_WORK_SCHEDULE_KIND));
  }

  Filter getTimeCardSelectorFilter() {
    FormView form = getFormView();
    CompoundFilter filter = CompoundFilter.and();


    if (form != null && getWorkScheduleKind(form) != null) {
      filter.add(Filter.notNull(getWorkScheduleKind(form).getTccColumnName()));
    }

    if (durationInput != null && timeCardCodeSelector != null) {
      if (BeeUtils.isEmpty(timeCardCodeSelector.getValue())
        && !BeeUtils.isEmpty(durationInput.getValue())) {
        filter.add(Filter.equals(COL_TC_DURATION_TYPE, TcDurationType.PART_TIME));
      }
    }
    return filter;
  }

  private String getCalendarId() {
    return calendarId;
  }

  private int getMonthLength() {
    return Grego.monthLength(date.getYear(), date.getMonth());
  }

  private Set<JustDate> getSelectedDates() {
    Set<JustDate> dates = new HashSet<>();
    if (date == null || BeeUtils.isEmpty(getCalendarId())) {
      return dates;
    }

    Element root = DomUtils.getElement(getCalendarId());
    List<Element> cells = Selectors.getElementsByClassName(root, STYLE_DAY_SELECTED);

    if (BeeUtils.isEmpty(cells)) {
      dates.add(date);
    } else {
      for (Element cell : cells) {
        int dom = DomUtils.getDataIndexInt(cell);

        if (dom > 0) {
          dates.add(new JustDate(date.getYear(), date.getMonth(), dom));
        }
      }
    }

    return dates;
  }

  private void onDurationInputValueChange() {
    if (timeCardCodeSelector != null) {
      timeCardCodeSelector.setAdditionalFilter(getTimeCardSelectorFilter());
    }
  }

  private void onTimeCardSelectorEdit() {
    FormView formView = getFormView();
    if (durationInput != null && timeCardCodeSelector.getRelatedRow() != null && formView != null) {
      IsRow row = formView.getActiveRow();
      TcDurationType type = Data.getEnum(VIEW_TIME_CARD_CODES,
        timeCardCodeSelector.getRelatedRow(), COL_TC_DURATION_TYPE, TcDurationType.class);

      if (type != TcDurationType.PART_TIME) {
        row.setValue(formView.getDataIndex(COL_WORK_SCHEDULE_DURATION), (String) null);
        formView.refreshBySource(COL_WORK_SCHEDULE_DURATION);
      }
    }
  }

  private void initTimeCardCodeSelectorFilter() {
    if (timeCardCodeSelector != null) {
      timeCardCodeSelector.setAdditionalFilter(getTimeCardSelectorFilter());
    }
  }

  private void renderCalendar(Flow panel) {
    if (!panel.isEmpty()) {
      panel.clear();
    }

    panel.addStyleName(STYLE_CALENDAR_PANEL);

    Label label = new Label(Format.render(PredefinedFormat.YEAR_MONTH_STANDALONE, date));
    label.addStyleName(STYLE_CALENDAR_LABEL);
    panel.add(label);

    HtmlTable table = new HtmlTable(STYLE_CALENDAR_TABLE);

    List<String> wn = Format.getWeekdaysNarrowStandalone();
    for (int i = 0; i < wn.size(); i++) {
      table.setText(0, i, wn.get(i), STYLE_WEEKDAY_CELL);
    }

    JustDate startOfMonth = new JustDate(date.getYear(), date.getMonth(), 1);
    int startDays = startOfMonth.getDays();

    int dow = startOfMonth.getDow();
    int shift = dow - 1;

    int length = getMonthLength();

    for (int dom = 1; dom <= length; dom++) {
      int row = (dom + shift - 1) / TimeUtils.DAYS_PER_WEEK + 1;
      int col = dow - 1;

      table.setText(row, col, BeeUtils.toString(dom), STYLE_DAY_CELL);

      TableCellElement cell = table.getCellFormatter().getElement(row, col);
      DomUtils.setDataIndex(cell, dom);
      DomUtils.preventSelection(cell);

      if (holidays.contains(startDays + dom - 1)) {
        cell.addClassName(STYLE_HOLIDAY);
      } else if (dow >= 6) {
        cell.addClassName(STYLE_WEEKEND);
      }

      if (date.getDom() == dom) {
        cell.addClassName(STYLE_DAY_SELECTED);
      }

      dow++;
      if (dow > TimeUtils.DAYS_PER_WEEK) {
        dow = 1;
      }
    }

    table.addClickHandler(event -> {
      TableCellElement cell =
          DomUtils.getParentCell(EventUtils.getEventTargetElement(event), true);
      int dom = DomUtils.getDataIndexInt(cell);

      if (dom > 0) {
        if (EventUtils.hasModifierKey(event.getNativeEvent())) {
          selectRange(dom, !cell.hasClassName(STYLE_DAY_SELECTED));
        }
        cell.toggleClassName(STYLE_DAY_SELECTED);
      }
    });

    setCalendarId(table.getId());
    panel.add(table);
  }

  private void selectRange(int boundDom, boolean select) {
    int lower = BeeConst.UNDEF;
    int upper = BeeConst.UNDEF;

    Set<Integer> selectedDoms = new HashSet<>();

    Element root = DomUtils.getElement(getCalendarId());
    List<Element> cells = Selectors.getElementsByClassName(root, STYLE_DAY_SELECTED);

    if (!BeeUtils.isEmpty(cells)) {
      for (Element cell : cells) {
        int dom = DomUtils.getDataIndexInt(cell);
        if (dom > 0 && dom != boundDom) {
          selectedDoms.add(dom);
        }
      }
    }

    if (selectedDoms.isEmpty()) {
      if (boundDom > 1 && select) {
        lower = 1;
        upper = boundDom - 1;
      }

    } else {
      boolean found = false;

      if (boundDom > 1) {
        for (int dom = boundDom - 1; dom >= 1; dom--) {
          if (selectedDoms.contains(dom) == select) {
            found = true;
            break;
          } else {
            lower = dom;
          }
        }

        if (!found) {
          lower = BeeConst.UNDEF;
        }
      }

      if (lower > 0) {
        upper = boundDom - 1;

      } else if (!found) {
        int length = getMonthLength();
        if (boundDom < length) {
          for (int dom = boundDom + 1; dom <= length; dom++) {
            if (selectedDoms.contains(dom) == select) {
              break;
            } else {
              upper = dom;
            }
          }
        }

        if (upper > 0) {
          lower = boundDom + 1;
        }
      }
    }

    if (lower > 0 && upper >= lower) {
      for (int dom = lower; dom <= upper; dom++) {
        Element cell = Selectors.getElementByDataIndex(root, dom);
        if (cell != null) {
          cell.toggleClassName(STYLE_DAY_SELECTED);
        }
      }
    }
  }

  private void setCalendarId(String calendarId) {
    this.calendarId = calendarId;
  }
}
