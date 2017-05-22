package com.butent.bee.client.modules.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.timeboard.TimeBoard;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.timeboard.TimeBoardRowLayout;
import com.butent.bee.client.timeboard.TimeBoardRowLayout.RowData;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XPicture;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.service.ServiceConstants.ServiceCompanyKind;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskPriority;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.ScheduleDateRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

final class ServiceCalendar extends TimeBoard {

  static final String SUPPLIER_KEY = "service_calendar";

  private static final BeeLogger logger = LogUtils.getLogger(ServiceCalendar.class);

  private static final String COL_COMPANY_KIND = "CalendarCompanyKind";

  private static final String COL_PIXELS_PER_COMPANY = "CalendarPixelsPerCompany";
  private static final String COL_PIXELS_PER_INFO = "CalendarPixelsPerInfo";

  private static final String COL_SEPARATE_OBJECTS = "CalendarSeparateObjects";

  private static final String COL_PIXELS_PER_DAY = "CalendarPixelsPerDay";
  private static final String COL_PIXELS_PER_ROW = "CalendarPixelsPerRow";

  private static final String COL_HEADER_HEIGHT = "CalendarHeaderHeight";
  private static final String COL_FOOTER_HEIGHT = "CalendarFooterHeight";

  private static final String COL_FOOTER_MAP = "CalendarFooterMap";

  private static final String COL_ITEM_OPACITY = "CalendarItemOpacity";
  private static final String COL_STRIP_OPACITY = "CalendarStripOpacity";

  private static final String COL_TASK_COLOR = "CalendarTaskColor";
  private static final String COL_RT_COLOR = "CalendarRTColor";

  private static final String FORM_SETTINGS = "ServiceCalendarSettings";

  private static final String STYLE_PREFIX = ServiceKeeper.STYLE_PREFIX + "calendar-";

  private static final String STYLE_VIEW = STYLE_PREFIX + "view";

  private static final String STYLE_COMPANY_PREFIX = STYLE_PREFIX + "company-";
  private static final String STYLE_COMPANY_ROW_SEPARATOR = STYLE_COMPANY_PREFIX + "row-sep";
  private static final String STYLE_COMPANY_PANEL = STYLE_COMPANY_PREFIX + "panel";
  private static final String STYLE_COMPANY_LABEL = STYLE_COMPANY_PREFIX + "label";

  private static final String STYLE_INFO_PREFIX = STYLE_PREFIX + "info-";
  private static final String STYLE_INFO_ROW_SEPARATOR = STYLE_INFO_PREFIX + "row-sep";
  private static final String STYLE_INFO_PANEL = STYLE_INFO_PREFIX + "panel";
  private static final String STYLE_INFO_LABEL = STYLE_INFO_PREFIX + "label";

  private static final String STYLE_TASK_PREFIX = STYLE_PREFIX + "task-";
  private static final String STYLE_TASK_PANEL = STYLE_TASK_PREFIX + "panel";
  private static final String STYLE_TASK_STATUS = STYLE_TASK_PREFIX + "status";
  private static final String STYLE_TASK_PRIORITY = STYLE_TASK_PREFIX + "priority";
  private static final String STYLE_TASK_STARRED = STYLE_TASK_PREFIX + "starred";

  private static final String STYLE_RT_PREFIX = STYLE_PREFIX + "rt-";
  private static final String STYLE_RT_PANEL = STYLE_RT_PREFIX + "panel";
  private static final String STYLE_RT_PRIORITY = STYLE_RT_PREFIX + "priority";

  private static final String STYLE_DATE_PREFIX = STYLE_PREFIX + "date-";
  private static final String STYLE_DATE_PANEL = STYLE_DATE_PREFIX + "panel";

  private static final String DEFAULT_TASK_COLOR = Colors.GOLD;
  private static final String DEFAULT_RT_COLOR = Colors.YELLOW;
  private static final String DEFAULT_DATE_COLOR = Colors.RED;

  private static final Set<String> relevantDataViews = Sets.newHashSet(VIEW_SERVICE_OBJECTS,
      VIEW_SERVICE_DATES, VIEW_TASKS, VIEW_RECURRING_TASKS, VIEW_RT_DATES, VIEW_TASK_TYPES);

