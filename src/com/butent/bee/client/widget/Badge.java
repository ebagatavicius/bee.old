package com.butent.bee.client.widget;

import com.google.gwt.core.client.Scheduler;

import com.butent.bee.shared.utils.BeeUtils;

public class Badge extends CustomDiv {

  private static final String STYLE_NAME = "bee-Badge";
  private static final String STYLE_EMPTY = STYLE_NAME + "-empty";
  private static final String STYLE_UPDATED = STYLE_NAME + "-updated";

  public Badge(int value) {
    super(STYLE_NAME);
    setValue(value);
  }

  public Badge(int value, String styleName) {
    this(value);
    if (!BeeUtils.isEmpty(styleName)) {
      addStyleName(styleName);
    }
  }

  public void decrement() {
    update(getValue() - 1);
  }

  @Override
  public String getIdPrefix() {
    return "badge";
  }

  public int getValue() {
    return BeeUtils.toInt(getText());
  }

  public void increment() {
    update(getValue() + 1);
  }

  public void setValue(int value) {
    setText(BeeUtils.toString(value));
    setStyleName(STYLE_EMPTY, value == 0);
  }

  public void update(int value) {
    setValue(value);

    if (value != 0) {
      removeStyleName(STYLE_UPDATED);
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          addStyleName(STYLE_UPDATED);
        }
      });
    }
  }
}
