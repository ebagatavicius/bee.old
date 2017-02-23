package com.butent.bee.shared.i18n.DateTimeFormatInfo;

import com.butent.bee.shared.i18n.DateOrdering;

/**
 * Implementation of DateTimeFormatInfo for the "fi" locale.
 */
public final class DateTimeFormatInfoFI implements DateTimeFormatInfo {

  private static DateTimeFormatInfoFI instance = new DateTimeFormatInfoFI();

  private DateTimeFormatInfoFI() {
  }

  public static DateTimeFormatInfo getInstance() {
    return instance;
  }

  @Override
  public String[] ampms() {
    return new String[] {
        "ap.",
        "ip."
    };
  }

  @Override
  public String dateFormatFull() {
    return "cccc d. MMMM y";
  }

  @Override
  public String dateFormatLong() {
    return "d. MMMM y";
  }

  @Override
  public String dateFormatMedium() {
    return "d.M.y";
  }

  @Override
  public String dateFormatShort() {
    return "d.M.y";
  }

  @Override
  public String dateFormatCompact() {
    return "d.M.yy";
  }

  @Override
  public DateOrdering dateOrdering() {
    return DateOrdering.DMY;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "ennen Kristuksen syntymää",
        "jälkeen Kristuksen syntymän"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "eKr.",
        "jKr."
    };
  }

  @Override
  public String formatHour12Minute() {
    return "h.mm a";
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "h.mm.ss a";
  }

  @Override
  public String formatHour24Minute() {
    return "H.mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "H.mm.ss";
  }

  @Override
  public String formatHour24MinuteSecondMillisecond() {
    return "H:mm:ss.SSS";
  }

  @Override
  public String formatMinuteSecond() {
    return "m.ss";
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
    return "cccc d. MMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "d.M.";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "LLL y";
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
    return "L.y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d.M.y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE d. MMM y";
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
        "tammikuuta",
        "helmikuuta",
        "maaliskuuta",
        "huhtikuuta",
        "toukokuuta",
        "kesäkuuta",
        "heinäkuuta",
        "elokuuta",
        "syyskuuta",
        "lokakuuta",
        "marraskuuta",
        "joulukuuta"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "tammikuu",
        "helmikuu",
        "maaliskuu",
        "huhtikuu",
        "toukokuu",
        "kesäkuu",
        "heinäkuu",
        "elokuu",
        "syyskuu",
        "lokakuu",
        "marraskuu",
        "joulukuu"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "T",
        "H",
        "M",
        "H",
        "T",
        "K",
        "H",
        "E",
        "S",
        "L",
        "M",
        "J"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "tammikuuta",
        "helmikuuta",
        "maaliskuuta",
        "huhtikuuta",
        "toukokuuta",
        "kesäkuuta",
        "heinäkuuta",
        "elokuuta",
        "syyskuuta",
        "lokakuuta",
        "marraskuuta",
        "joulukuuta"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "tammi",
        "helmi",
        "maalis",
        "huhti",
        "touko",
        "kesä",
        "heinä",
        "elo",
        "syys",
        "loka",
        "marras",
        "joulu"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1. neljännes",
        "2. neljännes",
        "3. neljännes",
        "4. neljännes"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "1. nelj.",
        "2. nelj.",
        "3. nelj.",
        "4. nelj."
    };
  }

  @Override
  public String timeFormatFull() {
    return "H.mm.ss zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "H.mm.ss z";
  }

  @Override
  public String timeFormatMedium() {
    return "H.mm.ss";
  }

  @Override
  public String timeFormatShort() {
    return "H.mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "sunnuntaina",
        "maanantaina",
        "tiistaina",
        "keskiviikkona",
        "torstaina",
        "perjantaina",
        "lauantaina"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return new String[] {
        "sunnuntai",
        "maanantai",
        "tiistai",
        "keskiviikko",
        "torstai",
        "perjantai",
        "lauantai"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "M",
        "T",
        "K",
        "T",
        "P",
        "L"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "su",
        "ma",
        "ti",
        "ke",
        "to",
        "pe",
        "la"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
