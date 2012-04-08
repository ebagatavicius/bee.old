package com.butent.bee.client.composite;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;

import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.HasDateTimeFormat;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.AbstractDate;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.DateTime;
import com.butent.bee.shared.HasDateValue;
import com.butent.bee.shared.JustDate;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.TimeUtils;

public class InputDate extends Composite implements Editor, HasDateTimeFormat {

  public static final String DEFAULT_STYLENAME = "bee-DateBox";

  private final InputText box;
  private final Popup popup;
  private DatePicker datePicker;

  private final ValueType dateType;
  private DateTimeFormat format;

  private boolean editing = false;

  public InputDate(ValueType type) {
    this(type, null);
  }

  public InputDate(ValueType type, DateTimeFormat format) {
    Assert.notNull(type, "input date: type not specified");
    Assert.isTrue(ValueType.isDateOrDateTime(type),
        "input date: invalid type " + type.getTypeCode());

    this.box = new InputText();
    this.datePicker = new DatePicker(new JustDate());
    this.popup = new Popup(true);

    this.format = format;
    this.dateType = type;

    popup.setWidget(datePicker);
    popup.setStyleName("dateBoxPopup");

    initWidget(box);
    setStyleName(DEFAULT_STYLENAME);

    datePicker.addValueChangeHandler(new ValueChangeHandler<JustDate>() {
      public void onValueChange(ValueChangeEvent<JustDate> event) {
        setDate(event.getValue());

        hideDatePicker();
        getBox().setFocus(true);

        fireEvent(new EditStopEvent(State.CHANGED));
      }
    });

    popup.addAutoHidePartner(getBox().getElement());
    popup.addCloseHandler(new CloseHandler<PopupPanel>() {
      public void onClose(CloseEvent<PopupPanel> event) {
        if (event.isAutoClosed()) {
          getBox().setFocus(true);
        }
      }
    });

    sinkEvents(Event.ONCLICK + Event.ONKEYDOWN + Event.ONKEYPRESS + Event.ONBLUR);
  }

  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  public HandlerRegistration addEditStopHandler(EditStopEvent.Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public HasDateValue getDate() {
    String v = getBox().getValue();
    if (BeeUtils.isEmpty(v)) {
      return null;
    }
    return AbstractDate.parse(getDateTimeFormat(), v, getDateType());
  }

  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  public String getId() {
    return DomUtils.getId(this);
  }

  public String getIdPrefix() {
    return "date-box";
  }

  public String getNormalizedValue() {
    HasDateValue date = getDate();
    if (date == null) {
      return null;
    }
    return date.serialize();
  }

  public int getTabIndex() {
    return getBox().getTabIndex();
  }

  public String getValue() {
    return getBox().getValue();
  }

  public boolean handlesKey(int keyCode) {
    return false;
  }

  public boolean isEditing() {
    return editing;
  }

  public boolean isEnabled() {
    return getBox().isEnabled();
  }

  public boolean isNullable() {
    return getBox().isNullable();
  }

  @Override
  public void onBrowserEvent(Event event) {
    boolean dp = getPopup().isShowing();
    String type = event.getType();

    if (dp && EventUtils.isBlur(type)) {
      return;
    }
    if (EventUtils.isClick(type)) {
      event.preventDefault();
      if (dp) {
        hideDatePicker();
      } else if (checkValue()) {
        showDatePicker();
      }
      return;
    }

    if (EventUtils.isKeyDown(type)) {
      if (dp) {
        hideDatePicker();
      }
    } else if (EventUtils.isKeyPress(type)) {
      if (handleChar(event.getCharCode())) {
        event.preventDefault();
        return;
      }
    }

    super.onBrowserEvent(event);
  }

  public void setAccessKey(char key) {
    getBox().setAccessKey(key);
  }

  public void setDate(HasDateValue date) {
    Assert.notNull(date);
    HasDateValue newValue;

    if (ValueType.DATETIME.equals(getDateType())) {
      HasDateValue oldValue = getDate();

      if (oldValue instanceof DateTime) {
        newValue = TimeUtils.combine(date, oldValue.getDateTime());
      } else {
        newValue = DateTime.get(date);
      }
    } else {
      newValue = JustDate.get(date);
    }

    setValue(newValue);
  }

  public void setDateTimeFormat(DateTimeFormat format) {
    this.format = format;
  }

  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  public void setEnabled(boolean enabled) {
    getBox().setEnabled(enabled);
  }

  public void setFocus(boolean focused) {
    getBox().setFocus(focused);
  }

  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setNullable(boolean nullable) {
    getBox().setNullable(nullable);
  }

  public void setTabIndex(int index) {
    getBox().setTabIndex(index);
  }

  public void setValue(String value) {
    setValue(value, false);
  }

  public void setValue(String value, boolean fireEvents) {
    HasDateValue oldValue = getDate();
    HasDateValue newValue = AbstractDate.restore(value, getDateType());
    setValue(newValue);

    if (fireEvents && !TimeUtils.equals(oldValue, newValue)) {
      ValueChangeEvent.fire(this, value);
    }
  }

  public void startEdit(String oldValue, char charCode, EditorAction onEntry) {
    setValue(oldValue);
    if (handleChar(charCode)) {
      return;
    }

    EditorAction action = (onEntry == null) ? EditorAction.REPLACE : onEntry;
    UiHelper.doEditorAction(getBox(), getBox().getValue(), charCode, action);
  }

  public String validate() {
    String v = getBox().getValue();
    if (BeeUtils.isEmpty(v)) {
      return null;
    }

    String msg = null;

    if (getDateTimeFormat() != null) {
      try {
        DateTime date = getDateTimeFormat().parse(v.trim());
        if (date == null) {
          msg = "cannot parse " + v.trim();
        }
      } catch (IllegalArgumentException ex) {
        msg = "format " + getDateTimeFormat().getPattern() + " cannot parse " + v.trim();
      }

      if (msg == null) {
        return msg;
      }
    }

    if (AbstractDate.parse(v, getDateType()) == null) {
      if (msg == null) {
        msg = "error parsing " + v.trim();
      }
    } else {
      msg = null;
    }

    return msg;
  }

  private boolean checkValue() {
    String msg = validate();
    if (BeeUtils.isEmpty(msg)) {
      return true;
    }

    fireEvent(new EditStopEvent(State.ERROR, msg));
    return false;
  }

  private InputText getBox() {
    return box;
  }

  private DatePicker getDatePicker() {
    return datePicker;
  }

  private ValueType getDateType() {
    return dateType;
  }

  private Popup getPopup() {
    return popup;
  }

  private boolean handleChar(int charCode) {
    if (!Character.isLetter(BeeUtils.toChar(charCode))
        && !BeeUtils.inList(charCode, BeeConst.CHAR_PLUS, BeeConst.CHAR_MINUS)) {
      return false;
    }

    HasDateValue oldDate = getDate();
    JustDate baseDate =
        (oldDate == null) ? new JustDate() : new JustDate(oldDate.getDate().getDays());
    HasDateValue newDate = null;

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
        newDate = new JustDate();
        break;

      case 'D':
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
          newDate = TimeUtils.endOfMonth(TimeUtils.nextMonth(oldDate));
        }
        break;

      case 'h':
      case 'H':
        if (ValueType.DATETIME.equals(getDateType())) {
          if (oldDate == null) {
            DateTime now = new DateTime();
            newDate =
                new DateTime(now.getYear(), now.getMonth(), now.getDom(), now.getHour(), 0, 0);
          } else {
            int incr = (charCode == 'h') ? -1 : 1;
            newDate =
                new DateTime(oldDate.getDateTime().getTime() + incr * TimeUtils.MILLIS_PER_HOUR);
          }
        }
        break;

      case 'i':
      case 'I':
        if (ValueType.DATETIME.equals(getDateType())) {
          if (oldDate == null) {
            DateTime now = new DateTime();
            newDate = new DateTime(now.getYear(), now.getMonth(), now.getDom(), now.getHour(),
                now.getMinute(), 0);
          } else {
            int incr = (charCode == 'i') ? -1 : 1;
            newDate =
                new DateTime(oldDate.getDateTime().getTime() + incr * TimeUtils.MILLIS_PER_MINUTE);
          }
        }
        break;

      case 'm':
        newDate = TimeUtils.startOfMonth(baseDate, 0);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfMonth(oldDate, -1);
        }
        break;

