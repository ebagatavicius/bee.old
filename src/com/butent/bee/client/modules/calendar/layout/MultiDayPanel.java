package com.butent.bee.client.modules.calendar.layout;

import com.google.gwt.user.client.ui.Composite;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;

public class MultiDayPanel extends Composite {

  private final Horizontal panel = new Horizontal();

  private final Flow grid = new Flow();

  public MultiDayPanel() {
    panel.addStyleName(CalendarStyleManager.MULTI_DAY_PANEL);
    initWidget(panel);

    CustomDiv leftFiller = new CustomDiv();
    panel.add(leftFiller);
    panel.addStyleToCell(leftFiller, CalendarStyleManager.TIMELINE_EMPTY_CELL);

    grid.addStyleName(CalendarStyleManager.MULTI_DAY_GRID);
    panel.add(grid);
    panel.addStyleToCell(grid, CalendarStyleManager.MULTI_DAY_GRID_CELL);

    CustomDiv rightFiller = new CustomDiv();
    panel.add(rightFiller);
    panel.addStyleToCell(rightFiller, CalendarStyleManager.SCROLLBAR_EMPTY_CELL);
    panel.setCellWidth(rightFiller, DomUtils.getScrollBarWidth());
  }

  public Flow getGrid() {
    return grid;
  }

  public void setColumns(int columnCount, int todayStartColumn, int todayEndColumn) {
    grid.clear();

    int todayWidth = CalendarLayoutManager.getTodayWidth(columnCount, todayStartColumn,
        todayEndColumn);
    int todayLeft = (todayWidth > 0)
        ? CalendarLayoutManager.getTodayLeft(columnCount, todayStartColumn) : BeeConst.UNDEF;

    if (todayWidth > 0) {
      CustomDiv today = new CustomDiv();
      today.addStyleName(CalendarStyleManager.TODAY_MARKER);
      today.addStyleName(CalendarStyleManager.TODAY);

      StyleUtils.setLeft(today, todayLeft, CssUnit.PCT);
      StyleUtils.setWidth(today, todayWidth, CssUnit.PCT);

      grid.add(today);
    }

    CalendarLayoutManager.addColumnSeparators(grid, columnCount);
  }
}
