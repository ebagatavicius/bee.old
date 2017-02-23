package com.butent.bee.shared.i18n.DateTimeFormatInfo;

import com.butent.bee.shared.i18n.DateOrdering;

/**
 * Implementation of DateTimeFormatInfo for the "de" locale.
 */
public final class DateTimeFormatInfoDE implements DateTimeFormatInfo {

  private static DateTimeFormatInfoDE instance = new DateTimeFormatInfoDE();

  private DateTimeFormatInfoDE() {
  }

  public static DateTimeFormatInfo getInstance() {
    return instance;
  }

  @Override
  public String[] ampms() {
    return new String[] {
        "vorm.",
        "nachm."
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
  public String dateFormatCompact() {
    return "dd.MM.yy";
  }

  @Override
  public DateOrdering dateOrdering() {
    return DateOrdering.DMY;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "v. Chr.",
        "n. Chr."
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "v. Chr.",
        "n. Chr."
    };
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
    return "d.M.";
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
    return "EEE, d. MMM y";
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
        "Januar",
        "Februar",
        "März",
        "April",
        "Mai",
        "Juni",
        "Juli",
        "August",
        "September",
        "Oktober",
        "November",
        "Dezember"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "J",
        "F",
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
        "Jan.",
        "Feb.",
        "März",
        "Apr.",
        "Mai",
        "Juni",
        "Juli",
        "Aug.",
        "Sep.",
        "Okt.",
        "Nov.",
        "Dez."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "Jan",
        "Feb",
        "Mär",
        "Apr",
        "Mai",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Okt",
        "Nov",
        "Dez"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1. Quartal",
        "2. Quartal",
        "3. Quartal",
        "4. Quartal"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "Q1",
        "Q2",
        "Q3",
        "Q4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "Sonntag",
        "Montag",
        "Dienstag",
        "Mittwoch",
        "Donnerstag",
        "Freitag",
        "Samstag"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "M",
        "D",
        "M",
        "D",
        "F",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "So.",
        "Mo.",
        "Di.",
        "Mi.",
        "Do.",
        "Fr.",
        "Sa."
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return new String[] {
        "So",
        "Mo",
        "Di",
        "Mi",
        "Do",
        "Fr",
        "Sa"
    };
  }
}
