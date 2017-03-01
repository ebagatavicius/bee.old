package com.butent.bee.client.timeboard;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class TimeBoard extends Flow implements Presenter, View, Printable,
    HandlesAllDataEvents, VisibilityChangeEvent.Handler, HasWidgetSupplier, HasVisibleRange,
    MoveEvent.Handler {

  private enum RangeMover {
    START_SLIDER, END_SLIDER, MOVER
  }

  static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "timeboard-";

  private static final BeeLogger logger = LogUtils.getLogger(TimeBoard.class);

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_CANVAS = STYLE_PREFIX + "canvas";

  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_HEADER_SPLITTER = STYLE_HEADER + "-splitter";

  private static final String STYLE_PROGRESS_CONTAINER = STYLE_PREFIX + "progress-container";
  private static final String STYLE_PROGRESS_BAR = STYLE_PREFIX + "progress-bar";

  private static final String STYLE_SCROLL_AREA = STYLE_PREFIX + "scroll-area";
  private static final String STYLE_CONTENT = STYLE_PREFIX + "content";
  private static final String STYLE_ROW_RESIZER = STYLE_CONTENT + "-row-resizer";

  private static final String STYLE_FOOTER = STYLE_PREFIX + "footer";
  private static final String STYLE_FOOTER_SPLITTER = STYLE_FOOTER + "-splitter";

  private static final String STYLE_SELECTOR_PREFIX = STYLE_PREFIX + "selector-";
  private static final String STYLE_SELECTOR_PANEL = STYLE_SELECTOR_PREFIX + "panel";
  private static final String STYLE_SELECTOR_STRIP = STYLE_SELECTOR_PREFIX + "strip";
  private static final String STYLE_SELECTOR_START_SLIDER = STYLE_SELECTOR_PREFIX + "start-slider";
  private static final String STYLE_SELECTOR_END_SLIDER = STYLE_SELECTOR_PREFIX + "end-slider";
  private static final String STYLE_SELECTOR_MOVER = STYLE_SELECTOR_PREFIX + "mover";

  private static final String STYLE_START_SLIDER_LABEL = STYLE_SELECTOR_START_SLIDER + "-label";
  private static final String STYLE_END_SLIDER_LABEL = STYLE_SELECTOR_END_SLIDER + "-label";

  private static final String STYLE_FILTER_LABEL = STYLE_PREFIX + "filter-label";
  private static final String STYLE_ACTION_REMOVE_FILTER = STYLE_PREFIX + "action-remove-filter";

  private static final String STYLE_ITEM_PREFIX = STYLE_PREFIX + "item-";
  private static final String STYLE_ITEM_START = STYLE_ITEM_PREFIX + "start";
  private static final String STYLE_ITEM_END = STYLE_ITEM_PREFIX + "end";
  private static final String STYLE_ITEM_HAS_HANDLING = STYLE_ITEM_PREFIX + "has-handling";

  private static final String STYLE_SHEET_NAME = "timeboard";
  private static final JustDate STYLE_SHEET_VERSION = new JustDate(2017, 2, 7);

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.VIEW);

  private static boolean styleSheetInjected;

  public static void ensureStyleSheet() {
    if (!styleSheetInjected) {
      styleSheetInjected = true;
      DomUtils.injectStyleSheet(Paths.getStyleSheetUrl(STYLE_SHEET_NAME, STYLE_SHEET_VERSION));
    }
  }

  private static List<List<HasDateRange>> doStripLayout(
      Collection<? extends HasDateRange> chartItems, int maxRows) {

    List<List<HasDateRange>> rows = new ArrayList<>();

    for (HasDateRange item : chartItems) {
      int row = BeeConst.UNDEF;
      for (int i = 0; i < rows.size(); i++) {
        if (!BeeUtils.intersects(rows.get(i), item.getRange())) {
          row = i;
          break;
        }
      }

      if (BeeConst.isUndef(row)) {
        if (rows.size() < maxRows) {
          row = rows.size();
          rows.add(new ArrayList<>());

        } else {
          row = BeeUtils.randomInt(0, rows.size());
        }
      }

      rows.get(row).add(item);
    }

    return rows;
  }

  private final HeaderView headerView;
  private final Flow canvas;
  private final Flow progress;

  private final List<HandlerRegistration> registry = new ArrayList<>();

  private boolean enabled = true;

  private final List<Color> colors = new ArrayList<>();

  private BeeRowSet settings;

  private Range<JustDate> maxRange;
  private Range<JustDate> visibleRange;

  private boolean renderPending;

  private int headerHeight;
  private int footerHeight;
  private int chartLeft;
  private int chartRight;

  private int chartWidth = BeeConst.UNDEF;
  private int dayColumnWidth = BeeConst.UNDEF;

  private int rowHeight = BeeConst.UNDEF;

  private int sliderWidth;
  private Widget startSliderLabel;
  private Widget endSliderLabel;

  private final EnumMap<RangeMover, Element> rangeMovers = new EnumMap<>(RangeMover.class);

  private String scrollAreaId;
  private String contentId;

  private int rowCount;

  private final CustomDiv filterLabel;
  private final CustomDiv removeFilter;

  private boolean filtered;

  protected TimeBoard() {
    super();

    ensureStyleSheet();

    addStyleName(STYLE_CONTAINER);
    addStyleName(UiOption.getStyleName(uiOptions));

    Set<Action> enabledActions = getEnabledActions();
    Set<Action> hiddenActions = getHiddenActions();

    this.headerView = new HeaderImpl();
    headerView.create(getCaption(), false, true, null, uiOptions,
        enabledActions, Action.NO_ACTIONS, hiddenActions);

    if (BeeUtils.contains(enabledActions, Action.FILTER)) {
      this.filterLabel = new CustomDiv(STYLE_FILTER_LABEL);
      headerView.addCommandItem(filterLabel);

      this.removeFilter = new CustomDiv(STYLE_ACTION_REMOVE_FILTER);
      removeFilter.setText(String.valueOf(BeeConst.CHAR_TIMES));
      removeFilter.setTitle(Action.REMOVE_FILTER.getCaption());

      removeFilter.addClickHandler(event -> handleAction(Action.REMOVE_FILTER));
      removeFilter.setVisible(false);

      headerView.addCommandItem(removeFilter);

    } else {
      this.filterLabel = null;
      this.removeFilter = null;
    }

    headerView.setViewPresenter(this);
    add(headerView);

    this.canvas = new Flow(STYLE_CANVAS);
    StyleUtils.setTop(canvas, headerView.getHeight());
    add(canvas);

    this.progress = new Flow(STYLE_PROGRESS_CONTAINER);
    progress.add(new CustomDiv(STYLE_PROGRESS_BAR));
    add(progress);
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {

    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public Range<JustDate> getMaxRange() {
    return maxRange;
  }

  @Override
  public int getMaxSize() {
    return getChartWidth();
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public String getViewKey() {
    return getSupplierKey();
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public Range<JustDate> getVisibleRange() {
    return visibleRange;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case REFRESH:
        refresh();
        break;

      case CONFIGURE:
        editSettings();
        break;

      case CANCEL:
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      case PRINT:
        Printer.print(this);
        break;

      default:
        logger.warning(getCaption(), action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isDataEventRelevant(event)) {
      onRelevantDataEvent(event);
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (isDataEventRelevant(event)) {
      onRelevantDataEvent(event);
    }
  }

  @Override
  public void onMove(MoveEvent event) {
    if (!(event.getSource() instanceof Mover)) {
      return;
    }

    int delta = event.getDeltaX();
    Element source = ((Mover) event.getSource()).getElement();

    RangeMover sourceType = null;
    for (Map.Entry<RangeMover, Element> entry : rangeMovers.entrySet()) {
      if (entry.getValue().getId().equals(source.getId())) {
        sourceType = entry.getKey();
        break;
      }
    }
    if (sourceType == null) {
      logger.warning("range mover not found:", source);
      return;
    }

    int itemAreaWidth = getChartWidth();

    double dayWidth = (double) itemAreaWidth / TimeBoardHelper.getSize(getMaxRange());
    int maxWidth = BeeUtils.round(dayWidth * getMaxSize());

    int startPos = StyleUtils.getLeft(rangeMovers.get(RangeMover.START_SLIDER));
    int endPos = StyleUtils.getLeft(rangeMovers.get(RangeMover.END_SLIDER)) - getSliderWidth();

    boolean hasMover = rangeMovers.containsKey(RangeMover.MOVER);

    boolean changed = false;

    if (delta != 0) {
      switch (sourceType) {
        case START_SLIDER:
          int newStart = BeeUtils.clamp(startPos + delta, 0, endPos - 1);
          if (maxWidth > 0 && endPos - newStart > maxWidth) {
            newStart = endPos - maxWidth;
          }

          if (newStart != startPos) {
            startPos = newStart;

            StyleUtils.setLeft(source, startPos);
            if (hasMover) {
              StyleUtils.setLeft(rangeMovers.get(RangeMover.MOVER), startPos + getSliderWidth());
              StyleUtils.setWidth(rangeMovers.get(RangeMover.MOVER), endPos - startPos);
            }

            changed = true;
          }
          break;

        case END_SLIDER:
          int newEnd = BeeUtils.clamp(endPos + delta, startPos + 1, itemAreaWidth);
          if (maxWidth > 0 && newEnd - startPos > maxWidth) {
            newEnd = startPos + maxWidth;
          }

          if (newEnd != endPos) {
            endPos = newEnd;

            StyleUtils.setLeft(source, endPos + getSliderWidth());
            if (hasMover) {
              StyleUtils.setWidth(rangeMovers.get(RangeMover.MOVER), endPos - startPos);
            }

            changed = true;
          }
          break;

        case MOVER:
          int dx = BeeUtils.clamp(delta, -startPos, itemAreaWidth - endPos);
          if (dx != 0) {
            startPos += dx;
            endPos += dx;

            StyleUtils.setLeft(rangeMovers.get(RangeMover.START_SLIDER), startPos);
            StyleUtils.setLeft(rangeMovers.get(RangeMover.END_SLIDER), endPos + getSliderWidth());
            StyleUtils.setLeft(source, startPos + getSliderWidth());

            changed = true;
          }
          break;
      }
    }

    if (changed || event.isFinished()) {

      if (event.isFinished()) {
        getStartSliderLabel().setVisible(false);
        getEndSliderLabel().setVisible(false);

        JustDate min = getMaxRange().lowerEndpoint();
        JustDate max = getMaxRange().upperEndpoint();

        JustDate start = TimeUtils.clamp(TimeBoardHelper.getDate(min, startPos, dayWidth),
            min, max);
        JustDate end = TimeUtils.clamp(TimeUtils.previousDay(TimeBoardHelper.getDate(min, endPos,
            dayWidth)), start, max);

        if (!setVisibleRange(start, end)) {
          JustDate firstVisible = getVisibleRange().lowerEndpoint();
          JustDate lastVisible = getVisibleRange().upperEndpoint();

          int p1 = TimeBoardHelper.getPosition(min, firstVisible, dayWidth);
          p1 = BeeUtils.clamp(p1, 0, itemAreaWidth - getSliderWidth());

          int p2 = TimeBoardHelper.getPosition(min, TimeUtils.nextDay(lastVisible), dayWidth);
          p2 = BeeUtils.clamp(p2, startPos + getSliderWidth(), itemAreaWidth);

          if (p1 != startPos || p2 != endPos) {
            StyleUtils.setLeft(rangeMovers.get(RangeMover.START_SLIDER), p1);
            StyleUtils.setLeft(rangeMovers.get(RangeMover.END_SLIDER), p2 + getSliderWidth());

            if (hasMover) {
              StyleUtils.setLeft(rangeMovers.get(RangeMover.MOVER), p1 + getSliderWidth());
              StyleUtils.setWidth(rangeMovers.get(RangeMover.MOVER), p2 - p1);
            }
          }
        }

      } else {
        JustDate min = getMaxRange().lowerEndpoint();
        JustDate max = TimeUtils.nextDay(getMaxRange().upperEndpoint());

        JustDate start = TimeUtils.clamp(TimeBoardHelper.getDate(min, startPos, dayWidth),
            min, max);
        JustDate end = TimeUtils.clamp(TimeBoardHelper.getDate(min, endPos, dayWidth),
            start, max);

        String startLabel = Format.renderDate(start);
        String endLabel = Format.renderDate(end);

        getStartSliderLabel().setVisible(true);
        getEndSliderLabel().setVisible(true);

        getStartSliderLabel().getElement().setInnerText(startLabel);
        getEndSliderLabel().getElement().setInnerText(endLabel);

        int startWidth = getStartSliderLabel().getOffsetWidth();
        int endWidth = getStartSliderLabel().getOffsetWidth();

        int canvasWidth = getCanvasWidth();
        int panelLeft = getChartLeft() - getSliderWidth();

        int startLeft = startPos - (startWidth - getSliderWidth()) / 2;
        startLeft = BeeUtils.clamp(startLeft, -panelLeft,
            canvasWidth - panelLeft - startWidth - endWidth);
        StyleUtils.setLeft(getStartSliderLabel(), panelLeft + startLeft);

        int endLeft = endPos - (endWidth - getSliderWidth()) / 2;
        endLeft = BeeUtils.clamp(endLeft, startLeft + startWidth,
            canvasWidth - panelLeft - endWidth);
        StyleUtils.setLeft(getEndSliderLabel(), panelLeft + endLeft);
      }
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (isDataEventRelevant(event)) {
      onRelevantDataEvent(event);
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;
    String id = source.getId();

    if (getId().equals(id)) {
      int adjustment = getPrintHeightAdjustment();
      if (adjustment != 0) {
        StyleUtils.setHeight(target, source.getClientHeight() + adjustment);
      }
      ok = true;

    } else if (headerView.asWidget().getElement().isOrHasChild(source)) {
      if (StyleUtils.hasClassName(source, STYLE_ACTION_REMOVE_FILTER)) {
        ok = false;
      } else {
        ok = headerView.onPrint(source, target);
      }

    } else {
      ok = true;
    }

    return ok;
  }

  @Override
  public void onResize() {
    render(false);
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isDataEventRelevant(event)) {
      onRelevantDataEvent(event);
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isDataEventRelevant(event)) {
      onRelevantDataEvent(event);
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isDataEventRelevant(event)) {
      onRelevantDataEvent(event);
    }
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      if (isRenderPending()) {
        render(false);
      }
    }
  }

  public static void openDataRow(HasNativeEvent event, String viewName, Long rowId) {
    Opener opener = EventUtils.hasModifierKey(event.getNativeEvent())
        ? Opener.NEW_TAB : Opener.MODAL;
    RowEditor.open(viewName, rowId, opener);
  }

  @Override
  public boolean reactsTo(Action action) {
    if (BeeUtils.contains(getEnabledActions(), action)) {
      return true;
    } else if (EnumUtils.in(action, Action.CANCEL, Action.CLOSE)) {
      return true;
    } else if (action == Action.REMOVE_FILTER) {
      return BeeUtils.contains(getEnabledActions(), Action.FILTER);
    } else {
      return false;
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  public boolean setVisibleRange(JustDate start, JustDate end) {
    if (start != null && end != null && TimeUtils.isLeq(start, end)) {

      Range<JustDate> range = Range.closed(start, end);
      if (getMaxSize() > 0 && TimeBoardHelper.getSize(range) > getMaxSize()) {
        range = Range.closed(start, TimeUtils.nextDay(start, getMaxSize() - 1));
      }

      if (!range.equals(getVisibleRange())) {
        setVisibleRange(range);
        render(false);
        return true;
      }
    }
    return false;
  }

  protected void addContentHandlers(Widget content) {
    Binder.addDoubleClickHandler(content, event -> {
      int x = event.getX();
      int y = event.getY();

      if (x >= getChartLeft() && getVisibleRange() != null && getDayColumnWidth() > 0) {
        JustDate date = TimeUtils.nextDay(getVisibleRange().lowerEndpoint(),
            (x - getChartLeft()) / getDayColumnWidth());

        if (getVisibleRange().contains(date)) {
          onDoubleClickChart(y / getRowHeight(), date);
        }
      }
    });
  }

  protected void adjustWidths() {
    int chartColumnCount = TimeBoardHelper.getSize(getVisibleRange());
    if (chartColumnCount > 0) {
      setDayColumnWidth(Math.max(getChartWidth() / chartColumnCount, 1));
    }
  }

  protected static void bindOpener(HasClickHandlers widget, final String viewName, final Long id) {
    if (widget != null && id != null) {
      widget.addClickHandler(event -> openDataRow(event, viewName, id));
    }
  }

  protected static int clampFooterHeight(int height, int canvasHeight) {
    return BeeUtils.clamp(height, 3, BeeUtils.clamp(canvasHeight / 2, 3, 1000));
  }

  protected static int clampHeaderHeight(int height, int canvasHeight) {
    return BeeUtils.clamp(height, 1, BeeUtils.clamp(canvasHeight / 5, 1, 100));
  }

  protected void clampMaxRange(JustDate min, JustDate max) {
    if (min != null || max != null) {
      JustDate lower;
      JustDate upper;

      if (getMaxRange() == null) {
        lower = BeeUtils.min(min, max);
        upper = BeeUtils.max(min, max);

      } else {
        lower = BeeUtils.nvl(min, getMaxRange().lowerEndpoint());
        upper = BeeUtils.nvl(max, getMaxRange().upperEndpoint());

        if (BeeUtils.isLess(upper, lower)) {
          if (min == null) {
            lower = JustDate.copyOf(upper);
          } else {
            upper = JustDate.copyOf(lower);
          }
        }
      }

      setMaxRange(Range.closed(lower, upper));
    }
  }

  protected int clampRowHeight(int height, int canvasHeight) {
    int defMax = (getScrollAreaHeight(canvasHeight) - TimeBoardHelper.DEFAULT_MOVER_HEIGHT) / 2;
    return BeeUtils.clamp(height, 2, BeeUtils.clamp(defMax, 2, 1000));
  }

  protected abstract void editSettings();

  protected void extendMaxRange(JustDate start, JustDate end) {
    if (start == null && end == null) {
      return;
    }

    JustDate lower;
    JustDate upper;

    if (getMaxRange() == null) {
      lower = BeeUtils.min(start, end);
      upper = BeeUtils.max(start, end);
    } else {
      lower = BeeUtils.min(getMaxRange().lowerEndpoint(), start);
      upper = BeeUtils.max(getMaxRange().upperEndpoint(), end);
    }

    setMaxRange(Range.closed(lower, upper));
  }

  protected void finalizeContent(ComplexPanel panel) {
    renderMovers(panel, getContentHeight());
    renderRowResizer(panel);
  }

  protected Color findColor(Long id) {
    if (id == null) {
      return null;
    }

    for (Color color : colors) {
      if (Objects.equals(color.getId(), id)) {
        return color;
      }
    }
    return null;
  }

  protected int getCalendarWidth() {
    return TimeBoardHelper.getSize(getVisibleRange()) * getDayColumnWidth();
  }

  protected Flow getCanvas() {
    return canvas;
  }

  protected int getCanvasHeight() {
    return canvas.getElement().getClientHeight();
  }

  protected int getCanvasWidth() {
    return canvas.getElement().getClientWidth();
  }

  protected abstract Collection<? extends HasDateRange> getChartItems();

  protected int getChartLeft() {
    return chartLeft;
  }

  protected int getChartRight() {
    return chartRight;
  }

  protected int getChartWidth() {
    return chartWidth;
  }

  protected Color getColor(Long colorSource) {
    if (colorSource == null) {
      return null;
    }

    int index = TimeBoardHelper.getColorIndex(colorSource, colors.size());
    if (BeeUtils.isIndex(colors, index)) {
      return colors.get(index);
    } else {
      return null;
    }
  }

  protected Element getContent() {
    if (BeeUtils.isEmpty(getContentId())) {
      return null;
    } else {
      return Document.get().getElementById(getContentId());
    }
  }

  protected String getContentId() {
    return contentId;
  }

  protected int getDayColumnWidth() {
    return dayColumnWidth;
  }

  protected abstract Set<Action> getEnabledActions();

  protected Set<Action> getHiddenActions() {
    return Action.NO_ACTIONS;
  }

  protected Widget getEndSliderLabel() {
    return endSliderLabel;
  }

  protected CustomDiv getFilterLabel() {
    return filterLabel;
  }

  protected int getFooterHeight() {
    return footerHeight;
  }

  protected abstract String getFooterHeightColumnName();

  protected Collection<? extends HasDateRange> getFooterItems() {
    return getChartItems();
  }

  protected static int getFooterSplitterSize() {
    return TimeBoardHelper.DEFAULT_MOVER_HEIGHT;
  }

  protected int getHeaderHeight() {
    return headerHeight;
  }

  protected abstract String getHeaderHeightColumnName();

  protected static int getHeaderSplitterSize() {
    return TimeBoardHelper.DEFAULT_MOVER_HEIGHT;
  }

  protected int getLastResizableColumnMaxLeft(int minLeft) {
    int maxLeft = minLeft + TimeBoardHelper.MAX_RESIZABLE_COLUMN_WIDTH;
    if (getChartWidth() > 0) {
      maxLeft = Math.min(maxLeft, getChartLeft() + getChartWidth() / 2);
    }
    return maxLeft;
  }

  protected int getPrintHeightAdjustment() {
    if (BeeUtils.anyEmpty(getScrollAreaId(), getContentId())) {
      return 0;
    }

    Element scrollArea = getScrollArea();
    Element content = Document.get().getElementById(getContentId());
    if (scrollArea == null || content == null) {
      return 0;
    }
    return content.getClientHeight() - scrollArea.getClientHeight();
  }

  protected Element getProgressElement() {
    return progress.getElement();
  }

  protected Rectangle getRectangle(Range<JustDate> range, int row) {
    return getRectangle(range, row, row);
  }

  protected Rectangle getRectangle(Range<JustDate> range, int firstRow, int lastRow) {
    JustDate start = TimeUtils.clamp(range.lowerEndpoint(), getVisibleRange().lowerEndpoint(),
        getVisibleRange().upperEndpoint());
    JustDate end = TimeUtils.clamp(range.upperEndpoint(), getVisibleRange().lowerEndpoint(),
        getVisibleRange().upperEndpoint());

    int left = getChartLeft()
        + TimeUtils.dayDiff(getVisibleRange().lowerEndpoint(), start) * getDayColumnWidth();
    int width = (TimeUtils.dayDiff(start, end) + 1) * getDayColumnWidth();

    return TimeBoardHelper.getRectangle(left, width, firstRow, lastRow, getRowHeight());
  }

  protected int getRelativeLeft(Range<JustDate> parent, JustDate date) {
    return TimeUtils.dayDiff(parent.lowerEndpoint(), date) * getDayColumnWidth();
  }

  protected CustomDiv getRemoveFilter() {
    return removeFilter;
  }

  protected int getRowHeight() {
    return rowHeight;
  }

  protected abstract String getRowHeightColumnName();

  protected Element getScrollArea() {
    if (BeeUtils.isEmpty(getScrollAreaId())) {
      return null;
    } else {
      return Document.get().getElementById(getScrollAreaId());
    }
  }

  protected int getScrollAreaHeight(int canvasHeight) {
    return canvasHeight - getHeaderHeight() - getHeaderSplitterSize() - getFooterHeight()
        - getFooterSplitterSize();
  }

  protected String getScrollAreaId() {
    return scrollAreaId;
  }

  protected BeeRowSet getSettings() {
    return settings;
  }

  protected BeeRow getSettingsRow() {
    return DataUtils.isEmpty(getSettings()) ? null : getSettings().getRow(0);
  }

  protected int getSliderWidth() {
    return sliderWidth;
  }

  protected Widget getStartSliderLabel() {
    return startSliderLabel;
  }

  protected abstract String getStripOpacityColumnName();

  protected boolean hasContent() {
    return !BeeUtils.isEmpty(getContentId()) && getRowCount() > 0;
  }

  protected void initContent(ComplexPanel panel, int rc) {
    setRowCount(rc);

    if (rc > 0) {
      int height = getContentHeight();
      StyleUtils.setHeight(panel, height + getRowResizerHeight());

      TimeBoardHelper.renderDayColumns(panel, getVisibleRange(), getChartLeft(),
          getDayColumnWidth(), height);
    } else {
      StyleUtils.clearHeight(panel);
    }
  }

  protected abstract boolean isDataEventRelevant(ModificationEvent<?> event);

  protected boolean isFiltered() {
    return filtered;
  }

  protected void onCreate(ResponseObject response, ViewCallback callback) {
    if (setData(response, true)) {
      callback.onSuccess(this);
    } else {
      callback.onFailure(getCaption(), Localized.dictionary().nothingFound());
    }
  }

  protected void onDoubleClickChart(int row, JustDate date) {
  }

  protected void onFooterSplitterMove(MoveEvent event) {
    int delta = event.getDeltaY();

    Element splitter = ((Mover) event.getSource()).getElement();
    int oldBottom = StyleUtils.getBottom(splitter);

    int canvasHeight = getCanvasHeight();
    int newBottom = Math.min(clampFooterHeight(oldBottom - delta, canvasHeight),
        canvasHeight - getHeaderHeight() - getHeaderSplitterSize() - getRowHeight());

    if (newBottom != oldBottom) {
      StyleUtils.setBottom(splitter, newBottom);
      StyleUtils.setBottom(getScrollArea(), newBottom + getFooterSplitterSize());
    }

    if (event.isFinished() && updateSetting(getFooterHeightColumnName(), newBottom)) {
      render(false);
    }
  }

  protected void onHeaderSplitterMove(MoveEvent event) {
    int delta = event.getDeltaY();

    Element splitter = ((Mover) event.getSource()).getElement();
    int oldTop = StyleUtils.getTop(splitter);

    int canvasHeight = getCanvasHeight();
    int newTop = Math.min(clampHeaderHeight(oldTop + delta, canvasHeight),
        canvasHeight - getFooterHeight() - getFooterSplitterSize() - getRowHeight());

    if (newTop != oldTop) {
      StyleUtils.setTop(splitter, newTop);
      StyleUtils.setTop(getScrollArea(), newTop + getHeaderSplitterSize());
    }

    if (event.isFinished() && updateSetting(getHeaderHeightColumnName(), newTop)) {
      render(false);
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    EventUtils.clearRegistry(registry);
    registry.addAll(register());

    render(true);

    ReadyEvent.fire(this);
  }

  protected void onRelevantDataEvent(ModificationEvent<?> event) {
    refresh();
  }

  protected void onRowResizerMove(MoveEvent event) {
    int delta = event.getDeltaY();
    Element resizer = ((Mover) event.getSource()).getElement();

    int oldTop = StyleUtils.getTop(resizer);
    int newTop = Math.max(oldTop + delta, getRowCount() * 2);

    if (newTop != oldTop) {
      StyleUtils.setHeight(getContent(), newTop + getRowResizerHeight());
      StyleUtils.setTop(resizer, newTop);
    }

    if (event.isFinished()) {
      int rh = BeeUtils.round((double) newTop / getRowCount());
      if (rh != getRowHeight()) {
        updateSetting(getRowHeightColumnName(), rh);
      }

      render(false);
    }
  }

  @Override
  protected void onUnload() {
    EventUtils.clearRegistry(registry);
    super.onUnload();
  }

  protected abstract void prepareChart(Size canvasSize);

  protected void prepareDefaults(Size canvasSize) {
    setChartRight(DomUtils.getScrollBarWidth());

    int hh = TimeBoardHelper.getPixels(getSettings(), getHeaderHeightColumnName(), 20);
    setHeaderHeight(clampHeaderHeight(hh, canvasSize.getHeight()));

    int fh = TimeBoardHelper.getPixels(getSettings(), getFooterHeightColumnName(), 30);
    setFooterHeight(clampFooterHeight(fh, canvasSize.getHeight()));

    int rh = TimeBoardHelper.getPixels(getSettings(), getRowHeightColumnName(), 20);
    setRowHeight(clampRowHeight(rh, canvasSize.getHeight()));
  }

  protected void prepareFooter() {
    if (getChartWidth() > 0) {
      setSliderWidth(BeeUtils.clamp(TimeBoardHelper.DEFAULT_MOVER_WIDTH,
          1, Math.min(getChartLeft(), getChartRight())));
    } else {
      setSliderWidth(0);
    }
  }

  protected abstract void refresh();

  protected List<HandlerRegistration> register() {
    List<HandlerRegistration> result = new ArrayList<>();

    result.add(VisibilityChangeEvent.register(this));
    result.addAll(BeeKeeper.getBus().registerDataHandler(this, false));

    return result;
  }

  protected void render(boolean updateRange) {
    render(updateRange, null);
  }

  protected void render(boolean updateRange, Callback<Integer> callback) {
    long startMillis = System.currentTimeMillis();

    canvas.clear();
    if (getMaxRange() == null) {
      if (callback != null) {
        callback.onFailure("max range not available");
      }
      return;
    }

    int width = getCanvasWidth();
    int height = getCanvasHeight();
    if (width < 30 || height < 30) {
      setRenderPending(true);
      if (callback != null) {
        callback.onFailure(BeeUtils.joinWords("canvas not available", width, height));
      }
      return;
    }

    Size canvasSize = new Size(width, height);

    prepareDefaults(canvasSize);
    prepareChart(canvasSize);

    if (updateRange || getVisibleRange() == null) {
      setVisibleRange(TimeBoardHelper.getDefaultRange(getMaxRange(), getChartWidth(),
          getDayColumnWidth()));
    } else {
      checkVisibleRange();
    }

    adjustWidths();

    renderHeader();
    renderHeaderSplitter();

    long headerMillis = System.currentTimeMillis();

    Flow content = new Flow();
    content.addStyleName(STYLE_CONTENT);

    renderContent(content);
    finalizeContent(content);

    addContentHandlers(content);

    Simple scroll = new Simple();
    scroll.addStyleName(STYLE_SCROLL_AREA);
    StyleUtils.setTop(scroll, getHeaderHeight() + getHeaderSplitterSize());
    StyleUtils.setBottom(scroll, getFooterHeight() + getFooterSplitterSize());

    scroll.setWidget(content);

    setContentId(content.getId());
    setScrollAreaId(scroll.getId());

    canvas.add(scroll);

    long contentMillis = System.currentTimeMillis();

    renderFooterSplitter();

    prepareFooter();
    renderFooter(getFooterItems());

    setRenderPending(false);

    long endMillis = System.currentTimeMillis();

    DateRange vr = DateRange.closed(getVisibleRange().lowerEndpoint(),
        getVisibleRange().upperEndpoint());
    int descendants = DomUtils.countDescendants(canvas);

    logger.debug(vr);
    logger.debug(content.getWidgetCount(), descendants,
        BeeUtils.bracket(BeeUtils.joinWords(headerMillis - startMillis,
            BeeConst.STRING_PLUS, contentMillis - headerMillis,
            BeeConst.STRING_PLUS, endMillis - contentMillis,
            BeeConst.STRING_EQ, endMillis - startMillis)));

    if (callback != null) {
      callback.onSuccess(descendants);
    }
  }

  protected abstract void renderContent(ComplexPanel panel);

  protected void renderDayLabels(HasWidgets panel) {
    TimeBoardHelper.renderDayLabels(panel, getVisibleRange(), getChartLeft(), getDayColumnWidth(),
        getHeaderHeight());
  }

  protected void renderFooter(Collection<? extends HasDateRange> chartItems) {
    if (getFooterHeight() <= 0) {
      return;
    }

    Flow footer = new Flow();
    footer.addStyleName(STYLE_FOOTER);
    StyleUtils.setHeight(footer, getFooterHeight());

    renderMaxRange(footer);
    renderRangeSelector(footer, chartItems);

    canvas.add(footer);

    renderSliderLabels();
  }

  protected void renderFooterSplitter() {
    if (getFooterSplitterSize() > 0) {
      Mover mover = TimeBoardHelper.createVerticalMover();

      mover.addStyleName(STYLE_FOOTER_SPLITTER);
      StyleUtils.setBottom(mover, Math.max(getFooterHeight(), 0));

      mover.addMoveHandler(this::onFooterSplitterMove);
      canvas.add(mover);
    }
  }

  protected void renderHeader() {
    if (getHeaderHeight() > 0) {
      Flow header = new Flow();
      header.addStyleName(STYLE_HEADER);
      StyleUtils.setHeight(header, getHeaderHeight());

      renderVisibleRange(header);
      renderDayLabels(header);

      canvas.add(header);
    }
  }

  protected void renderHeaderSplitter() {
    if (getHeaderSplitterSize() > 0) {
      Mover mover = TimeBoardHelper.createVerticalMover();

      mover.addStyleName(STYLE_HEADER_SPLITTER);
      StyleUtils.setTop(mover, Math.max(getHeaderHeight(), 0));

      mover.addMoveHandler(this::onHeaderSplitterMove);
      canvas.add(mover);
    }
  }

  protected void renderMaxRange(HasWidgets panel) {
    TimeBoardHelper.renderMaxRange(getMaxRange(), panel, getChartLeft() - getSliderWidth(),
        getFooterHeight());
  }

  protected abstract void renderMovers(ComplexPanel panel, int height);

  protected void renderRangeSelector(HasWidgets panel,
      Collection<? extends HasDateRange> chartItems) {

    Flow selector = new Flow();
    selector.addStyleName(STYLE_SELECTOR_PANEL);

    StyleUtils.setLeft(selector, getChartLeft() - getSliderWidth());
    StyleUtils.setWidth(selector, getChartWidth() + getSliderWidth() * 2);

    renderSelector(selector, chartItems);
    panel.add(selector);
  }

  protected void renderRowResizer(ComplexPanel panel) {
    if (getRowCount() > 0) {
      Mover mover = TimeBoardHelper.createVerticalMover();
      mover.addStyleName(STYLE_ROW_RESIZER);

      StyleUtils.setWidth(mover, getChartLeft() + getCalendarWidth());
      StyleUtils.setTop(mover, getContentHeight());

      mover.addMoveHandler(this::onRowResizerMove);
      panel.add(mover);
    }
  }

  protected void renderSelector(HasWidgets panel, Collection<? extends HasDateRange> chartItems) {
    if (getMaxRange() == null) {
      return;
    }

    int itemAreaLeft = getSliderWidth();
    int itemAreaWidth = getChartWidth();

    int height = getFooterHeight();

    if (itemAreaWidth <= 0 || height <= 0) {
      return;
    }

    JustDate firstDate = getMaxRange().lowerEndpoint();
    JustDate lastDate = getMaxRange().upperEndpoint();

    double dayWidth = (double) itemAreaWidth / TimeBoardHelper.getSize(getMaxRange());

    if (!BeeUtils.isEmpty(chartItems)) {
      int maxRows;
      if (height <= 10) {
        maxRows = height;
      } else if (height <= 20) {
        maxRows = height / 2;
      } else {
        maxRows = height / 3;
      }

      List<List<HasDateRange>> rows = doStripLayout(chartItems, maxRows);

      int rc = rows.size();
      int itemHeight = height / rc;

      Double stripOpacity = BeeUtils.isEmpty(getStripOpacityColumnName())
          ? null : TimeBoardHelper.getOpacity(getSettings(), getStripOpacityColumnName());

      for (int row = 0; row < rc; row++) {
        for (HasDateRange item : rows.get(row)) {

          Widget itemStrip = new CustomDiv(STYLE_SELECTOR_STRIP);
          setItemWidgetColor(item, itemStrip);

          if (stripOpacity != null) {
            StyleUtils.setOpacity(itemStrip, stripOpacity);
          }

          StyleUtils.setTop(itemStrip, row * itemHeight);
          StyleUtils.setHeight(itemStrip, itemHeight);

          JustDate start = TimeUtils.clamp(item.getRange().lowerEndpoint(), firstDate, lastDate);
          JustDate end = TimeUtils.clamp(item.getRange().upperEndpoint(), firstDate, lastDate);

          int left = BeeUtils.round(TimeUtils.dayDiff(firstDate, start) * dayWidth);
          StyleUtils.setLeft(itemStrip, itemAreaLeft + BeeUtils.clamp(left, 0, itemAreaWidth - 1));

          int w = BeeUtils.round((TimeUtils.dayDiff(start, end) + 1) * dayWidth);
          StyleUtils.setWidth(itemStrip, BeeUtils.clamp(w, 1, itemAreaWidth - left));

          panel.add(itemStrip);
        }
      }
    }

    JustDate firstVisible = getVisibleRange().lowerEndpoint();
    JustDate lastVisible = getVisibleRange().upperEndpoint();

    int startPos = TimeBoardHelper.getPosition(firstDate, firstVisible, dayWidth);
    startPos = BeeUtils.clamp(startPos, 0, itemAreaWidth - getSliderWidth());

    int endPos = TimeBoardHelper.getPosition(firstDate, TimeUtils.nextDay(lastVisible), dayWidth);
    endPos = BeeUtils.clamp(endPos, startPos + getSliderWidth(), itemAreaWidth);

    rangeMovers.clear();

    if (getSliderWidth() > 0) {
      Mover startSlider = new Mover(STYLE_SELECTOR_START_SLIDER, Orientation.HORIZONTAL);
      StyleUtils.setLeft(startSlider, startPos);
      StyleUtils.setWidth(startSlider, getSliderWidth());

      startSlider.setTitle(Format.renderDate(firstVisible));

      startSlider.addMoveHandler(this);
      rangeMovers.put(RangeMover.START_SLIDER, startSlider.getElement());

      panel.add(startSlider);

      Mover endSlider = new Mover(STYLE_SELECTOR_END_SLIDER, Orientation.HORIZONTAL);
      StyleUtils.setLeft(endSlider, endPos + getSliderWidth());
      StyleUtils.setWidth(endSlider, getSliderWidth());

      endSlider.setTitle(Format.renderDate(TimeUtils.nextDay(lastVisible)));

      endSlider.addMoveHandler(this);
      rangeMovers.put(RangeMover.END_SLIDER, endSlider.getElement());

      panel.add(endSlider);

      if (startPos < endPos) {
        Mover mover = new Mover(STYLE_SELECTOR_MOVER, Orientation.HORIZONTAL);
        StyleUtils.setLeft(mover, startPos + getSliderWidth());
        StyleUtils.setWidth(mover, endPos - startPos);

        mover.setTitle(TimeBoardHelper.getRangeLabel(firstVisible, lastVisible));

        mover.addMoveHandler(this);
        rangeMovers.put(RangeMover.MOVER, mover.getElement());

        panel.add(mover);
      }
    }
  }

  protected void renderSliderLabels() {
    Label startLabel = new Label();
    startLabel.addStyleName(STYLE_START_SLIDER_LABEL);
    StyleUtils.setBottom(startLabel, getFooterHeight());
    startLabel.setVisible(false);

    setStartSliderLabel(startLabel);
    canvas.add(startLabel);

    Label endLabel = new Label();
    endLabel.addStyleName(STYLE_END_SLIDER_LABEL);
    StyleUtils.setBottom(endLabel, getFooterHeight());
    endLabel.setVisible(false);

    setEndSliderLabel(endLabel);
    canvas.add(endLabel);
  }

  protected void renderVisibleRange(HasWidgets panel) {
    TimeBoardHelper.renderVisibleRange(this, panel, getChartLeft(), getHeaderHeight());
  }

  protected int restoreColors(String serialized) {
    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (arr != null && arr.length > 0) {
      colors.clear();

      for (String s : arr) {
        Color color = Color.restore(s);
        if (color != null) {
          colors.add(color);
        }
      }

      return colors.size();

    } else {
      return 0;
    }
  }

  protected void setChartLeft(int chartLeft) {
    this.chartLeft = chartLeft;
  }

  protected void setChartRight(int chartRight) {
    this.chartRight = chartRight;
  }

  protected void setChartWidth(int chartWidth) {
    this.chartWidth = chartWidth;
  }

  protected abstract boolean setData(ResponseObject response, boolean init);

  protected void setDayColumnWidth(int dayColumnWidth) {
    this.dayColumnWidth = dayColumnWidth;
  }

  protected void setEndSliderLabel(Widget endSliderLabel) {
    this.endSliderLabel = endSliderLabel;
  }

  protected void setFiltered(boolean filtered) {
    this.filtered = filtered;
  }

  protected void setFooterHeight(int footerHeight) {
    this.footerHeight = footerHeight;
  }

  protected void setHeaderHeight(int headerHeight) {
    this.headerHeight = headerHeight;
  }

  protected void setItemWidgetColor(HasDateRange item, Widget widget) {
    if (item instanceof HasColorSource) {
      Color color = getColor(((HasColorSource) item).getColorSource());
      if (color != null) {
        UiHelper.setColor(widget, color);
      }
    }
  }

  protected void setRowHeight(int rowHeight) {
    this.rowHeight = rowHeight;
  }

  protected void setSettings(BeeRowSet settings) {
    this.settings = settings;
  }

  protected void setSliderWidth(int sliderWidth) {
    this.sliderWidth = sliderWidth;
  }

  protected void setStartSliderLabel(Widget startSliderLabel) {
    this.startSliderLabel = startSliderLabel;
  }

  protected void setVisibleRange(Range<JustDate> visibleRange) {
    this.visibleRange = visibleRange;
  }

  protected static void styleItemEnd(Widget widget) {
    widget.addStyleName(STYLE_ITEM_END);
  }

  protected static void styleItemHasHandling(Widget widget) {
    widget.addStyleName(STYLE_ITEM_HAS_HANDLING);
  }

  protected static void styleItemStart(Widget widget) {
    widget.addStyleName(STYLE_ITEM_START);
  }

  protected void styleItemWidget(HasDateRange item, Widget widget) {
    if (getVisibleRange().contains(item.getRange().lowerEndpoint())) {
      styleItemStart(widget);
    }
    if (getVisibleRange().contains(item.getRange().upperEndpoint())) {
      styleItemEnd(widget);
    }
  }

  protected void updateMaxRange() {
    setMaxRange(getChartItems());
  }

  protected boolean updateSetting(BeeRow row) {
    if (row != null && getSettings() != null) {
      getSettings().clearRows();
      getSettings().addRow(DataUtils.cloneRow(row));
      return true;
    } else {
      return false;
    }
  }

  protected boolean updateSetting(String colName, int value) {
    List<String> colNames = Lists.newArrayList(colName);
    List<String> values = Lists.newArrayList(BeeUtils.toString(value));

    return updateSettings(colNames, values);
  }

  protected boolean updateSettings(List<String> colNames, List<String> values) {
    if (DataUtils.isEmpty(getSettings())) {
      logger.severe("update settings: rowSet is empty");
      return false;
    }

    List<Integer> indexes = new ArrayList<>();

    for (String colName : colNames) {
      int index = getSettings().getColumnIndex(colName);

      if (BeeConst.isUndef(index)) {
        logger.severe(getSettings().getViewName(), colName, "column not found");
        return false;
      } else {
        indexes.add(index);
      }
    }

    List<BeeColumn> columns = new ArrayList<>();
    List<String> oldValues = new ArrayList<>();
    List<String> newValues = new ArrayList<>();

    BeeRow row = getSettings().getRow(0);

    for (int i = 0; i < indexes.size(); i++) {
      int index = indexes.get(i);

      String oldValue = row.getString(index);
      String newValue = values.get(i);

      if (!BeeUtils.equalsTrim(oldValue, newValue)) {
        row.setValue(index, newValue);

        columns.add(getSettings().getColumn(index));
        oldValues.add(oldValue);
        newValues.add(newValue);
      }
    }

    if (columns.isEmpty()) {
      return false;
    }

    Queries.update(getSettings().getViewName(), row.getId(), row.getVersion(), columns, oldValues,
        newValues, null, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            if (result != null) {
              getSettings().clearRows();
              getSettings().addRow(DataUtils.cloneRow(result));
            }
          }
        });

    return true;
  }

  protected boolean updateSettings(String col1, int val1, String col2, int val2) {
    List<String> colNames = Lists.newArrayList(col1, col2);
    List<String> values = Lists.newArrayList(BeeUtils.toString(val1), BeeUtils.toString(val2));

    return updateSettings(colNames, values);
  }

  private void checkVisibleRange() {
    if (!getMaxRange().encloses(getVisibleRange())) {
      int visibleDays = TimeBoardHelper.getSize(getVisibleRange());

      if (visibleDays <= 0 || visibleDays >= TimeBoardHelper.getSize(getMaxRange())) {
        setVisibleRange(TimeBoardHelper.normalizedCopyOf(getMaxRange()));
      } else {
        JustDate lower;
        JustDate upper;

        if (TimeUtils.isLess(getVisibleRange().lowerEndpoint(), getMaxRange().lowerEndpoint())) {
          lower = JustDate.copyOf(getMaxRange().lowerEndpoint());
          upper = TimeUtils.nextDay(lower, visibleDays - 1);
        } else {
          upper = JustDate.copyOf(getMaxRange().upperEndpoint());
          lower = TimeUtils.nextDay(upper, 1 - visibleDays);
        }

        setVisibleRange(Range.closed(lower, upper));
      }
    }
  }

  private int getContentHeight() {
    return getRowCount() * getRowHeight();
  }

  private int getRowCount() {
    return rowCount;
  }

  private int getRowResizerHeight() {
    return (getRowCount() > 0) ? TimeBoardHelper.DEFAULT_MOVER_HEIGHT : 0;
  }

  private boolean isRenderPending() {
    return renderPending;
  }

  private void setContentId(String contentId) {
    this.contentId = contentId;
  }

  private void setMaxRange(Collection<? extends HasDateRange> items) {
    JustDate min = TimeUtils.today(-1);
    JustDate max = TimeUtils.startOfNextMonth(min);
    if (min.getDom() > 1) {
      TimeUtils.addDay(max, min.getDom() - 1);
    }

    setMaxRange(TimeBoardHelper.getSpan(items, min, max));
  }

  private void setMaxRange(Range<JustDate> maxRange) {
    this.maxRange = maxRange;
  }

  private void setRenderPending(boolean renderPending) {
    this.renderPending = renderPending;
  }

  private void setRowCount(int rowCount) {
    this.rowCount = rowCount;
  }

  private void setScrollAreaId(String scrollAreaId) {
    this.scrollAreaId = scrollAreaId;
  }
}
