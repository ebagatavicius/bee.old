package com.butent.bee.shared.testutils;

import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests {@link com.butent.bee.shared.utils.BeeUtils#intersects(Collection, Collection)}.
 */
@RunWith(value = Parameterized.class)
public class TestBeeUtilsIntersects extends TestCase {

  private static Set<ValueType> testc1 = new HashSet<>();
  private static Set<ValueType> testc2 = new HashSet<>();

  private static Set<ValueType> testc3 = new HashSet<>();
  private static Set<ValueType> testc4 = new HashSet<>();

  @Parameters
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]
    {
        {false, testc1, testc2},
        {false, testc1, testc3},
        {false, testc3, testc1},
        {true, testc1, testc4}
    });
  }

  boolean expected;

  Set<ValueType> value1;
  Set<ValueType> value2;

  public TestBeeUtilsIntersects(boolean expected, Set<ValueType> value1, Set<ValueType> value2) {
    this.expected = expected;
    this.value1 = value1;
    this.value2 = value2;
  }

  @Override
  @SuppressWarnings("static-access")
  @Before
  public void setUp() throws Exception {
    ValueType a = null;
    ValueType b = null;
    testc1.add(a.BOOLEAN);
    testc1.add(a.DECIMAL);
    testc1.add(a.NUMBER);

    testc2.add(b.DATE);
    testc2.add(b.LONG);
    testc2.add(b.INTEGER);

    testc3.add(a.TEXT);

    testc4.add(a.DECIMAL);
  }

  @Override
  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testIntersects() {
    assertEquals(expected, BeeUtils.intersects(value1, value2));
  }
}
