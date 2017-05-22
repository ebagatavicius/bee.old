package com.butent.bee.shared.data.value;

import com.butent.bee.shared.exceptions.BeeRuntimeException;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.DateValue}.
 */
@SuppressWarnings("static-method")
public class TestDateValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    DateValue data = new DateValue(2011, 04, 19);
    assertEquals(0, data.compareTo(data));
    assertEquals(1, data.compareTo(new DateValue(2011, 04, 18)));
    assertEquals(-1, data.compareTo(new DateValue(2011, 04, 20)));
    assertEquals(0, data.compareTo(new DateValue(2011, 04, 19)));
    assertEquals(1, data.compareTo(new DateValue(2011, 03, 19)));
    assertEquals(-1, data.compareTo(new DateValue(2011, 05, 19)));
    assertEquals(1, data.compareTo(new DateValue(2010, 04, 19)));
    assertEquals(-1, data.compareTo(new DateValue(2012, 04, 19)));
    assertEquals(1, data.compareTo(DateValue.getNullValue()));
    assertEquals(-1, DateValue.getNullValue().compareTo(new DateValue(1999, 01, 01)));

    try {
      data.compareTo(null);
    } catch (BeeRuntimeException e) {
      assertTrue(true);
      System.out.println("compare DateValue" + e.getMessage());
    } catch (Exception e) {
      fail("Java runtime error. Need BeeRuntimeException !!!");
    }
  }

  @Test
  public final void testGetObjectValue() {
    DateValue data = new DateValue(2011, 04, 19);
    assertEquals("2011-04-19", data.getObjectValue().toString());
    assertEquals(null, DateValue.getNullValue().getObjectValue());
  }

  @Test
  public final void testGetType() {
    DateValue data = new DateValue(2011, 04, 19);
    assertEquals(ValueType.DATE, data.getType());
  }

  @Test
  public final void testHashCode() {
    DateValue data = new DateValue(2011, 04, 19);
    DateValue data2 = new DateValue(2011, 04, 20);
    assertEquals(15083, data.hashCode());
    assertEquals(15084, data2.hashCode());
  }

  @Test
  public final void testToString() {
    DateValue data = new DateValue(2011, 04, 19);
    assertEquals("null", DateValue.getNullValue().toString());
    assertEquals("2011-04-19", data.toString());
  }
}
