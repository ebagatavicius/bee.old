package com.butent.bee.egg.client.logging;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.egg.client.widget.BeeHtml;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogWidgetHandler extends Handler {
  public static Formatter getDefaultFormatter() {
    return new LogFormatter();
  }

  public static Level getDefaultLevel() {
    return Level.ALL;
  }

  private HasWidgets container;

  private int counter = 0;

  public LogWidgetHandler() {
    super();
  }

  public LogWidgetHandler(HasWidgets cont) {
    this(cont, getDefaultFormatter(), getDefaultLevel());
  }

  public LogWidgetHandler(HasWidgets cont, Formatter format) {
    this(cont, format, getDefaultLevel());
  }

  public LogWidgetHandler(HasWidgets cont, Formatter format, Level lvl) {
    this.container = cont;
    setFormatter(format);
    setLevel(lvl);
  }

  public LogWidgetHandler(HasWidgets cont, Level lvl) {
    this(cont, getDefaultFormatter(), lvl);
  }

  public void clear() {
    container.clear();
  }

  @Override
  public void close() {
  }

  @Override
  public void flush() {
  }

  @Override
  public void publish(LogRecord record) {
    if (!isLoggable(record)) {
      return;
    }

    Formatter frmt = getFormatter();
    if (frmt == null) {
      return;
    }

    if (frmt instanceof LogFormatter
        && ((LogFormatter) frmt).isSeparator(record)) {
      Element elem = Document.get().createDivElement().cast();
      container.add(new BeeHtml(elem));
      elem.setClassName("bee-LogSeparator");

      return;
    }

    String msg = frmt.format(record);
    if (!BeeUtils.isEmpty(msg)) {
      counter++;
    }

    Element elem = Document.get().createDivElement().cast();
    elem.setInnerText(BeeUtils.concat(1, counter, msg));
    container.add(new BeeHtml(elem));
    elem.setClassName("bee-LogRecord");
  }

  public void setDefaultFormatter() {
    setFormatter(getDefaultFormatter());
  }

  public void setDefaultLevel() {
    setLevel(getDefaultLevel());
  }
}
