package com.butent.bee.client.timeboard;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TimeBoardHelper {

  public static final int DEFAULT_MOVER_WIDTH = 3;
  public static final int DEFAULT_MOVER_HEIGHT = 3;

  public static final int DAY_SEPARATOR_WIDTH = 1;
  public static final int ROW_SEPARATOR_HEIGHT = 1;

  static final int MAX_RESIZABLE_COLUMN_WIDTH = 300;

  private static final BeeLogger logger = LogUtils.getLogger(TimeBoardHelper.class);

  private static final String STYLE_MONTH_SEPARATOR = TimeBoard.STYLE_PREFIX + "month-separator";
  private static final String STYLE_DAY_SEPARATOR = TimeBoard.STYLE_PREFIX + "day-separator";
  private static final String STYLE_RIGHT_SEPARATOR = TimeBoard.STYLE_PREFIX + "right-separator";

  private static final String STYLE_DAY_LABEL = TimeBoard.STYLE_PREFIX + "day-label";

  private static final String STYLE_DAY_NARROW = TimeBoard.STYLE_PREFIX + "day-narrow";
  private static final String STYLE_DAY_NARROW_TENS = STYLE_DAY_NARROW + "-tens";
  private static final String STYLE_DAY_NARROW_ONES = STYLE_DAY_NARROW + "-ones";

  private static final String STYLE_DAY_PICTURE = TimeBoard.STYLE_PREFIX + "day-picture";
  private static final String STYLE_DAY_PICTURE_TENS = STYLE_DAY_PICTURE + "-tens";
  private static final String STYLE_DAY_PICTURE_ONES = STYLE_DAY_PICTURE + "-ones";

  private static final String STYLE_PAST = TimeBoard.STYLE_PREFIX + "past";
  private static final String STYLE_TODAY = TimeBoard.STYLE_PREFIX + "today";
  private static final String STYLE_WEEKDAY = TimeBoard.STYLE_PREFIX + "weekday";
  private static final String STYLE_WEEKEND = TimeBoard.STYLE_PREFIX + "weekend";

  private static final String STYLE_V_R_PREFIX = TimeBoard.STYLE_PREFIX + "visible-range-";
  private static final String STYLE_VISIBLE_RANGE_PANEL = STYLE_V_R_PREFIX + "panel";
  private static final String STYLE_VISIBLE_RANGE_START = STYLE_V_R_PREFIX + "start";
  private static final String STYLE_VISIBLE_RANGE_END = STYLE_V_R_PREFIX + "end";

  private static final String STYLE_M_R_PREFIX = TimeBoard.STYLE_PREFIX + "max-range-";
  private static final String STYLE_MAX_RANGE_PANEL = STYLE_M_R_PREFIX + "panel";
  private static final String STYLE_MAX_RANGE_START = STYLE_M_R_PREFIX + "start";
  private static final String STYLE_MAX_RANGE_END = STYLE_M_R_PREFIX + "end";

  private static final String STYLE_DAY_BACKGROUND = TimeBoard.STYLE_PREFIX + "day-background";

  private static final String STYLE_CONTENT_ROW_SEPARATOR = TimeBoard.STYLE_PREFIX + "row-sep";

  private static final String STYLE_HORIZONTAL_MOVER = TimeBoard.STYLE_PREFIX + "horizontal-mover";
  private static final String STYLE_VERTICAL_MOVER = TimeBoard.STYLE_PREFIX + "vertical-mover";

  private static final int MIN_DAY_WIDTH_FOR_SEPARATOR = 10;

  private static final String VALUE_SEPARATOR = BeeConst.STRING_COLON + BeeConst.STRING_SPACE;

  public static void addRowSeparator(HasWidgets panel, int top, int left, int width) {
    addRowSeparator(panel, STYLE_CONTENT_ROW_SEPARATOR, top, left, width);
  }

  public static void addRowSeparator(HasWidgets panel, String styleName, int top, int left,
      int width) {

    CustomDiv separator = new CustomDiv(styleName);

    if (top >= 0) {
      StyleUtils.setTop(separator, top - ROW_SEPARATOR_HEIGHT);
    }
    if (left >= 0) {
      StyleUtils.setLeft(separator, left);
    }
    if (width > 0) {
      StyleUtils.setWidth(separator, width);
    }

    panel.add(separator);
  }

  public static void apply(Widget widget, Rectangle rectangle, Edges margins) {
    Style style = widget.getElement().getStyle();

    if (rectangle.getLeftValue() != null) {
      int left = BeeUtils.toInt(rectangle.getLeftValue());
      if (margins.getLeftValue() != null) {
        left += BeeUtils.toInt(margins.getLeftValue());
      }

      StyleUtils.setLeft(style, left);
    }

    if (rectangle.getTopValue() != null) {
      int top = BeeUtils.toInt(rectangle.getTopValue());
      if (margins.getTopValue() != null) {
        top += BeeUtils.toInt(margins.getTopValue());
      }

      StyleUtils.setTop(style, top);
    }

    if (rectangle.getWidthValue() != null) {
      int width = BeeUtils.toInt(rectangle.getWidthValue());

      if (margins.getLeftValue() != null) {
        width -= BeeUtils.toInt(margins.getLeftValue());
      }
      if (margins.getRightValue() != null) {
        width -= BeeUtils.toInt(margins.getRightValue());
      }

      if (width > 0) {
        StyleUtils.setWidth(style, width);
      }
    }

    if (rectangle.getHeightValue() != null) {
      int height = BeeUtils.toInt(rectangle.getHeightValue());

      if (margins.getTopValue() != null) {
        height -= BeeUtils.toInt(margins.getTopValue());
      }
      if (margins.getBottomValue() != null) {
        height -= BeeUtils.toInt(margins.getBottomValue());
      }

      if (height > 0) {
        StyleUtils.setHeight(style, height);
      }
    }
  }

  public static String buildTitle(Object... labelsAndValues) {
    return buildMessage(BeeConst.STRING_EOL, labelsAndValues);
  }

  public static Mover createHorizontalMover() {
    return new Mover(STYLE_HORIZONTAL_MOVER, Orientation.HORIZONTAL);
  }

  public static List<HasDateRange> getActiveItems(Collection<? extends HasDateRange> items,
      Range<JustDate> activeRange) {

    List<HasDateRange> result = new ArrayList<>();
    if (items == null || activeRange == null) {
      return result;
    }

    for (HasDateRange item : items) {
      if (hasRangeAndIsActive(item, activeRange)) {
        result.add(item);
      }
    }
    return result;
  }

  public static Range<JustDate> getActivity(JustDate start, JustDate end) {
    if (start == null && end == null) {
      return null;
    } else if (end == null) {
      return Range.atLeast(start);
    } else if (start == null) {
      return Range.atMost(end);
    } else {
      return Range.closed(start, BeeUtils.max(start, end));
    }
  }

  public static boolean getBoolean(BeeRowSet settings, String colName) {
    if (DataUtils.isEmpty(settings)) {
      return false;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return false;
    }

    return BeeUtils.unbox(settings.getBoolean(0, index));
  }

  public static JustDate getDate(BeeRowSet settings, String colName) {
    if (DataUtils.isEmpty(settings)) {
      return null;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return null;
    }

    return settings.getDate(0, index);
  }

  public static List<HasDateRange> getInactivity(HasDateRange item, Range<JustDate> activeRange) {
    List<HasDateRange> result = new ArrayList<>();
    if (activeRange == null || item == null || item.getRange() == null) {
      return result;
    }

    if (activeRange.hasLowerBound() && item.getRange().hasLowerBound()
        && BeeUtils.isLess(activeRange.lowerEndpoint(), item.getRange().lowerEndpoint())) {
      result.add(DateRange.closed(activeRange.lowerEndpoint(),
          BeeUtils.min(activeRange.upperEndpoint(),
              TimeUtils.previousDay(item.getRange().lowerEndpoint()))));
    }

    if (activeRange.hasUpperBound() && item.getRange().hasUpperBound()
        && BeeUtils.isMore(activeRange.upperEndpoint(), item.getRange().upperEndpoint())) {
      result.add(DateRange.closed(BeeUtils.max(activeRange.lowerEndpoint(),
          TimeUtils.nextDay(item.getRange().upperEndpoint())), activeRange.upperEndpoint()));
    }

    return result;
  }

  public static Integer getInteger(BeeRowSet settings, String colName) {
    if (DataUtils.isEmpty(settings)) {
      return null;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return null;
    } else {
      return settings.getInteger(0, index);
    }
  }

  public static Long getLong(BeeRowSet settings, String colName) {
    if (DataUtils.isEmpty(settings)) {
      return null;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return null;
    } else {
      return settings.getLong(0, index);
    }
  }

  public static Double getOpacity(BeeRowSet settings, String colName) {
    if (DataUtils.isEmpty(settings)) {
      return null;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return null;
    }

    Integer value = settings.getInteger(0, index);
    return (BeeUtils.isPositive(value) && value < 100) ? value / 100.0 : null;
  }

  public static int getPixels(BeeRowSet settings, String colName, int def, int min, int max) {
    return BeeUtils.clamp(getPixels(settings, colName, def), min, max);
  }

  public static Range<JustDate> getRange(DateTime start, DateTime end) {
    if (start == null) {
      if (end == null) {
        return null;
      } else {
        JustDate date = end.getDate();
        return Range.closed(date, date);
      }

    } else if (end == null) {
      JustDate date = start.getDate();
      return Range.closed(date, date);

    } else {
      JustDate lower = start.getDate();

      JustDate upper;
      if (end.hasTimePart()) {
        upper = end.getDate();
      } else {
        upper = TimeUtils.previousDay(end);
      }

      return Range.closed(lower, BeeUtils.max(lower, upper));
    }
  }

  public static String getRangeLabel(Range<JustDate> range) {
    if (range == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return getRangeLabel(BeeUtils.getLowerEndpoint(range), BeeUtils.getUpperEndpoint(range));
    }
  }

  public static Rectangle getRectangle(int left, int width, int firstRow, int lastRow,
      int rowHeight) {

    Rectangle rectangle = new Rectangle();

    if (left >= 0) {
      rectangle.setLeft(left);
    }
    if (width > 0) {
      rectangle.setWidth(width);
    }

    if (firstRow >= 0 && lastRow >= firstRow && rowHeight > 0) {
      rectangle.setTop(firstRow * rowHeight);
      rectangle.setHeight((lastRow - firstRow + 1) * rowHeight);
    }

    return rectangle;
  }

  public static int getSize(Range<JustDate> range) {
    if (range == null || !range.hasLowerBound() || !range.hasUpperBound()) {
      return BeeConst.UNDEF;
    }

    int start = range.lowerEndpoint().getDays();
    if (range.lowerBoundType() == BoundType.OPEN) {
      start--;
    }

    int end = range.upperEndpoint().getDays();
    if (range.lowerBoundType() == BoundType.CLOSED) {
      end++;
    }

    return end - start;
  }

  public static Range<JustDate> getSpan(Collection<? extends HasDateRange> items) {
    return getSpan(items, null, null);
  }

  public static String getString(BeeRowSet settings, String colName) {
    if (DataUtils.isEmpty(settings)) {
      return null;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return null;
    }

    return settings.getString(0, index);
  }

  public static boolean hasRangeAndIsActive(HasDateRange item, Range<JustDate> activeRange) {
    if (item == null || item.getRange() == null) {
      return false;
    } else if (activeRange == null) {
      return true;
    } else {
      return activeRange.isConnected(item.getRange());
    }
  }

  public static boolean isActive(HasDateRange item, Range<JustDate> activeRange) {
    if (item == null) {
      return false;
    } else if (activeRange == null || item.getRange() == null) {
      return true;
    } else {
      return activeRange.isConnected(item.getRange());
    }
  }

  public static boolean isNormalized(Range<JustDate> range) {
    return range != null && !range.isEmpty() && range.hasLowerBound() && range.hasUpperBound()
        && range.lowerBoundType() == BoundType.CLOSED && range.upperBoundType() == BoundType.CLOSED;
  }

  public static String join(String label, Object value) {
    return isEmpty(value) ? BeeConst.STRING_EMPTY : BeeUtils.join(VALUE_SEPARATOR, label, value);
  }

  public static Range<JustDate> normalizedIntersection(Range<JustDate> r1, Range<JustDate> r2) {
    if (r1 == null || r2 == null) {
      return null;

    } else if (r1.isConnected(r2)) {
      Range<JustDate> section = r1.intersection(r2);
      return isNormalized(section) ? section : normalizedCopyOf(section);

    } else {
      return null;
    }
  }

  public static Size splitRectangle(int width, int height, int count) {
    if (width <= 0 || height <= 0 || count <= 0 || count > width * height) {
      return null;
    }
    if (count == 1) {
      return new Size(width, height);
    }
    if (count * 2 > width * height) {
      return new Size(1, 1);
    }

    int x = 0;
    int y = 0;

    for (int rows = 1; rows <= Math.min(count, height); rows++) {
      int cols = count / rows;
      if (count % rows > 0) {
        cols++;
      }

      if (cols > 0 && cols <= width) {
        int w = width / cols;
        int h = height / rows;

        if (Math.min(w, h) > Math.min(x, y)
            || Math.min(w, h) == Math.min(x, y) && Math.max(w, h) > Math.max(x, y)) {
          x = w;
          y = h;
        }
      }
    }

    return new Size(x, y);
  }

  public static boolean updateSettings(final BeeRowSet settings, String colName, String newValue,
      final Runnable callback) {

    if (DataUtils.isEmpty(settings)) {
      return false;
    }

    final int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return false;
    }

    BeeRow oldRow = settings.getRow(0);
    final String oldValue = oldRow.getString(index);

    BeeRowSet updated = DataUtils.getUpdated(settings.getViewName(),
        oldRow.getId(), oldRow.getVersion(), settings.getColumn(index), oldValue, newValue);
    if (DataUtils.isEmpty(updated)) {
      return false;
    }

    oldRow.setValue(index, newValue);

    Queries.updateRow(updated, new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        settings.setValue(0, index, oldValue);
        RowCallback.super.onFailure(reason);
      }

      @Override
      public void onSuccess(BeeRow result) {
        settings.clearRows();
        settings.addRow(result);

        if (callback != null) {
          callback.run();
        }
      }
    });

    return true;
  }

  static void addColumnSeparator(HasWidgets panel, String styleName, int left, int height) {
    CustomDiv separator = new CustomDiv(styleName);

    if (left >= 0) {
      StyleUtils.setLeft(separator, left);
    }
    if (height > 0) {
      StyleUtils.setHeight(separator, height);
    }

    panel.add(separator);
  }

  static String buildMessage(String separator, Object... labelsAndValues) {
    Assert.notNull(labelsAndValues);
    int c = labelsAndValues.length;
    Assert.parameterCount(c, 2);
    Assert.isEven(c);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < c - 1; i += 2) {
      Object label = labelsAndValues[i];
      Object value = labelsAndValues[i + 1];

      if (label instanceof String && !isEmpty(value)) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(BeeUtils.join(VALUE_SEPARATOR, label, value));
      }
    }
    return sb.toString();
  }

  static JustDate clamp(JustDate date, Range<JustDate> range) {
    return TimeUtils.clamp(date, BeeUtils.getLowerEndpoint(range),
        BeeUtils.getUpperEndpoint(range));
  }

  static Mover createVerticalMover() {
    return new Mover(STYLE_VERTICAL_MOVER, Orientation.VERTICAL);
  }

  static int getColorIndex(Long id, int count) {
    if (id == null || count <= 0) {
      return BeeConst.UNDEF;
    } else {
      return Math.abs(Codec.crc32(BeeUtils.toString(id)).hashCode()) % count;
    }
  }

  static JustDate getDate(JustDate start, int position, double daySize) {
    return TimeUtils.nextDay(start, BeeUtils.round(position / daySize));
  }

  static String getDateTitle(JustDate date) {
    if (date == null) {
      return null;
    } else {
      return BeeUtils.buildLines(Format.renderDate(date), Format.renderDayOfWeek(date));
    }
  }

  static Range<JustDate> getDefaultRange(Range<JustDate> span, int chartWidth, int dayWidth) {
    int spanSize = getSize(span);
    int days = Math.max(chartWidth / dayWidth, 1);

    if (days >= spanSize) {
      return normalizedCopyOf(span);
    }

    JustDate start = JustDate.copyOf(span.lowerEndpoint());
    JustDate end = TimeUtils.nextDay(start, days - 1);

    JustDate preferred = TimeUtils.today((days > 2) ? -1 : 0);
    int diff = TimeUtils.dayDiff(start, preferred);

    if (diff > 0) {
      int shift = Math.min(diff, spanSize - days);
      TimeUtils.addDay(start, shift);
      TimeUtils.addDay(end, shift);
    }

    return Range.closed(start, end);
  }

  static JustDate getLowerBound(JustDate min, int size, JustDate max) {
    if (max == null || size <= 0) {
      return min;
    } else if (min == null) {
      return TimeUtils.nextDay(max, 1 - size);
    } else {
      return TimeUtils.max(TimeUtils.nextDay(max, 1 - size), min);
    }
  }

  static int getPixels(BeeRowSet settings, String colName, int def) {
    if (DataUtils.isEmpty(settings)) {
      return def;
    }

    int index = settings.getColumnIndex(colName);
    if (BeeConst.isUndef(index)) {
      logger.severe(settings.getViewName(), colName, "column not found");
      return def;
    }

    Integer value = settings.getInteger(0, index);
    return BeeUtils.isPositive(value) ? value : def;
  }

  static int getPosition(JustDate start, JustDate date, double daySize) {
    return BeeUtils.round(TimeUtils.dayDiff(start, date) * daySize);
  }

  static String getRangeLabel(JustDate start, JustDate end) {
    if (start == null && end == null) {
      return BeeConst.STRING_EMPTY;
    } else if (start == null) {
      return Format.renderDate(end);
    } else if (end == null || start.equals(end)) {
      return Format.renderDate(start);
    } else {
      return BeeUtils.joinWords(Format.renderDate(start), Format.renderDate(end));
    }
  }

  static Range<JustDate> getSpan(Collection<? extends HasDateRange> items,
      JustDate defMin, JustDate defMax) {

    JustDate min = defMin;
    JustDate max = defMax;

    for (HasDateRange item : items) {
      JustDate lower = BeeUtils.getLowerEndpoint(item.getRange());
      if (lower != null && (min == null || TimeUtils.isLess(lower, min))) {
        min = lower;
      }

      JustDate upper = BeeUtils.getUpperEndpoint(item.getRange());
      if (upper != null && (max == null || TimeUtils.isMore(upper, max))) {
        max = upper;
      }
    }

    if (min == null || max == null) {
      return null;
    } else {
      return Range.closed(min, max);
    }
  }

  static JustDate getUpperBound(JustDate min, int size, JustDate max) {
    if (min == null || size <= 0) {
      return max;
    } else if (max == null) {
      return TimeUtils.nextDay(min, size - 1);
    } else {
      return TimeUtils.min(TimeUtils.nextDay(min, size - 1), max);
    }
  }

  static Range<JustDate> normalizedCopyOf(Range<JustDate> range) {
    if (range == null || range.isEmpty() || !range.hasLowerBound() || !range.hasUpperBound()) {
      return null;
    }

    if (range.lowerBoundType() == BoundType.CLOSED && range.upperBoundType() == BoundType.CLOSED) {
      return Range.closed(range.lowerEndpoint(), range.upperEndpoint());
    }

    int start = range.lowerEndpoint().getDays();
    if (range.lowerBoundType() == BoundType.OPEN) {
      start++;
    }

    int end = range.upperEndpoint().getDays();
    if (range.lowerBoundType() == BoundType.OPEN) {
      end--;
    }

    if (start <= end) {
      return Range.closed(new JustDate(start), new JustDate(end));
    } else {
      return null;
    }
  }

  static Range<JustDate> normalizedIntersection(Range<JustDate> r1, Range<JustDate> r2,
      Range<JustDate> r3) {
    return normalizedIntersection(r1, normalizedIntersection(r2, r3));
  }

  static void renderDayColumns(HasWidgets panel, Range<JustDate> range, int startLeft,
      int dayWidth, int height) {

    JustDate date = JustDate.copyOf(range.lowerEndpoint());
    int count = getSize(range);

    int left = startLeft;
    int separatorWidth;

    JustDate next = TimeUtils.nextDay(date);

    for (int i = 0; i < count; i++) {
      separatorWidth = getDaySeparatorWidth(next, dayWidth, i, count);

      Widget background = createDayBackground(date);
      StyleUtils.setLeft(background, left);
      StyleUtils.setWidth(background, dayWidth - separatorWidth);
      if (height > 0) {
        StyleUtils.setHeight(background, height);
      }

      panel.add(background);

      if (separatorWidth > 0) {
        Widget separator = createDaySeparator(next, i, count);
        StyleUtils.setLeft(separator, left + dayWidth - separatorWidth);
        if (height > 0) {
          StyleUtils.setHeight(separator, height);
        }

        panel.add(separator);

      } else {
        background.setTitle(getDateTitle(date));
      }

      TimeUtils.addDay(date, 1);
      TimeUtils.addDay(next, 1);

      left += dayWidth;
    }
  }

  static void renderDayLabels(HasWidgets panel, Range<JustDate> range, int startLeft,
      int dayWidth, int height) {

    JustDate date = JustDate.copyOf(range.lowerEndpoint());
    int count = getSize(range);

    int maxSeparatorWidth = getDaySeparatorWidth(range.upperEndpoint(), dayWidth, count - 1, count);
    int separatorWidth;

    int left = startLeft;
    JustDate next = TimeUtils.nextDay(date);

    for (int i = 0; i < count; i++) {
      separatorWidth = getDaySeparatorWidth(next, dayWidth, i, count);

      Widget label = createDayLabel(date, dayWidth - maxSeparatorWidth, height);
      StyleUtils.setLeft(label, left);
      StyleUtils.setWidth(label, dayWidth - separatorWidth);

      label.setTitle(getDateTitle(date));
      panel.add(label);

      if (separatorWidth > 0) {
        Widget separator = createDaySeparator(next, i, count);
        StyleUtils.setLeft(separator, left + dayWidth - separatorWidth);
        panel.add(separator);
      }

      TimeUtils.addDay(date, 1);
      TimeUtils.addDay(next, 1);

      left += dayWidth;
    }
  }

  static void renderMaxRange(Range<JustDate> range, HasWidgets container, int width, int height) {
    if (range == null || range.isEmpty() || !checkRangePanelSize(width, height)) {
      return;
    }

    Flow panel = new Flow();
    panel.addStyleName(STYLE_MAX_RANGE_PANEL);
    StyleUtils.setSize(panel, width, height);

    Widget startWidget = null;
    Widget endWidget = null;

    if (getSize(range) > 1) {
      Pair<Widget, Widget> widgets = renderRange(range.lowerEndpoint(), STYLE_MAX_RANGE_START,
          range.upperEndpoint(), STYLE_MAX_RANGE_END, width, height);

      if (widgets != null) {
        startWidget = widgets.getA();
        endWidget = widgets.getB();
      }

    } else {
      startWidget = renderDate(range.lowerEndpoint(), STYLE_MAX_RANGE_START, width, height);
    }

    if (startWidget != null) {
      panel.add(startWidget);
    }
    if (endWidget != null) {
      panel.add(endWidget);
    }

    container.add(panel);
  }

  static void renderVisibleRange(HasVisibleRange owner, HasWidgets container,
      int width, int height) {

    Range<JustDate> range = owner.getVisibleRange();
    if (range == null || range.isEmpty() || !checkRangePanelSize(width, height)) {
      return;
    }

    Flow panel = new Flow();
    panel.addStyleName(STYLE_VISIBLE_RANGE_PANEL);
    StyleUtils.setSize(panel, width, height);

    Widget startWidget = null;
    Widget endWidget = null;

    if (getSize(owner.getMaxRange()) > 1) {
      Pair<Widget, Widget> widgets = renderRange(range.lowerEndpoint(), STYLE_VISIBLE_RANGE_START,
          range.upperEndpoint(), STYLE_VISIBLE_RANGE_END, width, height);

      if (widgets != null) {
        startWidget = widgets.getA();
        endWidget = widgets.getB();
      }

    } else {
      startWidget = renderDate(range.lowerEndpoint(), STYLE_VISIBLE_RANGE_START, width, height);
    }

    if (startWidget != null) {
      addVisibleRangeDatePicker(owner, startWidget, true);
      panel.add(startWidget);
    }
    if (endWidget != null) {
      addVisibleRangeDatePicker(owner, endWidget, false);
      panel.add(endWidget);
    }

    container.add(panel);
  }

  private static void addDayStyle(Widget widget, JustDate date) {
    if (TimeUtils.isMore(TimeUtils.today(), date)) {
      widget.addStyleName(STYLE_PAST);
    } else if (TimeUtils.today().equals(date)) {
      widget.addStyleName(STYLE_TODAY);
    } else if (TimeUtils.isWeekend(date)) {
      widget.addStyleName(STYLE_WEEKEND);
    } else {
      widget.addStyleName(STYLE_WEEKDAY);
    }
  }

  private static void addVisibleRangeDatePicker(final HasVisibleRange owner,
      final Widget widget, final boolean isStart) {

    Binder.addClickHandler(widget, event -> {
      final JustDate startBound = owner.getMaxRange().lowerEndpoint();
      final JustDate endBound = owner.getMaxRange().upperEndpoint();

      final JustDate oldStart = owner.getVisibleRange().lowerEndpoint();
      final JustDate oldEnd = owner.getVisibleRange().upperEndpoint();

      final JustDate oldValue = isStart ? oldStart : oldEnd;

      final Popup popup = new Popup(OutsideClick.CLOSE);
      final DatePicker datePicker = new DatePicker(oldValue, startBound, endBound);

      datePicker.addValueChangeHandler(vce -> {
        popup.close();
        JustDate newValue = vce.getValue();

        if (newValue != null && !newValue.equals(oldValue)
            && owner.getMaxRange().contains(newValue)) {

          int maxSize = owner.getMaxSize();
          JustDate newStart;
          JustDate newEnd;

          if (isStart) {
            newStart = newValue;
            newEnd = TimeUtils.clamp(oldEnd, newValue,
                getUpperBound(newValue, maxSize, endBound));
          } else {
            newStart = TimeUtils.clamp(oldStart, getLowerBound(startBound, maxSize, newValue),
                newValue);
            newEnd = newValue;
          }

          owner.setVisibleRange(newStart, newEnd);
        }
      });

      popup.setWidget(datePicker);

      popup.setHideOnEscape(true);
      popup.addOpenHandler(oe -> datePicker.setFocus(true));

      popup.showRelativeTo(EventUtils.getTargetElement(event.getNativeEvent().getEventTarget()));
    });
  }

  private static boolean checkRangePanelSize(int width, int height) {
    return width > 2 && height > 2 && width * height > 100;
  }

  private static Widget createDayBackground(JustDate date) {
    CustomDiv widget = new CustomDiv();

    widget.addStyleName(STYLE_DAY_BACKGROUND);
    addDayStyle(widget, date);

    return widget;
  }

  private static Widget createDayLabel(JustDate date, int width, int height) {
    Label widget;
    int day = date.getDom();

    int tens = day / 10;
    int ones = day % 10;

    if (width >= 15 && height >= 10) {
      widget = new Label(BeeUtils.toString(day));

      widget.addStyleName(STYLE_DAY_LABEL);
      addDayStyle(widget, date);

    } else if (width >= 7 && height >= 22) {
      Element root = Document.get().createDivElement();
      if (tens > 0) {
        Element tensElement = Document.get().createDivElement();
        tensElement.setInnerText(BeeUtils.toString(tens));
        tensElement.addClassName(STYLE_DAY_NARROW_TENS);

        root.appendChild(tensElement);
      }

      Element onesElement = Document.get().createDivElement();
      onesElement.setInnerText(BeeUtils.toString(ones));
      onesElement.addClassName(STYLE_DAY_NARROW_ONES);

      root.appendChild(onesElement);

      widget = new Label(root);

      widget.addStyleName(STYLE_DAY_NARROW);
      addDayStyle(widget, date);

    } else {
      int tensWidth = 1;
      int tensHeight = 1;
      int onesWidth = 1;
      int onesHeight = 1;

      int tensCount = 1;
      int onesCount = 1;

      if (width >= 2 && height >= 9) {
        tensWidth = width / 2;
        tensHeight = height / 3 * tens;

        onesWidth = width / 2;
        onesHeight = height / 9 * ones;

      } else if (width >= 9 && height >= 2) {
        tensWidth = width / 3 * tens;
        tensHeight = height / 2;

        onesWidth = width / 9 * ones;
        onesHeight = height / 2;

      } else if (width >= 12) {
        tensWidth = (width - 9) / 3 * tens;
        onesWidth = ones;

      } else if (height >= 12) {
        tensHeight = (height - 9) / 3 * tens;
        onesHeight = ones;

      } else if (width >= 6 && height >= 2) {
        tensWidth = (width * height - 9) / 3;
        onesCount = ones;

      } else if (width >= 2 && height >= 6) {
        tensHeight = (width * height - 9) / 3;
        onesCount = ones;

      } else {
        tensCount = tens;
        onesCount = ones;
      }

      Element root = Document.get().createDivElement();
      String styleSuffix = BeeConst.STRING_MINUS + BeeUtils.toString(tens * 10);

      if (tens > 0) {
        for (int i = 0; i < tensCount; i++) {
          Element tensElement = Document.get().createDivElement();
          tensElement.addClassName(STYLE_DAY_PICTURE_TENS);
          tensElement.addClassName(STYLE_DAY_PICTURE_TENS + styleSuffix);
          StyleUtils.setSize(tensElement, tensWidth, tensHeight);

          root.appendChild(tensElement);
        }
      }

      if (ones > 0) {
        for (int i = 0; i < onesCount; i++) {
          Element onesElement = Document.get().createDivElement();
          onesElement.addClassName(STYLE_DAY_PICTURE_ONES);
          onesElement.addClassName(STYLE_DAY_PICTURE_ONES + styleSuffix);
          StyleUtils.setSize(onesElement, onesWidth, onesHeight);

          root.appendChild(onesElement);
        }
      }

      widget = new Label(root);
      widget.addStyleName(STYLE_DAY_PICTURE);
      widget.addStyleName(STYLE_DAY_PICTURE + styleSuffix);
    }

    return widget;
  }

  private static Widget createDaySeparator(JustDate date, int index, int count) {
    String styleName;
    if (index == count - 1) {
      styleName = STYLE_RIGHT_SEPARATOR;
    } else if (date.getDom() == 1) {
      styleName = STYLE_MONTH_SEPARATOR;
    } else {
      styleName = STYLE_DAY_SEPARATOR;
    }

    return new CustomDiv(styleName);
  }

  private static int getDaySeparatorWidth(JustDate date, int dayWidth, int index, int count) {
    if (DAY_SEPARATOR_WIDTH > 0
        && dayWidth > DAY_SEPARATOR_WIDTH * 2
        && (index == count - 1 || date.getDom() == 1
        || TimeUtils.isMore(date, TimeUtils.today())
        && dayWidth >= MIN_DAY_WIDTH_FOR_SEPARATOR)) {
      return DAY_SEPARATOR_WIDTH;
    } else {
      return 0;
    }
  }

  private static boolean isEmpty(Object value) {
    if (value == null) {
      return true;
    } else if (value instanceof String) {
      return BeeUtils.isEmpty((String) value);
    } else {
      return false;
    }
  }

  private static Widget renderDate(JustDate date, String styleName, int width, int height) {
    Size maxSize = new Size(width, height);

    String text = Format.renderDate(date);
    Size size = Rulers.getLineSize(null, text, false);

    if (maxSize.encloses(size)) {
      return renderLabel(text, styleName);

    } else if (BeeUtils.betweenExclusive(size.getHeight(), height - 3, height)) {
      return renderDate(date, styleName, new Size(width, size.getHeight()));

    } else {
      return renderDate(date, styleName, maxSize);
    }
  }

  private static Widget renderDate(JustDate date, String styleName, Size maxSize) {
    String text = Format.renderDate(date);
    Size size = Rulers.getLineSize(null, text, false);

    if (size.getWidth() > maxSize.getWidth()) {
      text = Format.renderDateCompact(date);
      size = Rulers.getLineSize(null, text, false);

      if (size.getWidth() > maxSize.getWidth()) {
        text = Format.render(PredefinedFormat.MONTH_NUM_DAY, date);
        size = Rulers.getLineSize(null, text, false);
      }
    }

    Widget widget = renderLabel(text, styleName);

    if (!maxSize.encloses(size) && size.isValid()) {
      double x = Math.min((double) maxSize.getWidth() / size.getWidth(), 1.0);
      double y = Math.min((double) maxSize.getHeight() / size.getHeight(), 1.0);

      StyleUtils.setTransformScale(widget, x, y);
    }

    return widget;
  }

  private static Widget renderLabel(String text, String styleName) {
    Label widget = new Label(text);
    if (!BeeUtils.isEmpty(styleName)) {
      widget.addStyleName(styleName);
    }

    return widget;
  }

  private static Pair<Widget, Widget> renderRange(JustDate start, String startStyle,
      JustDate end, String endStyle, int width, int height) {

    Size maxSize = new Size(width, height);

    String startText = Format.renderDate(start);
    Size startSize = Rulers.getLineSize(null, startText, false);

    String endText = Format.renderDate(end);
    Size endSize = Rulers.getLineSize(null, endText, false);

    if (maxSize.encloses(startSize, endSize)) {
      return Pair.of(renderLabel(startText, startStyle), renderLabel(endText, endStyle));
    }

    startText = Format.renderDateCompact(start);
    startSize = Rulers.getLineSize(null, startText, false);

    endText = Format.renderDateCompact(end);
    endSize = Rulers.getLineSize(null, endText, false);

    if (maxSize.encloses(startSize, endSize)) {
      return Pair.of(renderLabel(startText, startStyle), renderLabel(endText, endStyle));
    }

    if (start.getYear() == end.getYear()) {
      endText = Format.render(PredefinedFormat.MONTH_NUM_DAY, end);

      if (maxSize.encloses(startSize, endSize)) {
        return Pair.of(renderLabel(startText, startStyle), renderLabel(endText, endStyle));
      }
    }

    if (width >= height) {
      double z = (double) startSize.getWidth() / (startSize.getWidth() + endSize.getWidth());
      int startWidth = BeeUtils.clamp(BeeUtils.round(z * width), 1, width - 1);
      int endWidth = width - startWidth;

      Widget startWidget = renderDate(start, startStyle, startWidth, height);
      Widget endWidget = renderDate(end, endStyle, endWidth, height);

      return Pair.of(startWidget, endWidget);

    } else {
      int startHeight = height / 2 + 1;
      int endHeight = height - startHeight;

      Widget startWidget = renderDate(start, startStyle, width, startHeight);
      Widget endWidget = renderDate(end, endStyle, width, endHeight);

      return Pair.of(startWidget, endWidget);
    }
  }

  private TimeBoardHelper() {
  }
}
