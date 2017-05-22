package com.butent.bee.shared;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.Pair}.
 */
@SuppressWarnings("static-method")
public class TestPair {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @SuppressWarnings("rawtypes")
  @Test
  public final void testGetA() {
    Pair p = Pair.of("a", 1.2);
    assertEquals("a", p.getA());

    p = Pair.of(null, 5);
    assertEquals(null, p.getA());

    p = Pair.of('\0', 7);
    assertEquals('\0', p.getA());

    Pair<String, Double> g = Pair.of("a", 1.2);
    assertEquals("a", g.getA());

    g = Pair.of(null, (double) 5);
    assertEquals(null, g.getA());

    g = Pair.of(String.valueOf('\0'), (double) 7);
    assertEquals("\0", g.getA());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public final void testGetB() {
    Pair p = Pair.of(1.2, "a");
    assertEquals("a", p.getB());

    p = Pair.of(5, null);
    assertEquals(null, p.getB());

    p = Pair.of(7, '\0');
    assertEquals('\0', p.getB());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public final void testToString() {
    Pair p = Pair.of(1.2, "a");
    assertEquals("1.2 a", p.toString());

    p = Pair.of(5, null);
    assertEquals("5", p.toString());

    p = Pair.of(7, '\0');
    assertEquals("7 \0", p.toString());
  }
}
