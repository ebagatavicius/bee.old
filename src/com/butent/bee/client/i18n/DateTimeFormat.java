package com.butent.bee.client.i18n;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.shared.DateTimeFormatInfo;
import com.google.gwt.i18n.shared.DefaultDateTimeFormatInfo;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DateTimeFormat {

  public enum PredefinedFormat {
    ISO_8601,
    RFC_2822,

    DATE_FULL,
    DATE_LONG,
    DATE_MEDIUM,
    DATE_SHORT,

    TIME_FULL,
    TIME_LONG,
    TIME_MEDIUM,
    TIME_SHORT,

    DATE_TIME_FULL,
    DATE_TIME_LONG,
    DATE_TIME_MEDIUM,
    DATE_TIME_SHORT,

    DAY,
    HOUR_MINUTE,
    HOUR_MINUTE_SECOND,
    HOUR24_MINUTE,
    HOUR24_MINUTE_SECOND,
    MINUTE_SECOND,
    MONTH,
    MONTH_ABBR,
    MONTH_ABBR_DAY,
    MONTH_DAY,
    MONTH_NUM_DAY,
    MONTH_WEEKDAY_DAY,
    YEAR,
    YEAR_MONTH,
    YEAR_MONTH_ABBR,
    YEAR_MONTH_ABBR_DAY,
    YEAR_MONTH_DAY,
    YEAR_MONTH_NUM,
    YEAR_MONTH_NUM_DAY,
    YEAR_MONTH_WEEKDAY_DAY,
    YEAR_QUARTER,
    YEAR_QUARTER_ABBR,
  }

  private static class PatternPart {
    private String text;
    private int count;
    private boolean abutStart;

    public PatternPart(String txt, int cnt) {
      text = txt;
      count = cnt;
      abutStart = false;
    }
  }

  protected static final String RFC2822_PATTERN = "EEE, d MMM yyyy HH:mm:ss Z";
  protected static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";

  private static final int NUMBER_BASE = 10;

  private static final Map<String, DateTimeFormat> cache = new HashMap<>();

  private static final String PATTERN_CHARS = "GyMLdkHmsSEcDahKzZv";

  private static final String NUMERIC_FORMAT_CHARS = "MLydhHmsSDkK";

  private static final String WHITE_SPACE = " \t\r\n";

  private static final String GMT = "GMT";
  private static final String UTC = "UTC";

  public static DateTimeFormat getFormat(PredefinedFormat predef) {
    if (usesFixedEnglishStrings(predef)) {
      String pattern;
      switch (predef) {
        case RFC_2822:
          pattern = RFC2822_PATTERN;
          break;
        case ISO_8601:
          pattern = ISO8601_PATTERN;
          break;
        default:
          throw new IllegalStateException("Unexpected predef type " + predef);
      }
      return getFormat(pattern, LocaleInfo.getCurrentLocale(), new DefaultDateTimeFormatInfo());
    }

    DateTimeFormatInfo dtfi = getDefaultDateTimeFormatInfo();
    String pattern;

    switch (predef) {
      case DATE_FULL:
        pattern = dtfi.dateFormatFull();
        break;
      case DATE_LONG:
        pattern = dtfi.dateFormatLong();
        break;
      case DATE_MEDIUM:
        pattern = dtfi.dateFormatMedium();
        break;
      case DATE_SHORT:
        pattern = dtfi.dateFormatShort();
        break;
      case DATE_TIME_FULL:
        pattern = dtfi.dateTimeFull(dtfi.timeFormatFull(), dtfi.dateFormatFull());
        break;
      case DATE_TIME_LONG:
        pattern = dtfi.dateTimeLong(dtfi.timeFormatLong(), dtfi.dateFormatLong());
        break;
      case DATE_TIME_MEDIUM:
        pattern = dtfi.dateTimeMedium(dtfi.timeFormatMedium(), dtfi.dateFormatMedium());
        break;
      case DATE_TIME_SHORT:
        pattern = dtfi.dateTimeShort(dtfi.timeFormatShort(), dtfi.dateFormatShort());
        break;
      case DAY:
        pattern = dtfi.formatDay();
        break;
      case HOUR24_MINUTE:
        pattern = dtfi.formatHour24Minute();
        break;
      case HOUR24_MINUTE_SECOND:
        pattern = dtfi.formatHour24MinuteSecond();
        break;
      case HOUR_MINUTE:
        pattern = dtfi.formatHour12Minute();
        break;
      case HOUR_MINUTE_SECOND:
        pattern = dtfi.formatHour12MinuteSecond();
        break;
      case MINUTE_SECOND:
        pattern = dtfi.formatMinuteSecond();
        break;
      case MONTH:
        pattern = dtfi.formatMonthFull();
        break;
      case MONTH_ABBR:
        pattern = dtfi.formatMonthAbbrev();
        break;
      case MONTH_ABBR_DAY:
        pattern = LocaleUtils.formatMonthAbbrevDay(LocaleInfo.getCurrentLocale());
        break;
      case MONTH_DAY:
        pattern = LocaleUtils.formatMonthFullDay(LocaleInfo.getCurrentLocale());
        break;
      case MONTH_NUM_DAY:
        pattern = dtfi.formatMonthNumDay();
        break;
      case MONTH_WEEKDAY_DAY:
        pattern = dtfi.formatMonthFullWeekdayDay();
        break;
      case TIME_FULL:
        pattern = dtfi.timeFormatFull();
        break;
      case TIME_LONG:
        pattern = dtfi.timeFormatLong();
        break;
      case TIME_MEDIUM:
        pattern = dtfi.timeFormatMedium();
        break;
      case TIME_SHORT:
        pattern = dtfi.timeFormatShort();
        break;
      case YEAR:
        pattern = dtfi.formatYear();
        break;
      case YEAR_MONTH:
        pattern = dtfi.formatYearMonthFull();
        break;
      case YEAR_MONTH_ABBR:
        pattern = dtfi.formatYearMonthAbbrev();
        break;
      case YEAR_MONTH_ABBR_DAY:
        pattern = dtfi.formatYearMonthAbbrevDay();
        break;
      case YEAR_MONTH_DAY:
        pattern = dtfi.formatYearMonthFullDay();
        break;
      case YEAR_MONTH_NUM:
        pattern = dtfi.formatYearMonthNum();
        break;
      case YEAR_MONTH_NUM_DAY:
        pattern = dtfi.formatYearMonthNumDay();
        break;
      case YEAR_MONTH_WEEKDAY_DAY:
        pattern = dtfi.formatYearMonthWeekdayDay();
        break;
      case YEAR_QUARTER:
        pattern = dtfi.formatYearQuarterFull();
        break;
      case YEAR_QUARTER_ABBR:
        pattern = dtfi.formatYearQuarterShort();
        break;
      default:
        throw new IllegalArgumentException("Unexpected predefined format " + predef);
    }

    return getFormat(pattern, LocaleInfo.getCurrentLocale(), dtfi);
  }

  public static DateTimeFormat getFormat(String pattern) {
    return getFormat(pattern, LocaleInfo.getCurrentLocale(), getDefaultDateTimeFormatInfo());
  }

  protected static DateTimeFormat getFormat(String pattern, LocaleInfo localeInfo,
      DateTimeFormatInfo dtfi) {

    DateTimeFormatInfo defaultDtfi = getDefaultDateTimeFormatInfo();
    DateTimeFormat dtf = null;
    if (dtfi == defaultDtfi) {
      dtf = cache.get(pattern);
    }
    if (dtf == null) {
      dtf = new DateTimeFormat(pattern, localeInfo, dtfi);
      if (dtfi == defaultDtfi) {
        cache.put(pattern, dtf);
      }
    }
    return dtf;
  }

  private static DateTimeFormatInfo getDefaultDateTimeFormatInfo() {
    return LocaleInfo.getCurrentLocale().getDateTimeFormatInfo();
  }

  private static boolean usesFixedEnglishStrings(PredefinedFormat predef) {
    switch (predef) {
      case RFC_2822:
        return true;
      case ISO_8601:
        return true;
      default:
        return false;
    }
  }

  private final List<PatternPart> patternParts = new ArrayList<>();

  private final DateTimeFormatInfo dateTimeFormatInfo;
  private final String[] monthsFull;

  private final String pattern;

  protected DateTimeFormat(String pattern) {
    this(pattern, LocaleInfo.getCurrentLocale(), getDefaultDateTimeFormatInfo());
  }

  protected DateTimeFormat(String pattern, LocaleInfo localeInfo, DateTimeFormatInfo dtfi) {
    this.pattern = pattern;
    this.dateTimeFormatInfo = dtfi;
    this.monthsFull = LocaleUtils.monthsFull(localeInfo);

    parsePattern(pattern);
  }

  public String format(HasDateValue date) {
    return format(date, createTimeZone(date.getTimezoneOffset()));
  }

  public String format(HasDateValue date, TimeZone timeZone) {
    long diff = date.supportsTimezoneOffset()
        ? (date.getTimezoneOffset() - timeZone.getOffset(date)) * 60000 : 0;
    DateTime keepDate = new DateTime(date.getTime() + diff);
    DateTime keepTime = keepDate;

    if (date.supportsTimezoneOffset() && keepDate.getTimezoneOffset() != date.getTimezoneOffset()) {
      if (diff > 0) {
        diff -= TimeUtils.MILLIS_PER_DAY;
      } else {
        diff += TimeUtils.MILLIS_PER_DAY;
      }
      keepTime = new DateTime(date.getTime() + diff);
    }

    StringBuffer toAppendTo = new StringBuffer(64);
    int j;
    int n = pattern.length();

    int i = 0;
    while (i < n) {
      char ch = pattern.charAt(i);

      if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
        j = i + 1;
        while (j < n && pattern.charAt(j) == ch) {
          j++;
        }

        subFormat(toAppendTo, ch, j - i, date, keepDate, keepTime, timeZone);
        i = j;

      } else if (ch == '\'') {
        i++;

        if (i < n && pattern.charAt(i) == '\'') {
          toAppendTo.append('\'');
          i++;
          continue;
        }

        boolean trailQuote = false;
        while (!trailQuote) {
          j = i;
          while (j < n && pattern.charAt(j) != '\'') {
            j++;
          }

          if (j >= n) {
            throw new IllegalArgumentException("Missing trailing \'");
          }

          if (j + 1 < n && pattern.charAt(j + 1) == '\'') {
            j++;
          } else {
            trailQuote = true;
          }
          toAppendTo.append(pattern.substring(i, j));
          i = j + 1;
        }

      } else {
        toAppendTo.append(ch);
        i++;
      }
    }

    return toAppendTo.toString();
  }

  public String getPattern() {
    return pattern;
  }

  public boolean hasFractionalSeconds() {
    return getPattern().indexOf('S') >= 0;
  }

  public boolean hasHours() {
    return getPattern().indexOf('H') >= 0 || getPattern().indexOf('h') >= 0;
  }

  public boolean hasMinutes() {
    return getPattern().contains("mm");
  }

  public boolean hasSeconds() {
    return getPattern().indexOf('s') >= 0;
  }

  public DateTime parse(String text) {
    return parse(text, false);
  }

  public DateTime parseQuietly(String text) {
    if (BeeUtils.isEmpty(text)) {
      return null;
    }

    DateTime result;
    try {
      result = parse(text.trim());
    } catch (IllegalArgumentException ex) {
      result = null;
    }

    return result;
  }

  public DateTime parseStrict(String text) {
    return parse(text, true);
  }

  protected TimeZone createTimeZone(int timezoneOffset) {
    return TimeZone.createTimeZone(timezoneOffset);
  }

  private void addPart(StringBuffer buf, int count) {
    if (buf.length() > 0) {
      patternParts.add(new PatternPart(buf.toString(), count));
      buf.setLength(0);
    }
  }

  private static void format0To11Hours(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getHour() % 12;
    zeroPaddingNumber(buf, value, count);
  }

  private static void format0To23Hours(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getHour();
    zeroPaddingNumber(buf, value, count);
  }

  private static void format1To12Hours(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getHour() % 12;
    if (value == 0) {
      zeroPaddingNumber(buf, 12, count);
    } else {
      zeroPaddingNumber(buf, value, count);
    }
  }

  private static void format24Hours(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getHour();
    if (value == 0) {
      zeroPaddingNumber(buf, 24, count);
    } else {
      zeroPaddingNumber(buf, value, count);
    }
  }

  private void formatAmPm(StringBuffer buf, HasDateValue date) {
    if (date.getHour() >= 12 && date.getHour() < 24) {
      buf.append(dateTimeFormatInfo.ampms()[1]);
    } else {
      buf.append(dateTimeFormatInfo.ampms()[0]);
    }
  }

  private static void formatDayOfMonth(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getDom();
    zeroPaddingNumber(buf, value, count);
  }

  private void formatDayOfWeek(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getDow() % 7;
    if (count == 5) {
      buf.append(dateTimeFormatInfo.weekdaysNarrow()[value]);
    } else if (count == 4) {
      buf.append(dateTimeFormatInfo.weekdaysFull()[value]);
    } else {
      buf.append(dateTimeFormatInfo.weekdaysShort()[value]);
    }
  }

  private void formatEra(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getYear() >= 0 ? 1 : 0;
    if (count >= 4) {
      buf.append(dateTimeFormatInfo.erasFull()[value]);
    } else {
      buf.append(dateTimeFormatInfo.erasShort()[value]);
    }
  }

  private static void formatFractionalSeconds(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getMillis();
    if (value < 0) {
      value = 1000 - -value % 1000;
      if (value == 1000) {
        value = 0;
      }
    } else {
      value = value % 1000;
    }
    if (count == 1) {
      value = Math.min((value + 50) / 100, 9);
      buf.append((char) ('0' + value));
    } else if (count == 2) {
      value = Math.min((value + 5) / 10, 99);
      zeroPaddingNumber(buf, value, 2);
    } else {
      zeroPaddingNumber(buf, value, 3);

      if (count > 3) {
        zeroPaddingNumber(buf, 0, count - 3);
      }
    }
  }

  private static void formatMinutes(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getMinute();
    zeroPaddingNumber(buf, value, count);
  }

  private void formatMonth(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getMonth() - 1;
    switch (count) {
      case 5:
        buf.append(dateTimeFormatInfo.monthsNarrow()[value]);
        break;
      case 4:
        buf.append(monthsFull[value]);
        break;
      case 3:
        buf.append(dateTimeFormatInfo.monthsShort()[value]);
        break;
      default:
        zeroPaddingNumber(buf, value + 1, count);
    }
  }

  private void formatQuarter(StringBuffer buf, int count, HasDateValue date) {
    int value = (date.getMonth() - 1) / 3;
    if (count < 4) {
      buf.append(dateTimeFormatInfo.quartersShort()[value]);
    } else {
      buf.append(dateTimeFormatInfo.quartersFull()[value]);
    }
  }

  private static void formatSeconds(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getSecond();
    zeroPaddingNumber(buf, value, count);
  }

  private void formatStandaloneDayOfWeek(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getDow() % 7;
    if (count == 5) {
      buf.append(dateTimeFormatInfo.weekdaysNarrowStandalone()[value]);
    } else if (count == 4) {
      buf.append(dateTimeFormatInfo.weekdaysFullStandalone()[value]);
    } else if (count == 3) {
      buf.append(dateTimeFormatInfo.weekdaysShortStandalone()[value]);
    } else {
      zeroPaddingNumber(buf, value, 1);
    }
  }

  private void formatStandaloneMonth(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getMonth() - 1;
    if (count == 5) {
      buf.append(dateTimeFormatInfo.monthsNarrowStandalone()[value]);
    } else if (count == 4) {
      buf.append(dateTimeFormatInfo.monthsFullStandalone()[value]);
    } else if (count == 3) {
      buf.append(dateTimeFormatInfo.monthsShortStandalone()[value]);
    } else {
      zeroPaddingNumber(buf, value + 1, count);
    }
  }

  private static void formatTimeZone(StringBuffer buf, int count, HasDateValue date,
      TimeZone timeZone) {
    if (count < 4) {
      buf.append(timeZone.getShortName(date));
    } else {
      buf.append(timeZone.getLongName(date));
    }
  }

  private static void formatTimeZoneRFC(StringBuffer buf, int count, HasDateValue date,
      TimeZone timeZone) {
    if (count < 3) {
      buf.append(timeZone.getRFCTimeZoneString(date));
    } else if (count == 3) {
      buf.append(timeZone.getISOTimeZoneString(date));
    } else {
      buf.append(timeZone.getGMTString(date));
    }
  }

  private static void formatYear(StringBuffer buf, int count, HasDateValue date) {
    int value = date.getYear();
    if (value < 0) {
      value = -value;
    }
    switch (count) {
      case 1:
        buf.append(value);
        break;
      case 2:
        zeroPaddingNumber(buf, value % 100, 2);
        break;
      default:
        zeroPaddingNumber(buf, value, count);
        break;
    }
  }

  private static int getNextCharCountInPattern(String patt, int start) {
    char ch = patt.charAt(start);
    int next = start + 1;
    while (next < patt.length() && patt.charAt(next) == ch) {
      ++next;
    }
    return next - start;
  }

  private void identifyAbutStart() {
    boolean abut = false;

    int len = patternParts.size();
    for (int i = 0; i < len; i++) {
      if (isNumeric(patternParts.get(i))) {
        if (!abut && i + 1 < len && isNumeric(patternParts.get(i + 1))) {
          abut = true;
          patternParts.get(i).abutStart = true;
        }
      } else {
        abut = false;
      }
    }
  }

  private static boolean isNumeric(PatternPart part) {
    if (part.count <= 0) {
      return false;
    }
    int i = NUMERIC_FORMAT_CHARS.indexOf(part.text.charAt(0));
    return i > 1 || (i >= 0 && part.count < 3);
  }

  private static int matchString(String text, int start, String[] data, int[] pos) {
    int count = data.length;

    int bestMatchLength = 0;
    int bestMatch = -1;
    String textInLowerCase = text.substring(start).toLowerCase();
    for (int i = 0; i < count; ++i) {
      int length = data[i].length();
      if (length > bestMatchLength && textInLowerCase.startsWith(data[i].toLowerCase())) {
        bestMatch = i;
        bestMatchLength = length;
      }
    }
    if (bestMatch >= 0) {
      pos[0] = start + bestMatchLength;
    }
    return bestMatch;
  }

  private DateTime parse(String text, boolean strict) {
    DateTime curDate = new DateTime();
    DateTime date = new DateTime(curDate.getYear(), curDate.getMonth(), curDate.getDom());

    int charsConsumed = parse(text, 0, date, strict);
    if (charsConsumed == 0 || charsConsumed < text.length()) {
      throw new IllegalArgumentException(text);
    }
    return date;
  }

  private int parse(String text, int start, DateTime date, boolean strict) {
    DateRecord cal = new DateRecord();
    int[] parsePos = {start};

    int abutPat = -1;
    int abutStart = 0;
    int abutPass = 0;

    int i = 0;
    while (i < patternParts.size()) {
      PatternPart part = patternParts.get(i);

      if (part.count > 0) {
        if (abutPat < 0 && part.abutStart) {
          abutPat = i;
          abutStart = parsePos[0];
          abutPass = 0;
        }

        if (abutPat >= 0) {
          int count = part.count;
          if (i == abutPat) {
            count -= abutPass++;
            if (count == 0) {
              return 0;
            }
          }

          if (!subParse(text, parsePos, part, count, cal)) {
            parsePos[0] = abutStart;
            i = abutPat;
            continue;
          }

        } else {
          abutPat = -1;
          if (!subParse(text, parsePos, part, 0, cal)) {
            return 0;
          }
        }

      } else {
        abutPat = -1;
        if (part.text.charAt(0) == ' ') {
          int s = parsePos[0];
          skipSpace(text, parsePos);

          if (parsePos[0] > s) {
            i++;
            continue;
          }
        } else if (text.startsWith(part.text, parsePos[0])) {
          parsePos[0] += part.text.length();
          i++;
          continue;
        }

        return 0;
      }

      i++;
    }

    if (!cal.calcDate(date, strict)) {
      return 0;
    }

    return parsePos[0] - start;
  }

  private static int parseInt(String text, int[] pos) {
    int ret = 0;
    int ind = pos[0];
    if (ind >= text.length()) {
      return -1;
    }
    char ch = text.charAt(ind);
    while (ch >= '0' && ch <= '9') {
      ret = ret * 10 + (ch - '0');
      ind++;
      if (ind >= text.length()) {
        break;
      }
      ch = text.charAt(ind);
    }
    if (ind > pos[0]) {
      pos[0] = ind;
    } else {
      ret = -1;
    }
    return ret;
  }

  private void parsePattern(String patt) {
    StringBuffer buf = new StringBuffer(32);
    boolean inQuote = false;

    int i = 0;
    while (i < patt.length()) {
      char ch = patt.charAt(i);

      if (ch == ' ') {
        addPart(buf, 0);
        buf.append(' ');
        addPart(buf, 0);

        while (i + 1 < patt.length() && patt.charAt(i + 1) == ' ') {
          i++;
        }

      } else if (inQuote) {
        if (ch == '\'') {
          if (i + 1 < patt.length() && patt.charAt(i + 1) == '\'') {
            buf.append(ch);
            i++;
          } else {
            inQuote = false;
          }
        } else {
          buf.append(ch);
        }

      } else if (PATTERN_CHARS.indexOf(ch) > 0) {
        addPart(buf, 0);
        buf.append(ch);
        int count = getNextCharCountInPattern(patt, i);
        addPart(buf, count);

        i += count - 1;

      } else if (ch == '\'') {
        if (i + 1 < patt.length() && patt.charAt(i + 1) == '\'') {
          buf.append('\'');
          i++;
        } else {
          inQuote = true;
        }

      } else {
        buf.append(ch);
      }

      i++;
    }

    addPart(buf, 0);

    identifyAbutStart();
  }

  private static boolean parseTimeZoneOffset(String text, int[] pos, DateRecord cal) {
    if (pos[0] >= text.length()) {
      cal.setTzOffset(0);
      return true;
    }

    int sign;
    switch (text.charAt(pos[0])) {
      case '+':
        sign = 1;
        break;
      case '-':
        sign = -1;
        break;
      default:
        cal.setTzOffset(0);
        return true;
    }
    ++(pos[0]);

    int st = pos[0];
    int value = parseInt(text, pos);
    if (value == 0 && pos[0] == st) {
      return false;
    }

    int offset;
    if (pos[0] < text.length() && text.charAt(pos[0]) == ':') {
      offset = value * TimeUtils.MINUTES_PER_HOUR;
      ++(pos[0]);
      st = pos[0];
      value = parseInt(text, pos);
      if (value == 0 && pos[0] == st) {
        return false;
      }
      offset += value;
    } else {
      offset = value;
      if (offset < 24 && (pos[0] - st) <= 2) {
        offset *= TimeUtils.MINUTES_PER_HOUR;
      } else {
        offset = offset % 100 + offset / 100 * TimeUtils.MINUTES_PER_HOUR;
      }
    }

    offset *= sign;
    cal.setTzOffset(-offset);
    return true;
  }

  private static void skipSpace(String text, int[] pos) {
    while (pos[0] < text.length() && WHITE_SPACE.indexOf(text.charAt(pos[0])) >= 0) {
      ++(pos[0]);
    }
  }

  private boolean subFormat(StringBuffer buf, char ch, int count, HasDateValue date,
      HasDateValue adjustedDate, HasDateValue adjustedTime, TimeZone timezone) {
    switch (ch) {
      case 'G':
        formatEra(buf, count, adjustedDate);
        break;
      case 'y':
        formatYear(buf, count, adjustedDate);
        break;
      case 'M':
        formatMonth(buf, count, adjustedDate);
        break;
      case 'k':
        format24Hours(buf, count, adjustedTime);
        break;
      case 'S':
        formatFractionalSeconds(buf, count, adjustedTime);
        break;
      case 'E':
        formatDayOfWeek(buf, count, adjustedDate);
        break;
      case 'a':
        formatAmPm(buf, adjustedTime);
        break;
      case 'h':
        format1To12Hours(buf, count, adjustedTime);
        break;
      case 'K':
        format0To11Hours(buf, count, adjustedTime);
        break;
      case 'H':
        format0To23Hours(buf, count, adjustedTime);
        break;
      case 'c':
        formatStandaloneDayOfWeek(buf, count, adjustedDate);
        break;
      case 'L':
        formatStandaloneMonth(buf, count, adjustedDate);
        break;
      case 'Q':
        formatQuarter(buf, count, adjustedDate);
        break;
      case 'd':
        formatDayOfMonth(buf, count, adjustedDate);
        break;
      case 'm':
        formatMinutes(buf, count, adjustedTime);
        break;
      case 's':
        formatSeconds(buf, count, adjustedTime);
        break;
      case 'z':
        formatTimeZone(buf, count, date, timezone);
        break;
      case 'v':
        buf.append(timezone.getID());
        break;
      case 'Z':
        formatTimeZoneRFC(buf, count, date, timezone);
        break;
      default:
        return false;
    }
    return true;
  }

  private boolean subParse(String text, int[] pos, PatternPart part,
      int digitCount, DateRecord cal) {

    skipSpace(text, pos);

    int start = pos[0];
    char ch = part.text.charAt(0);

    int value = -1;
    if (isNumeric(part)) {
      if (digitCount > 0) {
        if ((start + digitCount) > text.length()) {
          return false;
        }
        value = parseInt(text.substring(0, start + digitCount), pos);
      } else {
        value = parseInt(text, pos);
      }
    }

    switch (ch) {
      case 'G':
        value = matchString(text, start, dateTimeFormatInfo.erasFull(), pos);
        cal.setEra(value);
        return true;

      case 'M':
        return subParseMonth(text, pos, cal, value, start);

      case 'L':
        return subParseStandaloneMonth(text, pos, cal, value, start);

      case 'E':
        return subParseDayOfWeek(text, pos, start, cal);

      case 'c':
        return subParseStandaloneDay(text, pos, start, cal);

      case 'a':
        value = matchString(text, start, dateTimeFormatInfo.ampms(), pos);
        cal.setAmpm(value);
        return true;

      case 'y':
        return subParseYear(text, pos, start, value, part, cal);

      case 'd':
        if (value <= 0) {
          return false;
        }
        cal.setDayOfMonth(value);
        return true;

      case 'S':
        if (value < 0) {
          return false;
        }
        return subParseFractionalSeconds(value, start, pos[0], cal);

      case 'h':
      case 'K':
      case 'H':
        if (ch == 'h' && value == 12) {
          value = 0;
        }
        if (value < 0) {
          return false;
        }
        cal.setHours(value);
        return true;

      case 'k':
        if (value < 0) {
          return false;
        }
        cal.setHours(value);
        return true;

      case 'm':
        if (value < 0) {
          return false;
        }
        cal.setMinutes(value);
        return true;

      case 's':
        if (value < 0) {
          return false;
        }
        cal.setSeconds(value);
        return true;

      case 'Z':
        if (start < text.length() && text.charAt(start) == 'Z') {
          pos[0]++;
          cal.setTzOffset(0);
          return true;
        }
        return subParseTimeZoneInGMT(text, start, pos, cal);

      case 'z':
      case 'v':
        return subParseTimeZoneInGMT(text, start, pos, cal);

      default:
        return false;
    }
  }

  private boolean subParseDayOfWeek(String text, int[] pos, int start, DateRecord cal) {
    int value = matchString(text, start, dateTimeFormatInfo.weekdaysFull(), pos);
    if (value < 0) {
      value = matchString(text, start, dateTimeFormatInfo.weekdaysShort(), pos);
    }
    if (value < 0) {
      return false;
    }
    cal.setDayOfWeek((value + 6) % 7 + 1);
    return true;
  }

  private static boolean subParseFractionalSeconds(int value, int start, int end, DateRecord cal) {
    int i = end - start;
    int v = value;

    if (i < 3) {
      while (i < 3) {
        v *= 10;
        i++;
      }
    } else {
      int a = 1;
      while (i > 3) {
        a *= 10;
        i--;
      }
      v = (v + (a >> 1)) / a;
    }
    cal.setMilliseconds(v);
    return true;
  }

  private boolean subParseMonth(String text, int[] pos, DateRecord cal, int value, int start) {
    int v = value;
    if (v < 0) {
      v = matchString(text, start, monthsFull, pos);
      if (v < 0) {
        v = matchString(text, start, dateTimeFormatInfo.monthsShort(), pos);
      }
      if (v < 0) {
        return false;
      }
      cal.setMonth(v + 1);
      return true;
    } else if (v > 0) {
      cal.setMonth(v);
      return true;
    }
    return false;
  }

  private boolean subParseStandaloneDay(String text, int[] pos, int start, DateRecord cal) {
    int value = matchString(text, start, dateTimeFormatInfo.weekdaysFullStandalone(), pos);
    if (value < 0) {
      value = matchString(text, start, dateTimeFormatInfo.weekdaysShortStandalone(), pos);
    }
    if (value < 0) {
      return false;
    }
    cal.setDayOfWeek((value + 6) % 7 + 1);
    return true;
  }

  private boolean subParseStandaloneMonth(String text, int[] pos,
      DateRecord cal, int value, int start) {
    int v = value;
    if (v < 0) {
      v = matchString(text, start, dateTimeFormatInfo.monthsFullStandalone(), pos);
      if (v < 0) {
        v = matchString(text, start, dateTimeFormatInfo.monthsShortStandalone(), pos);
      }
      if (v < 0) {
        return false;
      }
      cal.setMonth(v + 1);
      return true;
    } else if (v > 0) {
      cal.setMonth(v);
      return true;
    }
    return false;
  }

  private static boolean subParseTimeZoneInGMT(String text, int start, int[] pos, DateRecord cal) {
    if (text.startsWith(GMT, start)) {
      pos[0] = start + GMT.length();
      return parseTimeZoneOffset(text, pos, cal);
    }
    if (text.startsWith(UTC, start)) {
      pos[0] = start + UTC.length();
      return parseTimeZoneOffset(text, pos, cal);
    }

    return parseTimeZoneOffset(text, pos, cal);
  }

  private static boolean subParseYear(String text, int[] pos, int start, int value,
      PatternPart part, DateRecord cal) {
    char ch = ' ';
    int v = value;
    if (v < 0) {
      if (pos[0] >= text.length()) {
        return false;
      }
      ch = text.charAt(pos[0]);
      if (ch != '+' && ch != '-') {
        return false;
      }
      ++(pos[0]);
      v = parseInt(text, pos);
      if (v < 0) {
        return false;
      }
      if (ch == '-') {
        v = -v;
      }
    }

    if (ch == ' ' && (pos[0] - start) == 2 && part.count == 2) {
      int defaultCenturyStartYear = TimeUtils.today().getYear() - 80;
      int ambiguousTwoDigitYear = defaultCenturyStartYear % 100;
      v += (defaultCenturyStartYear / 100) * 100 + (v < ambiguousTwoDigitYear ? 100 : 0);
    }
    cal.setYear(v);
    return true;
  }

  private static void zeroPaddingNumber(StringBuffer buf, int value, int minWidth) {
    int b = NUMBER_BASE;
    for (int i = 0; i < minWidth - 1; i++) {
      if (value < b) {
        buf.append('0');
      }
      b *= NUMBER_BASE;
    }
    buf.append(value);
  }
}
