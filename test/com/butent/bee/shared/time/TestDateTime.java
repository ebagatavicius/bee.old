package com.butent.bee.shared.time;

import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.i18n.DateOrdering;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.time.DateTime}.
 */
public class TestDateTime {

  private DateTime varDate;

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    varDate = new DateTime(1298362388227L);
    DateTime dt = new DateTime(1298362388227L);

    assertEquals(0, varDate.compareTo(dt));
    dt = new DateTime(2011, 2, 22, 10, 13, 8, 446);
    assertEquals(-1, varDate.compareTo(dt));

    dt = new DateTime(2011, 2, 22);
    assertEquals(1, varDate.compareTo(dt));
  }

  @Test
  public final void testDateTime() {
    varDate = new DateTime();
    assertEquals(System.currentTimeMillis(), varDate.getTime(), 1000);
  }

  @SuppressWarnings("static-method")
  @Test
  public final void testDateTimeDate() {
    DateTime dt = new DateTime(new java.util.Date());
    assertEquals(System.currentTimeMillis(), dt.getTime(), 1000);

    dt = new DateTime((java.util.Date) null);
    assertEquals(0, dt.getTime());
  }

  @Test
  public final void testDateTimeIntIntIntIntIntInt() {
    varDate = new DateTime(2011, 2, 22, 10, 13, 8);
    assertEquals(1298362388000L, varDate.getTime());
  }

  @Test
  public final void testDateTimeJustDate() {
    JustDate jd = new JustDate(2011, 2, 22);
    varDate = new DateTime(jd);

    assertEquals(2011, varDate.getYear());
    assertEquals(2, varDate.getMonth());
    assertEquals(22, varDate.getDom());

    try {
      varDate = new DateTime((JustDate) null);
      assertEquals(0, varDate.getTime());
    } catch (BeeRuntimeException e) {
      assertTrue(true);
    } catch (Exception e) {
      fail("Java lang error. Need BeeRuntimeException " + e.getMessage());
    }
  }

  @Test
  public final void testClearTimePart() {
    DateTime dt = new DateTime();
    JustDate jd = dt.getDate();
    dt.clearTimePart();
    assertEquals(dt, jd.getDateTime());
  }

  @Test
  public final void testDeserialize() {
    String st = "1298362388227";
    varDate = DateTime.restore(st);

    assertEquals(1298362388227L, varDate.getTime());

    st = "1298362388446";
    varDate.deserialize(st);

    assertEquals(1298362388446L, varDate.getTime());

    st = "1298332800000";
    varDate.deserialize(st);

    assertEquals(1298332800000L, varDate.getTime());
  }

  @Test
  public final void testEqualsObject() {
    DateTime dt = new DateTime(1298362388227L);
    varDate = new DateTime(1298362388227L);
    assertEquals(true, varDate.equals(dt));

    dt = new DateTime(2011, 2, 22, 8, 13, 8);
    assertEquals(false, varDate.equals(dt));

    dt = null;
    assertEquals(false, varDate.equals(dt));
  }

  @Test
  public final void testGetDom() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(22, varDate.getDom());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(22, varDate.getDom());

    varDate = new DateTime(2011, 2, 29, 0, 0, 0, 0);
    assertEquals(1, varDate.getDom());
    assertEquals(3, varDate.getMonth());
  }

  @Test
  public final void testGetDow() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(2, varDate.getDow());

    varDate = new DateTime(2012, 9, 30, 0, 0, 0, 0);
    assertEquals(7, varDate.getDow());

    varDate = new DateTime(2012, 10, 1, 2, 1, 2);
    assertEquals(1, varDate.getDow());
  }

  @Test
  public final void testGetDoy() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(53, varDate.getDoy());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(53, varDate.getDoy());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(89, varDate.getDoy());

    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(65, varDate.getDoy());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(78, varDate.getDoy());

    varDate = new DateTime(2011, 1, 1, 5, 1, 45);
    assertEquals(1, varDate.getDoy());

    varDate = new DateTime(2011, 12, 31, 5, 1, 45);
    assertEquals(365, varDate.getDoy());

    varDate = new DateTime(2012, 12, 31, 5, 1, 45);
    assertEquals(366, varDate.getDoy());
  }

  @Test
  public final void testGetHour() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(8, varDate.getHour());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getHour());

    varDate = new DateTime(2011, 2, 22, 24, 0, 0, 0);
    assertEquals(0, varDate.getHour());
    assertEquals(23, varDate.getDom());

    varDate = new DateTime(2011, 3, 27, 4, 1, 2);
    assertEquals(4, varDate.getHour());
  }

  @Test
  public final void testGetMillis() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(446, varDate.getMillis());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getMillis());
  }

  @Test
  public final void testGetMinute() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(13, varDate.getMinute());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getMinute());
  }

  @Test
  public final void testGetMonth() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(2, varDate.getMonth());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(2, varDate.getMonth());
  }

  @Test
  public final void testGetSecond() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(8, varDate.getSecond());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getSecond());
  }

  @Test
  public final void testGetTime() {
    varDate = new DateTime(2011, 2, 22, 10, 13, 8, 446);
    java.util.Date dt = new java.util.Date(1298362388446L);
    assertEquals(dt.getTime(), varDate.getTime());

    varDate = new DateTime(2011, 2, 22, 2, 0, 0, 0);
    dt = new java.util.Date(1298332800000L);
    assertEquals(dt.getTime(), varDate.getTime());
  }

  @Test
  public final void testGetTimezoneOffset() {
    varDate = new DateTime(2017, 3, 26);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(2017, 3, 27);
    assertEquals(-180, varDate.getTimezoneOffset());

    varDate = new DateTime(2017, 3, 26, 2, 59, 59, 999);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(2017, 3, 26, 3, 0, 0);
    assertEquals(-180, varDate.getTimezoneOffset());
    varDate = new DateTime(2017, 3, 26, 3, 0, 0, 1);
    assertEquals(-180, varDate.getTimezoneOffset());

    varDate = new DateTime(-1L);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(0L);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(2 * TimeUtils.MILLIS_PER_HOUR);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(2 * TimeUtils.MILLIS_PER_HOUR + 1);
    assertEquals(-120, varDate.getTimezoneOffset());

    varDate = new DateTime(1969, 1, 1);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(1920, 1, 1);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(1910, 1, 1);
    assertEquals(-120, varDate.getTimezoneOffset());
    varDate = new DateTime(1900, 1, 1);
    assertEquals(-120, varDate.getTimezoneOffset());
  }

  @Test
  public final void testGetUtcDom() {
    varDate = new DateTime(2011, 2, 22, 10, 13, 8, 446);
    assertEquals(22, varDate.getUtcDom());

    varDate = new DateTime(2011, 2, 22, 2, 0, 0, 0);
    assertEquals(22, varDate.getUtcDom());
    varDate = new DateTime(2011, 3, 26);
    assertEquals(25, varDate.getUtcDom());
    varDate = new DateTime(2011, 3, 27);
    assertEquals(26, varDate.getUtcDom());
  }

  @Test
  public final void testGetUtcDow() {
    varDate = new DateTime(2011, 2, 22, 10, 13, 8, 446);
    assertEquals(2, varDate.getUtcDow());

    varDate = new DateTime(2011, 2, 22, 2, 0, 0, 0);
    assertEquals(2, varDate.getUtcDow());
    varDate = new DateTime(2012, 9, 29, 12, 0, 0);
    assertEquals(6, varDate.getUtcDow());
    varDate = new DateTime(2012, 9, 30, 12, 0, 0);
    assertEquals(7, varDate.getUtcDow());
  }

  @Test
  public final void testGetUtcDoy() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(53, varDate.getUtcDoy());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(52, varDate.getUtcDoy());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(23, varDate.getUtcHour());
    assertEquals(88, varDate.getUtcDoy());

    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(65, varDate.getUtcDoy());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(78, varDate.getUtcDoy());
  }

  @Test
  public final void testGetUtcHour() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(6, varDate.getUtcHour());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(3, varDate.getUtcHour());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(3, varDate.getUtcHour());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(22, varDate.getUtcHour());

    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals(1, varDate.getUtcHour());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(23, varDate.getUtcHour());
  }

  @Test
  public final void testGetUtcMillis() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(446, varDate.getUtcMillis());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(0, varDate.getUtcMillis());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(0, varDate.getUtcMillis());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getUtcMillis());
    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals(0, varDate.getUtcMillis());
    assertEquals(0, varDate.getUtcMillis());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(0, varDate.getUtcMillis());
    assertEquals(0, varDate.getUtcMillis());
  }

  @Test
  public final void testGetUtcMinute() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(13, varDate.getUtcMinute());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(1, varDate.getUtcMinute());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(1, varDate.getUtcMinute());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getUtcMinute());
    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals(1, varDate.getUtcMinute());
    assertEquals(1, varDate.getUtcMinute());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(1, varDate.getUtcMinute());
    assertEquals(1, varDate.getUtcMinute());
  }

  @Test
  public final void testGetUtcMonth() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(2, varDate.getUtcMonth());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(3, varDate.getUtcMonth());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(3, varDate.getUtcMonth());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(2, varDate.getUtcMonth());
    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals(3, varDate.getUtcMonth());
    assertEquals(3, varDate.getUtcMonth());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(3, varDate.getUtcMonth());
    assertEquals(3, varDate.getUtcMonth());

    varDate = new DateTime(2011, 1, 30, 2, 1, 2);
    assertEquals(1, varDate.getUtcMonth());
    assertEquals(1, varDate.getUtcMonth());

    varDate = new DateTime(2011, 12, 30, 2, 1, 2);
    assertEquals(12, varDate.getUtcMonth());
    assertEquals(12, varDate.getUtcMonth());
  }

  @Test
  public final void testGetUtcSecond() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(8, varDate.getUtcSecond());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(45, varDate.getUtcSecond());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals(45, varDate.getUtcSecond());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(0, varDate.getUtcSecond());
    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals(45, varDate.getUtcSecond());

    varDate = new DateTime(2011, 3, 30, 2, 1, 2);
    assertEquals(2, varDate.getUtcSecond());

    varDate = new DateTime(2011, 1, 30, 2, 1, 2);
    assertEquals(2, varDate.getUtcSecond());

    varDate = new DateTime(2011, 12, 30, 2, 1, 2);
    assertEquals(2, varDate.getUtcSecond());
  }

  @Test
  public final void testGetUtcYear() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(2011, varDate.getUtcYear());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals(2011, varDate.getUtcYear());
    varDate = new DateTime(2011, 1, 1, 1, 1, 45);
    assertEquals(2010, varDate.getUtcYear());
  }

  @Test
  public final void testGetYear() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals(2011, varDate.getYear());

    varDate = new DateTime(2011, 2, 22, 0, 0, 0, 0);
    assertEquals(2011, varDate.getYear());
  }

  @Test
  public final void testHashCode() {
    varDate = new DateTime(2011, 2, 22, 10, 13, 8, 446);
    assertEquals(Long.valueOf(varDate.getTime()).hashCode(), varDate.hashCode());
  }

  @Test
  public final void testParse() {
    String s1 = "2011-02-22";
    DateTime d1 = TimeUtils.parseDateTime(s1, DateOrdering.YMD);
    varDate = new DateTime(2011, 2, 22);

    assertEquals(d1.getTime(), varDate.getTime());

    s1 = "21:02:52";
    d1 = TimeUtils.parseDateTime(s1, DateOrdering.YMD);
    varDate = new DateTime(2021, 2, 1);
    assertEquals(d1.getTime(), varDate.getTime());

    s1 = "2011-02-22 10:15:10,5";
    d1 = TimeUtils.parseDateTime(s1, DateOrdering.YMD);
    varDate = new DateTime(2011, 2, 22, 10, 15, 10, 5);
    assertEquals(d1.getTime(), varDate.getTime());

    s1 = "11/02/22 10:15:10,5";
    d1 = TimeUtils.parseDateTime(s1, DateOrdering.YMD);
    varDate = new DateTime(2011, 2, 22, 10, 15, 10, 5);
    assertEquals(d1.getTime(), varDate.getTime());

    s1 = "2011-2";
    d1 = TimeUtils.parseDateTime(s1, DateOrdering.YMD);
    varDate = new DateTime(2011, 2, 0);
    assertEquals(varDate.toString(), d1.toString());

    DateTime dn = TimeUtils.parseDateTime("2011-", DateOrdering.YMD);
    assertEquals("2011-01-01", dn.toString());
  }

  @Test
  public final void testSerialize() {
    varDate = new DateTime(2011, 2, 22, 2, 0, 0, 0);
    String a = varDate.serialize();
    assertEquals("1298332800000", a);

    varDate = new DateTime(2011, 2, 22, 10, 13, 8, 227);
    a = varDate.serialize();
    assertEquals("1298362388227", a);
  }

  @Test
  public final void testSetTime() {
    varDate = new DateTime();
    varDate.setTime(1298362388227L);
    assertEquals(1298362388227L, varDate.getTime());
    assertEquals(2011, varDate.getYear());
    assertEquals(2, varDate.getMonth());
    assertEquals(22, varDate.getDom());
    assertEquals(8, varDate.getUtcHour());
    assertEquals(10, varDate.getHour());
    assertEquals(13, varDate.getMinute());
    assertEquals(8, varDate.getSecond());
    assertEquals(227, varDate.getMillis());
  }

  @Test
  public final void testToString() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals("2011-02-22 08:13:08.446", varDate.toString());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45, 1);
    assertEquals("2011-03-06 05:01:45.001", varDate.toString());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45, 10);
    assertEquals("2011-03-19 05:01:45.010", varDate.toString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals("2011-03-27 04:01:45", varDate.toString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 0);
    assertEquals("2011-03-27 04:01", varDate.toString());

    varDate = new DateTime(2011, 3, 27, 4, 0, 0);
    assertEquals("2011-03-27 04:00", varDate.toString());

    varDate = new DateTime(2011, 3, 27);
    assertEquals("2011-03-27", varDate.toString());

    varDate = new DateTime(2011, 1, 1, 0, 1, 2);
    assertEquals("2011-01-01 00:01:02", varDate.toString());

    varDate = new DateTime(2011, 1, 1, 0, 1, 0);
    assertEquals("2011-01-01 00:01", varDate.toString());

    varDate = new DateTime(2011, 1, 1, 0, 0, 0);
    assertEquals("2011-01-01", varDate.toString());
  }

  @Test
  public final void testToTimeString() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals("08:13:08.446", varDate.toTimeString());

    varDate = new DateTime(2011, 3, 6, 5, 1, 45, 1);
    assertEquals("05:01:45.001", varDate.toTimeString());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45, 10);
    assertEquals("05:01:45.010", varDate.toTimeString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals("04:01:45", varDate.toTimeString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 0);
    assertEquals("04:01", varDate.toTimeString());

    varDate = new DateTime(2011, 3, 27, 4, 0, 0);
    assertEquals("04:00", varDate.toTimeString());

    varDate = new DateTime(2011, 3, 27, 0, 0, 0);
    assertEquals("00:00", varDate.toTimeString());

    varDate = new DateTime(2011, 1, 1, 0, 1, 2);
    assertEquals("00:01:02", varDate.toTimeString());
  }

  @Test
  public final void testToUtcDateString() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals("2011-02-22", varDate.toUtcDateString());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45);
    assertEquals("2011-03-06", varDate.toUtcDateString());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45);
    assertEquals("2011-03-19", varDate.toUtcDateString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals("2011-03-27", varDate.toUtcDateString());

    varDate = new DateTime(2011, 1, 1, 0, 1, 2);
    assertEquals("2010-12-31", varDate.toUtcDateString());
  }

  @Test
  public final void testToUtcString() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals("2011-02-22 06:13:08.446", varDate.toUtcString());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45, 1);
    assertEquals("2011-03-06 03:01:45.001", varDate.toUtcString());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45, 10);
    assertEquals("2011-03-19 03:01:45.010", varDate.toUtcString());

    varDate = new DateTime(2011, 1, 1, 0, 1, 2);
    assertEquals("2010-12-31 22:01:02", varDate.toUtcString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals("2011-03-27 01:01:45", varDate.toUtcString());
  }

  @Test
  public final void testToUtcTimeString() {
    varDate = new DateTime(2011, 2, 22, 8, 13, 8, 446);
    assertEquals("06:13:08.446", varDate.toUtcTimeString());
    varDate = new DateTime(2011, 3, 6, 5, 1, 45, 1);
    assertEquals("03:01:45.001", varDate.toUtcTimeString());

    varDate = new DateTime(2011, 3, 19, 5, 1, 45, 10);
    assertEquals("03:01:45.010", varDate.toUtcTimeString());

    varDate = new DateTime(2011, 1, 1, 0, 1, 2);
    assertEquals("22:01:02", varDate.toUtcTimeString());

    varDate = new DateTime(2011, 3, 27, 4, 1, 45);
    assertEquals("01:01:45", varDate.toUtcTimeString());
  }
}
