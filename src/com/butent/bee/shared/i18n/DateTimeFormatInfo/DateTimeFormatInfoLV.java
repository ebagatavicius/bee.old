package com.butent.bee.shared.i18n.DateTimeFormatInfo;

import com.butent.bee.shared.i18n.DateOrdering;

/**
 * Implementation of DateTimeFormatInfo for the "lv" locale.
 */
public final class DateTimeFormatInfoLV implements DateTimeFormatInfo {

  private static DateTimeFormatInfoLV instance = new DateTimeFormatInfoLV();

  private DateTimeFormatInfoLV() {
  }

  public static DateTimeFormatInfo getInstance() {
    return instance;
  }

  @Override
  public String[] ampms() {
    return new String[] {
        "priekšpusdienā",
        "pēcpusdienā"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, y. 'gada' d. MMMM";
  }

  @Override
  public String dateFormatLong() {
    return "y. 'gada' d. MMMM";
  }

  @Override
  public String dateFormatMedium() {
    return "y. 'gada' d. MMM";
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
        "pirms mūsu ēras",
        "mūsu ērā"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "p.m.ē.",
        "m.ē."
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
    return "dd.MM.";
  }

  @Override
  public String formatYear() {
    return "y. 'g'.";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "y. 'g'. MMM";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "y. 'g'. d. MMM";
  }

  @Override
  public String formatYearMonthFull() {
    return "y. 'g'. MMMM";
  }

  @Override
  public String formatYearMonthStandalone() {
    return "y LLLL";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y. 'gada' d. MMMM";
  }

  @Override
  public String formatYearMonthNum() {
    return "MM.y.";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d.M.y.";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, y. 'g'. d. MMM";
  }

  @Override
  public String formatYearQuarterFull() {
    return "y. 'g'. QQQQ";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "janvāris",
        "februāris",
        "marts",
        "aprīlis",
        "maijs",
        "jūnijs",
        "jūlijs",
        "augusts",
        "septembris",
        "oktobris",
        "novembris",
        "decembris"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "Janvāris",
        "Februāris",
        "Marts",
        "Aprīlis",
        "Maijs",
        "Jūnijs",
        "Jūlijs",
        "Augusts",
        "Septembris",
        "Oktobris",
        "Novembris",
        "Decembris"
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
        "janv.",
        "febr.",
        "marts",
        "apr.",
        "maijs",
        "jūn.",
        "jūl.",
        "aug.",
        "sept.",
        "okt.",
        "nov.",
        "dec."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "Janv.",
        "Febr.",
        "Marts",
        "Apr.",
        "Maijs",
        "Jūn.",
        "Jūl.",
        "Aug.",
        "Sept.",
        "Okt.",
        "Nov.",
        "Dec."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1. ceturksnis",
        "2. ceturksnis",
        "3. ceturksnis",
        "4. ceturksnis"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "C1",
        "C2",
        "C3",
        "C4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "svētdiena",
        "pirmdiena",
        "otrdiena",
        "trešdiena",
        "ceturtdiena",
        "piektdiena",
        "sestdiena"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return new String[] {
        "Svētdiena",
        "Pirmdiena",
        "Otrdiena",
        "Trešdiena",
        "Ceturtdiena",
        "Piektdiena",
        "Sestdiena"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "P",
        "O",
        "T",
        "C",
        "P",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "Sv",
        "Pr",
        "Ot",
        "Tr",
        "Ce",
        "Pk",
        "Se"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
