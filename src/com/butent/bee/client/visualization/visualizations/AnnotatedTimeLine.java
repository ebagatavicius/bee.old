package com.butent.bee.client.visualization.visualizations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.ajaxloader.ArrayHelper;
import com.butent.bee.client.ajaxloader.JsDate;
import com.butent.bee.client.visualization.AbstractDataTable;
import com.butent.bee.client.visualization.AbstractDrawOptions;
import com.butent.bee.client.visualization.DateRange;
import com.butent.bee.client.visualization.Selection;
import com.butent.bee.client.visualization.events.Handler;
import com.butent.bee.client.visualization.events.RangeChangeHandler;
import com.butent.bee.client.visualization.events.ReadyHandler;
import com.butent.bee.client.visualization.events.SelectHandler;
import com.butent.bee.shared.Assert;

import java.util.Date;
import java.util.Map;

/**
 * Implements annotated time line chart visualization.
 */
public class AnnotatedTimeLine extends Visualization<AnnotatedTimeLine.Options> {

  /**
   * Contains available annotated legend positions (for example new row or same row).
   */

  public enum AnnotatedLegendPosition {
    NEW_ROW, SAME_ROW;

    @Override
    public String toString() {
      switch (this) {
        case SAME_ROW:
          return "sameRow";
        case NEW_ROW:
          return "newRow";
        default:
          Assert.untouchable();
      }
      return null;
    }
  }

  /**
   * Contains available highlight dot modes (nearest or last).
   */

  public enum HighlightDotMode {
    LAST("last"), NEAREST("nearest");
    private final String parameter;

    HighlightDotMode(String parameter) {
      this.parameter = parameter;
    }

    public String getParameter() {
      return parameter;
    }
  }

  /**
   * Sets option values for annotated time line chart.
   */

  public static class Options extends AbstractDrawOptions {
    public static Options create() {
      return JavaScriptObject.createObject().cast();
    }

    protected Options() {
    }

    public final native void setAllowHtml(boolean allowHtml) /*-{
      this.allowHtml = allowHtml;
    }-*/;

    public final native void setAllowRedraw(boolean allowRedraw) /*-{
      this.allowRedraw = allowRedraw;
    }-*/;

    public final native void setAllValuesSuffix(String suffix) /*-{
      this.allValuesSuffix = suffix;
    }-*/;

    public final native void setAnnotationsWidth(int width) /*-{
      this.annotationsWidth = width;
    }-*/;

    public final native void setColors(JsArrayString colors) /*-{
      this.colors = colors;
    }-*/;

    public final void setColors(String... colors) {
      setColors(ArrayHelper.toJsArrayString(colors));
    }

    public final native void setDateFormat(String dateFormat) /*-{
      this.dateFormat = dateFormat;
    }-*/;

    public final native void setDisplayAnnotations(boolean display) /*-{
      this.displayAnnotations = display;
    }-*/;

    public final native void setDisplayAnnotationsFilter(boolean display) /*-{
      this.displayAnnotationsFilter = display;
    }-*/;

    public final native void setDisplayDateBarSeparator(boolean display) /*-{
      this.displayDateBarSeparator = display;
    }-*/;

    public final native void setDisplayExactValues(boolean display) /*-{
      this.displayExactValues = display;
    }-*/;

    public final native void setDisplayLegendDots(boolean display) /*-{
      this.displayLegendDots = display;
    }-*/;

    public final native void setDisplayLegendValues(boolean display) /*-{
      this.displayLegendValues = display;
    }-*/;

    public final native void setDisplayRangeSelector(boolean display) /*-{
      this.displayRangeSelector = display;
    }-*/;

    public final native void setDisplayZoomButtons(boolean display) /*-{
      this.displayZoomButtons = display;
    }-*/;

    public final native void setFill(int fill) /*-{
      this.fill = fill;
    }-*/;

    public final void setHighlightDotMode(HighlightDotMode highlightDotMode) {
      setHighlightDotMode(highlightDotMode.getParameter());
    }

    public final void setLegendPosition(AnnotatedLegendPosition position) {
      setLegendPosition(position.toString());
    }

    public final native void setMax(int max) /*-{
      this.max = max;
    }-*/;

    public final native void setMin(int min) /*-{
      this.min = min;
    }-*/;

    public final native void setNumberFormat(String numberFormat) /*-{
      this.numberFormats = numberFormat;
    }-*/;

    public final void setNumberFormats(Map<Integer, String> numberFormats) {
      resetNumberFormats();
      for (Integer key : numberFormats.keySet()) {
        String numberFormat = numberFormats.get(key);
        setNumberFormats(key, numberFormat);
      }
    }

    public final void setScaleColumns(int... scaleColumns) {
      setScaleColumns(ArrayHelper.toJsArrayInteger(scaleColumns));
    }

    public final native void setScaleColumns(JsArrayInteger scaleColumns) /*-{
      this.scaleColumns = scaleColumns;
    }-*/;

    public final native void setScaleFormat(String scaleFormat) /*-{
      this.scaleFormat = scaleFormat;
    }-*/;

