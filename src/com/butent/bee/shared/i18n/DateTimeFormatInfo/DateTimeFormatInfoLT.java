package com.butent.bee.shared.i18n.DateTimeFormatInfo;

/**
 * Implementation of DateTimeFormatInfo for the "lt" locale.
 */
public final class DateTimeFormatInfoLT implements DateTimeFormatInfo {

  private static DateTimeFormatInfoLT instance = new DateTimeFormatInfoLT();

  private DateTimeFormatInfoLT() {
  }

  public static DateTimeFormatInfo getInstance() {
    return instance;
  }

  @Override
  public String[] ampms() {
    return new String[] {
        "pr.p.",
        "pop."
    };
  }

  @Override
  public String dateFormatFull() {
    return "y 'm'. MMMM d 'd'., EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "y 'm'. MMMM d 'd'.";
  }

  @Override
  public String dateFormatMedium() {
    return "y MMM d";
  }

  @Override
  public String dateFormatShort() {
    return "y-MM-dd";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "prieš Kristų",
        "po Kristaus"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "pr. Kr.",
        "po Kr."
    };
  }

  @Override
  public String formatDay() {
    return "dd";
  }

  @Override
  public String formatHour12Minute() {
    return "hh:mm a";
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "hh:mm:ss a";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "MMM d";
  }

  @Override
  public String formatMonthFullDay() {
    return "MMMM d";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "MMMM d, EEEE";
  }

  @Override
  public String formatMonthNumDay() {
    return "MM-d";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "y MMM";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "y MMM d";
  }

  @Override
  public String formatYearMonthFull() {
    return "y MMMM";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y 'm'. MMMM d 'd'.";
  }

  @Override
  public String formatYearMonthNum() {
    return "y-MM";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "y-MM-dd";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "y MMM d, EEE";
  }

  @Override
  public String formatYearQuarterFull() {
    return "y QQQQ";
  }

  @Override
  public String formatYearQuarterShort() {
    return "y Q";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "sausio",
        "vasario",
        "kovo",
        "balandžio",
        "gegužės",
        "birželio",
        "liepos",
        "rugpjūčio",
        "rugsėjo",
        "spalio",
        "lapkričio",
        "gruodžio"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "sausis",
        "vasaris",
        "kovas",
        "balandis",
        "gegužė",
        "birželis",
        "liepa",
        "rugpjūtis",
        "rugsėjis",
        "spalis",
        "lapkritis",
        "gruodis"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "S",
        "V",
        "K",
        "B",
        "G",
        "B",
        "L",
        "R",
        "R",
        "S",
        "L",
        "G"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "saus.",
        "vas.",
        "kov.",
        "bal.",
        "geg.",
        "birž.",
        "liep.",
        "rugp.",
        "rugs.",
        "spal.",
        "lapkr.",
        "gruod."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "I ketvirtis",
        "II ketvirtis",
        "III ketvirtis",
        "IV ketvirtis"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "I k.",
        "II k.",
        "III k.",
        "IV k."
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "sekmadienis",
        "pirmadienis",
        "antradienis",
        "trečiadienis",
        "ketvirtadienis",
        "penktadienis",
        "šeštadienis"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "P",
        "A",
        "T",
        "K",
        "P",
        "Š"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "sk",
        "pr",
        "an",
        "tr",
        "kt",
        "pn",
        "št"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
