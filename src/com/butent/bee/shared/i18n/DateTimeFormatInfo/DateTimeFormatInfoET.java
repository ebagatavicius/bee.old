package com.butent.bee.shared.i18n.DateTimeFormatInfo;

import com.butent.bee.shared.i18n.DateOrdering;

/**
 * Implementation of DateTimeFormatInfo for the "et" locale.
 */
public final class DateTimeFormatInfoET implements DateTimeFormatInfo {

  private static DateTimeFormatInfoET instance = new DateTimeFormatInfoET();

  private DateTimeFormatInfoET() {
  }

  public static DateTimeFormatInfo getInstance() {
    return instance;
  }

  @Override
  public String[] ampms() {
    return new String[] {
        "e.k.",
        "p.k."
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, d. MMMM y";
  }

  @Override
  public String dateFormatLong() {
    return "d. MMMM y";
  }

  @Override
  public String dateFormatMedium() {
    return "dd.MM.y";
  }

  @Override
  public String dateFormatShort() {
    return "dd.MM.y";
  }

  @Override
  public DateOrdering dateOrdering() {
    return DateOrdering.DMY;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "enne meie aega",
        "meie aja järgi"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "e.m.a.",
        "m.a.j."
    };
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "h:mm.ss a";
  }

  @Override
  public String formatHour24Minute() {
    return "H:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "H:mm.ss";
  }

  @Override
  public String formatHour24MinuteSecondMillisecond() {
    return "H:mm:ss.SSS";
  }

  @Override
  public String formatMinuteSecond() {
    return "mm.ss";
  }

  @Override
  public String formatMonthAbbrev() {
    return "MMMM";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d. MMM";
  }

  @Override
  public String formatMonthFullDay() {
    return "d. MMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE, d. MMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "d.M";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d. MMM y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthStandalone() {
    return "LLLL y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d. MMMM y";
  }

  @Override
  public String formatYearMonthNum() {
    return "M.y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d.M.y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d. MMMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ y";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "jaanuar",
        "veebruar",
        "märts",
        "aprill",
        "mai",
        "juuni",
        "juuli",
        "august",
        "september",
        "oktoober",
        "november",
        "detsember"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "J",
        "V",
        "M",
        "A",
        "M",
        "J",
        "J",
        "A",
        "S",
        "O",
        "N",
        "D"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "jaan",
        "veebr",
        "märts",
        "apr",
        "mai",
        "juuni",
        "juuli",
        "aug",
        "sept",
        "okt",
        "nov",
        "dets"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1. kvartal",
        "2. kvartal",
        "3. kvartal",
        "4. kvartal"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "K1",
        "K2",
        "K3",
        "K4"
    };
  }

  @Override
  public String timeFormatFull() {
    return "H:mm.ss zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "H:mm.ss z";
  }

  @Override
  public String timeFormatMedium() {
    return "H:mm.ss";
  }

  @Override
  public String timeFormatShort() {
    return "H:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "pühapäev",
        "esmaspäev",
        "teisipäev",
        "kolmapäev",
        "neljapäev",
        "reede",
        "laupäev"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "P",
        "E",
        "T",
        "K",
        "N",
        "R",
        "L"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "P",
        "E",
        "T",
        "K",
        "N",
        "R",
        "L"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