    public final void setScaleType(ScaleType type) {
      setScaleType(type.getParameter());
    }

    public final native void setThickness(int thickness) /*-{
      this.thickness = thickness;
    }-*/;

    public final void setWindowMode(WindowMode wmode) {
      setWindowMode(wmode.getParameter());
    }

    public final void setZoomEndTime(Date endTime) {
      setZoomEndTime(endTime.getTime());
    }

    public final void setZoomStartTime(Date startTime) {
      setZoomStartTime(startTime.getTime());
    }

    private native void resetNumberFormats() /*-{
      this.numberFormats = {};
    }-*/;

    private native void setHighlightDotMode(String highlightDotMode) /*-{
      this.highlightDot = highlightDotMode;
    }-*/;

    private native void setLegendPosition(String position) /*-{
      this.legendPosition = position;
    }-*/;

    private native void setNumberFormats(int key, String numberFormat) /*-{
      this.numberFormats[key] = numberFormat;
    }-*/;

    private native void setScaleType(String type) /*-{
      this.scaleType = type;
    }-*/;

    private native void setWindowMode(String wmode) /*-{
      this.wmode = wmode;
    }-*/;

    private native void setZoomEndTime(double endTime) /*-{
      this.zoomEndTime = new $wnd.Date(endTime);
    }-*/;

    private native void setZoomStartTime(double startTime) /*-{
      this.zoomStartTime = new $wnd.Date(startTime);
    }-*/;
  }

  /**
   * Contains available scale types for annotated time line chart.
   */

  public enum ScaleType {
    ALLFIXED("allfixed"),
    ALLMAXIMIZE("allmaximize"),
    FIXED("fixed"),
    MAXIMIZE("maximize");

    private final String parameter;

    ScaleType(String parameter) {
      this.parameter = parameter;
    }

    public String getParameter() {
      return parameter;
    }
  }

  /**
   * Contains available window modes (for example opaque ,transparent, window).
   */

  public enum WindowMode {
    OPAQUE("opaque"),
    TRANSPARENT("transparent"),
    WINDOW("window");

    private final String parameter;

    WindowMode(String parameter) {
      this.parameter = parameter;
    }

    public String getParameter() {
      return parameter;
    }
  }

  public static final String PACKAGE = "annotatedtimeline";

  public AnnotatedTimeLine(AbstractDataTable data, Options options, String width, String height) {
    super(data, options);
    setSize(width, height);
  }

  public AnnotatedTimeLine(String width, String height) {
    super();
    setSize(width, height);
  }

  public final void addRangeChangeHandler(RangeChangeHandler handler) {
    Handler.addHandler(this, "rangechange", handler);
  }

  public final void addReadyHandler(ReadyHandler handler) {
    Handler.addHandler(this, "ready", handler);
  }

  public final void addSelectHandler(SelectHandler handler) {
    Selection.addSelectHandler(this, handler);
  }

  public final JsArray<Selection> getSelections() {
    return Selection.getSelections(this);
  }

  public final DateRange getVisibleChartRange() {
    JsArray<JsDate> dates = getVisibleChartRange(getJso());
    if (dates == null) {
      return null;
    }

    Date start = JsDate.toJava(dates.get(0));
    Date end = JsDate.toJava(dates.get(1));
    return new DateRange(start, end);
  }

  public final void hideDataColumns(int... columnIndexes) {
    hideDataColumns(ArrayHelper.toJsArrayInteger(columnIndexes));
  }

  public final void hideDataColumns(JsArrayInteger columnIndexes) {
    this.hideDataColumns(getJso(), columnIndexes);
  }

  public final void setVisibleChartRange(Date startTime, Date endTime) {
    this.setVisibleChartRange(getJso(), startTime.getTime(), endTime.getTime());
  }

  public final void showDataColumns(int... columnIndexes) {
    showDataColumns(ArrayHelper.toJsArrayInteger(columnIndexes));
  }

  public final void showDataColumns(JsArrayInteger columnIndexes) {
    this.showDataColumns(getJso(), columnIndexes);
  }

  @Override
  protected native JavaScriptObject createJso(Element parent) /*-{
    return new $wnd.google.visualization.AnnotatedTimeLine(parent);
  }-*/;

  private native JsArray<JsDate> getVisibleChartRange(JavaScriptObject jso) /*-{
    var dates = jso.getVisibleChartRange();
    if (dates == null) {
      return null;
    }
    return [ dates['start'], dates['end'] ];
  }-*/;

  private native void hideDataColumns(JavaScriptObject jso, JsArrayInteger columnIndexes) /*-{
    jso.hideDataColumns(columnIndexes);
  }-*/;

  private native void setVisibleChartRange(JavaScriptObject jso,
      double startTime, double endTime) /*-{
    jso.setVisibleChartRange(new $wnd.Date(startTime), new $wnd.Date(endTime));
  }-*/;

  private native void showDataColumns(JavaScriptObject jso, JsArrayInteger columnIndexes) /*-{
    jso.showDataColumns(columnIndexes);
  }-*/;
}