  static void open(final ViewCallback callback) {
    BeeKeeper.getRpc().makeRequest(ServiceKeeper.createArgs(SVC_GET_CALENDAR_DATA),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            ServiceCalendar sc = new ServiceCalendar();
            sc.onCreate(response, callback);
          }
        });
  }

  private static void exportMonthLabels(Range<JustDate> range, XRow row, int colIndex,
      int styleRef) {

    JustDate start = JustDate.copyOf(range.lowerEndpoint());

    while (BeeUtils.isLeq(start, range.upperEndpoint())) {
      JustDate end = BeeUtils.min(TimeUtils.endOfMonth(start), range.upperEndpoint());
      int span = TimeUtils.dayDiff(start, end) + 1;

      int month = start.getMonth();

      String text;
      if (span < 3 && TimeBoardHelper.getSize(range) > 30) {
        text = BeeConst.STRING_EMPTY;
      } else if (month == 1 && span > 5 && TimeUtils.isMore(start, range.lowerEndpoint())) {
        text = BeeUtils.joinWords(start.getYear(),
            Format.renderMonthFullStandalone(start.getMonth()));
      } else {
        text = Format.renderMonthFullStandalone(start.getMonth());
      }

      int index = colIndex + TimeUtils.dayDiff(range.lowerEndpoint(), start);

      XCell cell = new XCell(index, text, styleRef);
      if (span > 1) {
        cell.setColSpan(span);
      }

      row.add(cell);
      start = TimeUtils.startOfNextMonth(start);
    }
  }

  private static String exportPeriod(Range<JustDate> range) {
    if (range == null) {
      return BeeConst.STRING_EMPTY;
    } else if (Objects.equals(range.lowerEndpoint(), range.upperEndpoint())) {
      return Format.renderDate(range.lowerEndpoint());
    } else {
      return Format.renderPeriod(range.lowerEndpoint(), range.upperEndpoint());
    }
  }

  private static ServiceCompanyKind getCompanyKind(BeeRowSet rowSet) {
    ServiceCompanyKind companyKind = EnumUtils.getEnumByIndex(ServiceCompanyKind.class,
        TimeBoardHelper.getInteger(rowSet, COL_COMPANY_KIND));

    return (companyKind == null) ? ServiceCompanyKind.DETAULT : companyKind;
  }

  private static String getDateColor(ServiceDateWrapper date) {
    return BeeUtils.notEmpty(date.getColor(), DEFAULT_DATE_COLOR);
  }

  private static void setDateColor(ServiceDateWrapper date, Widget widget) {
    String color = getDateColor(date);
    if (!BeeUtils.isEmpty(color)) {
      StyleUtils.setBackgroundColor(widget, color);
    }
  }

  private final List<ServiceCompanyWrapper> companies = new ArrayList<>();

  private final Multimap<Long, ServiceObjectWrapper> objects = ArrayListMultimap.create();
  private final Multimap<Long, TaskWrapper> tasks = ArrayListMultimap.create();

  private final Multimap<Long, RecurringTaskWrapper> recurringTasks = ArrayListMultimap.create();

  private final Multimap<Long, ServiceDateWrapper> dates = ArrayListMultimap.create();
  private int companyWidth = BeeConst.UNDEF;

  private int infoWidth = BeeConst.UNDEF;
  private boolean separateObjects;

  private final Set<String> companyPanels = new HashSet<>();

  private final Set<String> infoPanels = new HashSet<>();

  private final List<Integer> companyIndexesByRow = new ArrayList<>();

  private final Map<Integer, Long> objectsByRow = new HashMap<>();

  private ServiceCalendar() {
    super();
    addStyleName(STYLE_VIEW);
  }

  @Override
  public String getCaption() {
    return Localized.dictionary().svcCalendar();
  }

  @Override
  public String getIdPrefix() {
    return "svc-cal";
  }

  @Override
  public String getSupplierKey() {
    return SUPPLIER_KEY;
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case ADD:
        RowFactory.createRow(VIEW_SERVICE_OBJECTS, Modality.ENABLED);
        break;

      case EXPORT:
        export();
        break;

      default:
        super.handleAction(action);
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (event != null && !event.isSpookyActionAtADistance()
        && event.hasView(VIEW_RELATED_TASKS) && event.hasSource(PROP_STAR)) {
      refresh();
    } else {
      super.onCellUpdate(event);
    }
  }

  protected void addInfoWidget(HasWidgets panel, IdentifiableWidget widget,
      int firstRow, int lastRow) {

    Rectangle rectangle = TimeBoardHelper.getRectangle(getCompanyWidth(), getInfoWidth(),
        firstRow, lastRow, getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget.asWidget(), rectangle, margins);

    panel.add(widget.asWidget());

    infoPanels.add(widget.getId());
  }

  @Override
  protected void editSettings() {
    final BeeRow oldRow = getSettingsRow();
    Assert.notNull(oldRow);

    RowEditor.openForm(FORM_SETTINGS, getSettings().getViewName(), oldRow, Opener.MODAL,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            if (updateSetting(result)) {
              if (requiresRefresh(oldRow, result)) {
                refresh();
              } else {
                render(false);
              }
            }
          }
        });
  }

  @Override
  protected Collection<? extends HasDateRange> getChartItems() {
    List<HasDateRange> items = new ArrayList<>();

    items.addAll(tasks.values());
    items.addAll(recurringTasks.values());

    items.addAll(dates.values());

    return items;
  }

  @Override
  protected Set<Action> getEnabledActions() {
    return EnumSet.of(Action.REFRESH, Action.ADD, Action.EXPORT, Action.CONFIGURE, Action.PRINT);
  }

  @Override
  protected String getFooterHeightColumnName() {
    return COL_FOOTER_HEIGHT;
  }

  @Override
  protected Collection<? extends HasDateRange> getFooterItems() {
    if (TimeBoardHelper.getBoolean(getSettings(), COL_FOOTER_MAP)) {
      return super.getFooterItems();
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  protected String getHeaderHeightColumnName() {
    return COL_HEADER_HEIGHT;
  }

  @Override
  protected String getRowHeightColumnName() {
    return COL_PIXELS_PER_ROW;
  }

  @Override
  protected String getStripOpacityColumnName() {
    return COL_STRIP_OPACITY;
  }

  @Override
  protected boolean isDataEventRelevant(ModificationEvent<?> event) {
    return event != null && event.containsAny(relevantDataViews);
  }

  @Override
  protected void prepareChart(Size canvasSize) {
    setSeparateObjects(TimeBoardHelper.getBoolean(getSettings(), COL_SEPARATE_OBJECTS));

    int defWidth = Math.max(120, canvasSize.getWidth() / 10);

    int minWidth = TimeBoardHelper.DEFAULT_MOVER_WIDTH + 1;
    int maxWidth = canvasSize.getWidth() / 3;

    setCompanyWidth(TimeBoardHelper.getPixels(getSettings(), COL_PIXELS_PER_COMPANY,
        defWidth, minWidth, maxWidth));

    if (separateObjects()) {
      setInfoWidth(TimeBoardHelper.getPixels(getSettings(), COL_PIXELS_PER_INFO,
          defWidth, minWidth, maxWidth));
    } else {
      setInfoWidth(0);
    }

    setChartLeft(getCompanyWidth() + getInfoWidth());
    setChartWidth(canvasSize.getWidth() - getChartLeft() - getChartRight());

    setDayColumnWidth(TimeBoardHelper.getPixels(getSettings(), COL_PIXELS_PER_DAY,
        20, 1, getChartWidth()));
  }

  @Override
  protected void refresh() {
    BeeKeeper.getRpc().makeRequest(ServiceKeeper.createArgs(SVC_GET_CALENDAR_DATA),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (setData(response, false)) {
              render(false);
            }
          }
        });
  }

  @Override
  protected void renderContent(ComplexPanel panel) {
    companyPanels.clear();
    infoPanels.clear();

    companyIndexesByRow.clear();
    objectsByRow.clear();

    List<TimeBoardRowLayout> boardLayout = doLayout();

    int rc = TimeBoardRowLayout.countRows(boardLayout, 1);
    initContent(panel, rc);

    if (boardLayout.isEmpty()) {
      return;
    }

    int calendarWidth = getCalendarWidth();

    Double opacity = TimeBoardHelper.getOpacity(getSettings(), COL_ITEM_OPACITY);

    Edges margins = new Edges();
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    Widget itemWidget;
    int rowIndex = 0;

    for (TimeBoardRowLayout layout : boardLayout) {
      int companyIndex = layout.getDataIndex();

      int size = layout.getSize(1);
      int lastRow = rowIndex + size - 1;

      int top = rowIndex * getRowHeight();

      if (rowIndex > 0) {
        TimeBoardHelper.addRowSeparator(panel, STYLE_COMPANY_ROW_SEPARATOR, top, 0,
            getChartLeft() + calendarWidth);
      }

      ServiceCompanyWrapper company = companies.get(companyIndex);
      Assert.notNull(company, "company not found");

      IdentifiableWidget companyWidget = createCompanyWidget(company);
      addCompanyWidget(panel, companyWidget, rowIndex, lastRow);

      if (separateObjects()) {
        for (TimeBoardRowLayout.GroupLayout group : layout.getGroups()) {
          ServiceObjectWrapper object = findObject(group.getGroupId());

          if (object != null) {
            IdentifiableWidget infoWidget = createInfoWidget(object);

            int first = rowIndex + group.getFirstRow();
            int last = rowIndex + group.getLastRow();
            addInfoWidget(panel, infoWidget, first, last);

            for (int r = first; r <= last; r++) {
              objectsByRow.put(r, object.getId());
            }
          }
        }
      }

      if (size > 1) {
        renderRowSeparators(panel, rowIndex, lastRow);
      }

      for (int i = 0; i < layout.getRows().size(); i++) {
        RowData rowData = layout.getRows().get(i);

        for (HasDateRange item : rowData.getRowItems()) {
          if (item instanceof TaskWrapper) {
            itemWidget = createTaskWidget((TaskWrapper) item);
          } else if (item instanceof RecurringTaskWrapper) {
            itemWidget = createRecurringTaskWidget((RecurringTaskWrapper) item);
          } else if (item instanceof ServiceDateWrapper) {
            itemWidget = createDateWidget((ServiceDateWrapper) item);
          } else {
            itemWidget = null;
          }

          if (itemWidget != null) {
            Rectangle rectangle = getRectangle(item.getRange(), rowIndex + i);
            TimeBoardHelper.apply(itemWidget, rectangle, margins);

            if (opacity != null) {
              StyleUtils.setOpacity(itemWidget, opacity);
            }

            if (touchesLeft(item.getRange().lowerEndpoint(), rowData)) {
              styleItemStart(itemWidget);
            }
            if (touchesRight(item.getRange().upperEndpoint(), rowData)) {
              styleItemEnd(itemWidget);
            }

            panel.add(itemWidget);
          }
        }
      }

      for (int i = 0; i < size; i++) {
        companyIndexesByRow.add(companyIndex);
      }

      rowIndex += size;
    }
  }

  @Override
  protected void renderMovers(ComplexPanel panel, int height) {
    Mover companyMover = TimeBoardHelper.createHorizontalMover();
    StyleUtils.setLeft(companyMover, getCompanyWidth() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(companyMover, height);

    companyMover.addMoveHandler(this::onCompanyResize);

    panel.add(companyMover);

    if (separateObjects()) {
      Mover infoMover = TimeBoardHelper.createHorizontalMover();
      StyleUtils.setLeft(infoMover, getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
      StyleUtils.setHeight(infoMover, height);

      infoMover.addMoveHandler(this::onInfoResize);

      panel.add(infoMover);
    }
  }

  @Override
  protected boolean setData(ResponseObject response, boolean init) {
    if (!Queries.checkResponse(getCaption(), VIEW_SERVICE_SETTINGS, response, BeeRowSet.class)) {
      return false;
    }

    BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());
    setSettings(rowSet);

    ServiceCompanyKind companyKind = getCompanyKind(rowSet);

    JustDate minDate = TimeBoardHelper.getDate(rowSet, COL_SERVICE_CALENDAR_MIN_DATE);
    JustDate maxDate = TimeBoardHelper.getDate(rowSet, COL_SERVICE_CALENDAR_MAX_DATE);

    if (minDate != null && maxDate != null && BeeUtils.isLess(maxDate, minDate)) {
      maxDate = JustDate.copyOf(minDate);
    }

    initData(companyKind, minDate, maxDate, rowSet.getTableProperties());

    updateMaxRange();
    if (minDate != null || maxDate != null) {
      clampMaxRange(minDate, maxDate);
    }

    return true;
  }

  @Override
  protected void setItemWidgetColor(HasDateRange item, Widget widget) {
    if (item instanceof TaskWrapper) {
      setTaskColor((TaskWrapper) item, widget);
    } else if (item instanceof RecurringTaskWrapper) {
      setRecurringTaskColor((RecurringTaskWrapper) item, widget);
    } else if (item instanceof ServiceDateWrapper) {
      setDateColor((ServiceDateWrapper) item, widget);
    }
  }

  private void addCompanyWidget(HasWidgets panel, IdentifiableWidget widget,
      int firstRow, int lastRow) {

    Rectangle rectangle = TimeBoardHelper.getRectangle(0, getCompanyWidth(), firstRow, lastRow,
        getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget.asWidget(), rectangle, margins);

    panel.add(widget.asWidget());

    companyPanels.add(widget.getId());
  }

  private boolean containsCompany(Long id) {
    if (id == null) {
      return false;
    }

    for (ServiceCompanyWrapper wrapper : companies) {
      if (id.equals(wrapper.getId())) {
        return true;
      }
    }
    return false;
  }

  private static IdentifiableWidget createCompanyWidget(ServiceCompanyWrapper company) {
    Flow panel = new Flow(STYLE_COMPANY_PANEL);

    CustomDiv label = new CustomDiv(STYLE_COMPANY_LABEL);

    if (DataUtils.isId(company.getId())) {
      label.setText(company.getName());
      bindOpener(label, ClassifierConstants.VIEW_COMPANIES, company.getId());
    }

    panel.add(label);
    return panel;
  }

  private Widget createDateWidget(ServiceDateWrapper date) {
    Flow panel = new Flow(STYLE_DATE_PANEL);
    setDateColor(date, panel);

    Long objectId = date.getObjectId();
    String title = date.getTitle();

    ServiceObjectWrapper object = findObject(objectId);

    if (object == null) {
      if (!BeeUtils.isEmpty(title)) {
        panel.setTitle(title);
      }

    } else {
      panel.setTitle(BeeUtils.buildLines(date.getTitle(), BeeConst.STRING_EMPTY,
          object.getTitle()));
      bindOpener(panel, VIEW_SERVICE_OBJECTS, objectId);
    }

    return panel;
  }

  private static IdentifiableWidget createInfoWidget(ServiceObjectWrapper object) {
    Flow panel = new Flow(STYLE_INFO_PANEL);

    CustomDiv label = new CustomDiv(STYLE_INFO_LABEL);
    label.setText(object.getAddress());

    label.setTitle(object.getTitle());

    bindOpener(label, VIEW_SERVICE_OBJECTS, object.getId());

    panel.add(label);
    return panel;
  }

  private Widget createRecurringTaskWidget(RecurringTaskWrapper recurringTask) {
    Flow panel = new Flow(STYLE_RT_PANEL);

    TaskPriority priority = recurringTask.getPriority();
    if (priority != null) {
      panel.addStyleName(STYLE_RT_PRIORITY + BeeUtils.toString(priority.ordinal()));
    }

    setRecurringTaskColor(recurringTask, panel);

    panel.setTitle(recurringTask.getTitle());
    bindOpener(panel, VIEW_RECURRING_TASKS, recurringTask.getId());

    return panel;
  }

  private Widget createTaskWidget(TaskWrapper task) {
    Flow panel = new Flow(STYLE_TASK_PANEL);

    TaskPriority priority = task.getPriority();
    if (priority != null) {
      panel.addStyleName(STYLE_TASK_PRIORITY + BeeUtils.toString(priority.ordinal()));
    }

    TaskStatus status = task.getStatus();
    if (status != null) {
      panel.addStyleName(STYLE_TASK_STATUS + BeeUtils.toString(status.ordinal()));
    }

    setTaskColor(task, panel);

    if (task.getStar() != null) {
      ImageResource imageResource = Stars.get(task.getStar());

      if (imageResource != null) {
        StyleUtils.setBackgroundImage(panel, imageResource.getSafeUri().asString());
        panel.addStyleName(STYLE_TASK_STARRED);
      }
    }

    panel.setTitle(task.getTitle());
    bindOpener(panel, VIEW_TASKS, task.getId());

    return panel;
  }

  private void doExport(String fileName, List<String> filterLabels) {
    List<TimeBoardRowLayout> boardLayout = doLayout();
    if (boardLayout.isEmpty()) {
      BeeKeeper.getScreen().notifyWarning(Localized.dictionary().noData());
      return;
    }

    ServiceCompanyKind companyKind = getCompanyKind(getSettings());

    Range<JustDate> range = getVisibleRange();
    int dayColumns = TimeBoardHelper.getSize(range);
    Assert.isPositive(dayColumns);

    int groupColumns = separateObjects() ? 2 : 1;
    int cc = groupColumns + dayColumns;

    XSheet sheet = new XSheet();
    int rowIndex = 1;

    Exporter.addCaption(sheet, getCaption(), TextAlign.CENTER, rowIndex++, cc);

    if (!BeeUtils.isEmpty(filterLabels)) {
      for (String label : filterLabels) {
        Exporter.addFilterLabel(sheet, label, rowIndex++, cc);
      }
    }

    rowIndex++;

    XStyle headerStyle = XStyle.center();
    headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
    headerStyle.setColor(Colors.LIGHTGRAY);
    headerStyle.setFontRef(sheet.registerFont(XFont.bold()));

    int headerStyleRef = sheet.registerStyle(headerStyle);

    int colIndex = 0;

    XRow row = Exporter.createHeaderRow(rowIndex++);
    if (groupColumns == 1) {
      row.add(new XCell(colIndex++, exportPeriod(range), headerStyleRef));
    } else {
      row.add(new XCell(colIndex++, Format.renderDate(range.lowerEndpoint()), headerStyleRef));
      row.add(new XCell(colIndex++, Format.renderDate(range.upperEndpoint()), headerStyleRef));
    }

    exportMonthLabels(range, row, colIndex, headerStyleRef);
    sheet.add(row);

    row = Exporter.createHeaderRow(rowIndex++);
    colIndex = 0;

    row.add(new XCell(colIndex++, companyKind.getCaption(), headerStyleRef));
    if (separateObjects()) {
      row.add(new XCell(colIndex++, Localized.dictionary().address(), headerStyleRef));
    }

    XStyle dayStyle = XStyle.center();
    dayStyle.setColor(Colors.LIGHTGRAY);

    if (dayColumns > 100) {
      double factor = 1 / BeeUtils.rescale(dayColumns, 100, 400, 1, 1.25);
      XFont dayFont = new XFont();
      dayFont.setFactor(factor);
      dayStyle.setFontRef(sheet.registerFont(dayFont));
    }

    int dayStyleRef = sheet.registerStyle(dayStyle);

    for (int i = 0; i < dayColumns; i++) {
      int dom = TimeUtils.nextDay(range.lowerEndpoint(), i).getDom();
      row.add(new XCell(colIndex++, BeeUtils.toString(dom), dayStyleRef));
    }

    sheet.add(row);

    exportContent(boardLayout, range, sheet, rowIndex);

    for (int i = 0; i < groupColumns; i++) {
      sheet.autoSizeColumn(i);
    }

    if (dayColumns > 15) {
      double factor = 1 / BeeUtils.rescale(dayColumns, 15, 200, 1, 4);
      for (int i = groupColumns; i < cc; i++) {
        sheet.setColumnWidthFactor(i, factor);
      }
    }

    Exporter.export(sheet, fileName);
  }

  private List<TimeBoardRowLayout> doLayout() {
    List<TimeBoardRowLayout> result = new ArrayList<>();
    Range<JustDate> range = getVisibleRange();

    List<HasDateRange> items;

    for (int companyIndex = 0; companyIndex < companies.size(); companyIndex++) {
      ServiceCompanyWrapper company = companies.get(companyIndex);
      long companyId = company.getId();

      TimeBoardRowLayout layout = new TimeBoardRowLayout(companyIndex);

      List<ServiceObjectWrapper> companyObjects = getObjectsForLayout(companyId);

      for (ServiceObjectWrapper object : companyObjects) {
        Long objectId = object.getId();
        Long groupId = separateObjects() ? objectId : companyId;

        if (tasks.containsKey(objectId)) {
          items = TimeBoardHelper.getActiveItems(tasks.get(objectId), range);
          layout.addItems(groupId, items, range);
        }
        if (recurringTasks.containsKey(objectId)) {
          items = TimeBoardHelper.getActiveItems(recurringTasks.get(objectId), range);
          layout.addItems(groupId, items, range);
        }

        if (dates.containsKey(objectId)) {
          items = TimeBoardHelper.getActiveItems(dates.get(objectId), range);
          layout.addItems(groupId, items, range);
        }
      }

      if (!layout.isEmpty()) {
        result.add(layout);
      }
    }

    return result;
  }

  private void export() {
    if (!hasContent()) {
      BeeKeeper.getScreen().notifyWarning(Localized.dictionary().noData());
      return;
    }

    Exporter.confirm(getCaption(), new Exporter.FileNameCallback() {
      @Override
      public void onSuccess(final String value) {
        getFilterLabels(input -> doExport(value, input));
      }
    });
  }

  private void exportContent(List<TimeBoardRowLayout> boardLayout, Range<JustDate> range,
      XSheet sheet, int sheetRowIndex) {

    int rowIndex = sheetRowIndex;
    int chartStartCol = separateObjects() ? 2 : 1;

    for (TimeBoardRowLayout layout : boardLayout) {
      XRow row = new XRow(rowIndex);

      int companyIndex = layout.getDataIndex();

      int size = layout.getSize(1);

      ServiceCompanyWrapper company = companies.get(companyIndex);

      XCell cell = new XCell(0, company.getName());
      if (size > 1) {
        cell.setStyleRef(sheet.registerStyle(XStyle.middle()));
        cell.setRowSpan(size);
      }
      row.add(cell);

      sheet.add(row);

      if (separateObjects()) {
        for (TimeBoardRowLayout.GroupLayout group : layout.getGroups()) {
          ServiceObjectWrapper object = findObject(group.getGroupId());

          if (object != null) {
            cell = new XCell(1, object.getAddress());
            if (group.getSize() > 1) {
              cell.setStyleRef(sheet.registerStyle(XStyle.middle()));
              cell.setRowSpan(group.getSize());
            }

            sheet.ensureRow(rowIndex + group.getFirstRow()).add(cell);
          }
        }
      }

      for (int i = 0; i < layout.getRows().size(); i++) {
        RowData rowData = layout.getRows().get(i);

        for (HasDateRange item : rowData.getRowItems()) {
          String color;
          Integer pictureRef = null;

          if (item instanceof TaskWrapper) {
            TaskWrapper task = (TaskWrapper) item;
            color = getTaskColor(task);

            if (task.getStar() != null) {
              pictureRef = Stars.export(task.getStar(), sheet);
            }

          } else if (item instanceof RecurringTaskWrapper) {
            color = getRecurringTaskColor((RecurringTaskWrapper) item);

          } else if (item instanceof ServiceDateWrapper) {
            color = getDateColor((ServiceDateWrapper) item);

          } else {
            color = null;
          }

          if (!BeeUtils.isEmpty(color) || pictureRef != null) {
            JustDate startDate = TimeUtils.clamp(item.getRange().lowerEndpoint(),
                range.lowerEndpoint(), range.upperEndpoint());
            JustDate endDate = TimeUtils.clamp(item.getRange().upperEndpoint(),
                range.lowerEndpoint(), range.upperEndpoint());

            int colIndex = chartStartCol + TimeUtils.dayDiff(range.lowerEndpoint(), startDate);
            int colSpan = TimeUtils.dayDiff(startDate, endDate) + 1;

            cell = new XCell(colIndex);
            if (colSpan > 1) {
              cell.setColSpan(colSpan);
            }

            if (!BeeUtils.isEmpty(color)) {
              cell.setStyleRef(sheet.registerStyle(XStyle.background(color)));
            }
            if (pictureRef != null) {
              cell.setPictureRef(pictureRef);
              if (colSpan > 1) {
                cell.setPictureLayout(XPicture.Layout.REPAEAT);
              }
            }

            sheet.ensureRow(rowIndex + i).add(cell);
          }
        }
      }

      rowIndex += size;
    }
  }

  private ServiceObjectWrapper findObject(Long id) {
    if (DataUtils.isId(id)) {
      for (ServiceObjectWrapper object : objects.values()) {
        if (id.equals(object.getId())) {
          return object;
        }
      }
    }
    return null;
  }

  private int getCompanyWidth() {
    return companyWidth;
  }

  private void getFilterLabels(final Consumer<List<String>> consumer) {
    String tts = TimeBoardHelper.getString(getSettings(), COL_SERVICE_CALENDAR_TASK_TYPES);
    final List<Long> taskTypes = DataUtils.parseIdList(tts);

    if (taskTypes.isEmpty()) {
      consumer.accept(BeeConst.EMPTY_IMMUTABLE_STRING_LIST);
      return;
    }

    Queries.getRowSet(VIEW_TASK_TYPES, Lists.newArrayList(COL_TASK_TYPE_NAME),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            List<String> typeLabels = new ArrayList<>();
            int index = result.getColumnIndex(COL_TASK_TYPE_NAME);

            for (Long tt : taskTypes) {
              BeeRow row = result.getRowById(tt);

              if (row != null) {
                String label = row.getString(index);
                if (!BeeUtils.isEmpty(label) && !typeLabels.contains(label)) {
                  typeLabels.add(label);
                }
              }
            }

            List<String> labels = new ArrayList<>();
            if (!typeLabels.isEmpty()) {
              labels.add(BeeUtils.joinItems(typeLabels));
            }

            consumer.accept(labels);
          }
        });
  }

  private int getInfoWidth() {
    return infoWidth;
  }

  private List<ServiceObjectWrapper> getObjectsForLayout(Long companyId) {
    List<ServiceObjectWrapper> result = new ArrayList<>();

    if (objects.containsKey(companyId)) {
      result.addAll(objects.get(companyId));
    }
    return result;
  }

  private String getRecurringTaskColor(RecurringTaskWrapper recurringTask) {
    return BeeUtils.notEmpty(recurringTask.getTypeColor(),
        TimeBoardHelper.getString(getSettings(), COL_RT_COLOR), DEFAULT_RT_COLOR);
  }

  private String getTaskColor(TaskWrapper task) {
    String typeColor = task.getTypeColor();

    String statusColor;
    if (task.getStatus() != null) {
      String colName = COL_TASK_COLOR + BeeUtils.toString(task.getStatus().ordinal());
      statusColor = TimeBoardHelper.getString(getSettings(), colName);
    } else {
      statusColor = null;
    }

    String color;

    if (BeeUtils.allEmpty(typeColor, statusColor)) {
      color = TimeBoardHelper.getString(getSettings(), COL_TASK_COLOR);
      if (BeeUtils.isEmpty(color)) {
        color = DEFAULT_TASK_COLOR;
      }

    } else if (BeeUtils.isEmpty(typeColor)) {
      color = statusColor;
    } else if (BeeUtils.isEmpty(statusColor)) {
      color = typeColor;

    } else {
      color = Color.blend(typeColor, statusColor);
    }

    return color;
  }

  private void initData(ServiceCompanyKind companyKind, JustDate minDate, JustDate maxDate,
      Map<String, String> properties) {

    companies.clear();
    objects.clear();

    tasks.clear();
    recurringTasks.clear();

    dates.clear();

    if (BeeUtils.isEmpty(properties)) {
      return;
    }

    SimpleRowSet objectData = SimpleRowSet.getIfPresent(properties, TBL_SERVICE_OBJECTS);
    if (DataUtils.isEmpty(objectData)) {
      return;
    }

    SimpleRowSet objectDatesData = SimpleRowSet.getIfPresent(properties, TBL_SERVICE_DATES);

    SimpleRowSet taskData = SimpleRowSet.getIfPresent(properties, TBL_TASKS);
    SimpleRowSet rtData = SimpleRowSet.getIfPresent(properties, TBL_RECURRING_TASKS);

    String cId = (companyKind == ServiceCompanyKind.CONTRACTOR)
        ? COL_SERVICE_CONTRACTOR : COL_SERVICE_CUSTOMER;
    String cName = (companyKind == ServiceCompanyKind.CONTRACTOR)
        ? ALS_SERVICE_CONTRACTOR_NAME : ALS_SERVICE_CUSTOMER_NAME;

    for (SimpleRow row : objectData) {
      long companyId = BeeUtils.unbox(row.getLong(cId));
      if (!containsCompany(companyId)) {
        companies.add(new ServiceCompanyWrapper(companyId, row.getValue(cName)));
      }

      objects.put(companyId, new ServiceObjectWrapper(row));
    }

    if (companies.size() > 1) {
      Collections.sort(companies);
    }

    if (!DataUtils.isEmpty(objectDatesData)) {
      for (SimpleRow row : objectDatesData) {
        Long objId = row.getLong(COL_SERVICE_OBJECT);

        if (DataUtils.isId(objId)) {
          ServiceDateWrapper wrapper = new ServiceDateWrapper(row);
          dates.put(objId, wrapper);
        }
      }
    }

    if (!DataUtils.isEmpty(taskData)) {
      for (SimpleRow taskRow : taskData) {
        TaskWrapper wrapper = new TaskWrapper(taskRow);

        if (!BeeUtils.isEmpty(taskRow.getValue(AdministrationConstants.COL_RELATION))) {
          for (Long objId : DataUtils.parseIdList(taskRow
              .getValue(AdministrationConstants.COL_RELATION))) {
            if (DataUtils.isId(objId)) {
              tasks.put(objId, wrapper);
            }
          }
        }
      }
    }

    if (!DataUtils.isEmpty(rtData)) {
      long startMillis = System.currentTimeMillis();

      BeeRowSet rtDates = BeeRowSet.getIfPresent(properties, VIEW_RT_DATES);
      Multimap<Long, ScheduleDateRange> sdRanges = TaskUtils.getScheduleDateRangesByTask(rtDates);

      for (SimpleRow rtRow : rtData) {
        Long rtId = rtRow.getLong(RecurringTaskWrapper.ID_COLUMN);

        if (DataUtils.isId(rtId)) {
          Collection<ScheduleDateRange> sdrs =
              sdRanges.containsKey(rtId) ? sdRanges.get(rtId) : null;

          List<RecurringTaskWrapper> rts = RecurringTaskWrapper.spawn(rtRow, sdrs,
              minDate, maxDate);

          if (!rts.isEmpty()
              && !BeeUtils.isEmpty(rtRow.getValue(AdministrationConstants.COL_RELATION))) {
            for (Long objId : DataUtils.parseIdList(rtRow
                .getValue(AdministrationConstants.COL_RELATION))) {

              if (DataUtils.isId(objId)) {
                recurringTasks.putAll(objId, rts);
              }
            }
          }
        }
      }

      if (!recurringTasks.isEmpty()) {
        logger.debug("spawned", recurringTasks.size(), "recurring tasks in",
            System.currentTimeMillis() - startMillis);
      }
    }
  }

  private void onCompanyResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft;
    if (separateObjects()) {
      maxLeft = getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH * 2 - 1;
    } else {
      maxLeft = getLastResizableColumnMaxLeft(0);
    }

    int newLeft = BeeUtils.clamp(oldLeft + delta, 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int companyPx = newLeft + TimeBoardHelper.DEFAULT_MOVER_WIDTH;
      int infoPx = separateObjects() ? getChartLeft() - companyPx : BeeConst.UNDEF;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : companyPanels) {
          StyleUtils.setWidth(DomUtils.getElement(id),
              companyPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
        }

        if (separateObjects()) {
          for (String id : infoPanels) {
            Element element = Document.get().getElementById(id);
            if (element != null) {
              StyleUtils.setLeft(element, companyPx);
              StyleUtils.setWidth(element, infoPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
            }
          }
        }
      }

      if (event.isFinished()) {
        if (separateObjects()) {
          if (updateSettings(COL_PIXELS_PER_COMPANY, companyPx, COL_PIXELS_PER_INFO, infoPx)) {
            setCompanyWidth(companyPx);
            setInfoWidth(infoPx);
          }

        } else if (updateSetting(COL_PIXELS_PER_COMPANY, companyPx)) {
          setCompanyWidth(companyPx);
          render(false);
        }
      }
    }
  }

  private void onInfoResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft = getLastResizableColumnMaxLeft(getCompanyWidth());
    int newLeft = BeeUtils.clamp(oldLeft + delta, getCompanyWidth() + 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int infoPx = newLeft - getCompanyWidth() + TimeBoardHelper.DEFAULT_MOVER_WIDTH;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : infoPanels) {
          StyleUtils.setWidth(DomUtils.getElement(id),
              infoPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
        }
      }

      if (event.isFinished() && updateSetting(COL_PIXELS_PER_INFO, infoPx)) {
        setInfoWidth(infoPx);
        render(false);
      }
    }
  }

  private void renderRowSeparators(ComplexPanel panel, int firstRow, int lastRow) {
    Long lastObject = objectsByRow.get(firstRow);

    for (int rowIndex = firstRow + 1; rowIndex <= lastRow; rowIndex++) {
      int top = rowIndex * getRowHeight();
      Long currentObject = objectsByRow.get(rowIndex);

      if (Objects.equals(lastObject, currentObject)) {
        TimeBoardHelper.addRowSeparator(panel, top, getChartLeft(), getCalendarWidth());
      } else {
        TimeBoardHelper.addRowSeparator(panel, STYLE_INFO_ROW_SEPARATOR, top,
            getCompanyWidth(), getInfoWidth() + getCalendarWidth());
        lastObject = currentObject;
      }
    }
  }

  private boolean requiresRefresh(BeeRow oldSettings, BeeRow newSettings) {
    Set<String> colNames = Sets.newHashSet(COL_SERVICE_CALENDAR_TASK_TYPES, COL_COMPANY_KIND,
        COL_SERVICE_CALENDAR_MIN_DATE, COL_SERVICE_CALENDAR_MAX_DATE);

    for (String colName : colNames) {
      int index = getSettings().getColumnIndex(colName);
      if (!Objects.equals(oldSettings.getString(index), newSettings.getString(index))) {
        return true;
      }
    }
    return false;
  }

  private boolean separateObjects() {
    return separateObjects;
  }

  private void setCompanyWidth(int companyWidth) {
    this.companyWidth = companyWidth;
  }

  private void setInfoWidth(int infoWidth) {
    this.infoWidth = infoWidth;
  }

  private void setRecurringTaskColor(RecurringTaskWrapper recurringTask, Widget widget) {
    String color = getRecurringTaskColor(recurringTask);
    if (!BeeUtils.isEmpty(color)) {
      StyleUtils.setBackgroundColor(widget, color);
    }
  }

  private void setSeparateObjects(boolean separateObjects) {
    this.separateObjects = separateObjects;
  }

  private void setTaskColor(TaskWrapper task, Widget widget) {
    String color = getTaskColor(task);
    if (!BeeUtils.isEmpty(color)) {
      StyleUtils.setBackgroundColor(widget, color);
    }
  }

  private boolean touchesLeft(JustDate date, RowData rowData) {
    if (date != null && getVisibleRange().contains(date)) {
      if (date.equals(getVisibleRange().lowerEndpoint())) {
        return true;
      } else {
        return rowData.contains(TimeUtils.previousDay(date));
      }

    } else {
      return false;
    }
  }

  private boolean touchesRight(JustDate date, RowData rowData) {
    if (date != null && getVisibleRange().contains(date)) {
      if (date.equals(getVisibleRange().upperEndpoint())) {
        return true;
      } else {
        return rowData.contains(TimeUtils.nextDay(date));
      }

    } else {
      return false;
    }
  }
}
