package com.butent.bee.client.widget;

import com.google.common.base.CharMatcher;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.HasIntStep;
import com.butent.bee.shared.State;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class InputTime extends InputText implements HasBounds, HasIntStep {

  private static final String STYLE_INPUT = BeeConst.CSS_CLASS_PREFIX + "InputTime";
  private static final String STYLE_ACTIVE = STYLE_INPUT + "-active";

  private static final String STYLE_POPUP = BeeConst.CSS_CLASS_PREFIX + "TimeBox-popup";
  private static final String STYLE_TIME_PICKER = BeeConst.CSS_CLASS_PREFIX + "TimePicker";

  private static final int DEFAULT_PICKER_SIZE = 10;

  private static final int DEFAULT_STEP = 30;

  private String minValue;
  private String maxValue;

  private int stepValue = BeeConst.UNDEF;

  private State pickerState = State.CLOSED;
  private String lastEventType;

  public InputTime() {
    super();

    setMaxLength(getDefaultMaxLength());
    sinkEvents(Event.ONCLICK | Event.ONKEYPRESS | Event.ONFOCUS | Event.ONBLUR | Event.ONMOUSEDOWN);
  }

  @Override
  public String getIdPrefix() {
    return "time";
  }

  @Override
  public String getMaxValue() {
    return maxValue;
  }

  public Long getMillis() {
    return TimeUtils.parseTime(getValue());
  }

  public Integer getMinutes() {
    return toMinutes(getMillis());
  }

  @Override
  public String getMinValue() {
    return minValue;
  }

  public int getNormalizedStep() {
    return BeeUtils.positive(getStepValue(), DEFAULT_STEP);
  }

  @Override
  public String getNormalizedValue() {
    Long millis = getMillis();
    return (millis == null) ? null : TimeUtils.renderTime(millis, true);
  }

  @Override
  public int getStepValue() {
    return stepValue;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_TIME;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    Long millis = TimeUtils.parseTime(normalizedValue);

    if (millis == null) {
      clearValue();
    } else {
      setValue(TimeUtils.renderTime(millis, true));
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    String type = event.getType();

    String last = getLastEventType();
    setLastEventType(type);

    if (EventUtils.isClick(type)) {
      if (isPickerClosing()) {
        setPickerState(State.CLOSED);
      } else if (isPickerClosed() && EventUtils.isMouseDown(last)) {
        event.preventDefault();
        event.stopPropagation();
        pickTime();
        return;
      }

    } else if (EventUtils.isKeyPress(type)) {
      if (handleChar(event.getCharCode())) {
        event.preventDefault();
        event.stopPropagation();
        return;
      }

    } else if (EventUtils.isFocus(type)) {
      if (isPickerClosing()) {
        return;
      }

    } else if (EventUtils.isBlur(type)) {
      if (isPickerOpen()) {
        return;
      }
    }

    super.onBrowserEvent(event);
  }

  public void setMaxMinutes(Integer minutes) {
    if (BeeUtils.isPositive(minutes)) {
      setMaxValue(renderMinutes(minutes));
    } else {
      setMaxValue(null);
    }
  }

  @Override
  public void setMaxValue(String maxValue) {
    this.maxValue = maxValue;
  }

  public void setMinMinutes(Integer minutes) {
    if (BeeUtils.isPositive(minutes)) {
      setMinValue(renderMinutes(minutes));
    } else {
      setMinValue(null);
    }
  }

  public void setMillis(Long millis) {
    if (millis == null) {
      clearValue();
    } else {
      setValue(TimeUtils.renderTime(millis, true));
    }
  }

  public void setMinutes(int minutes) {
    if (minutes < 0) {
      clearValue();
    } else {
      setValue(renderMinutes(minutes));
    }
  }

  @Override
  public void setMinValue(String minValue) {
    this.minValue = minValue;
  }

  @Override
  public void setStepValue(int stepValue) {
    this.stepValue = stepValue;
  }

  public void setTime(DateTime dateTime) {
    if (dateTime == null) {
      clearValue();
    } else {
      setMinutes(TimeUtils.minutesSinceDayStarted(dateTime));
    }
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    setValue(oldValue);
    if (!handleChar(charCode)) {
      if (BeeUtils.isDigit(charCode)) {
        setText(BeeUtils.toString(charCode));
      } else {
        selectAll();
      }
    }
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    List<String> messages = new ArrayList<>();
    messages.addAll(super.validate(checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (isEmpty()) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.dictionary().enterTime());
      }
      return messages;
    }

    String v = BeeUtils.trim(getValue());
    if (getCharMatcher() != null && !getCharMatcher().matchesAllOf(v)) {
      messages.add(BeeUtils.joinWords(Localized.dictionary().invalidTime(), v));
      return messages;
    }

    messages.addAll(validateBounds(getMillis()));
    return messages;
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    List<String> messages = new ArrayList<>();
    messages.addAll(super.validate(normalizedValue, checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (BeeUtils.isEmpty(normalizedValue)) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.dictionary().enterTime());
      }
      return messages;
    }

    messages.addAll(validateBounds(TimeUtils.parseTime(normalizedValue)));
    return messages;
  }

  @Override
  protected CharMatcher getDefaultCharMatcher() {
    return CharMatcher.inRange(BeeConst.CHAR_ZERO, BeeConst.CHAR_NINE)
        .or(CharMatcher.is(TimeUtils.TIME_FIELD_SEPARATOR))
        .or(CharMatcher.is(TimeUtils.MILLIS_SEPARATOR));
  }

  protected int getDefaultMaxLength() {
    return 8;
  }

  protected long getDefaultMaxMillis() {
    return TimeUtils.MILLIS_PER_HOUR * 999;
  }

  @Override
  protected String getDefaultStyleName() {
    return STYLE_INPUT;
  }

  protected boolean handleChar(int charCode) {
    if (charCode == BeeConst.CHAR_ASTERISK) {
      pickTime();
      return true;
    }

    if (getCharMatcher() != null && getCharMatcher().matches(BeeUtils.toChar(charCode))) {
      return false;
    }

    int oldMinutes = BeeUtils.unbox(getMinutes());
    int newMinutes = oldMinutes;

    int incr;

    switch (charCode) {
      case 'h':
      case 'H':
        incr = (charCode == 'h') ? -1 : 1;
        newMinutes = oldMinutes + incr * TimeUtils.MINUTES_PER_HOUR;
        break;

      case 't':
      case 'T':
      case 'l':
      case 'L':
        newMinutes = TimeUtils.minutesSinceDayStarted(new DateTime());
        if (getStepValue() > 1) {
          newMinutes = BeeUtils.snap(newMinutes, getStepValue());
        } else {
          newMinutes = BeeUtils.snap(newMinutes, TimeUtils.MINUTES_PER_HOUR);
        }
        break;

      case '+':
      case '-':
        incr = (charCode == '+') ? 1 : -1;
        if (getStepValue() > 1) {
          incr *= getStepValue();
        }
        newMinutes = oldMinutes + incr;
        break;
    }

    if (newMinutes != oldMinutes) {
      setMinutes(clamp(newMinutes));
    }
    return true;
  }

  protected void pickTime() {
    Integer minutes = getMinutes();

    Integer min = getMinMinutes();
    Integer max = getMaxMinutes();

    int step = getNormalizedStep();

    int start = 0;
    if (BeeUtils.isPositive(min)) {
      start = min;
    } else if (step <= TimeUtils.MINUTES_PER_HOUR) {
      if (BeeUtils.isPositive(minutes)) {
        start = minutes % step;
      }
    }

    int end;
    if (BeeUtils.isPositive(max) && max > start) {
      end = max;
    } else {
      end = TimeUtils.MINUTES_PER_DAY;
    }
    if (end <= start) {
      return;
    }

    final ListBox widget = new ListBox();
    widget.addStyleName(STYLE_TIME_PICKER);

    for (int i = start; i < end; i += step) {
      String item = renderMinutes(i);
      widget.addItem(item);
    }

    int itemCount = widget.getItemCount();
    int visibleCount = (itemCount <= DEFAULT_PICKER_SIZE * 3 / 2) ? itemCount : DEFAULT_PICKER_SIZE;

    widget.setVisibleItemCount(visibleCount);

    final Popup popup = new Popup(OutsideClick.CLOSE, STYLE_POPUP);

    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onPick(popup, widget);
      }
    });

    widget.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          onPick(popup, widget);
        }
      }
    });

    popup.setHideOnEscape(true);

    popup.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
        if (event.mouseEvent()) {
          if (event.isTarget(getElement())) {
            setPickerState(State.CLOSING);
          } else {
            setPickerState(State.CLOSED);
            DomEvent.fireNativeEvent(Document.get().createBlurEvent(), InputTime.this);
          }

        } else if (event.keyboardEvent()) {
          setPickerState(State.CLOSING);
          setFocus(true);
          selectAll();

        } else {
          setPickerState(State.CLOSED);
        }

        InputTime.this.removeStyleName(STYLE_ACTIVE);
      }
    });

    setPickerState(State.OPEN);
    addStyleName(STYLE_ACTIVE);

    if (minutes != null) {
      int index = widget.getItems().indexOf(renderMinutes(minutes));
      if (index > 0) {
        widget.setSelectedIndex(index);
      }
    }

    popup.setWidget(widget);
    popup.focusOnOpen(widget);

    popup.showRelativeTo(getElement());
  }

  private boolean checkBounds(Long millis) {
    if (millis == null) {
      return isNullable();
    }

    Long min = getMinMillis();
    if (min != null && millis < min) {
      return false;
    }

    long max = getUpperBoundMillis();
    if (millis > max) {
      return false;
    }

    return true;
  }

  private int clamp(int minutes) {
    int min = BeeUtils.nvl(getMinMinutes(), 0);
    if (minutes < min) {
      return min;
    }

    Integer max = getMaxMinutes();
    if (max == null) {
      if (minutes >= TimeUtils.MINUTES_PER_DAY) {
        return TimeUtils.MINUTES_PER_DAY - getNormalizedStep();
      }
    } else if (minutes > max) {
      return max;
    }

    return minutes;
  }

  private String getLastEventType() {
    return lastEventType;
  }

  private Long getMaxMillis() {
    return TimeUtils.parseTime(getMaxValue());
  }

  private Integer getMaxMinutes() {
    return toMinutes(getMaxMillis());
  }

  private Long getMinMillis() {
    return TimeUtils.parseTime(getMinValue());
  }

  private Integer getMinMinutes() {
    return toMinutes(getMinMillis());
  }

  private State getPickerState() {
    return pickerState;
  }

  private long getUpperBoundMillis() {
    Long millis = getMaxMillis();
    return BeeUtils.isPositive(millis) ? millis : getDefaultMaxMillis();
  }

  private boolean isPickerClosed() {
    return State.CLOSED.equals(getPickerState());
  }

  private boolean isPickerClosing() {
    return State.CLOSING.equals(getPickerState());
  }

  private boolean isPickerOpen() {
    return State.OPEN.equals(getPickerState());
  }

  private void onPick(Popup popup, ListBox widget) {
    if (widget.getSelectedIndex() >= 0) {
      String text = widget.getItemText(widget.getSelectedIndex());
      popup.close();
      setValue(text);

      fireEvent(new EditStopEvent(State.CHANGED));
    }
  }

  private static String renderMinutes(int minutes) {
    return TimeUtils.renderMinutes(minutes, true);
  }

  private void setLastEventType(String lastEventType) {
    this.lastEventType = lastEventType;
  }

  private void setPickerState(State pickerState) {
    this.pickerState = pickerState;
  }

  private static Integer toMinutes(Long millis) {
    return (millis == null) ? null : (int) (millis / TimeUtils.MILLIS_PER_MINUTE);
  }

  private List<String> validateBounds(Long millis) {
    List<String> result = new ArrayList<>();

    if (millis != null && !checkBounds(millis)) {
      result.add(TimeUtils.renderTime(millis, true));
      result.addAll(ValidationHelper.getBounds(getMinValue(),
          TimeUtils.renderTime(getUpperBoundMillis(), false)));
    }
    return result;
  }
}
