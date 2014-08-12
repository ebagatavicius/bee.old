package com.butent.bee.client.modules.calendar.layout;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.calendar.CalendarFormat;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.calendar.CalendarSettings;

public class Timeline extends Flow {

  private int nowIndex = BeeConst.UNDEF;

  public Timeline() {
    super();
  }

  public void build(CalendarSettings settings) {
    clear();

    int labelHeight = settings.getHourHeight();

    for (int i = 0; i < 24; i++) {
      String hour = CalendarFormat.getHourLabels()[i];

      Simple hourWrapper = new Simple();
      hourWrapper.addStyleName(CalendarStyleManager.HOUR_PANEL);
      StyleUtils.setHeight(hourWrapper, labelHeight - 1);

      Flow flowPanel = new Flow();
      flowPanel.addStyleName(CalendarStyleManager.HOUR_LAYOUT);

      Label hourLabel = new Label(hour);
      hourLabel.addStyleName(CalendarStyleManager.HOUR_LABEL);
      flowPanel.add(hourLabel);

      hourWrapper.add(flowPanel);
      add(hourWrapper);
    }

    CustomDiv now = new CustomDiv();
    now.addStyleName(CalendarStyleManager.NOW_POINTER);
    add(now);
    setNowIndex(getWidgetCount() - 1);

    onClock(settings);
  }

  public void onClock(CalendarSettings settings) {
    if (getNowIndex() >= 0) {
      int y = CalendarUtils.getNowY(settings);
      StyleUtils.setTop(getWidget(getNowIndex()), y);
    }
  }

  private int getNowIndex() {
    return nowIndex;
  }

  private void setNowIndex(int nowIndex) {
    this.nowIndex = nowIndex;
  }
}
