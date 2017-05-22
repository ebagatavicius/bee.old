package com.butent.bee.shared.utils;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.Grego;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Tests {@link com.butent.bee.shared.time.TimeUtils}.
 */
@SuppressWarnings("static-method")
public class TestTimeUtils {

  public static final long MAX_RANDOM_REQUEST = 1000L;

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testDateDiff() {
    DateTime start = new DateTime(2011, 3, 7, 11, 50, 10, 0);
    DateTime end = new DateTime(2012, 3, 8, 12, 40, 20, 0);

    assertEquals(367, TimeUtils.dateDiff(start, end));
  }

  @Test
  public final void testDayOfWeek() {
    assertEquals(2, Grego.dayOfWeek(11));
    assertEquals(1, Grego.dayOfWeek(17));
    assertEquals(2, Grego.dayOfWeek(18));
    assertEquals(3, Grego.dayOfWeek(19));
    assertEquals(4, Grego.dayOfWeek(20));
  }

  @Test
  public final void testFieldDifference() {

    DateTime start = new DateTime();
    start.setTime(1298362388227L);
    DateTime end = new DateTime();
    end.setTime(1298362388327L);

    assertEquals(100,
        TimeUtils.fieldDifference(start, end, TimeUtils.FIELD_MILLISECOND));
    assertEquals(-100,
        TimeUtils.fieldDifference(end, start, TimeUtils.FIELD_MILLISECOND));
    assertEquals(0,
        TimeUtils.fieldDifference(start, start, TimeUtils.FIELD_MILLISECOND));

    DateTime start1 = new DateTime(2010, 3, 7, 7, 10, 10);

    DateTime start2 = new DateTime(2011, 3, 7, 17, 30, 20);
    DateTime start3 = new DateTime(2011, 2, 7, 10, 30, 10);

    assertEquals(365, TimeUtils.fieldDifference(start1, start2, TimeUtils.FIELD_DAY_OF_YEAR));
    assertEquals(12, TimeUtils.fieldDifference(start1, start2, TimeUtils.FIELD_MONTH));
    assertEquals(1, TimeUtils.fieldDifference(start1, start2, TimeUtils.FIELD_YEAR));
    assertEquals(52, TimeUtils.fieldDifference(start1, start2,
        TimeUtils.FIELD_DAY_OF_WEEK_IN_MONTH));

    assertEquals(352, TimeUtils.fieldDifference(start1, start, TimeUtils.FIELD_DAY_OF_YEAR));
    assertEquals(11, TimeUtils.fieldDifference(start1, start, TimeUtils.FIELD_MONTH));
    assertEquals(0, TimeUtils.fieldDifference(start1, start, TimeUtils.FIELD_YEAR));
    assertEquals(50, TimeUtils.fieldDifference(start1, start,
        TimeUtils.FIELD_DAY_OF_WEEK_IN_MONTH));

    assertEquals(14, TimeUtils.fieldDifference(start3, start, TimeUtils.FIELD_DAY_OF_YEAR));
    assertEquals(0, TimeUtils.fieldDifference(start3, start, TimeUtils.FIELD_MONTH));
    assertEquals(0, TimeUtils.fieldDifference(start3, start, TimeUtils.FIELD_YEAR));
    assertEquals(2, TimeUtils.fieldDifference(start3, start,
        TimeUtils.FIELD_DAY_OF_WEEK_IN_MONTH));

    DateTime start4 = new DateTime(2011, 3, 1);
    DateTime end1 = new DateTime(2012, 3, 1);

    assertEquals(366, TimeUtils.fieldDifference(start4, end1, TimeUtils.FIELD_DAY_OF_YEAR));
    assertEquals(12, TimeUtils.fieldDifference(start4, end1, TimeUtils.FIELD_MONTH));
    assertEquals(1, TimeUtils.fieldDifference(start4, end1, TimeUtils.FIELD_YEAR));
    assertEquals(2, TimeUtils.fieldDifference(start3, start,
        TimeUtils.FIELD_DAY_OF_WEEK_IN_MONTH));

    DateTime start5 = new DateTime(2011, 2, 7, 11, 30, 20);
    DateTime start6 = new DateTime(2011, 2, 8, 12, 30, 20);

    assertEquals(0, TimeUtils.fieldDifference(start3, start5, TimeUtils.FIELD_AM_PM));
    assertEquals(2, TimeUtils.fieldDifference(start3, start6, TimeUtils.FIELD_AM_PM));
    assertEquals(60, TimeUtils.fieldDifference(start3, start5, TimeUtils.FIELD_MINUTE));
    assertEquals(3610, TimeUtils.fieldDifference(start3, start5, TimeUtils.FIELD_SECOND));
    assertEquals(1, TimeUtils.fieldDifference(start5, start6, TimeUtils.FIELD_DAY_OF_WEEK));
  }

