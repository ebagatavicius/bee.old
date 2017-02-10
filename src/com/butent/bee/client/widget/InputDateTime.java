package com.butent.bee.client.widget;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class InputDateTime extends InputDate {

  private static final DateTime DEFAULT_MIN_DATE_TIME = DEFAULT_MIN_DATE.getDateTime();
  private static final DateTime DEFAULT_MAX_DATE_TIME =
      new DateTime(DEFAULT_MAX_DATE, 23, 59, 0, 0);

  public InputDateTime() {
    super();
  }

  public DateTime getDateTime() {
    String v = getValue();
    if (BeeUtils.isEmpty(v)) {
      return null;
    } else {
      return Format.parseDateTimeQuietly(getDateTimeFormat(), v);
    }
  }

  @Override
  public String getIdPrefix() {
    return "dt";
  }

  @Override
  public String getNormalizedValue() {
    DateTime dt = getDateTime();
    return (dt == null) ? null : dt.serialize();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_DATE_TIME;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    setDateTime(TimeUtils.toDateTimeOrNull(normalizedValue));
  }

  @Override
  public void setDate(HasDateValue date) {
    if (date == null) {
      setDateTime(null);

    } else if (date instanceof JustDate && !isEmpty()) {
      DateTime oldValue = getDateTime();
      setDateTime(TimeUtils.combine(date, oldValue));

    } else {
      setDateTime(DateTime.get(date));
    }
  }

  public void setDateTime(DateTime dt) {
    String text;
    if (dt == null) {
      text = BeeConst.STRING_EMPTY;
    } else if (getDateTimeFormat() == null) {
      text = Format.renderDateTime(dt);
    } else {
      text = getDateTimeFormat().format(dt);
    }
    setText(text);
  }

  @Override
  public void setMaxValue(String maxValue) {
    setMaxDate(TimeUtils.parseDateTime(maxValue));
  }

  @Override
  public void setMinValue(String minValue) {
    setMinDate(TimeUtils.parseDateTime(minValue));
  }

  @Override
  public void setValue(String value) {
    setDateTime(TimeUtils.toDateTimeOrNull(value));
  }

  @Override
  protected boolean checkBounds(HasDateValue value) {
    DateTime dt = DateTime.get(value);
    if (dt == null) {
      return isNullable();
    }

    DateTime min = DateTime.get(getMinBound());
    if (min != null && TimeUtils.isLess(dt, min)) {
      return false;
    }

    DateTime max = DateTime.get(getMaxBound());
    if (max != null && TimeUtils.isMore(dt, max)) {
      return false;
    }
    return true;
  }

  @Override
  protected HasDateValue getDateValue(String normalizedValue) {
    return TimeUtils.toDateTimeOrNull(normalizedValue);
  }

  @Override
  protected HasDateValue getDateValue() {
    return getDateTime();
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "InputDateTime";
  }

  @Override
  protected HasDateValue getMaxBound() {
    return BeeUtils.nvl(getMaxDate(), DEFAULT_MAX_DATE_TIME);
  }

  @Override
  protected HasDateValue getMinBound() {
    return BeeUtils.nvl(getMinDate(), DEFAULT_MIN_DATE_TIME);
  }

  @Override
  protected boolean handleChar(int charCode) {
    DateTime oldValue = getDateTime();
    DateTime newValue = null;

    switch (charCode) {
      case 'h':
      case 'H':
        if (hasHours()) {
          if (oldValue == null) {
            newValue = TimeUtils.nowHours();
          } else {
            int incr = (charCode == 'h') ? -1 : 1;
            newValue = new DateTime(oldValue.getTime() + incr * TimeUtils.MILLIS_PER_HOUR);
          }
        }
        break;

      case 'i':
      case 'I':
        if (hasMinutes()) {
          if (oldValue == null) {
            newValue = TimeUtils.nowMinutes();
          } else {
            int incr = (charCode == 'i') ? -1 : 1;
            newValue = new DateTime(oldValue.getTime() + incr * TimeUtils.MILLIS_PER_MINUTE);
          }
        }
        break;

      case 't':
      case 'T':
      case 'l':
      case 'L':
        if (hasFractionalSeconds()) {
          newValue = new DateTime();
        } else if (hasSeconds()) {
          newValue = TimeUtils.nowSeconds();
        } else if (hasMinutes()) {
          newValue = TimeUtils.nowMinutes();
        } else if (hasHours()) {
          newValue = TimeUtils.nowHours();
        } else {
          newValue = TimeUtils.today().getDateTime();
        }
        break;

      case 'x':
      case 'X':
        if (hasSeconds()) {
          if (oldValue == null) {
            newValue = TimeUtils.nowSeconds();
          } else {
            int incr = (charCode == 'x') ? -1 : 1;
            newValue = new DateTime(oldValue.getTime() + incr * TimeUtils.MILLIS_PER_SECOND);
          }
        }
        break;

      case '+':
      case '-':
        int cnt = TimeUtils.countFields(getValue());
        if (cnt == 0 || cnt >= 3) {
          int incr = (charCode == '+') ? 1 : -1;
          if (getStepValue() > 1) {
            incr *= getStepValue();
          }

          if (oldValue == null) {
            if (getStepValue() > 0 && hasMinutes()) {
              newValue = TimeUtils.nowMinutes(incr);
            } else {
              newValue = TimeUtils.today(incr).getDateTime();
            }

          } else {
            if (getStepValue() > 0 && hasMinutes()) {
              newValue = new DateTime(oldValue.getTime() + TimeUtils.MILLIS_PER_MINUTE * incr);
            } else {
              newValue = new DateTime(oldValue.getTime() + TimeUtils.MILLIS_PER_DAY * incr);
            }
          }
        }
        break;
    }

    if (newValue != null) {
      DateTime min = DateTime.get(getMinBound());
      DateTime max = DateTime.get(getMaxBound());

      newValue = TimeUtils.clamp(newValue, min, max);

      if (!TimeUtils.sameDateTime(newValue, oldValue)) {
        setDateTime(newValue);
      }
      return true;
    }

    return super.handleChar(charCode);
  }

  @Override
  protected boolean willParse(String text) {
    return Format.parseDateTimeQuietly(getDateTimeFormat(), text) != null;
  }

  private boolean hasFractionalSeconds() {
    if (getDateTimeFormat() == null) {
      return false;
    } else {
      return getDateTimeFormat().hasFractionalSeconds();
    }
  }

  private boolean hasHours() {
    if (getDateTimeFormat() == null) {
      return true;
    } else {
      return getDateTimeFormat().hasHours();
    }
  }

  private boolean hasMinutes() {
    if (getDateTimeFormat() == null) {
      return true;
    } else {
      return getDateTimeFormat().hasMinutes();
    }
  }

  private boolean hasSeconds() {
    if (getDateTimeFormat() == null) {
      return false;
    } else {
      return getDateTimeFormat().hasSeconds();
    }
  }
}
