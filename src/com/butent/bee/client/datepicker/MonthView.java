package com.butent.bee.client.datepicker;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.impl.ElementMapperImpl;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.datepicker.DatePicker.CssClasses;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.List;

class MonthView extends Component implements HasKeyDownHandlers {

  private final class DayGrid extends HtmlTable {

    private final class Cell extends UIObject implements EnablableWidget {

      private final int index;
      private final JustDate value = new JustDate();

      private final String cellStyle;
      private String dateStyle;

      private boolean enabled = true;

      private Cell(int index, Element elem, boolean isWeekend) {
        this.index = index;
        this.cellStyle =
            isWeekend ? StyleUtils.buildClasses(css().day(), css().dayIsWeekend()) : css().day();

        setElement(elem);
      }

      @Override
      public void addStyleName(String styleName) {
        Assert.notNull(styleName);
        setDateStyle(StyleUtils.addClassName(getDateStyle(), styleName));
        updateStyle();
      }

      @Override
      public boolean isEnabled() {
        return enabled;
      }

      @Override
      public void removeStyleName(String styleName) {
        Assert.notEmpty(styleName);
        setDateStyle(StyleUtils.removeClassName(getDateStyle(), styleName));
        updateStyle();
      }

      @Override
      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      private String getDateStyle() {
        return dateStyle;
      }

      private boolean isFiller() {
        return !getModel().isInCurrentMonth(value);
      }

      private void setDateStyle(String dateStyle) {
        this.dateStyle = dateStyle;
      }

      private void setHtml(String html) {
        getElement().setInnerHTML(html);
      }

      private void update(JustDate date, boolean enable) {
        setEnabled(enable);

        value.setDate(date);
        setHtml(Model.formatDayOfMonth(value));

        StringList styles = StringList.uniqueCaseInsensitive();
        styles.add(cellStyle);

        if (isFiller()) {
          styles.add(css().dayIsFiller());
        } else {
          String extraStyle = getDatePicker().getStyleOfDate(date);
          if (extraStyle != null) {
            styles.add(extraStyle);
          }
        }

        setDateStyle(StyleUtils.buildClasses(styles));
        updateStyle();
      }

      private void updateStyle() {
        StringList styles = StringList.uniqueCaseInsensitive();
        styles.add(dateStyle);

        if (getActiveCellIndex() == index) {
          styles.add(css().dayIsActive());
        }
        if (!isEnabled()) {
          styles.add(css().dayIsDisabled());
        }

        setStyleName(StyleUtils.buildClasses(styles));
      }
    }

    private final ElementMapperImpl<Cell> elementToCell = new ElementMapperImpl<>();
    private final List<Cell> cellList = new ArrayList<>();

    private int activeCellIndex = BeeConst.UNDEF;

    private DayGrid() {
      sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONKEYDOWN);
      DomUtils.makeFocusable(this);

      CellFormatter formatter = getCellFormatter();

      for (int i = 0; i < DAYS_IN_WEEK; i++) {
        setHtml(0, i, Model.formatDayOfWeek(i));

        if (TimeUtils.isWeekend(i + 1)) {
          formatter.setStyleName(0, i, css().weekendLabel());
        } else {
          formatter.setStyleName(0, i, css().weekdayLabel());
        }
      }

      int index = 0;
      for (int row = 1; row <= WEEKS_IN_MONTH; row++) {
        for (int column = 0; column < DAYS_IN_WEEK; column++) {
          Element element = formatter.ensureElement(row, column);
          Cell cell = new Cell(index++, element, TimeUtils.isWeekend(column + 1));

          cellList.add(cell);
          elementToCell.put(cell);
        }
      }
    }

    @Override
    public void onBrowserEvent(Event event) {
      Element e;
      Cell cell;

      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
          cell = getCell(event);
          if (isActive(cell)) {
            getDatePicker().setValue(cell.value, true);
          }
          break;

        case Event.ONMOUSEOUT:
          e = DOM.eventGetFromElement(event);
          if (e != null) {
            cell = elementToCell.get(e);
            if (cell != null && cell.index == getActiveCellIndex()) {
              activateCell(cell, false);
            }
          }
          break;

        case Event.ONMOUSEOVER:
          e = DOM.eventGetToElement(event);
          if (e != null) {
            cell = elementToCell.get(e);
            if (isActive(cell)) {
              activateCell(cell, true);
            }
          }
          break;

        case Event.ONKEYDOWN:
          if (navigate(event.getKeyCode(), EventUtils.hasModifierKey(event))) {
            event.preventDefault();
            event.stopPropagation();
          }
          break;
      }

