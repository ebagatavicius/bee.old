package com.butent.bee.shared.data.value;

import com.butent.bee.shared.exceptions.BeeRuntimeException;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.BooleanValue}.
 */
@SuppressWarnings("static-method")
public class TestBooleanValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompare() {
    assertEquals(1, BooleanValue.TRUE.compareTo(BooleanValue.FALSE));
    assertEquals(-1, BooleanValue.FALSE.compareTo(BooleanValue.TRUE));
    assertEquals(0, BooleanValue.FALSE.compareTo(BooleanValue.FALSE));
    assertEquals(-1, BooleanValue.getNullValue().compareTo(BooleanValue.TRUE));
    assertEquals(-1, BooleanValue.getNullValue().compareTo(BooleanValue.TRUE));
    assertEquals(0, BooleanValue.getNullValue().compareTo(BooleanValue.getNullValue()));
    assertEquals(1, BooleanValue.FALSE.compareTo(BooleanValue.getNullValue()));

    try {
      assertEquals(1, BooleanValue.TRUE.compareTo(null));
    } catch (BeeRuntimeException e) {
      assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Java runtime error. Need BeeRuntimeException " + e.getMessage());
    }
  }

  @Test
  public final void testGetInstance() {
    assertEquals(BooleanValue.getNullValue(), BooleanValue.of(null));
    assertEquals(BooleanValue.FALSE, BooleanValue.of(false));
    assertEquals(BooleanValue.TRUE, BooleanValue.of(true));
  }

  @Test
  public final void testGetObjectValue() {
    assertEquals(false, BooleanValue.FALSE.getObjectValue());
    assertEquals(true, BooleanValue.TRUE.getObjectValue());
    assertEquals(null, BooleanValue.getNullValue().getObjectValue());
  }

  @Test
  public final void testGetType() {
    assertEquals(ValueType.BOOLEAN, BooleanValue.FALSE.getType());
    assertEquals(ValueType.BOOLEAN, BooleanValue.TRUE.getType());
    assertEquals(ValueType.BOOLEAN, BooleanValue.getNullValue().getType());
  }

  @Test
  public final void testHashCode() {
    assertEquals(0, BooleanValue.FALSE.hashCode());
    assertEquals(1, BooleanValue.TRUE.hashCode());
    assertEquals(-1, BooleanValue.getNullValue().hashCode());
  }

  @SuppressWarnings("static-access")
  @Test
  public final void testPack() {
    assertEquals(null, BooleanValue.pack(null));
    assertEquals("f", BooleanValue.pack(false));
    assertEquals("t", BooleanValue.pack(true));
    assertEquals("t", ((BooleanValue) null).pack(true));
  }

  @Test
  public final void testToString() {
    assertEquals("true", BooleanValue.TRUE.toString());
    assertEquals("false", BooleanValue.FALSE.toString());
    assertEquals("null", BooleanValue.getNullValue().toString());
  }

  @SuppressWarnings("static-access")
  @Test
  public final void testUnpack() {
    assertEquals(true, BooleanValue.unpack(BooleanValue.pack(true)));
    assertEquals(false, BooleanValue.unpack(BooleanValue.pack(false)));
    assertEquals(null, BooleanValue.unpack(BooleanValue.pack(null)));
    assertEquals(null, BooleanValue.TRUE.unpack(null));
  }
}
