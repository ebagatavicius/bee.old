package com.butent.bee.shared.data.value;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.IntValue}.
 */
public class TestIntValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @SuppressWarnings("static-method")
  @Test
  public final void testIntValue() {
    IntegerValue iv = new IntegerValue(10);

    assertEquals((Integer) 10, iv.getInteger());
  }
}
