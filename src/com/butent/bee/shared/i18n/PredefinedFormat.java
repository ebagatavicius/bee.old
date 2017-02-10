package com.butent.bee.shared.i18n;

import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;

public enum PredefinedFormat {
  ISO_8601 {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";
    }
  },
  RFC_2822 {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return "EEE, d MMM yyyy HH:mm:ss Z";
    }
  },

  DATE_FULL {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateFormatFull();
    }
  },
  DATE_LONG {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateFormatLong();
    }
  },
  DATE_MEDIUM {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateFormatMedium();
    }
  },
  DATE_SHORT {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateFormatShort();
    }
  },

  TIME_FULL {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.timeFormatFull();
    }
  },
  TIME_LONG {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.timeFormatLong();
    }
  },
  TIME_MEDIUM {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.timeFormatMedium();
    }
  },
  TIME_SHORT {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.timeFormatShort();
    }
  },

  DATE_TIME_FULL {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateTimeFull();
    }
  },
  DATE_TIME_LONG {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateTimeLong();
    }
  },
  DATE_TIME_MEDIUM {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateTimeMedium();
    }
  },
  DATE_TIME_SHORT {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateTimeShort();
    }
  },
  DATE_SHORT_TIME_MEDIUM {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.dateTime(dateTimeFormatInfo.dateFormatShort(),
          dateTimeFormatInfo.timeFormatMedium());
    }
  },

  HOUR_MINUTE {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatHour12Minute();
    }
  },
  HOUR_MINUTE_SECOND {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatHour12MinuteSecond();
    }
  },
  HOUR24_MINUTE {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatHour24Minute();
    }
  },
  HOUR24_MINUTE_SECOND {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatHour24MinuteSecond();
    }
  },
  HOUR24_MINUTE_SECOND_MILLISECOND {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatHour24MinuteSecondMillisecond();
    }
  },
  MINUTE_SECOND {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMinuteSecond();
    }
  },

  YEAR {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYear();
    }
  },
  YEAR_MONTH {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthFull();
    }
  },
  YEAR_MONTH_ABBR {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthAbbrev();
    }
  },
  YEAR_MONTH_ABBR_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthAbbrevDay();
    }
  },
  YEAR_MONTH_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthFullDay();
    }
  },
  YEAR_MONTH_NUM {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthNum();
    }
  },
  YEAR_MONTH_NUM_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthNumDay();
    }
  },
  YEAR_MONTH_WEEKDAY_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearMonthWeekdayDay();
    }
  },
  YEAR_QUARTER {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearQuarterFull();
    }
  },
  YEAR_QUARTER_ABBR {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatYearQuarterShort();
    }
  },

  MONTH {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMonthFull();
    }
  },
  MONTH_ABBR {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMonthAbbrev();
    }
  },
  MONTH_ABBR_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMonthAbbrevDay();
    }
  },
  MONTH_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMonthFullDay();
    }
  },
  MONTH_NUM_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMonthNumDay();
    }
  },
  MONTH_WEEKDAY_DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatMonthFullWeekdayDay();
    }
  },

  DAY {
    @Override
    public String getPattern(DateTimeFormatInfo dateTimeFormatInfo) {
      return dateTimeFormatInfo.formatDay();
    }
  };

  public abstract String getPattern(DateTimeFormatInfo dateTimeFormatInfo);
}
