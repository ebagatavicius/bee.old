package com.butent.bee.shared.testutils;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Tests {@link com.butent.bee.shared.time.JustDate}.
 */
@SuppressWarnings("static-method")
public class TestJustDate {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    JustDate jd = new JustDate(2011, 4, 8);
    JustDate jd1 = new JustDate(1298362388227L);
    JustDate jd2 = new JustDate(2021, 4, 8);
    JustDate jd3 = TimeUtils.parseDate("2011,04,08");

    assertEquals(true, jd.compareTo(jd1) > 0);
    assertEquals(0, jd.compareTo(jd3));
    assertEquals(true, jd.compareTo(jd2) < 0);
  }

  @Test
  public final void testDeserialize() {
    JustDate a = new JustDate();
    a.deserialize("10");

    assertEquals(1970, a.getYear());
    assertEquals(1, a.getMonth());
    assertEquals(11, a.getDom());

    a = new JustDate();
    a.deserialize("-10");

    assertEquals(1969, a.getYear());
    assertEquals(12, a.getMonth());
    assertEquals(22, a.getDom());
  }

  @Test
  public final void testEqualsObject() {
    JustDate jd = new JustDate(1298362388227L);

    assertEquals(true, jd.equals(new JustDate(2011, 2, 22)));
    assertEquals(false, jd.equals(new DateTime(2011, 2, 22)));
  }

  @Test
  public final void testGetDay() {
    JustDate dt = new JustDate(1970, 1, 11);
    assertEquals(10, dt.getDays());

    dt = new JustDate(1969, 12, 22);
    assertEquals(-10, dt.getDays());

    dt = new JustDate(1970, 2, 6);

    assertEquals(36, dt.getDays());
  }

  @Test
  public final void testGetDom() {
    JustDate jd = new JustDate(2011, 2, 25);

    assertEquals(25, jd.getDom());

    jd = new JustDate(2011, 2, 29);
    assertEquals(1, jd.getDom());
    assertEquals(3, jd.getMonth());

    jd = new JustDate(1298362388227L);
    assertEquals(22, jd.getDom());

    jd = new JustDate(2011, 2, -1);
    assertEquals(31, jd.getDom());
    assertEquals(1, jd.getMonth());

    jd = new JustDate(2011, 2, 0);
    assertEquals(1, jd.getDom());
    assertEquals(2, jd.getMonth());
  }

  @Test
  public final void testGetDow() {
    JustDate jd = new JustDate(2011, 2, 25);
    assertEquals(25, jd.getDom());

    jd = new JustDate(2011, 2, 29);
    assertEquals(2, jd.getDow());
    assertEquals(3, jd.getMonth());

    jd = new JustDate(2011, 3, -1);
    assertEquals(1, jd.getDow());
    assertEquals(2, jd.getMonth());

    jd = new JustDate(2011, 3, 0);
    assertEquals(2, jd.getDow());
    assertEquals(3, jd.getMonth());
  }

  @Test
  public final void testGetDoy() {
    JustDate jd = new JustDate(2011, 2, 22);
    assertEquals(53, jd.getDoy());

    jd = new JustDate(2011, 2, 22);
    assertEquals(53, jd.getDoy());

    jd = new JustDate(2011, 3, 30);
    assertEquals(89, jd.getDoy());

    jd = new JustDate(2011, 3, 6);
    assertEquals(65, jd.getDoy());

    jd = new JustDate(2011, 3, 19);
    assertEquals(78, jd.getDoy());

    jd = new JustDate(2011, 1, 1);
    assertEquals(1, jd.getDoy());

    jd = new JustDate(2011, 12, 31);
    assertEquals(365, jd.getDoy());

    jd = new JustDate(2012, 12, 31);
    assertEquals(366, jd.getDoy());
  }

  @Test
  public final void testGetMonth() {

    JustDate dt = new JustDate(2011, 1, 12);
    assertEquals(1, dt.getMonth());

    dt = new JustDate(2011, 12, 12);
    assertEquals(12, dt.getMonth());

    dt = new JustDate(2012, 0, 1);
    assertEquals(2012, dt.getYear());
    assertEquals(1, dt.getMonth());
    assertEquals(1, dt.getDom());

    dt = new JustDate(2011, 0, 1);
    assertEquals(2011, dt.getYear());
    assertEquals(1, dt.getMonth());

    dt = new JustDate(2011, -1, 1);
    assertEquals(2011, dt.getYear());
    assertEquals(1, dt.getMonth());
  }

  @Test
  public final void testHashCode() {
    JustDate dt = new JustDate(1970, 1, 11);
    assertEquals(10, dt.hashCode());

    dt = new JustDate(1969, 12, 22);
    assertEquals(-10, dt.hashCode());

    dt = new JustDate(1970, 2, 6);

    assertEquals(36, dt.hashCode());
  }

  @Test
  public final void testJustDate() {
    JustDate jd = new JustDate();
    DateTime dt = new DateTime();

    assertEquals(dt.getYear(), jd.getYear());
    assertEquals(dt.getMonth(), jd.getMonth());
    assertEquals(dt.getDom(), jd.getDom());
    System.out.println(jd.getYear() + "-" + jd.getMonth() + "-"
        + jd.getDom());
  }

  @Test
  public final void testJustDateDate() {

    DateTime d = new DateTime();
    JustDate dt = new JustDate(new Date());

    assertEquals(d.getYear(), dt.getYear());
    assertEquals(d.getMonth(), dt.getMonth());
    assertEquals(d.getDom(), dt.getDom());

    JustDate dtf = new JustDate((Date) null);
    assertEquals(dtf.getDateTime().getTimezoneOffset() * 60000, dtf.getDateTime().getTime());
  }

  @Test
  public final void testJustDateDateTime() {
    DateTime dt = new DateTime(2011, 4, 8);
    JustDate jd = new JustDate(dt);

    assertEquals(2011, jd.getYear());
    assertEquals(4, jd.getMonth());
    assertEquals(8, jd.getDom());

    dt = new DateTime();
    jd = new JustDate(new DateTime());

    assertEquals(dt.getYear(), jd.getYear());
    assertEquals(dt.getMonth(), jd.getMonth());
    assertEquals(dt.getDom(), jd.getDom());

    JustDate dtf = new JustDate((DateTime) null);
    assertEquals(dtf.getDateTime().getTimezoneOffset() * 60000, dtf.getDateTime().getTime());
  }

  @Test
  public final void testJustDateInt() {
    JustDate a = new JustDate(10);

    assertEquals(1970, a.getYear());
    assertEquals(1, a.getMonth());
    assertEquals(11, a.getDom());

    a = new JustDate(-10);
    assertEquals(1969, a.getYear());
    assertEquals(12, a.getMonth());
    assertEquals(22, a.getDom());
  }

  @Test
  public final void testJustDateIntIntInt() {
    JustDate dt = new JustDate(2011, 4, 8);

    assertEquals(2011, dt.getYear());
    assertEquals(4, dt.getMonth());
    assertEquals(8, dt.getDom());

    dt = new JustDate(0, 1, 1);

    assertEquals(0, dt.getYear());
    assertEquals(1, dt.getMonth());
    assertEquals(1, dt.getDom());

    dt = new JustDate(-21, 1, 1);

    assertEquals(-21, dt.getYear());
    assertEquals(1, dt.getMonth());
    assertEquals(1, dt.getDom());
  }

  @Test
  public final void testJustDateLong() {
    JustDate jd = new JustDate(1298362388227L);

    assertEquals(2011, jd.getYear());
    assertEquals(2, jd.getMonth());
    assertEquals(22, jd.getDom());
  }

  @Test
  public final void testGetTime() {
    JustDate jd = new JustDate(2017, 1, 30);
    assertEquals(1485727200000L, jd.getTime());

    jd = new JustDate(2017, 3, 26);
    assertEquals(jd.getDays() * TimeUtils.MILLIS_PER_DAY - 120 * 60000, jd.getTime());
    jd.setDom(27);
    assertEquals(jd.getDays() * TimeUtils.MILLIS_PER_DAY - 180 * 60000, jd.getTime());

    jd = new JustDate(1);
    assertEquals(TimeUtils.MILLIS_PER_HOUR * 22, jd.getTime());
    jd = new JustDate(0);
    assertEquals(TimeUtils.MILLIS_PER_HOUR * -2, jd.getTime());
    jd = new JustDate(-1);
    assertEquals(-TimeUtils.MILLIS_PER_DAY - TimeUtils.MILLIS_PER_HOUR * 2, jd.getTime());

    jd = new JustDate(1900, 1, 1);
    assertEquals(-25567, jd.getDays());
    assertEquals(-2208996000000L, jd.getTime());
  }

  @Test
  public final void testParse() {
    String str = "2046";
    JustDate jd = TimeUtils.parseDate(str);

    assertEquals(2046, jd.getYear());
    assertEquals(1, jd.getMonth());
    assertEquals(1, jd.getDom());

    str = "2011-04-06";

    jd = TimeUtils.parseDate(str);

    assertEquals(2011, jd.getYear());
    assertEquals(4, jd.getMonth());
    assertEquals(6, jd.getDom());

    str = "2011;-04;-06";

    jd = TimeUtils.parseDate(str);

    assertEquals(2011, jd.getYear());
    assertEquals(4, jd.getMonth());
    assertEquals(6, jd.getDom());

    str = "2011/04/06";

    jd = TimeUtils.parseDate(str);

    assertEquals(2011, jd.getYear());
    assertEquals(4, jd.getMonth());
    assertEquals(6, jd.getDom());

    str = "2011/04/06 12:36:59,78";

    jd = TimeUtils.parseDate(str);

    assertEquals(2011, jd.getYear());
    assertEquals(4, jd.getMonth());
    assertEquals(6, jd.getDom());

    str = "2011-02";

    jd = TimeUtils.parseDate(str);

    assertEquals(2011, jd.getYear());
    assertEquals(2, jd.getMonth());
    assertEquals(1, jd.getDom());

    String str1 = "2010-";
    JustDate jd1 = TimeUtils.parseDate(str1);
    assertEquals("2010-01-01", jd1.toString());
  }

  @Test
  public final void testSerialize() {
    JustDate jd = new JustDate(1970, 1, 1);

    assertEquals("0", jd.serialize());

    jd = new JustDate(1969, 12, 1);
    assertEquals("-31", jd.serialize());

    jd = new JustDate(1971, 1, 1);
    assertEquals("365", jd.serialize());

    jd = new JustDate(1972, 1, 1);
    assertEquals("730", jd.serialize());

    jd = new JustDate(1973, 1, 1);
    assertEquals("1096", jd.serialize());
  }

  @Test
  public final void testSetDay() {
    JustDate jd = new JustDate();

    jd.setDays(0);

    assertEquals(1970, jd.getYear());
    assertEquals(1, jd.getMonth());
    assertEquals(1, jd.getDom());

    jd.setDays(1098);

    assertEquals(1973, jd.getYear());
    assertEquals(1, jd.getMonth());
    assertEquals(3, jd.getDom());
  }

  @Test
  public final void testToString() {
    JustDate jd = new JustDate(2011, 4, 8);

    assertEquals("2011-04-08", jd.toString());

    jd = new JustDate(1298362388227L);
    assertEquals("2011-02-22", jd.toString());
  }
}