  @Test
  public final void testFieldsToDay() {
    assertEquals(15075, Grego.fieldsToDay(2011, 4, 11));
  }

  @Test
  public final void testGoMonth() {
    JustDate date = new JustDate(2016, 5, 31);

    assertEquals(TimeUtils.goMonth(date, 0), date);

    assertEquals(TimeUtils.goMonth(date, -1), new JustDate(2016, 4, 30));
    assertEquals(TimeUtils.goMonth(date, -2), new JustDate(2016, 3, 31));
    assertEquals(TimeUtils.goMonth(date, -3), new JustDate(2016, 2, 29));
    assertEquals(TimeUtils.goMonth(date, -5), new JustDate(2015, 12, 31));

    assertEquals(TimeUtils.goMonth(date, 1), new JustDate(2016, 6, 30));
    assertEquals(TimeUtils.goMonth(date, 2), new JustDate(2016, 7, 31));

    date = new JustDate(2016, 5, 30);
    assertEquals(TimeUtils.goMonth(date, -1), new JustDate(2016, 4, 30));
    assertEquals(TimeUtils.goMonth(date, -2), new JustDate(2016, 3, 30));
    assertEquals(TimeUtils.goMonth(date, -3), new JustDate(2016, 2, 29));
    assertEquals(TimeUtils.goMonth(date, -5), new JustDate(2015, 12, 30));

    assertEquals(TimeUtils.goMonth(date, 1), new JustDate(2016, 6, 30));
    assertEquals(TimeUtils.goMonth(date, 2), new JustDate(2016, 7, 30));

    date = new JustDate(2016, 6, 1);
    assertEquals(TimeUtils.goMonth(date, -100 * 12), new JustDate(1916, 6, 1));
  }

  @Test
  public final void testIsDateOrDateTime() {
    assertTrue(TimeUtils.isDateOrDateTime(new DateTime(2011)));
    assertTrue(TimeUtils.isDateOrDateTime(new JustDate()));
    assertTrue(TimeUtils.isDateOrDateTime(new Date()));
    assertFalse(TimeUtils.isDateOrDateTime(null));
    assertFalse(TimeUtils.isDateOrDateTime("2011-12-20"));
    assertTrue(TimeUtils
        .isDateOrDateTime(new java.sql.Date(1298362388227L)));
    assertFalse(TimeUtils.isDateOrDateTime(1298362388227L));
  }

  @Test
  public final void testMillisToString() {
    assertEquals("500", TimeUtils.millisToString(500));
    assertEquals("010", TimeUtils.millisToString(10));
    assertEquals("5000", TimeUtils.millisToString(5000));
  }

  @Test
  public final void testRandomDate() {
    JustDate min = new JustDate(2011, 3, 7);
    JustDate max = new JustDate(2012, 3, 7);

    for (int i = 0; i < MAX_RANDOM_REQUEST; i++) {

      JustDate dt = TimeUtils.randomDate(min, max);

      assertEquals(true, (dt.compareTo(min) >= 0)
          && (dt.compareTo(max) <= 0));
    }
  }

  @Test
  public final void testRandomDateTime() {
    DateTime min = new DateTime(2011, 3, 7, 12, 25, 10);
    DateTime max = new DateTime(2012, 3, 7, 14, 15, 17);

    for (int i = 0; i < MAX_RANDOM_REQUEST; i++) {

      DateTime dt = TimeUtils.randomDateTime(min, max);

      assertEquals(true, (dt.compareTo(min) >= 0)
          && (dt.compareTo(max) <= 0));
    }
  }

