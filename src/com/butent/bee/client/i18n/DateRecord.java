package com.butent.bee.client.i18n;

import com.butent.bee.shared.time.DateTime;

public class DateRecord {

  public static final int AM = 0;
  public static final int PM = 1;

  private int era;
  private int year;
  private int month;
  private int dayOfMonth;
  private int ampm;
  private int hours;
  private int minutes;
  private int seconds;
  private int milliseconds;

  private int tzOffset;
  private int dayOfWeek;

  public DateRecord() {
    era = -1;
    year = Integer.MIN_VALUE;
    month = -1;
    dayOfMonth = -1;
    ampm = -1;
    hours = -1;
    minutes = -1;
    seconds = -1;
    milliseconds = -1;
    dayOfWeek = -1;
    tzOffset = Integer.MIN_VALUE;
  }

  public boolean calcDate(DateTime date, boolean strict) {
    if (this.era == 0 && this.year > 0) {
      this.year = -(this.year - 1);
    }

    if (this.year > Integer.MIN_VALUE) {
      date.setYear(this.year);
    }
    if (this.month > 0) {
      date.setMonth(this.month);
    }
    if (this.dayOfMonth > 0) {
      date.setDom(this.dayOfMonth);
    }

    int h = this.hours;
    if (h < 0) {
      h = date.getHour();
    }
    if (this.ampm == PM && h < 12) {
      h += 12;
    }
    date.setHour(h);

    if (this.minutes >= 0) {
      date.setMinute(this.minutes);
    }
    if (this.seconds >= 0) {
      date.setSecond(this.seconds);
    }
    if (this.milliseconds >= 0) {
      date.setMillis(this.milliseconds);
    }

    if (strict) {
      if (this.year > Integer.MIN_VALUE && this.year != date.getYear()) {
        return false;
      }
      if (this.month > 0 && this.month != date.getMonth()) {
        return false;
      }
      if (this.dayOfMonth > 0 && this.dayOfMonth != date.getDom()) {
        return false;
      }

      if (this.hours >= 24) {
        return false;
      }
      if (this.minutes >= 60) {
        return false;
      }
      if (this.seconds >= 60) {
        return false;
      }
      if (this.milliseconds >= 1000) {
        return false;
      }
    }

    if (this.dayOfWeek > 0) {
      if (this.dayOfMonth <= 0) {
        int adjustment = (7 + this.dayOfWeek - date.getDow()) % 7;
        if (adjustment > 3) {
          adjustment -= 7;
        }
        int orgMonth = date.getMonth();
        date.setDom(date.getDom() + adjustment);

        if (date.getMonth() != orgMonth) {
          date.setDom(date.getDom() + (adjustment > 0 ? -7 : 7));
        }
      } else {
        if (date.getDow() != this.dayOfWeek) {
          return false;
        }
      }
    }

    if (this.tzOffset > Integer.MIN_VALUE) {
      int offset = date.getTimezoneOffset();
      date.setTime(date.getTime() + (this.tzOffset - offset) * 60 * 1000);
    }

    return true;
  }

  public void setAmpm(int ampm) {
    this.ampm = ampm;
  }

  public void setDayOfMonth(int day) {
    this.dayOfMonth = day;
  }

  public void setDayOfWeek(int dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public void setEra(int era) {
    this.era = era;
  }

  public void setHours(int hours) {
    this.hours = hours;
  }

  public void setMilliseconds(int milliseconds) {
    this.milliseconds = milliseconds;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public void setSeconds(int seconds) {
    this.seconds = seconds;
  }

  public void setTzOffset(int tzOffset) {
    this.tzOffset = tzOffset;
  }

  public void setYear(int value) {
    this.year = value;
  }
}
