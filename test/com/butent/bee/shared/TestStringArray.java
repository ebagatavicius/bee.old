package com.butent.bee.shared;

import static org.junit.Assert.assertArrayEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.StringArray}.
 */
public class TestStringArray {

  private StringArray sa;

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testStringArrayStringArray() {
    String[] a = {"one", "two", "three"};

    sa = new StringArray(a);

    assertArrayEquals(a, sa.getArray(null).getA());
  }
}
