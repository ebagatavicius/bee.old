package com.butent.bee.shared.data.value;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.TextValue}.
 */
@SuppressWarnings("static-method")
public class TestTextValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    TextValue value = new TextValue("a");
    assertEquals(-5, new TextValue("Str").compareTo(new TextValue("Stw")));
    assertEquals(1, new TextValue("Str").compareTo(TextValue.getNullValue()));
    assertEquals(0, new TextValue("Str").compareTo(new TextValue("Str")));
    assertEquals(0, value.compareTo(value));
  }

  @Test
  public final void testGetobjectValue() {
    TextValue value = new TextValue("a");
    assertEquals("a", value.getObjectValue());
    assertEquals("Hello", new TextValue("Hello").getObjectValue());

    assertEquals("a", value.toString());
    assertEquals("Hello", new TextValue("Hello").toString());
  }

  @Test
  public final void testGetType() {
    TextValue value = new TextValue("a");
    assertEquals(ValueType.TEXT, value.getType());
  }

  @Test
  public final void testHashCode() {
    assertEquals(97, new TextValue("a").hashCode());
    assertEquals(98, new TextValue("b").hashCode());
  }

  @Test
  public final void testIsNull() {
    assertEquals(true, TextValue.getNullValue().isNull());
    assertEquals(false, new TextValue("a").isNull());
  }

}
