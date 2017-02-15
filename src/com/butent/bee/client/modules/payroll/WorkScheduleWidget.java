package com.butent.bee.client.modules.payroll;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Storage;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.CloseButton;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndSource;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent.Handler;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IdPair;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.payroll.PayrollUtils;
import com.butent.bee.shared.modules.payroll.PayrollConstants.WorkScheduleKind;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

abstract class WorkScheduleWidget extends Flow implements HasSummaryChangeHandlers, Printable,
    VisibilityChangeEvent.Handler {

  protected static final class CalendarInfo {

    private Multimap<Integer, Long> tcChanges;
    private Set<Integer> inactiveDays;
    private String subTitle;

    private CalendarInfo() {
    }

    protected Set<Integer> getInactiveDays() {
      return inactiveDays;
    }

    protected String getSubTitle() {
      return subTitle;
    }

    protected Multimap<Integer, Long> getTcChanges() {
      return tcChanges;
    }

    protected void setInactiveDays(Set<Integer> inactiveDays) {
      this.inactiveDays = inactiveDays;
    }

    protected void setSubTitle(String subTitle) {
      this.subTitle = subTitle;
    }

    protected void setTcChanges(Multimap<Integer, Long> tcChanges) {
      this.tcChanges = tcChanges;
    }

    private boolean isInactive(int day) {
      return inactiveDays != null && inactiveDays.contains(day);
    }
  }

  private static final String STYLE_PREFIX = PayrollKeeper.STYLE_PREFIX + "ws-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER_PANEL = STYLE_PREFIX + "header-panel";
  private static final String STYLE_CAPTION = STYLE_PREFIX + "caption";
  private static final String STYLE_ACTION = STYLE_PREFIX + "action";

  private static final String STYLE_MONTH_SELECTOR = STYLE_PREFIX + "month-selector";
  private static final String STYLE_MONTH_PANEL = STYLE_PREFIX + "month-panel";
  private static final String STYLE_MONTH_LABEL = STYLE_PREFIX + "month-label";
  private static final String STYLE_MONTH_ACTIVE = STYLE_PREFIX + "month-active";

  private static final String STYLE_DAY_LABEL = STYLE_PREFIX + "day-label";
  private static final String STYLE_DAY_CONTENT = STYLE_PREFIX + "day-content";
  private static final String STYLE_DAY_EMPTY = STYLE_PREFIX + "day-empty";

  private static final String STYLE_PARTITION_PANEL = STYLE_PREFIX + "partition-panel";
  private static final String STYLE_PARTITION_CONTAINER = STYLE_PREFIX + "partition-container";
  private static final String STYLE_PARTITION_NAME = STYLE_PREFIX + "partition-name";
  private static final String STYLE_PARTITION_INFO = STYLE_PREFIX + "partition-info";
  private static final String STYLE_PARTITION_SUBST = STYLE_PREFIX + "partition-subst";
  private static final String STYLE_PARTITION_CLEAR = STYLE_PREFIX + "partition-clear";

  private static final String STYLE_APPEND_PANEL = STYLE_PREFIX + "append-panel";
  private static final String STYLE_APPEND_SELECTOR = STYLE_PREFIX + "append-selector";

  private static final String STYLE_TC_CHANGE = STYLE_PREFIX + "tc-change";

  private static final String STYLE_SCHEDULE_ITEM = STYLE_PREFIX + "item";
  private static final String STYLE_SCHEDULE_TR = STYLE_PREFIX + "item-tr";
  private static final String STYLE_SCHEDULE_TC = STYLE_PREFIX + "item-tc";
  private static final String STYLE_SCHEDULE_RANGE = STYLE_PREFIX + "item-range";
  private static final String STYLE_SCHEDULE_FROM = STYLE_PREFIX + "item-from";
  private static final String STYLE_SCHEDULE_UNTIL = STYLE_PREFIX + "item-until";
  private static final String STYLE_SCHEDULE_DURATION = STYLE_PREFIX + "item-duration";
  private static final String STYLE_SCHEDULE_DRAG = STYLE_PREFIX + "item-drag";

  private static final String STYLE_WEEKEND = STYLE_PREFIX + "weekend";
  private static final String STYLE_HOLIDAY = STYLE_PREFIX + "holiday";
  private static final String STYLE_TODAY = STYLE_PREFIX + "today";

  private static final String STYLE_CONTROL_PANEL = STYLE_PREFIX + "control-panel";
  private static final String STYLE_MODE_PANEL = STYLE_PREFIX + "mode-panel";

  private static final String STYLE_COMMAND = STYLE_PREFIX + "command";
  private static final String STYLE_COMMAND_SUBSTITUTION = STYLE_PREFIX + "command-substitution";
  private static final String STYLE_COMMAND_FETCH = STYLE_PREFIX + "command-fetch";
  private static final String STYLE_COMMAND_EXTEND = STYLE_PREFIX + "command-extend";

  private static final String STYLE_INPUT_MODE_PANEL = STYLE_PREFIX + "input-mode-panel";
  private static final String STYLE_INPUT_MODE_SIMPLE = STYLE_PREFIX + "input-mode-simple";
  private static final String STYLE_INPUT_MODE_FULL = STYLE_PREFIX + "input-mode-full";
  private static final String STYLE_INPUT_MODE_TOGGLE = STYLE_PREFIX + "input-mode-toggle";
  private static final String STYLE_INPUT_MODE_ACTIVE = STYLE_PREFIX + "input-mode-active";

  private static final String STYLE_DND_MODE_PANEL = STYLE_PREFIX + "dnd-mode-panel";
  private static final String STYLE_DND_MODE_MOVE = STYLE_PREFIX + "dnd-mode-move";
  private static final String STYLE_DND_MODE_COPY = STYLE_PREFIX + "dnd-mode-copy";
  private static final String STYLE_DND_MODE_TOGGLE = STYLE_PREFIX + "dnd-mode-toggle";
  private static final String STYLE_DND_MODE_ACTIVE = STYLE_PREFIX + "dnd-mode-active";

  private static final String STYLE_INACTIVE_DAY = STYLE_PREFIX + "inactive-day";
  private static final String STYLE_INACTIVE_MONTH = STYLE_PREFIX + "inactive-month";
  private static final String STYLE_OVERLAP_WARNING = STYLE_PREFIX + "overlap-warn";
  private static final String STYLE_OVERLAP_ERROR = STYLE_PREFIX + "overlap-err";

  private static final String STYLE_WD_LABEL = STYLE_PREFIX + "wd-label";
  private static final String STYLE_WH_LABEL = STYLE_PREFIX + "wh-label";
  private static final String STYLE_WD_SUM = STYLE_PREFIX + "wd-sum";
  private static final String STYLE_WH_SUM = STYLE_PREFIX + "wh-sum";
  private static final String STYLE_WD_TOTAL = STYLE_PREFIX + "wd-total";
  private static final String STYLE_WH_TOTAL = STYLE_PREFIX + "wh-total";

  private static final String STYLE_TRC_DIALOG = STYLE_PREFIX + "trc-dialog";
  private static final String STYLE_TRC_PANEL = STYLE_PREFIX + "trc-panel";
  private static final String STYLE_TRC_HEADER = STYLE_PREFIX + "trc-header";

  private static final String STYLE_TRC_INPUT_CONTAINER = STYLE_PREFIX + "trc-input-container";
  private static final String STYLE_TRC_INPUT_LABEL = STYLE_PREFIX + "trc-input-label";
  private static final String STYLE_TRC_INPUT_WIDGET = STYLE_PREFIX + "trc-input-widget";

  private static final String STYLE_TRC_OPTIONS_CONTAINER = STYLE_PREFIX + "trc-options-container";
  private static final String STYLE_TRC_OPTIONS_TABLE = STYLE_PREFIX + "trc-options-table";

  private static final String STYLE_TRC_OPTION_WIDGET = STYLE_PREFIX + "trc-option-widget";
  private static final String STYLE_TRC_OPTION_CODE = STYLE_PREFIX + "trc-option-code";
  private static final String STYLE_TRC_OPTION_SEPARATOR = STYLE_PREFIX + "trc-option-separator";
  private static final String STYLE_TRC_OPTION_INFO = STYLE_PREFIX + "trc-option-info";

  private static final String STYLE_TRC_CONTROLS = STYLE_PREFIX + "trc-controls";
  private static final String STYLE_TRC_CANCEL = STYLE_PREFIX + "trc-cancel";

  private static final String STYLE_NEW_SUBSTITUTION_PREFIX = STYLE_PREFIX + "new-substitution-";

  private static final String STYLE_FETCH_PREFIX = STYLE_PREFIX + "fetch-";

  private static final String STYLE_FETCH_DIALOG = STYLE_FETCH_PREFIX + "dialog";
  private static final String STYLE_FETCH_PANEL = STYLE_FETCH_PREFIX + "panel";
  private static final String STYLE_FETCH_TABLE_WRAPPER = STYLE_FETCH_PREFIX + "table-wrapper";

  private static final String STYLE_FETCH_TABLE = STYLE_FETCH_PREFIX + "table";
  private static final String STYLE_FETCH_COL = STYLE_FETCH_PREFIX + "col";
  private static final String STYLE_FETCH_COL_LABEL = STYLE_FETCH_PREFIX + "col-label";
  private static final String STYLE_FETCH_ROW = STYLE_FETCH_PREFIX + "row";
  private static final String STYLE_FETCH_ROW_LABEL = STYLE_FETCH_PREFIX + "row-label";
  private static final String STYLE_FETCH_NAME = STYLE_FETCH_PREFIX + "name";
  private static final String STYLE_FETCH_SUBST = STYLE_FETCH_PREFIX + "subst";
  private static final String STYLE_FETCH_DAY_CONTENT = STYLE_FETCH_PREFIX + "day-content";

  private static final String STYLE_FETCH_SELECTION_PANEL = STYLE_FETCH_PREFIX + "selection-panel";
  private static final String STYLE_FETCH_ROW_TOGGLE = STYLE_FETCH_PREFIX + "row-toggle";
  private static final String STYLE_FETCH_COL_TOGGLE = STYLE_FETCH_PREFIX + "col-toggle";

  private static final String STYLE_FETCH_ROW_SELECTED = STYLE_FETCH_PREFIX + "row-selected";
  private static final String STYLE_FETCH_COL_SELECTED = STYLE_FETCH_PREFIX + "col-selected";
  private static final String STYLE_FETCH_CELL_SELECTED = STYLE_FETCH_PREFIX + "cell-selected";

  private static final String STYLE_FETCH_COMMAND_PANEL = STYLE_FETCH_PREFIX + "command-panel";
  private static final String STYLE_FETCH_SUBMIT = STYLE_FETCH_PREFIX + "submit";
  private static final String STYLE_FETCH_CANCEL = STYLE_FETCH_PREFIX + "cancel";

  private static final String KEY_YM = "ym";
  private static final String KEY_DAY = "day";

  private static final String KEY_PART = "part";
  private static final String KEY_SUBST = "subst";

  private static final String KEY_MILLIS = "millis";

  private static final String DATA_TYPE_WS_ITEM = "WorkScheduleItem";

  private static final String NAME_ACTIVE_MONTH = "ActiveMonth";
  private static final String NAME_INPUT_MODE = "InputMode";
  private static final String NAME_DND_MODE = "DndMode";

  private static final int HEADER_ROW = 0;
  private static final int MONTH_ROW = HEADER_ROW + 1;
  private static final int DAY_ROW = MONTH_ROW + 1;
  private static final int CALENDAR_START_ROW = DAY_ROW + 1;

  private static final int MONTH_COL = 1;
  private static final int DAY_START_COL = 1;

  private static final int CALENDAR_PARTITION_COL = 0;

  private static final Set<String> NON_PRINTABLE = Sets.newHashSet(STYLE_ACTION,
      STYLE_MONTH_SELECTOR, STYLE_APPEND_PANEL, STYLE_CONTROL_PANEL);

  protected static Set<Integer> getInactiveDays(YearMonth ym,
      JustDate activeFrom, JustDate activeUntil) {

    Set<Integer> days = new HashSet<>();

    if (activeFrom != null && BeeUtils.isMore(activeFrom, ym.getDate())) {
      int max = TimeUtils.sameMonth(activeFrom, ym) ? activeFrom.getDom() - 1 : ym.getLength();
      for (int day = 1; day <= max; day++) {
        days.add(day);
      }
    }

    if (activeUntil != null && BeeUtils.isLess(activeUntil, ym.getLast())) {
      int min = TimeUtils.sameMonth(activeUntil, ym) ? activeUntil.getDom() + 1 : 1;
      int max = ym.getLength();

      for (int day = min; day <= max; day++) {
        days.add(day);
      }
    }

    return days;
  }

  private static String buildTitle(BeeRowSet rowSet, BeeRow row, String... colNames) {
    List<String> values = new ArrayList<>();

    for (String colName : colNames) {
      String value = DataUtils.getString(rowSet, row, colName);
      if (!BeeUtils.isEmpty(value)) {
        values.add(value);
      }
    }

    return BeeUtils.buildLines(values);
  }

  private static String extendStyleName(String styleName, String extension) {
    return BeeUtils.join(BeeConst.STRING_MINUS, styleName,
        BeeUtils.removeWhiteSpace(extension.toLowerCase()));
  }

  private static String extendWorkScheduleMessage(YearMonth ym) {
    if (ym == null) {
      return Localized.dictionary().extendWorkSchedule(BeeConst.STRING_EMPTY);
    } else {
      return Localized.dictionary().extendWorkSchedule(
          Format.render(PredefinedFormat.YEAR_MONTH_FULL, ym.getDate()));
    }
  }

  private static String formatDuration(String duration) {
    return BeeUtils.parenthesize(duration);
  }

  private static IdPair getPartitionIds(Element elem) {
    return IdPair.of(DomUtils.getDataPropertyLong(elem, KEY_PART),
        DomUtils.getDataPropertyLong(elem, KEY_SUBST));
  }

  private static Filter getSubstituteForFilter(Long value) {
    if (DataUtils.isId(value)) {
      return Filter.equals(COL_SUBSTITUTE_FOR, value);
    } else {
      return Filter.isNull(COL_SUBSTITUTE_FOR);
    }
  }

  private final WorkScheduleKind kind;
  private final ScheduleParent scheduleParent;

  private BeeRowSet emData;
  private BeeRowSet obData;

  private BeeRowSet wsData;
  private BeeRowSet eoData;
  private BeeRowSet tcData;

  private BeeRowSet timeCardCodes;
  private BeeRowSet timeRanges;

  private final Set<Integer> holidays = new HashSet<>();

  private final HtmlTable table;

  private YearMonth activeMonth;

  private final Toggle inputMode;
  private final Toggle dndMode;

  private boolean summarize = true;

  private final List<com.google.web.bindery.event.shared.HandlerRegistration> registry =
      new ArrayList<>();

  WorkScheduleWidget(WorkScheduleKind kind, ScheduleParent scheduleParent) {
    super(STYLE_CONTAINER);

    this.kind = kind;
    this.scheduleParent = scheduleParent;

    addStyleName(STYLE_PREFIX + kind.getStyleSuffix());
    addStyleName(STYLE_PREFIX + scheduleParent.getStyleSuffix());

    this.table = new HtmlTable(STYLE_TABLE);
    add(table);

    this.inputMode = new Toggle(FontAwesome.TOGGLE_OFF, FontAwesome.TOGGLE_ON,
        STYLE_INPUT_MODE_TOGGLE, readBoolean(NAME_INPUT_MODE));

    inputMode.addClickHandler(event -> activateInputMode(inputMode.isChecked()));

    this.dndMode = new Toggle(FontAwesome.ARROW_RIGHT, FontAwesome.RETWEET,
        STYLE_DND_MODE_TOGGLE, readBoolean(NAME_DND_MODE));

    dndMode.addClickHandler(event -> activateDndMode(dndMode.isChecked()));

    table.addClickHandler(event -> {
      Element targetElement = EventUtils.getEventTargetElement(event);
      TableCellElement cell = DomUtils.getParentCell(targetElement, true);

      if (cell != null) {
        IdPair partIds = getPartitionIds(DomUtils.getParentRow(cell, false));
        Integer day = DomUtils.getDataPropertyInt(cell, KEY_DAY);

        if (partIds != null && partIds.hasA() && BeeUtils.isPositive(day)) {
          Widget content = table.getWidgetByElement(cell.getFirstChildElement());
          Flow panel = (content instanceof Flow) ? (Flow) content : null;

          if (EventUtils.hasModifierKey(event.getNativeEvent()) ^ inputMode.isChecked()) {
            editSchedule(partIds, day, panel);
          } else {
            inputTimeRangeCode(partIds, day, panel);
          }
        }
      }
    });

    DndHelper.makeTarget(this, Collections.singleton(DATA_TYPE_WS_ITEM), null,
        DndHelper.ALWAYS_TARGET, (event, u) -> {
          Element targetElement = EventUtils.getEventTargetElement(event);
          TableCellElement cell = DomUtils.getParentCell(targetElement, true);

          if (cell != null && u instanceof Long) {
            IdPair partIds = getPartitionIds(DomUtils.getParentRow(cell, false));
            Integer day = DomUtils.getDataPropertyInt(cell, KEY_DAY);

            long wsId = (long) u;
            boolean copy = EventUtils.hasModifierKey(event.getNativeEvent())
                ^ dndMode.isChecked();

            if (partIds != null && partIds.hasA()
                && BeeUtils.isPositive(day) && activeMonth != null) {

              JustDate date = new JustDate(activeMonth.getYear(), activeMonth.getMonth(), day);
              onDrop(wsId, partIds, date, copy);

            } else if (!copy) {
              removeFromSchedule(wsId);
            }
          }
        });
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public Element getPrintElement() {
    return table.getElement();
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (StyleUtils.hasAnyClass(source, NON_PRINTABLE)) {
      return false;
    } else if (source.hasClassName(STYLE_MONTH_LABEL)) {
      return source.hasClassName(STYLE_MONTH_ACTIVE);
    } else {
      return true;
    }
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      refresh();
    }
  }

  @Override
  public Value getSummary() {
    return new IntegerValue(getScheduledMonths().size());
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  protected void addEmployeeObject(long employeeId, long objectId, final boolean fire) {
    if (activeMonth != null) {
      List<BeeColumn> columns = Data.getColumns(VIEW_EMPLOYEE_OBJECTS,
          Lists.newArrayList(COL_EMPLOYEE, COL_PAYROLL_OBJECT, COL_EMPLOYEE_OBJECT_FROM));
      List<String> values = Queries.asList(employeeId, objectId, activeMonth.getDate());

      Queries.insert(VIEW_EMPLOYEE_OBJECTS, columns, values, null, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          refresh();

          if (fire) {
            RowInsertEvent.fire(BeeKeeper.getBus(), VIEW_EMPLOYEE_OBJECTS, result, getId());
          }
        }
      });
    }
  }

  protected void clearData() {
    setWsData(null);
    setEoData(null);
    setTcData(null);

    setTimeCardCodes(null);
    setTimeRanges(null);
  }

  protected BeeRow findObject(long id) {
    if (DataUtils.isEmpty(obData)) {
      return null;
    } else {
      return obData.getRowById(id);
    }
  }

  protected BeeRow findEmployee(long id) {
    if (DataUtils.isEmpty(emData)) {
      return null;
    } else {
      return emData.getRowById(id);
    }
  }

  protected abstract List<Partition> filterPartitions(DateRange filterRange);

  protected BeeRowSet getEmData() {
    return emData;
  }

  protected String getEmployeeFullName(long id) {
    BeeRow row = findEmployee(id);

    if (row == null) {
      return null;
    } else {
      return BeeUtils.joinWords(DataUtils.getString(emData, row, COL_FIRST_NAME),
          DataUtils.getString(emData, row, COL_LAST_NAME));
    }
  }

  protected abstract long getEmployeeId(long partId);

  protected DateRange getEmployeeRange(long id) {
    BeeRow row = findEmployee(id);

    if (row == null) {
      return null;

    } else {
      JustDate from = DataUtils.getDate(emData, row, COL_DATE_OF_EMPLOYMENT);
      JustDate until = DataUtils.getDate(emData, row, COL_DATE_OF_DISMISSAL);

      return DateRange.closed(from, until);
    }
  }

  protected BeeRowSet getEoData() {
    return eoData;
  }

  protected BeeRowSet getObData() {
    return obData;
  }

  protected String getObjectName(long id) {
    BeeRow row = findObject(id);

    if (row == null) {
      return null;
    } else {
      return DataUtils.getString(obData, row, COL_LOCATION_NAME);
    }
  }

  protected abstract String getPartitionCaption(long partId);

  protected abstract List<Integer> getPartitionContactIndexes();

  protected abstract List<BeeColumn> getPartitionDataColumns();

  protected abstract List<Integer> getPartitionInfoIndexes();

  protected abstract List<Integer> getPartitionNameIndexes();

  protected abstract long getRelationId();

  protected Multimap<Integer, Long> getTimeCardChanges(long employeeId, YearMonth ym) {
    Multimap<Integer, Long> result = ArrayListMultimap.create();

    if (!DataUtils.isEmpty(tcData)) {
      DateRange activeRange = ym.getRange();

      int employeeIndex = tcData.getColumnIndex(COL_EMPLOYEE);
      int codeIndex = tcData.getColumnIndex(COL_TIME_CARD_CODE);

      int fromIndex = tcData.getColumnIndex(COL_TIME_CARD_CHANGES_FROM);
      int untilIndex = tcData.getColumnIndex(COL_TIME_CARD_CHANGES_UNTIL);

      for (BeeRow row : tcData) {
        if (Objects.equals(row.getLong(employeeIndex), employeeId)) {
          JustDate from = row.getDate(fromIndex);
          JustDate until = row.getDate(untilIndex);

          if (DateRange.isValidClosedRange(from, until)) {
            DateRange range = activeRange.intersection(DateRange.closed(from, until));

            if (range != null) {
              int min = range.getMinDate().getDom();
              int max = range.getMaxDate().getDom();

              for (int day = min; day <= max; day++) {
                result.put(day, row.getLong(codeIndex));
              }
            }
          }
        }
      }
    }

    return result;
  }

  protected Filter getTimeCardChangesFilter() {
    return Filter.notNull(kind.getTccColumnName());
  }

  protected Filter getWorkScheduleFilter() {
    return Filter.and(getWorkScheduleKindFilter(), getWorkScheduleRelationFilter());
  }

  protected Filter getWorkScheduleKindFilter() {
    return Filter.equals(COL_WORK_SCHEDULE_KIND, kind);
  }

  protected abstract Filter getWorkScheduleRelationFilter();

  protected BeeRowSet getWsData() {
    return wsData;
  }

  protected abstract void initCalendarInfo(YearMonth ym, CalendarInfo calendarInfo);

  protected abstract boolean isActive(YearMonth ym);

  protected boolean isSubstitutionEnabled() {
    return kind.isSubstitutionEnabled();
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    EventUtils.clearRegistry(registry);
    registry.add(VisibilityChangeEvent.register(this));
  }

  @Override
  protected void onUnload() {
    EventUtils.clearRegistry(registry);

    super.onUnload();
  }

  protected void render() {
    if (!table.isEmpty()) {
      table.clear();
    }

    List<YearMonth> months = getMonths();

    if (activeMonth == null || !months.contains(activeMonth)) {
      String s = BeeKeeper.getStorage().get(storageKey(NAME_ACTIVE_MONTH));
      YearMonth ym = BeeUtils.isEmpty(s) ? null : YearMonth.parse(s);

      if (ym == null || !months.contains(ym)) {
        ym = new YearMonth(TimeUtils.today());
      }
      activateMonth(ym);
    }

    setStyleName(STYLE_INACTIVE_MONTH, !isActive(activeMonth));

    renderHeaders(months);

    int r = CALENDAR_START_ROW;

    List<Partition> partitions = filterPartitions(activeMonth.getRange());
    List<IdPair> ids = new ArrayList<>();

    if (!partitions.isEmpty()) {
      List<Integer> nameIndexes = getPartitionNameIndexes();
      List<Integer> contactIndexes = getPartitionContactIndexes();
      List<Integer> infoIndexes = getPartitionInfoIndexes();

      CalendarInfo calendarInfo = new CalendarInfo();
      initCalendarInfo(activeMonth, calendarInfo);

      for (Partition partition : partitions) {
        Widget ew = renderPartition(partition, nameIndexes, contactIndexes, infoIndexes);
        table.setWidgetAndStyle(r, CALENDAR_PARTITION_COL, ew, STYLE_PARTITION_PANEL);

        updateCalendarInfo(activeMonth, partition, calendarInfo);
        renderSchedule(partition.getIds(), calendarInfo, r);

        Element rowElement = table.getRowFormatter().getElement(r);
        DomUtils.setDataProperty(rowElement, KEY_PART, partition.getRow().getId());
        if (partition.hasSubstituteFor()) {
          DomUtils.setDataProperty(rowElement, KEY_SUBST, partition.getSubstituteFor());
        }

        ids.add(partition.getIds());
        r++;
      }

      checkOverlap();
    }

    renderFooters(months, ids, r);
    updateSums();

    SummaryChangeEvent.maybeFire(this);
  }

  protected abstract Widget renderAppender(Collection<IdPair> partIds, YearMonth ym,
      String selectorStyleName);

  protected void setEmData(BeeRowSet emData) {
    this.emData = emData;
  }

  protected void setEoData(BeeRowSet eoData) {
    this.eoData = eoData;
  }

  protected void setHolidays(Set<Integer> input) {
    BeeUtils.overwrite(holidays, input);
  }

  protected void setObData(BeeRowSet obData) {
    this.obData = obData;
  }

  protected void setTcData(BeeRowSet tcData) {
    this.tcData = tcData;
  }

  protected void setTimeCardCodes(BeeRowSet timeCardCodes) {
    this.timeCardCodes = timeCardCodes;
  }

  protected void setTimeRanges(BeeRowSet timeRanges) {
    this.timeRanges = timeRanges;
  }

  protected void setWsData(BeeRowSet wsData) {
    this.wsData = wsData;
  }

  protected abstract void updateCalendarInfo(YearMonth ym, Partition partition,
      CalendarInfo calendarInfo);

  abstract void refresh();

  private void activateDndMode(boolean modeCopy) {
    Element root = dndMode.getParent().getElement();

    Element el = Selectors.getElementByClassName(root, STYLE_DND_MODE_MOVE);
    if (el != null) {
      UIObject.setStyleName(el, STYLE_DND_MODE_ACTIVE, !modeCopy);
    }

    el = Selectors.getElementByClassName(root, STYLE_DND_MODE_COPY);
    if (el != null) {
      UIObject.setStyleName(el, STYLE_DND_MODE_ACTIVE, modeCopy);
    }

    if (dndMode.isChecked() != modeCopy) {
      dndMode.setChecked(modeCopy);
    }

    BeeKeeper.getStorage().set(storageKey(NAME_DND_MODE), modeCopy);
  }

  private void activateInputMode(boolean modeFull) {
    Element root = inputMode.getParent().getElement();

    Element el = Selectors.getElementByClassName(root, STYLE_INPUT_MODE_SIMPLE);
    if (el != null) {
      UIObject.setStyleName(el, STYLE_INPUT_MODE_ACTIVE, !modeFull);
    }

    el = Selectors.getElementByClassName(root, STYLE_INPUT_MODE_FULL);
    if (el != null) {
      UIObject.setStyleName(el, STYLE_INPUT_MODE_ACTIVE, modeFull);
    }

    if (inputMode.isChecked() != modeFull) {
      inputMode.setChecked(modeFull);
    }

    BeeKeeper.getStorage().set(storageKey(NAME_INPUT_MODE), modeFull);
  }

  private boolean activateMonth(YearMonth ym) {
    if (ym == null || Objects.equals(activeMonth, ym)) {
      return false;
    }

    if (activeMonth != null) {
      Element el = getMonthElement(activeMonth);
      if (el != null) {
        el.removeClassName(STYLE_MONTH_ACTIVE);
      }
    }

    Element monthElement = getMonthElement(ym);
    if (monthElement != null) {
      monthElement.addClassName(STYLE_MONTH_ACTIVE);
    }

    Element selectorElement = Selectors.getElementByClassName(getElement(), STYLE_MONTH_SELECTOR);
    if (selectorElement != null) {
      selectorElement.setInnerText(Format.renderYearMonth(ym));
    }

    setActiveMonth(ym);
    return true;
  }

  private void addDateStyles(int r, int c, JustDate date, boolean today) {
    if (TimeUtils.isWeekend(date)) {
      table.getCellFormatter().addStyleName(r, c, STYLE_WEEKEND);
    }

    if (holidays.contains(date.getDays())) {
      table.getCellFormatter().addStyleName(r, c, STYLE_HOLIDAY);
    }

    if (today) {
      table.getCellFormatter().addStyleName(r, c, STYLE_TODAY);
    }
  }

  private boolean allowDrop(BeeRow source, IdPair partIds, JustDate date) {
    if (source == null) {
      return false;
    }

    int partIndex = wsData.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
    int substIndex = wsData.getColumnIndex(COL_SUBSTITUTE_FOR);

    int dateIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DATE);

    if (Objects.equals(IdPair.get(source, partIndex, substIndex), partIds)
        && Objects.equals(source.getDate(dateIndex), date)) {
      return false;

    } else {
      int trIndex = wsData.getColumnIndex(COL_TIME_RANGE_CODE);
      int tcIndex = wsData.getColumnIndex(COL_TIME_CARD_CODE);

      int fromIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_FROM);
      int untilIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_UNTIL);
      int durIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DURATION);

      Long srcTr = source.getLong(trIndex);
      Long srcTc = source.getLong(tcIndex);

      String srcFrom = source.getString(fromIndex);
      String srcUntil = source.getString(untilIndex);
      String srcDur = source.getString(durIndex);

      TimeRange srcRange = BeeUtils.isEmpty(srcFrom)
          ? null : TimeRange.of(srcFrom, srcUntil, srcDur);

      for (BeeRow row : wsData) {
        if (Objects.equals(IdPair.get(row, partIndex, substIndex), partIds)
            && Objects.equals(row.getDate(dateIndex), date)) {

          Long dstTr = row.getLong(trIndex);
          Long dstTc = row.getLong(tcIndex);

          String dstFrom = row.getString(fromIndex);
          String dstUntil = row.getString(untilIndex);
          String dstDur = row.getString(durIndex);

          TimeRange dstRange = BeeUtils.isEmpty(dstFrom)
              ? null : TimeRange.of(dstFrom, dstUntil, dstDur);

          if (DataUtils.isId(srcTr) || DataUtils.isId(dstTr)) {
            if (Objects.equals(srcTr, dstTr)) {
              return false;
            }

          } else if (DataUtils.isId(srcTc) || DataUtils.isId(dstTc)) {
            if (Objects.equals(srcTc, dstTc)) {
              return false;
            }

          } else if (srcRange != null) {
            if (dstRange != null && dstRange.encloses(srcRange)) {
              return false;
            }

          } else if (!BeeUtils.isEmpty(srcDur)) {
            if (dstRange == null && BeeUtils.equalsTrim(srcDur, dstDur)) {
              return false;
            }
          }
        }
      }

      return true;
    }
  }

  private void checkOverlap() {
    if (activeMonth != null) {
      final int startDay = activeMonth.getDate().getDays();
      final int lastDay = activeMonth.getLast().getDays();

      ParameterList params = PayrollKeeper.createArgs(SVC_GET_SCHEDULE_OVERLAP);

      params.addQueryItem(Service.VAR_COLUMN, scheduleParent.getWorkScheduleRelationColumn());
      params.addQueryItem(Service.VAR_VALUE, getRelationId());

      params.addQueryItem(Service.VAR_FROM, startDay);
      params.addQueryItem(Service.VAR_TO, lastDay);

      params.addQueryItem(COL_WORK_SCHEDULE_KIND, kind.ordinal());

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          Set<Element> warnings = new HashSet<>();
          Set<Element> errors = new HashSet<>();

          if (response.hasResponse()) {
            Splitter splitter = Splitter.on(BeeConst.DEFAULT_ROW_SEPARATOR);

            for (String s : splitter.split(response.getResponseAsString())) {
              String pfx = BeeUtils.getPrefix(s, BeeConst.DEFAULT_VALUE_SEPARATOR);
              String sfx = BeeUtils.getSuffix(s, BeeConst.DEFAULT_VALUE_SEPARATOR);

              Long partId = BeeUtils.toLongOrNull(pfx);
              List<Integer> days = BeeUtils.toInts(sfx);

              if (DataUtils.isId(partId) && !BeeUtils.isEmpty(days)) {
                for (int day : days) {
                  if (BeeUtils.betweenInclusive(Math.abs(day), startDay, lastDay)) {
                    List<Element> cells = findCells(partId, Math.abs(day) - startDay + 1);

                    if (!cells.isEmpty()) {
                      if (day > 0) {
                        warnings.addAll(cells);
                      } else {
                        errors.addAll(cells);
                      }
                    }
                  }
                }
              }
            }
          }

          updateStyles(warnings, STYLE_OVERLAP_WARNING);
          updateStyles(errors, STYLE_OVERLAP_ERROR);
        }
      });
    }
  }

  private void clearSchedule(final IdPair partIds) {
    if (activeMonth == null) {
      return;
    }

    final DateRange range = activeMonth.getRange();

    if (hasSchedule(partIds, range)) {
      String caption = getPartitionCaption(partIds);
      List<String> messages = Lists.newArrayList(Format.renderYearMonth(activeMonth),
          kind.getClearDataQuestion(Localized.dictionary()));

      Global.confirmDelete(caption, Icon.WARNING, messages, () -> {
        Filter filter = Filter.and(getWorkScheduleFilter(),
            Filter.equals(scheduleParent.getWorkSchedulePartitionColumn(), partIds.getA()),
            getSubstituteForFilter(partIds.getB()),
            range.getFilter(COL_WORK_SCHEDULE_DATE));

        Queries.delete(VIEW_WORK_SCHEDULE, filter, new Queries.IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            if (BeeUtils.isPositive(result)) {
              onScheduleModified();
              refresh();
            }
          }
        });
      });

    } else {
      noData();
    }
  }

  private boolean containsSchedule(IdPair partIds, JustDate date) {
    if (!DataUtils.isEmpty(wsData)) {
      int partIndex = wsData.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
      int substIndex = wsData.getColumnIndex(COL_SUBSTITUTE_FOR);

      int dateIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DATE);

      for (BeeRow row : wsData) {
        if (partIds.aEquals(row.getLong(partIndex))
            && partIds.bEquals(row.getLong(substIndex))
            && Objects.equals(row.getDate(dateIndex), date)) {

          return true;
        }
      }
    }

    return false;
  }

  private void doFetch(Multimap<IdPair, BeeRow> selection) {
    BeeRowSet rowSet = Data.createRowSet(VIEW_WORK_SCHEDULE);

    int kindIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_KIND);

    int relIndex = rowSet.getColumnIndex(scheduleParent.getEmployeeObjectRelationColumn());
    int partIndex = rowSet.getColumnIndex(scheduleParent.getEmployeeObjectPartitionColumn());
    int substIndex = rowSet.getColumnIndex(COL_SUBSTITUTE_FOR);

    int dateIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_DATE);

    Set<Integer> copyIndexes = new HashSet<>();
    copyIndexes.add(dateIndex);

    copyIndexes.add(rowSet.getColumnIndex(COL_TIME_RANGE_CODE));
    copyIndexes.add(rowSet.getColumnIndex(COL_TIME_CARD_CODE));

    copyIndexes.add(rowSet.getColumnIndex(COL_WORK_SCHEDULE_FROM));
    copyIndexes.add(rowSet.getColumnIndex(COL_WORK_SCHEDULE_UNTIL));
    copyIndexes.add(rowSet.getColumnIndex(COL_WORK_SCHEDULE_DURATION));

    for (IdPair pair : selection.keySet()) {
      for (BeeRow oldRow : selection.get(pair)) {
        if (!containsSchedule(pair, oldRow.getDate(dateIndex))) {
          BeeRow newRow = DataUtils.createEmptyRow(rowSet.getNumberOfColumns());

          newRow.setValue(kindIndex, kind.ordinal());

          newRow.setValue(relIndex, getRelationId());
          newRow.setValue(partIndex, pair.getA());
          newRow.setValue(substIndex, pair.getB());

          for (int index : copyIndexes) {
            newRow.setValue(index, oldRow.getString(index));
          }

          rowSet.addRow(newRow);
        }
      }
    }

    if (!DataUtils.isEmpty(rowSet)) {
      Queries.insertRows(DataUtils.createRowSetForInsert(rowSet), new RpcCallback<RowInfoList>() {
        @Override
        public void onSuccess(RowInfoList result) {
          onScheduleModified();
          refresh();
        }
      });
    }
  }

  private void editSchedule(final IdPair partIds, int day, final Flow contentPanel) {
    final JustDate date = new JustDate(activeMonth.getYear(), activeMonth.getMonth(), day);

    DataInfo dataInfo = Data.getDataInfo(VIEW_WORK_SCHEDULE);
    BeeRow row = RowFactory.createEmptyRow(dataInfo, true);

    row.setValue(dataInfo.getColumnIndex(COL_WORK_SCHEDULE_KIND), kind.ordinal());

    row.setValue(dataInfo.getColumnIndex(scheduleParent.getWorkScheduleRelationColumn()),
        getRelationId());
    row.setValue(dataInfo.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn()),
        partIds.getA());

    if (partIds.hasB()) {
      row.setValue(dataInfo.getColumnIndex(COL_SUBSTITUTE_FOR), partIds.getB());
    }

    final int dateIndex = dataInfo.getColumnIndex(COL_WORK_SCHEDULE_DATE);
    row.setValue(dateIndex, date);

    Filter filter = Filter.and(getWorkScheduleKindFilter(),
        Filter.or(getWorkScheduleRelationFilter(),
            Filter.equals(scheduleParent.getWorkSchedulePartitionColumn(), partIds.getA())),
        Filter.equals(COL_WORK_SCHEDULE_DATE, date));
    GridFactory.registerImmutableFilter(GRID_WORK_SCHEDULE_DAY, filter);

    WorkScheduleEditor wsEditor = new WorkScheduleEditor(date, holidays,
        () -> {
          updateSchedule(partIds, date);
          onScheduleModified();
        });

    String caption = getPartitionCaption(partIds);

    RowFactory.createRow(FORM_WORK_SCHEDULE_EDITOR, caption, dataInfo, row, Modality.ENABLED,
        contentPanel, wsEditor, null, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            if (result != null) {
              updateSchedule(partIds, result.getDate(dateIndex));
            } else {
              updateSchedule();
            }

            onScheduleModified();
          }
        });
  }

  private List<BeeRow> filterSchedule(IdPair partIds, JustDate date) {
    List<BeeRow> result = new ArrayList<>();

    if (!DataUtils.isEmpty(wsData)) {
      int partIndex = wsData.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
      int substIndex = wsData.getColumnIndex(COL_SUBSTITUTE_FOR);

      int dateIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DATE);

      for (BeeRow row : wsData) {
        if (partIds.aEquals(row.getLong(partIndex))
            && partIds.bEquals(row.getLong(substIndex))
            && Objects.equals(row.getDate(dateIndex), date)) {

          result.add(DataUtils.cloneRow(row));
        }
      }
    }

    return result;
  }

  private Element findCell(IdPair partIds, int day) {
    for (int r = CALENDAR_START_ROW; r < table.getRowCount(); r++) {
      if (Objects.equals(getPartitionIds(table.getRow(r)), partIds)) {
        int c = DAY_START_COL + day - 1;

        if (c < table.getCellCount(r)) {
          return table.getCellFormatter().getElement(r, c);
        }
      }
    }
    return null;
  }

  private List<Element> findCells(long partId, int day) {
    List<Element> result = new ArrayList<>();

    for (int r = CALENDAR_START_ROW; r < table.getRowCount(); r++) {
      if (Objects.equals(DomUtils.getDataPropertyLong(table.getRow(r), KEY_PART), partId)) {
        int c = DAY_START_COL + day - 1;

        if (c < table.getCellCount(r)) {
          TableCellElement cell = table.getCellFormatter().getElement(r, c);
          if (cell != null) {
            result.add(cell);
          }
        }
      }
    }

    return result;
  }

  private Flow getDayContentPanel(IdPair partIds, int day) {
    Element cell = findCell(partIds, day);
    if (cell == null) {
      return null;
    }

    Widget content = table.getWidgetByElement(cell.getFirstChildElement());
    return (content instanceof Flow) ? (Flow) content : null;
  }

  private Table<Integer, Integer, Long> getDurations() {
    Table<Integer, Integer, Long> durations = HashBasedTable.create();

    List<Element> sources = Selectors.getElementsWithDataProperty(table.getElement(), KEY_MILLIS);

    for (Element source : sources) {
      Long millis = DomUtils.getDataPropertyLong(source, KEY_MILLIS);

      TableCellElement cell = DomUtils.getParentCell(source, true);
      Integer day = (cell == null) ? null : DomUtils.getDataPropertyInt(cell, KEY_DAY);

      TableRowElement rowElement = (cell == null) ? null : DomUtils.getParentRow(cell, false);
      Integer r = (rowElement == null) ? null : rowElement.getRowIndex();

      if (BeeUtils.isPositive(r) && BeeUtils.isPositive(day) && BeeUtils.isPositive(millis)) {
        Long value = durations.get(r, day);
        if (BeeUtils.isPositive(value)) {
          millis += value;
        }

        durations.put(r, day, millis);
      }
    }

    return durations;
  }

  private Element getMonthElement(YearMonth ym) {
    return Selectors.getElementByDataProperty(this, KEY_YM, ym.serialize());
  }

  private List<YearMonth> getMonths() {
    List<YearMonth> result = new ArrayList<>();

    YearMonth ym = new YearMonth(TimeUtils.today());
    result.add(ym.previousMonth());
    result.add(ym);
    result.add(ym.nextMonth());

    Set<YearMonth> scheduledMonths = getScheduledMonths();

    if (!scheduledMonths.isEmpty()) {
      for (YearMonth scheduledMonth : scheduledMonths) {
        if (!result.contains(scheduledMonth)) {
          result.add(scheduledMonth);
        }
      }

      Collections.sort(result);
    }

    return result;
  }

  private String getPartitionCaption(IdPair partIds) {
    return BeeUtils.joinWords(getPartitionCaption(partIds.getA()),
        getSubstituteForLabel(partIds.getB()));
  }

  private List<IdPair> getPartitionIds() {
    List<IdPair> result = new ArrayList<>();

    for (int r = CALENDAR_START_ROW; r < table.getRowCount(); r++) {
      Element rowElement = table.getRow(r);

      Long id = DomUtils.getDataPropertyLong(rowElement, KEY_PART);
      if (DataUtils.isId(id)) {
        result.add(IdPair.of(id, DomUtils.getDataPropertyLong(rowElement, KEY_SUBST)));
      }
    }

    return result;
  }

  private Set<YearMonth> getScheduledMonths() {
    Set<YearMonth> result = new HashSet<>();

    if (!DataUtils.isEmpty(wsData)) {
      int dateIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DATE);

      for (BeeRow row : wsData) {
        JustDate date = row.getDate(dateIndex);
        if (date != null) {
          result.add(new YearMonth(date));
        }
      }
    }

    return result;
  }

  private String getSubstituteForLabel(Long id) {
    if (DataUtils.isId(id)) {
      return BeeUtils.bracket(getEmployeeFullName(id));
    } else {
      return null;
    }
  }

  private Multimap<IdPair, Integer> getSubstitutionDays(BeeRowSet data) {
    Multimap<IdPair, Integer> result = HashMultimap.create();

    if (!DataUtils.isEmpty(data)) {
      int partIndex = data.getColumnIndex(scheduleParent.getEmployeeObjectPartitionColumn());
      int substIndex = data.getColumnIndex(COL_SUBSTITUTE_FOR);

      int fromIndex = data.getColumnIndex(COL_EMPLOYEE_OBJECT_FROM);
      int untilIndex = data.getColumnIndex(COL_EMPLOYEE_OBJECT_UNTIL);

      int startDay = activeMonth.getDate().getDays();
      int lastDay = activeMonth.getLast().getDays();

      for (BeeRow row : data) {
        Long part = row.getLong(partIndex);
        Long subst = row.getLong(substIndex);

        JustDate from = row.getDate(fromIndex);
        JustDate until = row.getDate(untilIndex);

        int min = (from == null) ? startDay : Math.max(startDay, from.getDays());
        int max = (until == null) ? lastDay : Math.min(lastDay, until.getDays());

        if (DataUtils.isId(part) && DataUtils.isId(subst) && !Objects.equals(part, subst)
            && min <= max) {

          IdPair pair = IdPair.of(part, subst);

          for (int d = min; d <= max; d++) {
            result.put(pair, d - startDay + 1);
          }
        }
      }
    }

    return result;
  }

  private boolean hasSchedule(IdPair partIds, DateRange range) {
    if (!DataUtils.isEmpty(wsData)) {
      int partIndex = wsData.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
      int substIndex = wsData.getColumnIndex(COL_SUBSTITUTE_FOR);

      int dateIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DATE);

      for (BeeRow row : wsData) {
        if (Objects.equals(IdPair.get(row, partIndex, substIndex), partIds)
            && range.contains(row.getDate(dateIndex))) {
          return true;
        }
      }
    }

    return false;
  }

  private void inputTimeRangeCode(final IdPair partIds, int day, Flow contentPanel) {
    if (DataUtils.isEmpty(timeRanges)) {
      return;
    }

    final JustDate date = new JustDate(activeMonth.getYear(), activeMonth.getMonth(), day);

    Set<Long> usedCodes = new HashSet<>();

    List<BeeRow> schedule = filterSchedule(partIds, date);

    if (!schedule.isEmpty()) {
      int index = wsData.getColumnIndex(COL_TIME_RANGE_CODE);
      for (BeeRow row : schedule) {
        Long code = row.getLong(index);
        if (DataUtils.isId(code)) {
          usedCodes.add(code);
        }
      }
    }

    final List<Long> ids = new ArrayList<>();
    final List<String> codes = new ArrayList<>();
    List<String> labels = new ArrayList<>();

    int codeIndex = timeRanges.getColumnIndex(COL_TR_CODE);
    int nameIndex = timeRanges.getColumnIndex(COL_TR_NAME);

    int fromIndex = timeRanges.getColumnIndex(COL_TR_FROM);
    int untilIndex = timeRanges.getColumnIndex(COL_TR_UNTIL);

    for (BeeRow row : timeRanges) {
      if (!usedCodes.contains(row.getId())) {
        ids.add(row.getId());
        codes.add(row.getString(codeIndex));

        labels.add(BeeUtils.joinWords(row.getString(nameIndex),
            TimeUtils.renderPeriod(row.getString(fromIndex), row.getString(untilIndex))));
      }
    }

    if (!ids.isEmpty()) {
      String caption = getPartitionCaption(partIds);
      final DialogBox dialog = DialogBox.create(caption, STYLE_TRC_DIALOG);

      Flow panel = new Flow(STYLE_TRC_PANEL);

      Label header = new Label(Format.renderDateFull(date));
      header.addStyleName(STYLE_TRC_HEADER);
      panel.add(header);

      Flow inputContainer = new Flow(STYLE_TRC_INPUT_CONTAINER);

      Label inputLabel = new Label(Localized.dictionary().timeRangeCode());
      inputLabel.addStyleName(STYLE_TRC_INPUT_LABEL);
      inputContainer.add(inputLabel);

      final InputText inputWidget = new InputText();
      inputWidget.addStyleName(STYLE_TRC_INPUT_WIDGET);

      inputWidget.setMaxLength(Data.getColumnPrecision(VIEW_TIME_RANGES, COL_TR_CODE));
      inputWidget.setUpperCase(true);

      inputWidget.addKeyDownHandler(event -> {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          String value = inputWidget.getValue();

          if (!BeeUtils.isEmpty(value)) {
            long trId = BeeConst.LONG_UNDEF;

            for (int i = 0; i < codes.size(); i++) {
              if (BeeUtils.same(codes.get(i), value)) {
                trId = ids.get(i);
                break;
              }
            }

            if (DataUtils.isId(trId)) {
              event.preventDefault();
              dialog.close();
              scheduleTimeRange(partIds, date, trId);
            }
          }
        }
      });

      inputContainer.add(inputWidget);
      panel.add(inputContainer);

      int width = BeeKeeper.getScreen().getWidth();
      int minCols = BeeUtils.resize(width, 300, 1920, 2, 4);
      int maxCols = Math.max(BeeUtils.resize(width, 300, 1920, 3, 8), minCols + 1);

      int size = codes.size();
      int cols = UiHelper.getLayoutColumns(size, minCols, maxCols);

      String separator = new Span().addClass(STYLE_TRC_OPTION_SEPARATOR).build();

      HtmlTable options = new HtmlTable(STYLE_TRC_OPTIONS_TABLE);

      for (int i = 0; i < size; i++) {
        Span code = new Span().addClass(STYLE_TRC_OPTION_CODE).text(codes.get(i));
        Span info = new Span().addClass(STYLE_TRC_OPTION_INFO).text(labels.get(i));
        String html = code.build() + separator + info.build();

        Button option = new Button(html);
        DomUtils.setDataIndex(option.getElement(), ids.get(i));

        option.addClickHandler(event -> {
          long trId = DomUtils.getDataIndexLong(EventUtils.getSourceElement(event));

          if (DataUtils.isId(trId)) {
            dialog.close();
            scheduleTimeRange(partIds, date, trId);
          }
        });

        int r = i / cols;
        int c = i % cols;
        options.setWidgetAndStyle(r, c, option, STYLE_TRC_OPTION_WIDGET);
      }

      Flow optionsContainer = new Flow(STYLE_TRC_OPTIONS_CONTAINER);
      optionsContainer.add(options);
      panel.add(optionsContainer);

      Button cancel = new CloseButton(Localized.dictionary().cancel());
      cancel.addStyleName(STYLE_TRC_CANCEL);

      Flow controls = new Flow(STYLE_TRC_CONTROLS);
      controls.add(cancel);
      panel.add(controls);

      dialog.setWidget(panel);

      dialog.setAnimationEnabled(true);
      dialog.setHideOnEscape(true);

      dialog.focusOnOpen(inputWidget);

      if (contentPanel == null) {
        dialog.center();
      } else {
        dialog.showRelativeTo(contentPanel.getElement());
      }
    }
  }

  private Table<IdPair, Integer, List<BeeRow>> layoutPlannedSchedule(List<IdPair> partIds,
      BeeRowSet workSchedule, Multimap<IdPair, Integer> substitutionDays) {

    Table<IdPair, Integer, List<BeeRow>> layout = HashBasedTable.create();

    int relIndex = workSchedule.getColumnIndex(scheduleParent.getWorkScheduleRelationColumn());
    int partIndex = workSchedule.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());

    int dateIndex = workSchedule.getColumnIndex(COL_WORK_SCHEDULE_DATE);

    boolean ok;

    for (BeeRow wsRow : workSchedule) {
      Long rel = wsRow.getLong(relIndex);
      Long part = wsRow.getLong(partIndex);

      int day = wsRow.getDate(dateIndex).getDom();

      for (IdPair pair : partIds) {
        if (pair.hasB()) {
          ok = false;

          switch (scheduleParent) {
            case EMPLOYEE:
              ok = pair.aEquals(part) && pair.bEquals(rel);
              break;

            case LOCATION:
              ok = Objects.equals(getRelationId(), rel) && pair.bEquals(part);
              break;
          }

          if (ok && substitutionDays != null && substitutionDays.containsKey(pair)) {
            ok = substitutionDays.containsEntry(pair, day);
          }

        } else {
          ok = Objects.equals(getRelationId(), rel) && pair.aEquals(part);
        }

        if (ok) {
          BeeRow row = DataUtils.cloneRow(wsRow);

          if (layout.contains(pair, day)) {
            layout.get(pair, day).add(row);

          } else {
            List<BeeRow> list = new ArrayList<>();
            list.add(row);

            layout.put(pair, day, list);
          }
        }
      }
    }

    return layout;
  }

  private void noData() {
    notifyWarning(Localized.dictionary().noData());
  }

  private void notifyWarning(String... messages) {
    FormView form = ViewHelper.getForm(this);

    if (form == null) {
      BeeKeeper.getScreen().notifyWarning(messages);
    } else {
      form.notifyWarning(messages);
    }
  }

  private void onDrop(final long wsId, final IdPair partIds, final JustDate date, boolean copy) {
    BeeRow source = (wsData == null) ? null : wsData.getRowById(wsId);

    if (allowDrop(source, partIds, date)) {
      String viewName = VIEW_WORK_SCHEDULE;

      int partIndex = wsData.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
      int substIndex = wsData.getColumnIndex(COL_SUBSTITUTE_FOR);

      int dateIndex = wsData.getColumnIndex(COL_WORK_SCHEDULE_DATE);

      if (copy) {
        BeeRow target = DataUtils.cloneRow(source);
        target.setValue(partIndex, partIds.getA());
        target.setValue(substIndex, partIds.getB());

        target.setValue(dateIndex, date);

        BeeRowSet rowSet = DataUtils.createRowSetForInsert(viewName, wsData.getColumns(), target);

        Queries.insertRow(rowSet, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            wsData.addRow(result);
            updateDayContent(partIds, date);

            checkOverlap();
            updateSums();

            SummaryChangeEvent.maybeFire(WorkScheduleWidget.this);
            onScheduleModified();
          }
        });

      } else {
        final Long srcPart = source.getLong(partIndex);
        final Long srcSubst = source.getLong(substIndex);

        final JustDate srcDate = source.getDate(dateIndex);

        List<BeeColumn> columns = DataUtils.getColumns(wsData.getColumns(),
            Lists.newArrayList(scheduleParent.getWorkSchedulePartitionColumn(),
                COL_SUBSTITUTE_FOR, COL_WORK_SCHEDULE_DATE));

        List<String> oldValues = Queries.asList(srcPart, srcSubst, srcDate);
        List<String> newValues = Queries.asList(partIds.getA(), partIds.getB(), date);

        Queries.update(viewName, source.getId(), source.getVersion(),
            columns, oldValues, newValues, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                wsData.removeRowById(wsId);
                wsData.addRow(result);

                updateDayContent(IdPair.of(srcPart, srcSubst), srcDate);
                updateDayContent(partIds, date);

                checkOverlap();
                updateSums();
              }
            });
      }
    }
  }

  private void onExtend() {
    final List<IdPair> partIds = getPartitionIds();

    if (BeeUtils.isEmpty(partIds) || activeMonth == null) {
      noData();
      return;
    }

    CompoundFilter partFilter = Filter.or();

    for (IdPair pair : partIds) {
      if (pair.hasB()) {
        partFilter.add(Filter.and(
            Filter.equals(scheduleParent.getEmployeeObjectPartitionColumn(), pair.getA()),
            Filter.equals(COL_SUBSTITUTE_FOR, pair.getB())));

      } else {
        partFilter.add(Filter.and(
            Filter.equals(scheduleParent.getEmployeeObjectPartitionColumn(), pair.getA()),
            Filter.isNull(COL_SUBSTITUTE_FOR)));
      }
    }

    if (partFilter.isEmpty()) {
      noData();
      return;
    }

    final YearMonth previousMonth = activeMonth.previousMonth();

    Filter filter = Filter.and(Filter.equals(COL_WORK_SCHEDULE_KIND, kind),
        Filter.equals(scheduleParent.getEmployeeObjectRelationColumn(), getRelationId()),
        previousMonth.getRange().getFilter(COL_WORK_SCHEDULE_DATE),
        partFilter);

    Queries.getRowSet(VIEW_WORK_SCHEDULE, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(final BeeRowSet rowSet) {
        if (DataUtils.isEmpty(rowSet)) {
          notifyWarning(Format.renderYearMonth(previousMonth),
              BeeUtils.joinWords(Localized.dictionary().workSchedule(), kind.getCaption()),
              Localized.dictionary().nothingFound());

        } else if (Objects.equals(previousMonth.nextMonth(), activeMonth)) {
          Table<IdPair, Integer, List<BeeRow>> extension =
              tryExtend(previousMonth, partIds, rowSet);

          List<IdPair> keys = new ArrayList<>(partIds);
          keys.retainAll(extension.rowKeySet());

          if (keys.isEmpty()) {
            notifyWarning(extendWorkScheduleMessage(previousMonth),
                Localized.dictionary().noData());

          } else {
            String caption = Localized.dictionary().workScheduleExtension(
                Format.renderYearMonth(previousMonth), Format.renderYearMonth(activeMonth));

            renderFetch(caption, keys, extension, extendWorkScheduleMessage(null),
                WorkScheduleWidget.this::doFetch);
          }
        }
      }
    });
  }

  private void onFetch() {
    final List<IdPair> partIds = getPartitionIds();

    if (BeeUtils.isEmpty(partIds)) {
      noData();
      return;
    }

    CompoundFilter filter = null;
    CompoundFilter substFilter = Filter.or();

    switch (scheduleParent) {
      case EMPLOYEE:
        Set<IdPair> objEmpl = new HashSet<>();

        for (IdPair pair : partIds) {
          if (pair.hasB()) {
            objEmpl.add(pair);
            substFilter.add(Filter.and(
                Filter.equals(scheduleParent.getEmployeeObjectRelationColumn(), getRelationId()),
                Filter.equals(COL_SUBSTITUTE_FOR, pair.getB()),
                Filter.equals(scheduleParent.getEmployeeObjectPartitionColumn(), pair.getA())));

          } else {
            objEmpl.add(IdPair.of(pair.getA(), getRelationId()));
          }
        }

        if (!objEmpl.isEmpty()) {
          filter = Filter.or();

          for (IdPair oe : objEmpl) {
            filter.add(Filter.and(
                Filter.equals(scheduleParent.getWorkSchedulePartitionColumn(), oe.getA()),
                Filter.equals(scheduleParent.getWorkScheduleRelationColumn(), oe.getB())));
          }
        }

        break;

      case LOCATION:
        Set<Long> emplIds = new HashSet<>();

        for (IdPair pair : partIds) {
          if (pair.hasB()) {
            emplIds.add(pair.getB());
            substFilter.add(Filter.and(
                Filter.equals(scheduleParent.getEmployeeObjectRelationColumn(), getRelationId()),
                Filter.equals(COL_SUBSTITUTE_FOR, pair.getB()),
                Filter.equals(scheduleParent.getEmployeeObjectPartitionColumn(), pair.getA())));

          } else {
            emplIds.add(pair.getA());
          }
        }

        if (!emplIds.isEmpty()) {
          filter = Filter.and();
          filter.add(getWorkScheduleRelationFilter(),
              Filter.any(scheduleParent.getWorkSchedulePartitionColumn(), emplIds));
        }

        break;
    }

    if (filter == null || filter.isEmpty()) {
      noData();
      return;
    }

    Filter wsFilter = Filter.and(Filter.equals(COL_WORK_SCHEDULE_KIND, WorkScheduleKind.PLANNED),
        Filter.isNull(COL_SUBSTITUTE_FOR),
        activeMonth.getRange().getFilter(COL_WORK_SCHEDULE_DATE),
        filter);

    final Filter eoFilter;
    if (substFilter == null || substFilter.isEmpty()) {
      eoFilter = null;
    } else {
      eoFilter = Filter.and(substFilter,
          Filter.or(Filter.notNull(COL_EMPLOYEE_OBJECT_FROM),
              Filter.notNull(COL_EMPLOYEE_OBJECT_UNTIL)),
          PayrollUtils.getIntersectionFilter(activeMonth,
              COL_EMPLOYEE_OBJECT_FROM, COL_EMPLOYEE_OBJECT_UNTIL));
    }

    Queries.getRowSet(VIEW_WORK_SCHEDULE, null, wsFilter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(final BeeRowSet wsPlanned) {
        final String caption = Localized.dictionary().workSchedule()
            + BeeUtils.space(10) + Format.renderYearMonth(activeMonth);

        if (DataUtils.isEmpty(wsPlanned)) {
          notifyWarning(Format.renderYearMonth(activeMonth),
              Localized.dictionary().workSchedulePlanned(), Localized.dictionary().nothingFound());

        } else if (eoFilter == null) {
          renderFetch(caption, partIds, layoutPlannedSchedule(partIds, wsPlanned, null),
              Localized.dictionary().fetchWorkSchedule(), WorkScheduleWidget.this::doFetch);

        } else {
          Queries.getRowSet(VIEW_EMPLOYEE_OBJECTS, null, eoFilter, new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet substData) {
              renderFetch(caption, partIds,
                  layoutPlannedSchedule(partIds, wsPlanned, getSubstitutionDays(substData)),
                  Localized.dictionary().fetchWorkSchedule(), WorkScheduleWidget.this::doFetch);
            }
          });
        }
      }
    });
  }

  private static void onScheduleModified() {
    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TIME_RANGES);
  }

  private void onSubstitution() {
    DataInfo dataInfo = Data.getDataInfo(VIEW_EMPLOYEE_OBJECTS);
    BeeRow row = RowFactory.createEmptyRow(dataInfo, true);

    row.setValue(dataInfo.getColumnIndex(scheduleParent.getEmployeeObjectRelationColumn()),
        getRelationId());

    row.setValue(dataInfo.getColumnIndex(COL_EMPLOYEE_OBJECT_FROM), activeMonth.getDate());
    row.setValue(dataInfo.getColumnIndex(COL_EMPLOYEE_OBJECT_UNTIL), activeMonth.getLast());

    String styleName = STYLE_NEW_SUBSTITUTION_PREFIX + scheduleParent.getStyleSuffix();

    RowFactory.createRow(FORM_NEW_SUBSTITUTION, null, dataInfo, row, Modality.ENABLED, null, null,
        formView -> formView.addStyleName(styleName), new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            refresh();
          }
        });
  }

  private boolean readBoolean(String name) {
    String key = storageKey(name);
    if (BeeKeeper.getStorage().hasItem(key)) {
      return BeeKeeper.getStorage().getBoolean(key);
    } else {
      return false;
    }
  }

  private void removeFromSchedule(final long wsId) {
    Queries.deleteRow(VIEW_WORK_SCHEDULE, wsId, new Queries.IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        if (BeeUtils.isPositive(result)) {
          BeeRow row = (wsData == null) ? null : wsData.getRowById(wsId);

          if (row != null) {
            int partIndex = wsData.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
            int substIndex = wsData.getColumnIndex(COL_SUBSTITUTE_FOR);
            IdPair partIds = IdPair.get(row, partIndex, substIndex);

            JustDate date = DataUtils.getDate(wsData, row, COL_WORK_SCHEDULE_DATE);

            wsData.removeRowById(wsId);

            updateDayContent(partIds, date);

            checkOverlap();
            updateSums();

            SummaryChangeEvent.maybeFire(WorkScheduleWidget.this);
            onScheduleModified();
          }
        }
      }
    });
  }

  private void renderDayContent(Flow panel, IdPair partIds, JustDate date,
      Multimap<Integer, Long> tcChanges) {

    if (!panel.isEmpty()) {
      panel.clear();
    }

    int day = date.getDom();

    List<BeeRow> schedule = filterSchedule(partIds, date);

    if (tcChanges.containsKey(day)) {
      for (Long codeId : tcChanges.get(day)) {
        panel.add(renderTimeCardChange(codeId));
      }
    }

    for (BeeRow wsRow : schedule) {
      DndSource widget = renderScheduleItem(wsRow);

      if (widget != null) {
        widget.addStyleName(STYLE_SCHEDULE_ITEM);
        DndHelper.makeSource(widget, DATA_TYPE_WS_ITEM, wsRow.getId(), STYLE_SCHEDULE_DRAG);

        panel.add(widget);
      }
    }

    panel.setStyleName(STYLE_DAY_EMPTY, panel.isEmpty());
  }

  private Widget renderDndMode() {
    Flow panel = new Flow(STYLE_DND_MODE_PANEL);

    Label modeMove = new Label(Localized.dictionary().actionMove());
    modeMove.addStyleName(STYLE_DND_MODE_MOVE);

    modeMove.addClickHandler(event -> {
      if (dndMode.isChecked()) {
        activateDndMode(false);
      }
    });

    Label modeCopy = new Label(Localized.dictionary().actionCopy());
    modeCopy.addStyleName(STYLE_DND_MODE_COPY);

    modeCopy.addClickHandler(event -> {
      if (!dndMode.isChecked()) {
        activateDndMode(true);
      }
    });

    if (dndMode.isChecked()) {
      modeCopy.addStyleName(STYLE_DND_MODE_ACTIVE);
    } else {
      modeMove.addStyleName(STYLE_DND_MODE_ACTIVE);
    }

    panel.add(modeMove);
    panel.add(dndMode);
    panel.add(modeCopy);

    return panel;
  }

  private void renderFetch(String caption, List<IdPair> partIds,
      final Table<IdPair, Integer, List<BeeRow>> layout, String submissionLabel,
      final Consumer<Multimap<IdPair, BeeRow>> consumer) {

    final DialogBox dialog = DialogBox.create(caption, STYLE_FETCH_DIALOG);

    Flow panel = new Flow(STYLE_FETCH_PANEL);

    final HtmlTable schedule = new HtmlTable(STYLE_FETCH_TABLE);

    final int calendarStartRow = 1;
    final int calendarStartCol = 1;

    final int dayCount = activeMonth.getLength();
    int r = 0;

    Flow selectionPanel = new Flow();

    final Toggle rowSelection = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
        STYLE_FETCH_ROW_TOGGLE, true);
    rowSelection.setTitle(scheduleParent.getPartitionTitle());

    rowSelection.addClickHandler(event -> {
      if (event.getSource() instanceof HasCheckedness) {
        boolean select = ((HasCheckedness) event.getSource()).isChecked();

        if (select) {
          List<Element> rows = Selectors.getElementsByClassName(schedule.getElement(),
              STYLE_FETCH_ROW);
          if (!BeeUtils.isEmpty(rows)) {
            StyleUtils.addClassName(rows, STYLE_FETCH_ROW_SELECTED);
          }

        } else {
          List<Element> rows = Selectors.getElementsByClassName(schedule.getElement(),
              STYLE_FETCH_ROW_SELECTED);
          if (!BeeUtils.isEmpty(rows)) {
            StyleUtils.removeClassName(rows, STYLE_FETCH_ROW_SELECTED);
          }
        }
      }
    });

    selectionPanel.add(rowSelection);

    final Toggle colSelection = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
        STYLE_FETCH_COL_TOGGLE, true);
    colSelection.setTitle(Localized.dictionary().days());

    colSelection.addClickHandler(event -> {
      if (event.getSource() instanceof HasCheckedness) {
        boolean select = ((HasCheckedness) event.getSource()).isChecked();

        List<TableCellElement> cells = schedule.getRowCells(0);
        for (TableCellElement cell : cells) {
          if (cell.hasClassName(STYLE_FETCH_COL)
              && (cell.hasClassName(STYLE_FETCH_COL_SELECTED) != select)) {

            setFetchColumnSelection(schedule, cell, select);
          }
        }
      }
    });

    selectionPanel.add(colSelection);

    schedule.setWidgetAndStyle(r, 0, selectionPanel, STYLE_FETCH_SELECTION_PANEL);

    for (int day = 1; day <= dayCount; day++) {
      int c = calendarStartCol + day - 1;

      Label label = new Label(BeeUtils.toString(day));

      label.addClickHandler(event -> {
        Element targetElement = EventUtils.getEventTargetElement(event);
        TableCellElement cell = DomUtils.getParentCell(targetElement, true);

        if (cell != null) {
          boolean select = !cell.hasClassName(STYLE_FETCH_COL_SELECTED);
          setFetchColumnSelection(schedule, cell, select);
        }
      });

      schedule.setWidgetAndStyle(r, c, label, STYLE_FETCH_COL_LABEL);

      schedule.getCellFormatter().addStyleName(r, c, STYLE_FETCH_COL);
      if (colSelection.isChecked()) {
        schedule.getCellFormatter().addStyleName(r, c, STYLE_FETCH_COL_SELECTED);
      }
    }

    r = calendarStartRow;
    for (IdPair pair : partIds) {
      Flow rowLabel = new Flow();

      Label nameWidget = new Label(getPartitionCaption(pair.getA()));
      nameWidget.addStyleName(STYLE_FETCH_NAME);

      rowLabel.add(nameWidget);

      if (pair.hasB()) {
        Label substWidget = new Label(getSubstituteForLabel(pair.getB()));
        substWidget.addStyleName(STYLE_FETCH_SUBST);

        rowLabel.add(substWidget);
      }

      rowLabel.addClickHandler(event -> {
        Element targetElement = EventUtils.getEventTargetElement(event);
        TableRowElement rowElement = DomUtils.getParentRow(targetElement, false);

        if (rowElement != null) {
          rowElement.toggleClassName(STYLE_FETCH_ROW_SELECTED);
        }
      });

      schedule.setWidgetAndStyle(r, 0, rowLabel, STYLE_FETCH_ROW_LABEL);

      Element rowElement = schedule.getRowFormatter().getElement(r);
      rowElement.addClassName(STYLE_FETCH_ROW);
      if (rowSelection.isChecked()) {
        rowElement.addClassName(STYLE_FETCH_ROW_SELECTED);
      }

      DomUtils.setDataProperty(rowElement, KEY_PART, pair.getA());
      if (pair.hasB()) {
        DomUtils.setDataProperty(rowElement, KEY_SUBST, pair.getB());
      }

      r++;
    }

    for (IdPair pair : layout.rowKeySet()) {
      r = calendarStartRow + partIds.indexOf(pair);

      List<Integer> days = new ArrayList<>(layout.row(pair).keySet());
      if (days.size() > 1) {
        Collections.sort(days);
      }

      for (int day : days) {
        Flow content = new Flow();

        for (BeeRow wsRow : layout.get(pair, day)) {
          DndSource widget = renderScheduleItem(wsRow);
          if (widget != null) {
            content.add(widget);
          }
        }

        if (!content.isEmpty()) {
          int c = calendarStartCol + day - 1;
          schedule.setWidgetAndStyle(r, c, content, STYLE_FETCH_DAY_CONTENT);

          if (colSelection.isChecked()) {
            schedule.getCellFormatter().addStyleName(r, c, STYLE_FETCH_CELL_SELECTED);
          }

          DomUtils.setDataProperty(schedule.getCellFormatter().getElement(r, c), KEY_DAY, day);
        }
      }
    }

    for (int i = calendarStartRow; i < schedule.getRowCount(); i++) {
      if (schedule.getCellCount(i) < calendarStartCol + dayCount) {
        schedule.setText(i, calendarStartCol + dayCount - 1, null);
      }
    }

    Flow wrapper = new Flow(STYLE_FETCH_TABLE_WRAPPER);
    wrapper.add(schedule);

    panel.add(wrapper);

    Flow commandPanel = new Flow(STYLE_FETCH_COMMAND_PANEL);

    Button submit = new Button(submissionLabel);
    submit.addStyleName(STYLE_FETCH_SUBMIT);

    submit.addClickHandler(event -> {
      List<Element> selectedRows = Selectors.getElementsByClassName(schedule.getElement(),
          STYLE_FETCH_ROW_SELECTED);
      if (BeeUtils.isEmpty(selectedRows)) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
        return;
      }

      Multimap<IdPair, BeeRow> selection = ArrayListMultimap.create();

      for (Element selectedRow : selectedRows) {
        IdPair pair = getPartitionIds(selectedRow);
        List<Element> cells = Selectors.getElementsByClassName(selectedRow,
            STYLE_FETCH_CELL_SELECTED);

        if (layout.containsRow(pair) && !BeeUtils.isEmpty(cells)) {
          for (Element cell : cells) {
            Integer day = DomUtils.getDataPropertyInt(cell, KEY_DAY);

            if (BeeUtils.isPositive(day)) {
              List<BeeRow> wsRows = layout.get(pair, day);

              if (!BeeUtils.isEmpty(wsRows)) {
                for (BeeRow wsRow : wsRows) {
                  selection.put(pair, wsRow);
                }
              }
            }
          }
        }
      }

      if (selection.isEmpty()) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().noData());

      } else {
        dialog.close();
        consumer.accept(selection);
      }
    });
    commandPanel.add(submit);

    Button cancel = new Button(Localized.dictionary().cancel());
    cancel.addStyleName(STYLE_FETCH_CANCEL);

    cancel.addClickHandler(event -> dialog.close());
    commandPanel.add(cancel);

    panel.add(commandPanel);

    dialog.setWidget(panel);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.center();
  }

  private void renderFooters(List<YearMonth> months, Collection<IdPair> partIds, int r) {
    Widget appender = renderAppender(partIds, activeMonth, STYLE_APPEND_SELECTOR);
    table.setWidgetAndStyle(r, CALENDAR_PARTITION_COL, appender, STYLE_APPEND_PANEL);

    Flow controlPanel = new Flow();

    if (!BeeUtils.isEmpty(partIds)) {
      if (isSubstitutionEnabled()) {
        Button substitutionCommand = new Button(Localized.dictionary().employeeSubstitution());
        substitutionCommand.addStyleName(STYLE_COMMAND);
        substitutionCommand.addStyleName(STYLE_COMMAND_SUBSTITUTION);

        substitutionCommand.addClickHandler(event -> onSubstitution());
        controlPanel.add(substitutionCommand);
      }

      if (kind == WorkScheduleKind.ACTUAL) {
        Button fetchCommand = new Button(Localized.dictionary().fetchWorkSchedule());
        fetchCommand.addStyleName(STYLE_COMMAND);
        fetchCommand.addStyleName(STYLE_COMMAND_FETCH);

        fetchCommand.addClickHandler(event -> onFetch());
        controlPanel.add(fetchCommand);
      }

      if (kind.isExtensionEnabled() && activeMonth != null) {
        YearMonth ym = activeMonth.previousMonth();

        if (BeeUtils.contains(months, ym)) {
          Button extendCommand = new Button(extendWorkScheduleMessage(ym));
          extendCommand.addStyleName(STYLE_COMMAND);
          extendCommand.addStyleName(STYLE_COMMAND_EXTEND);

          extendCommand.addClickHandler(event -> onExtend());
          controlPanel.add(extendCommand);
        }
      }

      Flow modePanel = new Flow(STYLE_MODE_PANEL);
      modePanel.add(renderInputMode());
      modePanel.add(renderDndMode());

      controlPanel.add(modePanel);
    }

    int c = DAY_START_COL;
    table.setWidgetAndStyle(r, c, controlPanel, STYLE_CONTROL_PANEL);
    table.getCellFormatter().setColSpan(r, c, activeMonth.getLength());

    c++;
    CustomDiv wdTotal = new CustomDiv();
    table.setWidgetAndStyle(r, c, wdTotal, STYLE_WD_TOTAL);

    c++;
    CustomDiv whTotal = new CustomDiv();
    table.setWidgetAndStyle(r, c, whTotal, STYLE_WH_TOTAL);
  }

  private void renderHeaders(List<YearMonth> months) {
    int days = activeMonth.getLength();

    Flow headerPanel = new Flow();

    Label caption = new Label(getCaption());
    caption.addStyleName(STYLE_CAPTION);
    headerPanel.add(caption);

    FaLabel refresh = new FaLabel(Action.REFRESH.getIcon(), STYLE_ACTION);
    refresh.addStyleName(STYLE_PREFIX + Action.REFRESH.getStyleSuffix());
    refresh.setTitle(Action.REFRESH.getCaption());
    StyleUtils.enableAnimation(Action.REFRESH, refresh);

    refresh.addClickHandler(event -> refresh());
    headerPanel.add(refresh);

    FaLabel print = new FaLabel(Action.PRINT.getIcon(), STYLE_ACTION);
    print.addStyleName(STYLE_PREFIX + Action.PRINT.getStyleSuffix());
    print.setTitle(Action.PRINT.getCaption());
    StyleUtils.enableAnimation(Action.PRINT, print);

    print.addClickHandler(event -> Printer.print(WorkScheduleWidget.this));
    headerPanel.add(print);

    table.setWidgetAndStyle(HEADER_ROW, 0, headerPanel, STYLE_HEADER_PANEL);
    table.getCellFormatter().setColSpan(HEADER_ROW, 0, DAY_START_COL + days + 2);

    Widget monthSelector = renderMonthSelector();
    table.setWidgetAndStyle(MONTH_ROW, MONTH_COL - 1, monthSelector, STYLE_MONTH_SELECTOR);

    Widget monthPanel = renderMonths(months);
    table.setWidgetAndStyle(MONTH_ROW, MONTH_COL, monthPanel, STYLE_MONTH_PANEL);
    table.getCellFormatter().setColSpan(MONTH_ROW, MONTH_COL, days + 2);

    JustDate date = activeMonth.getDate();

    JustDate today = TimeUtils.today();
    int td = TimeUtils.sameMonth(date, today) ? today.getDom() : BeeConst.UNDEF;

    for (int i = 0; i < days; i++) {
      int day = i + 1;
      date.setDom(day);

      Label label = new Label(BeeUtils.toString(day));

      int c = DAY_START_COL + i;
      table.setWidgetAndStyle(DAY_ROW, c, label, STYLE_DAY_LABEL);

      addDateStyles(DAY_ROW, c, date, day == td);
      table.getCellFormatter().getElement(DAY_ROW, c).setTitle(Format.renderDateFull(date));
    }

    Label wdLabel = new Label(Localized.dictionary().daysShort());
    table.setWidgetAndStyle(DAY_ROW, DAY_START_COL + days, wdLabel, STYLE_WD_LABEL);

    Label whLabel = new Label(Localized.dictionary().hours());
    table.setWidgetAndStyle(DAY_ROW, DAY_START_COL + days + 1, whLabel, STYLE_WH_LABEL);
  }

  private Widget renderInputMode() {
    Flow panel = new Flow(STYLE_INPUT_MODE_PANEL);

    Label modeSimple = new Label(Localized.dictionary().inputSimple());
    modeSimple.addStyleName(STYLE_INPUT_MODE_SIMPLE);

    modeSimple.addClickHandler(event -> {
      if (inputMode.isChecked()) {
        activateInputMode(false);
      }
    });

    Label modeFull = new Label(Localized.dictionary().inputFull());
    modeFull.addStyleName(STYLE_INPUT_MODE_FULL);

    modeFull.addClickHandler(event -> {
      if (!inputMode.isChecked()) {
        activateInputMode(true);
      }
    });

    if (inputMode.isChecked()) {
      modeFull.addStyleName(STYLE_INPUT_MODE_ACTIVE);
    } else {
      modeSimple.addStyleName(STYLE_INPUT_MODE_ACTIVE);
    }

    panel.add(modeSimple);
    panel.add(inputMode);
    panel.add(modeFull);

    return panel;
  }

  private Widget renderMonths(List<YearMonth> months) {
    Flow panel = new Flow();

    int size = months.size();
    int from = Math.max(size - 6, 0);

    for (YearMonth ym : months.subList(from, size)) {
      Label widget = new Label(Format.renderYearMonth(ym));

      widget.addStyleName(STYLE_MONTH_LABEL);
      if (ym.equals(activeMonth)) {
        widget.addStyleName(STYLE_MONTH_ACTIVE);
      }

      DomUtils.setDataProperty(widget.getElement(), KEY_YM, ym.serialize());

      widget.addClickHandler(event -> {
        String s = DomUtils.getDataProperty(EventUtils.getEventTargetElement(event), KEY_YM);

        if (!BeeUtils.isEmpty(s) && activateMonth(YearMonth.parse(s))) {
          BeeKeeper.getStorage().set(storageKey(NAME_ACTIVE_MONTH), s);
          render();
        }
      });

      panel.add(widget);
    }

    return panel;
  }

  private Widget renderMonthSelector() {
    Button selector = new Button();
    if (activeMonth != null) {
      selector.setText(Format.renderYearMonth(activeMonth));
    }

    selector.addClickHandler(event -> {
      final List<YearMonth> months = getMonths();

      List<String> labels = new ArrayList<>();
      for (YearMonth ym : months) {
        labels.add(Format.renderYearMonth(ym));
      }

      Global.choiceWithCancel(Localized.dictionary().yearMonth(), null, labels, value -> {
        YearMonth ym = BeeUtils.getQuietly(months, value);

        if (activateMonth(ym)) {
          BeeKeeper.getStorage().set(storageKey(NAME_ACTIVE_MONTH), ym.serialize());
          render();
        }
      });
    });

    return selector;
  }

  private Widget renderPartition(Partition partition, List<Integer> nameIndexes,
      List<Integer> contactIndexes, List<Integer> infoIndexes) {

    Flow panel = new Flow();

    Flow container = new Flow(STYLE_PARTITION_CONTAINER);

    Label nameWidget = new Label(DataUtils.join(getPartitionDataColumns(), partition.getRow(),
        nameIndexes, BeeConst.STRING_SPACE));
    nameWidget.addStyleName(STYLE_PARTITION_NAME);

    if (!BeeUtils.isEmpty(contactIndexes)) {
      String title = DataUtils.join(getPartitionDataColumns(), partition.getRow(),
          contactIndexes, BeeConst.STRING_EOL);
      if (!BeeUtils.isEmpty(title)) {
        nameWidget.setTitle(title);
      }
    }

    nameWidget.addClickHandler(event -> {
      Element targetElement = EventUtils.getEventTargetElement(event);
      Long id = DomUtils.getDataPropertyLong(DomUtils.getParentRow(targetElement, false),
          KEY_PART);

      if (DataUtils.isId(id)) {
        RowEditor.open(scheduleParent.getPartitionViewName(), id, Opener.MODAL);
      }
    });

    container.add(nameWidget);

    if (!BeeUtils.isEmpty(infoIndexes)) {
      Label infoWidget = new Label(DataUtils.join(getPartitionDataColumns(), partition.getRow(),
          infoIndexes, BeeConst.DEFAULT_LIST_SEPARATOR));
      infoWidget.addStyleName(STYLE_PARTITION_INFO);

      container.add(infoWidget);
    }

    if (partition.hasSubstituteFor()) {
      Label substWidget = new Label(getSubstituteForLabel(partition.getSubstituteFor()));
      substWidget.addStyleName(STYLE_PARTITION_SUBST);

      substWidget.addClickHandler(event -> {
        Element targetElement = EventUtils.getEventTargetElement(event);
        Long id = DomUtils.getDataPropertyLong(DomUtils.getParentRow(targetElement, false),
            KEY_SUBST);

        if (DataUtils.isId(id)) {
          RowEditor.open(VIEW_EMPLOYEES, id, Opener.MODAL);
        }
      });

      container.add(substWidget);
    }

    panel.add(container);

    FaLabel clear = new FaLabel(FontAwesome.TRASH, STYLE_PARTITION_CLEAR);

    clear.addClickHandler(event -> {
      Element targetElement = EventUtils.getEventTargetElement(event);
      IdPair partIds = getPartitionIds(DomUtils.getParentRow(targetElement, false));

      if (partIds != null && partIds.hasA()) {
        clearSchedule(partIds);
      }
    });

    panel.add(clear);

    return panel;
  }

  private void renderSchedule(IdPair partIds, CalendarInfo calendarInfo, int r) {
    JustDate date = activeMonth.getDate();
    int days = activeMonth.getLength();

    JustDate today = TimeUtils.today();
    int td = TimeUtils.sameMonth(date, today) ? today.getDom() : BeeConst.UNDEF;

    String partName = getPartitionCaption(partIds.getA());
    String substLabel = getSubstituteForLabel(partIds.getB());

    String partTitle = BeeUtils.buildLines(partName, substLabel);

    for (int i = 0; i < days; i++) {
      int day = i + 1;
      date.setDom(day);

      Flow panel = new Flow();
      renderDayContent(panel, partIds, date, calendarInfo.getTcChanges());

      int c = DAY_START_COL + i;
      table.setWidgetAndStyle(r, c, panel, STYLE_DAY_CONTENT);

      addDateStyles(r, c, date, day == td);

      TableCellElement cell = table.getCellFormatter().getElement(r, c);
      DomUtils.setDataProperty(cell, KEY_DAY, day);
      cell.setTitle(BeeUtils.buildLines(partTitle, Format.renderDateLong(date),
          Format.renderDayOfWeek(date), calendarInfo.getSubTitle()));

      if (calendarInfo.isInactive(day)) {
        cell.addClassName(STYLE_INACTIVE_DAY);
      }
    }

    CustomDiv wdSum = new CustomDiv();
    wdSum.setTitle(partTitle);
    table.setWidgetAndStyle(r, DAY_START_COL + days, wdSum, STYLE_WD_SUM);

    CustomDiv whSum = new CustomDiv();
    whSum.setTitle(partTitle);
    table.setWidgetAndStyle(r, DAY_START_COL + days + 1, whSum, STYLE_WH_SUM);
  }

  private DndSource renderScheduleItem(BeeRow item) {
    String note = DataUtils.getString(wsData, item, COL_WORK_SCHEDULE_NOTE);

    Long trId = DataUtils.getLong(wsData, item, COL_TIME_RANGE_CODE);

    if (DataUtils.isId(trId) && !DataUtils.isEmpty(timeRanges)) {
      BeeRow trRow = timeRanges.getRowById(trId);

      if (trRow != null) {
        DndDiv widget = new DndDiv(STYLE_SCHEDULE_TR);

        String trCode = DataUtils.getString(timeRanges, trRow, COL_TR_CODE);
        if (!BeeUtils.isEmpty(trCode)) {
          widget.setText(trCode);
          widget.addStyleName(extendStyleName(STYLE_SCHEDULE_TR, trCode));
        }

        String from = DataUtils.getString(timeRanges, trRow, COL_TR_FROM);
        String until = DataUtils.getString(timeRanges, trRow, COL_TR_UNTIL);

        String title = BeeUtils.buildLines(DataUtils.getString(timeRanges, trRow, COL_TR_NAME),
            TimeUtils.renderPeriod(from, until),
            DataUtils.getString(timeRanges, trRow, COL_TR_DESCRIPTION), note);
        if (!BeeUtils.isEmpty(title)) {
          widget.setTitle(title);
        }

        UiHelper.setColor(widget,
            DataUtils.getString(timeRanges, trRow, AdministrationConstants.COL_BACKGROUND),
            DataUtils.getString(timeRanges, trRow, AdministrationConstants.COL_FOREGROUND));

        long millis = PayrollUtils.getMillis(from, until,
            DataUtils.getString(timeRanges, trRow, COL_TR_DURATION));
        if (millis > 0) {
          DomUtils.setDataProperty(widget.getElement(), KEY_MILLIS, millis);
        }

        return widget;
      }
    }

    Long tcId = DataUtils.getLong(wsData, item, COL_TIME_CARD_CODE);

    if (DataUtils.isId(tcId) && !DataUtils.isEmpty(timeCardCodes)) {
      BeeRow tcRow = timeCardCodes.getRowById(tcId);

      if (tcRow != null) {
        DndDiv widget = new DndDiv(STYLE_SCHEDULE_TC);

        String tcCode = DataUtils.getString(timeCardCodes, tcRow, COL_TC_CODE);
        if (!BeeUtils.isEmpty(tcCode)) {
          widget.setText(tcCode);
          widget.addStyleName(extendStyleName(STYLE_SCHEDULE_TC, tcCode));
        }

        String title = BeeUtils.buildLines(
            buildTitle(timeCardCodes, tcRow, COL_TC_NAME, COL_TC_DESCRIPTION), note);
        if (!BeeUtils.isEmpty(title)) {
          widget.setTitle(title);
        }

        UiHelper.setColor(widget,
            DataUtils.getString(timeCardCodes, tcRow, AdministrationConstants.COL_BACKGROUND),
            DataUtils.getString(timeCardCodes, tcRow, AdministrationConstants.COL_FOREGROUND));

        return widget;
      }
    }

    String from = DataUtils.getString(wsData, item, COL_WORK_SCHEDULE_FROM);
    String until = DataUtils.getString(wsData, item, COL_WORK_SCHEDULE_UNTIL);
    String duration = DataUtils.getString(wsData, item, COL_WORK_SCHEDULE_DURATION);

    if (!BeeUtils.isEmpty(from) || !BeeUtils.isEmpty(until)) {
      Flow panel = new Flow(STYLE_SCHEDULE_RANGE);
      if (!BeeUtils.isEmpty(note)) {
        panel.setTitle(note);
      }

      if (!BeeUtils.isEmpty(from)) {
        CustomDiv widget = new CustomDiv(STYLE_SCHEDULE_FROM);
        widget.setText(from);
        panel.add(widget);
      }

      if (!BeeUtils.isEmpty(until)) {
        CustomDiv widget = new CustomDiv(STYLE_SCHEDULE_UNTIL);
        widget.setText(until);
        panel.add(widget);
      }

      if (!BeeUtils.isEmpty(duration)) {
        CustomDiv widget = new CustomDiv(STYLE_SCHEDULE_DURATION);
        widget.setText(formatDuration(duration));
        panel.add(widget);
      }

      long millis = PayrollUtils.getMillis(from, until, duration);
      if (millis > 0) {
        DomUtils.setDataProperty(panel.getElement(), KEY_MILLIS, millis);
      }

      return panel;
    }

    if (!BeeUtils.isEmpty(duration)) {
      DndDiv widget = new DndDiv(STYLE_SCHEDULE_DURATION);
      widget.setText(formatDuration(duration));

      if (!BeeUtils.isEmpty(note)) {
        widget.setTitle(note);
      }

      Long time = TimeUtils.parseTime(duration);
      if (BeeUtils.isPositive(time)) {
        DomUtils.setDataProperty(widget.getElement(), KEY_MILLIS, time);
      }

      return widget;
    }

    return null;
  }

  private Widget renderTimeCardChange(long codeId) {
    CustomDiv widget = new CustomDiv(STYLE_TC_CHANGE);

    if (!DataUtils.isEmpty(timeCardCodes)) {
      BeeRow row = timeCardCodes.getRowById(codeId);

      if (row != null) {
        String code = DataUtils.getString(timeCardCodes, row, COL_TC_CODE);
        if (!BeeUtils.isEmpty(code)) {
          widget.setText(code);
          widget.addStyleName(extendStyleName(STYLE_TC_CHANGE, code));
        }

        String title = buildTitle(timeCardCodes, row, COL_TC_NAME, COL_TC_DESCRIPTION);
        if (!BeeUtils.isEmpty(title)) {
          widget.setTitle(title);
        }

        UiHelper.setColor(widget,
            DataUtils.getString(timeCardCodes, row, AdministrationConstants.COL_BACKGROUND),
            DataUtils.getString(timeCardCodes, row, AdministrationConstants.COL_FOREGROUND));
      }
    }

    return widget;
  }

  private void scheduleTimeRange(final IdPair partIds, final JustDate date, long trId) {
    List<String> colNames = Lists.newArrayList(COL_WORK_SCHEDULE_KIND,
        scheduleParent.getWorkScheduleRelationColumn(),
        scheduleParent.getWorkSchedulePartitionColumn(),
        COL_WORK_SCHEDULE_DATE,
        COL_TIME_RANGE_CODE);

    List<String> values = Queries.asList(kind.ordinal(), getRelationId(), partIds.getA(), date,
        trId);

    if (partIds.hasB()) {
      colNames.add(COL_SUBSTITUTE_FOR);
      values.add(BeeUtils.toString(partIds.getB()));
    }

    List<BeeColumn> columns = Data.getColumns(VIEW_WORK_SCHEDULE, colNames);

    Queries.insert(VIEW_WORK_SCHEDULE, columns, values, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        if (wsData == null) {
          updateSchedule(partIds, date);

        } else {
          wsData.addRow(result);
          updateDayContent(partIds, date);

          checkOverlap();
          updateSums();

          SummaryChangeEvent.maybeFire(WorkScheduleWidget.this);
          onScheduleModified();
        }
      }
    });
  }

  private void setActiveMonth(YearMonth activeMonth) {
    this.activeMonth = activeMonth;
  }

  private static void setFetchColumnSelection(HtmlTable schedule, TableCellElement colElement,
      boolean select) {

    if (select) {
      colElement.addClassName(STYLE_FETCH_COL_SELECTED);
    } else {
      colElement.removeClassName(STYLE_FETCH_COL_SELECTED);
    }

    List<TableCellElement> cells = schedule.getColumnCells(colElement.getCellIndex());

    for (TableCellElement cell : cells) {
      if (!cell.hasClassName(STYLE_FETCH_COL)) {
        if (select) {
          cell.addClassName(STYLE_FETCH_CELL_SELECTED);
        } else {
          cell.removeClassName(STYLE_FETCH_CELL_SELECTED);
        }
      }
    }
  }

  private String storageKey(String name) {
    return Storage.getUserKey(kind.getStorageKeyPrefix(), name);
  }

  private Table<IdPair, Integer, List<BeeRow>> tryExtend(YearMonth previousMonth,
      List<IdPair> partIds, BeeRowSet rowSet) {

    Table<IdPair, Integer, List<BeeRow>> result = HashBasedTable.create();

    Multimap<IdPair, Integer> exceptionalDays = HashMultimap.create();
    Table<IdPair, Integer, List<BeeRow>> input = HashBasedTable.create();

    int partIndex = rowSet.getColumnIndex(scheduleParent.getWorkSchedulePartitionColumn());
    int substIndex = rowSet.getColumnIndex(COL_SUBSTITUTE_FOR);

    int dateIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_DATE);

    int trIndex = rowSet.getColumnIndex(COL_TIME_RANGE_CODE);

    int fromIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_FROM);
    int untilIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_UNTIL);
    int durationIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_DURATION);

    for (BeeRow row : rowSet) {
      IdPair part = IdPair.get(row, partIndex, substIndex);

      if (partIds.contains(part)) {
        int day = row.getDate(dateIndex).getDom();

        Long timeRange = row.getLong(trIndex);

        String from = row.getString(fromIndex);
        String until = row.getString(untilIndex);
        String duration = row.getString(durationIndex);

        if (DataUtils.isId(timeRange) || PayrollUtils.getMillis(from, until, duration) > 0) {
          if (input.contains(part, day)) {
            input.get(part, day).add(row);

          } else {
            List<BeeRow> values = new ArrayList<>();
            values.add(row);

            input.put(part, day, values);
          }

        } else {
          exceptionalDays.put(part, day);
        }
      }
    }

    if (input.isEmpty()) {
      return result;
    }

    DateRange defaultActivity = null;
    Map<IdPair, DateRange> partActivity = new HashMap<>();

    switch (scheduleParent) {
      case LOCATION:
        for (IdPair part : input.rowKeySet()) {
          DateRange range = getEmployeeRange(part.getA());
          if (range != null) {
            partActivity.put(part, range);
          }
        }
        break;

      case EMPLOYEE:
        defaultActivity = getEmployeeRange(getRelationId());
        break;
    }

    int prevDays = previousMonth.getLength();
    int days = activeMonth.getLength();

    for (IdPair part : input.rowKeySet()) {
      int minDay = 1;
      int maxDay = days;

      DateRange range = partActivity.get(part);
      if (range == null) {
        range = defaultActivity;
      }

      if (range != null) {
        JustDate minDate = range.getMinDate();
        if (minDate != null && BeeUtils.isMore(minDate, previousMonth.getDate())) {
          minDay = Math.max(minDay, minDate.getDays() - previousMonth.getDate().getDays() + 1);
        }

        JustDate maxDate = range.getMaxDate();
        if (maxDate != null && BeeUtils.isLess(maxDate, activeMonth.getLast())) {
          maxDay = Math.min(maxDay, maxDate.getDays() - activeMonth.getDate().getDays() + 1);
        }

      }

      if (exceptionalDays.containsKey(part)) {
        for (int day : exceptionalDays.get(part)) {
          minDay = Math.max(minDay, day + 1);
        }
      }

      if (prevDays - minDay > 1 && maxDay > 0) {
        List<List<BeeRow>> sequel = PayrollUtils.getSequel(input.row(part), rowSet.getColumns(),
            minDay, prevDays);

        if (!BeeUtils.isEmpty(sequel)) {
          int index = 0;

          for (int day = 1; day <= maxDay; day++) {
            if (!BeeUtils.isEmpty(sequel.get(index))) {
              JustDate date = new JustDate(activeMonth.getYear(), activeMonth.getMonth(), day);
              List<BeeRow> values = new ArrayList<>();

              for (BeeRow inputRow : sequel.get(index)) {
                BeeRow resultRow = DataUtils.cloneRow(inputRow);

                resultRow.setId(DataUtils.NEW_ROW_ID);
                resultRow.setVersion(DataUtils.NEW_ROW_VERSION);
                resultRow.setValue(dateIndex, date);

                values.add(resultRow);
              }

              result.put(part, day, values);
            }

            index++;
            if (index >= sequel.size()) {
              index = 0;
            }
          }
        }
      }
    }

    return result;
  }

  private boolean updateDayContent(IdPair partIds, JustDate date) {
    Flow contentPanel = getDayContentPanel(partIds, date.getDom());

    if (contentPanel == null) {
      return false;

    } else {
      Multimap<Integer, Long> tcc = getTimeCardChanges(getEmployeeId(partIds.getA()),
          YearMonth.of(date));
      renderDayContent(contentPanel, partIds, date, tcc);

      return true;
    }
  }

  private void updateSchedule() {
    Queries.getRowSet(VIEW_WORK_SCHEDULE, null, getWorkScheduleFilter(),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            setWsData(result);
            render();
          }
        });
  }

  private void updateSchedule(final IdPair partIds, final JustDate date) {
    Queries.getRowSet(VIEW_WORK_SCHEDULE, null, getWorkScheduleFilter(),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            setWsData(result);

            if (updateDayContent(partIds, date)) {
              checkOverlap();
              updateSums();

              SummaryChangeEvent.maybeFire(WorkScheduleWidget.this);

            } else {
              render();
            }
          }
        });
  }

  private void updateStyles(Collection<Element> newElements, String styleName) {
    List<Element> oldElements = Selectors.getElementsByClassName(getElement(), styleName);

    if (BeeUtils.isEmpty(oldElements)) {
      if (!BeeUtils.isEmpty(newElements)) {
        StyleUtils.addClassName(newElements, styleName);
      }

    } else if (BeeUtils.isEmpty(newElements)) {
      StyleUtils.removeClassName(oldElements, styleName);

    } else {
      for (Element el : oldElements) {
        if (!newElements.contains(el)) {
          el.removeClassName(styleName);
        }
      }

      for (Element el : newElements) {
        if (!oldElements.contains(el)) {
          el.addClassName(styleName);
        }
      }
    }
  }

  private void updateSums() {
    Table<Integer, Integer, Long> durations = getDurations();

    List<Element> wdSums = Selectors.getElementsByClassName(table.getElement(), STYLE_WD_SUM);
    for (Element wdSum : wdSums) {
      Integer r = DomUtils.getParentRowIndex(wdSum);

      if (r != null && durations.containsRow(r)) {
        wdSum.setInnerText(BeeUtils.toString(durations.row(r).size()));
      } else {
        wdSum.setInnerText(BeeConst.STRING_EMPTY);
      }
    }

    List<Element> whSums = Selectors.getElementsByClassName(table.getElement(), STYLE_WH_SUM);
    for (Element whSum : whSums) {
      Integer r = DomUtils.getParentRowIndex(whSum);

      if (r != null && durations.containsRow(r)) {
        long millis = 0L;
        for (Long value : durations.row(r).values()) {
          millis += value;
        }

        whSum.setInnerText(TimeUtils.renderTime(millis, false));
      } else {
        whSum.setInnerText(BeeConst.STRING_EMPTY);
      }
    }

    Element wdTot = Selectors.getElementByClassName(table.getElement(), STYLE_WD_TOTAL);
    if (wdTot != null) {
      if (durations.isEmpty()) {
        wdTot.setInnerText(BeeConst.STRING_EMPTY);
      } else {
        wdTot.setInnerText(BeeUtils.toString(durations.columnKeySet().size()));
      }
    }

    Element whTot = Selectors.getElementByClassName(table.getElement(), STYLE_WH_TOTAL);
    if (whTot != null) {
      if (durations.isEmpty()) {
        whTot.setInnerText(BeeConst.STRING_EMPTY);

      } else {
        long millis = 0L;
        for (Long value : durations.values()) {
          millis += value;
        }

        whTot.setInnerText(TimeUtils.renderTime(millis, false));
      }
    }
  }
}
