package com.butent.bee.egg.client;

import java.util.Date;

import com.butent.bee.egg.client.widget.BeeLabel;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.Timer;

public class BeeScheduler implements BeeModule {

  class ClockTimer extends Timer {
    @Override
    public void run() {
      updateClock();
    }
  }

  private BeeLabel clockLabel = new BeeLabel();
  private Timer clockTimer = new ClockTimer();

  public BeeScheduler() {
    clockTimer.scheduleRepeating(1000);
  }

  private void updateClock() {
    clockLabel.setText(DateTimeFormat
        .getFormat(PredefinedFormat.DATE_TIME_FULL).format(new Date()));
  }

  public String getName() {
    return getClass().getName();
  }

  public int getPriority(int p) {
    switch (p) {
    case PRIORITY_INIT:
      return DO_NOT_CALL;
    case PRIORITY_START:
      return DO_NOT_CALL;
    case PRIORITY_END:
      return DO_NOT_CALL;
    default:
      return DO_NOT_CALL;
    }
  }

  public void init() {
  }

  public void start() {
  }

  public void end() {
  }

}
