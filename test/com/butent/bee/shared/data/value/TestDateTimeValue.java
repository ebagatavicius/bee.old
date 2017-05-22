package com.butent.bee.shared.data.value;

import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.time.DateTime;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.DateTimeValue}.
 */
@SuppressWarnings("static-method")
public class TestDateTimeValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    DateTime data = new DateTime();
    DateTimeValue laikas = new DateTimeValue(data);

    DateTimeValue laikas1 = new DateTimeValue(2011, 2, 22, 8, 13, 8);
    DateTimeValue laikas2 = new DateTimeValue(2011, 2, 22, 8, 13, 8);

    assertEquals(1, laikas.compareTo(laikas1));
    assertEquals(-1, laikas1.compareTo(laikas));
    System.out.println(laikas1);
    System.out.println(laikas2);
    assertEquals(0, laikas1.compareTo(laikas2));
    assertEquals(0, laikas1.compareTo(laikas1));

    assertEquals(1, laikas.compareTo(DateTimeValue.getNullValue()));
    assertEquals(-1, DateTimeValue.getNullValue().compareTo(laikas));

    try {
      laikas1.compareTo(null);
    } catch (BeeRuntimeException e) {
      assertTrue(true);
      System.out.println("compare error" + e.getMessage());
    } catch (Exception e) {
      fail("Java runtime error. Need BeeRuntimeException !!!");
    }
  }

  @Test
  public final void testDateTimeValueDateTime() {
    DateTime data = new DateTime();
    DateTimeValue a = new DateTimeValue(data);
    assertEquals(data.getYear(), a.getYear());
    assertEquals(data.getMonth(), a.getMonth());
    assertEquals(data.getHour(), a.getHourOfDay());
    assertEquals(data.getMinute(), a.getMinute());
    assertEquals(data.getDom(), a.getDayOfMonth());
    assertEquals(data.getSecond(), a.getSecond());
    assertEquals(data.getMillis(), a.getMillisecond());
  }

  @Test
  public final void testDateTimeValueSeconds() {
    DateTimeValue a = new DateTimeValue(2011, 2, 22, 8, 13, 8);
    assertEquals(2011, a.getYear());
    assertEquals(2, a.getMonth());
    assertEquals(22, a.getDayOfMonth());
    assertEquals(8, a.getHourOfDay());
    assertEquals(13, a.getMinute());
    assertEquals(8, a.getSecond());

    try {
      DateTimeValue b = new DateTimeValue(2011, -888, 22, 8, 13, 8);
      assertEquals(2011, b.getYear());
      assertEquals(1, b.getMonth());
      assertEquals(22, b.getDayOfMonth());
    } catch (BeeRuntimeException e) {
      assertTrue(true);
      System.out.println("creating date error" + e.getMessage());
    } catch (Exception e) {
      fail("Java runtime error. Need BeeRuntimeException !!!");
    }
  }

  @Test
  public final void testGetObjectValue() {

    DateTimeValue laikas1 = new DateTimeValue(2011, 2, 22, 8, 13, 8);

    assertEquals(null, DateTimeValue.getNullValue().getObjectValue());
    assertEquals("2011-02-22 08:13:08", laikas1.getObjectValue().toString());
  }

  @Test
  public final void testGetType() {
    DateTimeValue laikas1 = new DateTimeValue(2011, 2, 22, 8, 13, 8);
    assertEquals(ValueType.DATE_TIME, laikas1.getType());
  }

  @Test
  public final void testToString() {
    DateTimeValue laikas1 = new DateTimeValue(2011, 2, 22, 8, 13, 8);

    DateTime tempLaikas = new DateTime(1298362388227L);
    DateTimeValue laikas2 = new DateTimeValue(tempLaikas);

    assertEquals("2011-02-22 08:13:08", laikas1.toString());
    assertEquals("null", DateTimeValue.getNullValue().toString());
    assertEquals("2011-02-22 10:13:08.227", laikas2.toString());
  }

}
