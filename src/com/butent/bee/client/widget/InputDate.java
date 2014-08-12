package com.butent.bee.client.widget;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasDateTimeFormat;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.HasIntStep;
import com.butent.bee.shared.State;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class InputDate extends InputText implements HasDateTimeFormat, HasIntStep, HasBounds {

  protected static final JustDate DEFAULT_MIN_DATE = new JustDate(2000, 1, 1);
  protected static final JustDate DEFAULT_MAX_DATE = TimeUtils.endOfYear(TimeUtils.year(), 10);

  private static final String STYLE_INPUT = "bee-InputDate";
  private static final String STYLE_ACTIVE = STYLE_INPUT + "-active";

  private static final String STYLE_POPUP = "bee-DateBox-popup";

  private DateTimeFormat format;

  private HasDateValue minDate;
  private HasDateValue maxDate;

  private int stepValue = BeeConst.UNDEF;

  private State pickerState = State.CLOSED;

  public InputDate() {
    super();
    sinkEvents(Event.ONCLICK | Event.ONKEYPRESS | Event.ONFOCUS | Event.ONBLUR);
  }

  public JustDate getDate() {
    String v = getValue();
    if (BeeUtils.isEmpty(v)) {
      return null;
    } else {
      return Format.parseDateQuietly(getDateTimeFormat(), v);
    }
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  @Override
  public String getIdPrefix() {
    return "date";
  }

  public HasDateValue getMaxDate() {
    return maxDate;
  }

  @Override
  public String getMaxValue() {
    return (getMaxDate() == null) ? null : getMaxDate().toString();
  }

  public HasDateValue getMinDate() {
    return minDate;
  }

  @Override
  public String getMinValue() {
    return (getMinDate() == null) ? null : getMinDate().toString();
  }

  @Override
  public String getNormalizedValue() {
    JustDate date = getDate();
    return (date == null) ? null : date.serialize();
  }

  @Override
  public int getStepValue() {
    return stepValue;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_DATE;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    setDate(TimeUtils.toDateOrNull(normalizedValue));
  }

  @Override
  public void onBrowserEvent(Event event) {
    String type = event.getType();

    if (EventUtils.isClick(type)) {
      if (isPickerClosing()) {
        setPickerState(State.CLOSED);
      } else if (isPickerClosed()) {
        event.preventDefault();
        event.stopPropagation();
        pickDate();
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

  public void setDate(HasDateValue date) {
    String text;
    if (date == null) {
      text = BeeConst.STRING_EMPTY;
    } else if (getDateTimeFormat() == null) {
      text = date.getDate().toString();
    } else {
      text = getDateTimeFormat().format(date.getDate());
    }
    setText(text);
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat dtFormat) {
    this.format = dtFormat;
  }

  public void setMaxDate(HasDateValue maxDate) {
    this.maxDate = maxDate;
  }

  @Override
  public void setMaxValue(String maxValue) {
    setMaxDate(TimeUtils.parseDate(maxValue));
  }

  public void setMinDate(HasDateValue minDate) {
    this.minDate = minDate;
  }

  @Override
  public void setMinValue(String minValue) {
    setMinDate(TimeUtils.parseDate(minValue));
  }

  @Override
  public void setStepValue(int stepValue) {
    this.stepValue = stepValue;
  }

  @Override
  public void setValue(String value) {
    setDate(TimeUtils.toDateOrNull(value));
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
    List<String> messages = Lists.newArrayList();
    messages.addAll(super.validate(checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (isEmpty()) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.getConstants().enterDate());
      }
      return messages;
    }

    String v = BeeUtils.trim(getValue());
    if (getDateTimeFormat() == null) {
      if (!willParse(v)) {
        messages.add(BeeUtils.joinWords(Localized.getConstants().invalidDate(), v));
      }
    } else {
      if (getDateTimeFormat().parseQuietly(v) == null) {
        messages.add(Localized.getConstants().invalidDateFormat());
        messages.add(BeeUtils.joinWords(v, BeeUtils.bracket(getDateTimeFormat().getPattern())));
      }
    }

    if (messages.isEmpty()) {
      messages.addAll(validateBounds(getDateValue()));
    }
    return messages;
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    List<String> messages = Lists.newArrayList();
    messages.addAll(super.validate(normalizedValue, checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (BeeUtils.isEmpty(normalizedValue)) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.getConstants().enterDate());
      }
      return messages;
    }

    if (messages.isEmpty()) {
      messages.addAll(validateBounds(getDateValue(normalizedValue)));
    }
    return messages;
  }

  protected boolean checkBounds(HasDateValue value) {
    JustDate date = JustDate.get(value);
    if (date == null) {
      return isNullable();
    }

    JustDate min = JustDate.get(getMinBound());
    if (min != null && TimeUtils.isLess(date, min)) {
      return false;
    }

    JustDate max = JustDate.get(getMaxBound());
    if (max != null && TimeUtils.isMore(date, max)) {
      return false;
    }
    return true;
  }

  protected HasDateValue getDateValue() {
    return getDate();
  }

  protected HasDateValue getDateValue(String normalizedValue) {
    return TimeUtils.toDateOrNull(normalizedValue);
  }

  @Override
  protected String getDefaultStyleName() {
    return STYLE_INPUT;
  }

  protected HasDateValue getMaxBound() {
    return BeeUtils.nvl(getMaxDate(), DEFAULT_MAX_DATE);
  }

  protected HasDateValue getMinBound() {
    return BeeUtils.nvl(getMinDate(), DEFAULT_MIN_DATE);
  }

  protected boolean handleChar(int charCode) {
    if (charCode == BeeConst.CHAR_ASTERISK) {
      pickDate();
      return true;
    }

    if (!Character.isLetter(BeeUtils.toChar(charCode))
        && !BeeUtils.inList(charCode, BeeConst.CHAR_PLUS, BeeConst.CHAR_MINUS)) {
      return false;
    }

    JustDate oldDate = getDate();
    JustDate baseDate = (oldDate == null) ? new JustDate() : oldDate;
    JustDate newDate = null;

    switch (charCode) {
      case 'a':
      case 'A':
      case 'p':
      case 'P':
        newDate = TimeUtils.getDate(baseDate, 2);
        break;

      case 'b':
      case 'B':
      case 'u':
      case 'U':
        newDate = TimeUtils.getDate(baseDate, -2);
        break;

      case 'd':
      case 'D':
        newDate = TimeUtils.today();
        break;

      case 'r':
      case 'R':
      case 'o':
      case 'O':
        newDate = TimeUtils.today(1);
        break;

      case 'e':
      case 'E':
      case 'v':
      case 'V':
        newDate = TimeUtils.today(-1);
        break;

      case 'f':
        newDate = TimeUtils.endOfPreviousMonth(baseDate);
        break;

      case 'F':
        newDate = TimeUtils.endOfMonth(baseDate);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.endOfMonth(oldDate, 1);
        }
        break;

      case 'm':
        newDate = TimeUtils.startOfMonth(baseDate);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfMonth(oldDate, -1);
        }
        break;

      case 'M':
        newDate = TimeUtils.startOfNextMonth(baseDate);
        break;

      case 'n':
        int step = (baseDate.getDom() == 1) ? -2 : -1;
        newDate = TimeUtils.startOfMonth(baseDate, step);
        break;

      case 'N':
        newDate = TimeUtils.startOfMonth(baseDate, 2);
        break;

      case 'q':
      case 'k':
        newDate = TimeUtils.startOfQuarter(baseDate);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfQuarter(oldDate, -1);
        }
        break;

      case 'Q':
      case 'K':
        newDate = TimeUtils.startOfQuarter(baseDate, 1);
        break;

      case 't':
      case 'T':
      case 'l':
      case 'L':
        newDate = TimeUtils.today();
        break;

      case 'w':
      case 's':
        newDate = TimeUtils.startOfWeek(baseDate);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfWeek(oldDate, -1);
        }
        break;

      case 'W':
      case 'S':
        newDate = TimeUtils.startOfWeek(baseDate, 1);
        break;

      case 'y':
        newDate = TimeUtils.startOfYear(baseDate);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfYear(oldDate, -1);
        }
        break;

      case 'Y':
        newDate = TimeUtils.startOfYear(baseDate, 1);
        break;

      case '+':
      case '-':
        int cnt = TimeUtils.countFields(getValue());
        if (cnt == 0 || cnt >= 3) {
          int incr = (charCode == '+') ? 1 : -1;
          if (getStepValue() > 1) {
            incr *= getStepValue();
          }

          if (oldDate == null) {
            newDate = TimeUtils.today(incr);
          } else {
            newDate = TimeUtils.getDate(oldDate, incr);
          }
        }
        break;
    }

    if (newDate == null) {
      return false;
    }

    newDate = clamp(newDate);
    if (!TimeUtils.sameDate(newDate, oldDate)) {
      setDate(newDate);
    }
    return true;
  }

  protected void pickDate() {
    final Popup popup = new Popup(OutsideClick.CLOSE, STYLE_POPUP);

    JustDate date = getDate();
    if (date == null) {
      date = new JustDate();
    }

    JustDate min = JustDate.get(getMinBound());
    JustDate max = JustDate.get(getMaxBound());

    DatePicker picker = new DatePicker(TimeUtils.clamp(date, min, max), min, max);

    picker.addValueChangeHandler(new ValueChangeHandler<JustDate>() {
      @Override
      public void onValueChange(ValueChangeEvent<JustDate> event) {
        setDate(event.getValue());
        popup.close();

        fireEvent(new EditStopEvent(State.CHANGED));
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
            DomEvent.fireNativeEvent(Document.get().createBlurEvent(), InputDate.this);
          }

        } else if (event.keyboardEvent()) {
          setPickerState(State.CLOSING);
          setFocus(true);
          selectAll();

        } else {
          setPickerState(State.CLOSED);
        }

        InputDate.this.removeStyleName(STYLE_ACTIVE);
      }
    });

    setPickerState(State.OPEN);
    addStyleName(STYLE_ACTIVE);

    popup.setWidget(picker);
    popup.showRelativeTo(getElement());

    picker.setFocus(true);
  }

  protected boolean willParse(String text) {
    return Format.parseDateQuietly(getDateTimeFormat(), text) != null;
  }

  private JustDate clamp(JustDate date) {
    JustDate min = JustDate.get(getMinBound());
    JustDate max = JustDate.get(getMaxBound());

    return TimeUtils.clamp(date, min, max);
  }

  private State getPickerState() {
    return pickerState;
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

  private void setPickerState(State pickerState) {
    this.pickerState = pickerState;
  }

  private List<String> validateBounds(HasDateValue dateValue) {
    List<String> result = Lists.newArrayList();

    if (dateValue != null && !checkBounds(dateValue)) {
      result.add(TimeUtils.renderCompact(dateValue));
      result.addAll(ValidationHelper.getBounds(TimeUtils.renderCompact(getMinBound()),
          TimeUtils.renderCompact(getMaxBound())));
    }
    return result;
  }
}