      super.onBrowserEvent(event);
    }

    private void activateCell(Cell cell, boolean activate) {
      int oldIndex = getActiveCellIndex();
      if (activate && cell.index == oldIndex) {
        return;
      }

      setActiveCellIndex(activate ? cell.index : BeeConst.UNDEF);

      if (activate && oldIndex >= 0) {
        getCell(oldIndex).updateStyle();
      }
      cell.updateStyle();
    }

    private int getActiveCellIndex() {
      return activeCellIndex;
    }

    private Cell getCell(Event event) {
      TableCellElement cellElement =
          DomUtils.getParentCell(EventUtils.getEventTargetElement(event), true);

      while (cellElement != null) {
        Cell cell = elementToCell.get(cellElement);
        if (cell != null) {
          return cell;
        }

        cellElement = DomUtils.getParentCell(cellElement, false);
      }

      return null;
    }

    private Cell getCell(int index) {
      return cellList.get(index);
    }

    private int getNumCells() {
      return cellList.size();
    }

    private boolean isActive(Cell cell) {
      return cell != null && cell.isEnabled();
    }

    private void setActiveCellIndex(int activeCellIndex) {
      this.activeCellIndex = activeCellIndex;
    }
  }

  private static final int WEEKS_IN_MONTH = 6;

  private static final int DAYS_IN_WEEK = 7;

  private final CssClasses cssClasses;
  private final DayGrid grid;

  private final JustDate firstDisplayed = new JustDate();
  private final JustDate lastDisplayed = new JustDate();

  MonthView(CssClasses cssClasses) {
    this.cssClasses = cssClasses;
    this.grid = new DayGrid();
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return Binder.addKeyDownHandler(grid, handler);
  }

  @Override
  protected void refresh() {
    JustDate start = getModel().getCurrentMonth().getDate();
    int incr = (start.getDow() == 1) ? -1 : 0;
    firstDisplayed.setDate(TimeUtils.startOfWeek(start, incr));

    grid.setActiveCellIndex(BeeConst.UNDEF);

    int days = firstDisplayed.getDays();
    for (int i = 0; i < grid.getNumCells(); i++) {
      lastDisplayed.setDays(days + i);
      grid.getCell(i).update(lastDisplayed, getDatePicker().isDateEnabled(lastDisplayed));
    }
  }

  @Override
  protected void setUp() {
    initWidget(grid);
    grid.setStyleName(css().days());
  }

  void addStyleToDate(String styleName, JustDate date) {
    Assert.state(getDatePicker().isDateVisible(date));
    getCell(date).addStyleName(styleName);
  }

  JustDate getFirstDate() {
    return firstDisplayed;
  }

  JustDate getLastDate() {
    return lastDisplayed;
  }

  void removeStyleFromDate(String styleName, JustDate date) {
    if (getDatePicker().isDateVisible(date)) {
      getCell(date).removeStyleName(styleName);
    }
  }

  void setFocus(boolean focus) {
    DomUtils.setFocus(grid, focus);
  }

  private JustDate clamp(JustDate date) {
    return TimeUtils.clamp(date, getDatePicker().getMinDate(), getDatePicker().getMaxDate());
  }

  private CssClasses css() {
    return cssClasses;
  }

  private DayGrid.Cell getCell(JustDate date) {
    int index = date.getDays() - firstDisplayed.getDays();
    DayGrid.Cell cell = grid.getCell(index);
    Assert.state(cell.value.equals(date));
    return cell;
  }

  private boolean isIndex(int index) {
    return BeeUtils.betweenExclusive(index, 0, grid.getNumCells());
  }

  private boolean navigate(int keyCode, boolean hasModifiers) {
    int oldIndex = grid.getActiveCellIndex();
    if (BeeConst.isUndef(oldIndex)) {
      int z = TimeUtils.dayDiff(firstDisplayed, getDatePicker().getValue());
      if (isIndex(z)) {
        oldIndex = z;
      }
    }
    int newIndex = BeeConst.UNDEF;

    int increment = 0;
    JustDate newDate = null;
    boolean ok = false;

    DayGrid.Cell cell = null;

    switch (keyCode) {
      case KeyCodes.KEY_ENTER:
        if (oldIndex >= 0) {
          cell = grid.getCell(oldIndex);
          if (grid.isActive(cell)) {
            getDatePicker().setValue(cell.value, true);
            ok = true;
          }
        } else {
          newIndex = 0;
        }
        break;

      case KeyCodes.KEY_LEFT:
        increment = -1;
        break;

      case KeyCodes.KEY_RIGHT:
        increment = 1;
        break;

      case KeyCodes.KEY_UP:
        increment = -7;
        break;

      case KeyCodes.KEY_DOWN:
        increment = 7;
        break;

      case KeyCodes.KEY_HOME:
        JustDate today = TimeUtils.today();
        int z = TimeUtils.dayDiff(firstDisplayed, today);
        if (isIndex(z)) {
          newIndex = (z == oldIndex) ? 0 : z;
        } else {
          newDate = clamp(today);
        }
        break;

      case KeyCodes.KEY_END:
        newIndex = grid.getNumCells() - 1;
        if (newIndex == oldIndex) {
          newIndex = 0;
        }
        break;

      case KeyCodes.KEY_PAGEUP:
        newDate = clamp(hasModifiers ? TimeUtils.endOfMonth(getModel().getCurrentMonth(), -12)
            : TimeUtils.endOfPreviousMonth(getModel().getCurrentMonth()));
        break;

      case KeyCodes.KEY_PAGEDOWN:
        newDate = clamp(hasModifiers ? TimeUtils.startOfMonth(getModel().getCurrentMonth(), 12)
            : TimeUtils.startOfNextMonth(getModel().getCurrentMonth()));
        break;
    }
    if (ok) {
      return ok;
    }

    if (newIndex >= 0 && newIndex != oldIndex) {
      cell = grid.getCell(newIndex);
      ok = true;

    } else if (increment != 0) {
      newIndex = (oldIndex >= 0) ? oldIndex + increment : 0;
      if (isIndex(newIndex)) {
        cell = grid.getCell(newIndex);
      } else {
        newDate = clamp(TimeUtils.nextDay(firstDisplayed, newIndex));
      }
      ok = true;
    }

    if (newDate != null) {
      if (getDatePicker().isDateEnabled(newDate)) {
        getDatePicker().setCurrentMonth(YearMonth.of(newDate));
        cell = grid.getCell(TimeUtils.dayDiff(firstDisplayed, newDate));
      }
      ok = true;
    }

    if (grid.isActive(cell)) {
      grid.activateCell(cell, true);
    }

    return ok;
  }
}
