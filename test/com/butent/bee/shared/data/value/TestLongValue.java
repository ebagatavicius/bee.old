package com.butent.bee.shared.data.value;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.LongValue}.
 */
public class TestLongValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @SuppressWarnings("static-method")
  @Test
  public final void testLongValue() {
    LongValue lv = new LongValue(5L);

    assertEquals((Long) 5L, lv.getLong());
  }
}
