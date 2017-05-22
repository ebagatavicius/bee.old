package com.butent.bee.shared;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.BeeConst}.
 */
@SuppressWarnings("static-method")
public class TestBeeConst {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testIsDefault() {
    assertFalse(BeeConst.isDefault(null));
    assertFalse(BeeConst.isDefault(""));
    assertFalse(BeeConst.isDefault("\r \n \t \t \0 \n \t \t \t"));
    assertFalse(BeeConst.isDefault("\r \n \t \t \0 def\n \t \t \t"));
    assertFalse(BeeConst.isDefault("is default"));
    assertFalse(BeeConst.isDefault("_default"));
    assertTrue(BeeConst.isDefault("\r \n \t \t \0   default \n \t \t \t"));
    assertTrue(BeeConst.isDefault("         DeFaUlT       \t \t"));
    assertTrue(BeeConst.isDefault("default"));
  }

  @Test
  public final void testIsError() {
    assertTrue(BeeConst.isError(-1));
    assertTrue(BeeConst.isError(0xffffffff));
    assertFalse(BeeConst.isError(0xFFFF));
    assertFalse(BeeConst.isError(Integer.MAX_VALUE));
    assertFalse(BeeConst.isError(Integer.MIN_VALUE));
    assertFalse(BeeConst.isError(Integer.MIN_VALUE - 1));
    assertFalse(BeeConst.isError('\0'));
    assertFalse(BeeConst.isError('\001'));
  }

  @Test
  public final void testIsFalse() {
    assertTrue(BeeConst.isFalse('f'));
    assertTrue(BeeConst.isFalse('F'));
    assertTrue(BeeConst.isFalse('n'));
    assertTrue(BeeConst.isFalse('N'));
    assertTrue(BeeConst.isFalse('0'));

    assertFalse(BeeConst.isFalse('O'));
    assertFalse(BeeConst.isFalse((char) 10));
    assertFalse(BeeConst.isFalse((char) -86));
    assertFalse(BeeConst.isFalse('t'));
    assertFalse(BeeConst.isFalse('T'));
    assertFalse(BeeConst.isFalse('y'));
    assertFalse(BeeConst.isFalse('Y'));
    assertFalse(BeeConst.isFalse('1'));
  }

  @Test
  public final void testIsTrue() {
    assertFalse(BeeConst.isTrue('f'));
    assertFalse(BeeConst.isTrue('F'));
    assertFalse(BeeConst.isTrue('n'));
    assertFalse(BeeConst.isTrue('N'));
    assertFalse(BeeConst.isTrue('0'));

    assertFalse(BeeConst.isTrue('O'));
    assertFalse(BeeConst.isTrue((char) 10));
    assertFalse(BeeConst.isTrue((char) -86));
    assertTrue(BeeConst.isTrue('t'));
    assertTrue(BeeConst.isTrue('T'));
    assertTrue(BeeConst.isTrue('y'));
    assertTrue(BeeConst.isTrue('Y'));
    assertTrue(BeeConst.isTrue('1'));
  }

  @Test
  public final void testSetClient() {
    BeeConst.setClient();
    assertTrue(BeeConst.isClient());
    assertFalse(BeeConst.isServer());
    assertEquals(BeeConst.CLIENT, BeeConst.whereAmI());
  }

  @Test
  public final void testSetServer() {
    BeeConst.setServer();
    assertFalse(BeeConst.isClient());
    assertTrue(BeeConst.isServer());
    assertEquals(BeeConst.SERVER, BeeConst.whereAmI());
  }
}