      case 'M':
        newDate = TimeUtils.nextMonth(baseDate);
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
        newDate = TimeUtils.startOfQuarter(baseDate, 0);
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
        newDate = new DateTime();
        break;

      case 'w':
      case 's':
        newDate = TimeUtils.startOfWeek(baseDate, 0);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfWeek(oldDate, -1);
        }
        break;

      case 'W':
      case 'S':
        newDate = TimeUtils.startOfWeek(baseDate, 1);
        break;

      case 'y':
        newDate = TimeUtils.startOfYear(baseDate, 0);
        if (TimeUtils.sameDate(newDate, oldDate)) {
          newDate = TimeUtils.startOfYear(oldDate, -1);
        }
        break;

      case 'Y':
        newDate = TimeUtils.startOfYear(baseDate, 1);
        break;

      case 'x':
      case 'X':
        if (ValueType.DATETIME.equals(getDateType())) {
          if (oldDate == null) {
            DateTime now = new DateTime();
            newDate = new DateTime(now.getYear(), now.getMonth(), now.getDom(), now.getHour(),
                now.getMinute(), now.getSecond());
          } else {
            int incr = (charCode == 'x') ? -1 : 1;
            newDate =
                new DateTime(oldDate.getDateTime().getTime() + incr * TimeUtils.MILLIS_PER_SECOND);
          }
        }
        break;

      case '+':
      case '-':
        int cnt = TimeUtils.countFields(getBox().getValue());
        if (cnt == 0 || cnt >= 3) {
          int incr = (charCode == '+') ? 1 : -1;
          if (oldDate == null) {
            newDate = TimeUtils.today(incr);
          } else if (oldDate instanceof JustDate) {
            newDate = new JustDate(oldDate.getDate().getDays() + incr);
          } else if (oldDate instanceof DateTime) {
            newDate =
                new DateTime(oldDate.getDateTime().getTime() + TimeUtils.MILLIS_PER_DAY * incr);
          }
        }
        break;
    }

    if (newDate == null) {
      return false;
    }

    switch (getDateType()) {
      case DATE:
        if (!TimeUtils.sameDate(newDate, oldDate)) {
          setValue(newDate.getDate());
        }
        break;
      case DATETIME:
        if (!TimeUtils.sameDateTime(newDate, oldDate)) {
          setValue(newDate.getDateTime());
        }
        break;
      default:
        Assert.untouchable();
    }
    return true;
  }

  private void hideDatePicker() {
    getPopup().hide();
  }

  private void setValue(HasDateValue value) {
    String text;
    if (value == null) {
      text = BeeConst.STRING_EMPTY;
    } else if (getDateTimeFormat() == null) {
      text = value.toString();
    } else {
      text = getDateTimeFormat().format(value);
    }
    getBox().setValue(text);
  }

  private void showDatePicker() {
    HasDateValue date = getDate();
    if (date == null) {
      date = new JustDate();
    }
    getDatePicker().setDate(date.getDate());
    getPopup().showRelativeTo(getBox());
  }
}