  @Test
  public final void testStartOfWeekYear() {
    JustDate prev = new JustDate(2012, 12, 31);
    JustDate next = new JustDate(2013, 1, 7);

    assertEquals(TimeUtils.startOfWeekYear(2013, 0), prev);
    assertEquals(TimeUtils.startOfWeekYear(2013, 6), prev);
    assertEquals(TimeUtils.startOfWeekYear(2013, 7), next);
    assertEquals(TimeUtils.startOfWeekYear(2013, 100), next);

    prev = new JustDate(2011, 12, 26);
    next = new JustDate(2012, 1, 2);

    assertEquals(TimeUtils.startOfWeekYear(2012, 0), prev);
    assertEquals(TimeUtils.startOfWeekYear(2012, 1), prev);
    assertEquals(TimeUtils.startOfWeekYear(2012, 2), next);
    assertEquals(TimeUtils.startOfWeekYear(2012, 7), next);
  }

  @Test
  public final void testToDate() {
    JustDate first = new JustDate(2011, 2, 22);
    DateTime dt = new DateTime(2011, 2, 22);
    Date d = new Date(1298362388227L);

    assertEquals(first, TimeUtils.toDate(first));
    assertEquals(true, TimeUtils.toDate(dt).compareTo(first) == 0);
    assertEquals(true, TimeUtils.toDate(d).compareTo(first) == 0);
  }

  @Test
  public final void testToDateTime() {
    DateTime first = new DateTime(2011, 3, 29, 8, 54, 6, 875);
    DateTime first2 = new DateTime(2011, 3, 29);
    JustDate second = new JustDate(2011, 3, 29);

    DateTime dt = new DateTime(2011, 3, 29, 8, 54, 6, 875);
    Date d = new Date(1301378046875L);

    assertEquals(first, TimeUtils.toDateTime(first));
    assertEquals(true, TimeUtils.toDateTime(dt).compareTo(first) == 0);
    assertEquals(true, TimeUtils.toDateTime(d).compareTo(first) == 0);
    assertEquals(true, TimeUtils.toDateTime(second).compareTo(first2) == 0);
  }

  @Test
  public final void testToJava() {
    Date first = new Date(1298362388227L);
    Date first2 = new Date(1301346000000L);

    DateTime dt = new DateTime(2011, 2, 22, 10, 13, 8, 227);
    JustDate d = new JustDate(2011, 3, 29);

    assertEquals(first, TimeUtils.toJava(first));
    assertEquals(true, TimeUtils.toJava(dt).compareTo(first) == 0);
    assertEquals(true, TimeUtils.toJava(d).compareTo(first2) == 0);
  }

  @Test
  public final void testWeekOfYear() {
    JustDate d = new JustDate(2000, 1, 1);
    assertEquals(TimeUtils.weekOfYear(d), 52);

    d.setYear(2001);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2002);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2003);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2004);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2005);
    assertEquals(TimeUtils.weekOfYear(d), 53);

    d.setYear(2006);
    assertEquals(TimeUtils.weekOfYear(d), 52);

    d.setYear(2007);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2008);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2009);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2010);
    assertEquals(TimeUtils.weekOfYear(d), 53);

    d.setYear(2011);
    assertEquals(TimeUtils.weekOfYear(d), 52);

    d.setYear(2012);
    assertEquals(TimeUtils.weekOfYear(d), 52);

    d.setYear(2013);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    d.setYear(2014);
    assertEquals(TimeUtils.weekOfYear(d), 1);

    DateTime dt = new DateTime(2013, 1, 13, 23, 59, 59);
    assertEquals(TimeUtils.weekOfYear(dt), 2);

    dt = new DateTime(2013, 1, 14);
    assertEquals(TimeUtils.weekOfYear(dt), 3);
  }

  @Test
  public final void testWeekOfYearMin() {
    JustDate d = new JustDate(2013, 1, 1);

    assertEquals(TimeUtils.weekOfYear(d, 0), 1);
    assertEquals(TimeUtils.weekOfYear(d, 6), 1);

    int diff = TimeUtils.dayDiff(TimeUtils.startOfWeekYear(2012, 7),
        TimeUtils.startOfWeekYear(2013, 7));
    assertEquals(diff, 53 * 7);

    assertEquals(TimeUtils.weekOfYear(d, 7), 53);
    assertEquals(TimeUtils.weekOfYear(d, 10), 53);
  }

  @Test
  public final void testYearToString() {
    assertEquals("1995", TimeUtils.yearToString(1995));
    assertEquals("0", TimeUtils.yearToString(0));
  }
}
