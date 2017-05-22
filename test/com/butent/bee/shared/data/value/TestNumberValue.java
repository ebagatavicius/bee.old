package com.butent.bee.shared.data.value;

import com.butent.bee.shared.exceptions.BeeRuntimeException;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.NumberValue}.
 */
@SuppressWarnings("static-method")
public class TestNumberValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    NumberValue skaicius = new NumberValue(9.0);
    assertEquals(0, skaicius.compareTo(new NumberValue(9.0)));
    assertEquals(1, skaicius.compareTo(new NumberValue(-5.0)));
    assertEquals(-1, skaicius.compareTo(new NumberValue(500.0)));
    assertEquals(1, skaicius.compareTo(NumberValue.getNullValue()));
    assertEquals(-1, NumberValue.getNullValue().compareTo(skaicius));
    assertEquals(0, skaicius.compareTo(skaicius));

    try {
      skaicius.compareTo(null);
    } catch (BeeRuntimeException e) {
      assertTrue(true);
      System.out.println("compare number" + e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Java runtime error. Need BeeRuntimeException " + e.getMessage());
    }
  }

  @Test
  public final void testGetObjectValue() {
    assertEquals(5.0, new NumberValue(5.0).getObjectValue());
    assertEquals(null, NumberValue.getNullValue().getObjectValue());
  }

  @Test
  public final void testGetType() {
    assertEquals(ValueType.NUMBER, new NumberValue(5.0).getType());
  }

  @Test
  public final void testHashcode() {
    assertEquals(1075052544, new NumberValue(5.0).hashCode());
    assertEquals(645005312, new NumberValue(5.1).hashCode());
    assertEquals(0, NumberValue.getNullValue().hashCode());
  }

  @Test
  public final void testToString() {
    assertEquals("null", NumberValue.getNullValue().toString());
    assertEquals("88", new NumberValue(88.0).toString());
  }
}
