package com.butent.bee.shared.i18n.DateTimeFormatInfo;

/**
 * Implementation of DateTimeFormatInfo for the "ru" locale.
 */
public final class DateTimeFormatInfoRU implements DateTimeFormatInfo {

  private static DateTimeFormatInfoRU instance = new DateTimeFormatInfoRU();

  private DateTimeFormatInfoRU() {
  }

  public static DateTimeFormatInfo getInstance() {
    return instance;
  }

  @Override
  public String[] ampms() {
    return new String[] {
        "AM",
        "PM"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, d MMMM y 'г'.";
  }

  @Override
  public String dateFormatLong() {
    return "d MMMM y 'г'.";
  }

  @Override
  public String dateFormatMedium() {
    return "d MMM y 'г'.";
  }

  @Override
  public String dateFormatShort() {
    return "dd.MM.y";
  }

  @Override
  public String dateTime(String datePattern, String timePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "до н.э.",
        "н.э."
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "до н. э.",
        "н. э."
    };
  }

  @Override
  public String formatHour24Minute() {
    return "H:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "H:mm:ss";
  }

  @Override
  public String formatHour24MinuteSecondMillisecond() {
    return "H:mm:ss.SSS";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d MMM";
  }

  @Override
  public String formatMonthFullDay() {
    return "d MMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "cccc, d MMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "dd.MM";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "LLL y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM y 'г'.";
  }

  @Override
  public String formatYearMonthFull() {
    return "LLLL y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y 'г'.";
  }

  @Override
  public String formatYearMonthNum() {
    return "MM.y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "dd.MM.y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d MMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ y 'г'.";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y 'г'.";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "января",
        "февраля",
        "марта",
        "апреля",
        "мая",
        "июня",
        "июля",
        "августа",
        "сентября",
        "октября",
        "ноября",
        "декабря"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "Январь",
        "Февраль",
        "Март",
        "Апрель",
        "Май",
        "Июнь",
        "Июль",
        "Август",
        "Сентябрь",
        "Октябрь",
        "Ноябрь",
        "Декабрь"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "Я",
        "Ф",
        "М",
        "А",
        "М",
        "И",
        "И",
        "А",
        "С",
        "О",
        "Н",
        "Д"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "янв.",
        "февр.",
        "марта",
        "апр.",
        "мая",
        "июня",
        "июля",
        "авг.",
        "сент.",
        "окт.",
        "нояб.",
        "дек."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "Янв.",
        "Февр.",
        "Март",
        "Апр.",
        "Май",
        "Июнь",
        "Июль",
        "Авг.",
        "Сент.",
        "Окт.",
        "Нояб.",
        "Дек."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1-й квартал",
        "2-й квартал",
        "3-й квартал",
        "4-й квартал"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "1-й кв.",
        "2-й кв.",
        "3-й кв.",
        "4-й кв."
    };
  }

  @Override
  public String timeFormatFull() {
    return "H:mm:ss zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "H:mm:ss z";
  }

  @Override
  public String timeFormatMedium() {
    return "H:mm:ss";
  }

  @Override
  public String timeFormatShort() {
    return "H:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "воскресенье",
        "понедельник",
        "вторник",
        "среда",
        "четверг",
        "пятница",
        "суббота"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return new String[] {
        "Воскресенье",
        "Понедельник",
        "Вторник",
        "Среда",
        "Четверг",
        "Пятница",
        "Суббота"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "вс",
        "пн",
        "вт",
        "ср",
        "чт",
        "пт",
        "сб"
    };
  }

  @Override
  public String[] weekdaysNarrowStandalone() {
    return new String[] {
        "В",
        "П",
        "В",
        "С",
        "Ч",
        "П",
        "С"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "вс",
        "пн",
        "вт",
        "ср",
        "чт",
        "пт",
        "сб"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return new String[] {
        "Вс",
        "Пн",
        "Вт",
        "Ср",
        "Чт",
        "Пт",
        "Сб"
    };
  }
}
