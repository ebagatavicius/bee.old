package com.butent.bee.client.logging;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasVisibility;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Settings;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Manages content of log information showing user interface component.
 */

public class PanelHandler extends Handler implements HasVisibility, HasEnabled {

  private static final String STYLENAME_DEFAULT = BeeConst.CSS_CLASS_PREFIX + "LogRecord";
  private static final String STYLENAME_SEPARATOR = BeeConst.CSS_CLASS_PREFIX + "LogSeparator";

  private static final String STORAGE_KEY = "logSize";

  private final Flow panel;

  private final int capacity;

  private int hiddenSize = BeeConst.UNDEF;

  private boolean enabled = true;

  public PanelHandler(Flow panel, Formatter formatter, Level level) {
    this.panel = panel;
    this.capacity = Settings.getLogCapacity();

    setFormatter(formatter);
    if (level != null) {
      setLevel(level);
    }
  }

  public PanelHandler(Level level) {
    this(new LogPanel(), new LogFormatter(), level);
  }

  public void clear() {
    panel.clear();
  }

  @Override
  public void close() {
    setEnabled(false);
  }

  @Override
  public void flush() {
  }

  public int getInitialSize() {
    return BeeUtils.unbox(BeeKeeper.getStorage().getInteger(STORAGE_KEY));
  }

  public Flow getPanel() {
    return panel;
  }

  public boolean isEmpty() {
    return panel.isEmpty();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isVisible() {
    return getSize() > 0;
  }

  @Override
  public void publish(LogRecord record) {
    if (!isEnabled() || !isLoggable(record)) {
      return;
    }

    Formatter frmt = getFormatter();
    if (frmt == null) {
      return;
    }

    if (capacity > 0 && panel.getWidgetCount() >= capacity) {
      panel.remove(0);
    }

    if (frmt instanceof LogFormatter && ((LogFormatter) frmt).isSeparator(record)) {
      Element elem = Document.get().createDivElement().cast();
      panel.add(new CustomWidget(elem));
      elem.setClassName(STYLENAME_SEPARATOR);

      if (isVisible()) {
        elem.scrollIntoView();
      }

      return;
    }

    String msg = frmt.format(record);

    Element elem = Document.get().createDivElement().cast();
    elem.setInnerText(msg);
    elem.setClassName(STYLENAME_DEFAULT);
    elem.addClassName(STYLENAME_DEFAULT + BeeConst.STRING_MINUS
        + record.getLevel().getName().toLowerCase());

    panel.add(new CustomWidget(elem));
  }

  public void resize(int size) {
    Widget parent = getPanel().getParent();
    if (parent instanceof Split) {
      if (size <= 0) {
        hiddenSize = BeeUtils.positive(getSize(), 256);
      } else {
        hiddenSize = BeeConst.UNDEF;
      }

      ((Split) parent).setWidgetSize(getPanel(), size);
      BeeKeeper.getStorage().set(STORAGE_KEY, size);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      if (hiddenSize > 0) {
        resize(hiddenSize);
      }
    } else {
      resize(0);
    }
  }

  private int getSize() {
    Widget parent = getPanel().getParent();
    if (parent instanceof Split) {
      return ((Split) parent).getWidgetSize(getPanel());
    } else {
      return BeeConst.UNDEF;
    }
  }
}
