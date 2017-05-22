package com.butent.bee.shared;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Tests {@link com.butent.bee.shared.ListSequence}.
 */
public class TestListSequence {

  private ListSequence<String> ls;

  @Before
  public void setUp() throws Exception {
    ArrayList<String> ar = new ArrayList<>();
    ar.add("this");
    ar.add("the");
    ar.add("junit");
    ar.add("test");

    ls = new ListSequence<>(ar);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testClear() {
    ls.clear();
    assertEquals(0, ls.getLength());
  }

  @Test
  public final void testGet() {
    assertEquals("junit", ls.get(2));

    assertEquals("this", ls.get(0));

    assertEquals("test", ls.get(3));
  }

  @Test
  public final void testGetArray() {
    String[] ex1 = {"this", "the", "junit", "test"};
    String[] pr2 = {};
    Object[] re1 = ls.getArray(pr2).getA();

    assertArrayEquals(ex1, re1);
  }

  @Test
  public final void testGetList() {
    ArrayList<String> ex1 = new ArrayList<>();

    ex1.add("this");
    ex1.add("the");
    ex1.add("junit");
    ex1.add("test");

    assertArrayEquals(ex1.toArray(), ls.getList().toArray());
  }

  @Test
  public final void testInsert() {
    String[] exp1a = {"this", "is", "the", "junit", "test"};
    String[] exp1b = {".", "this", "is", "the", "junit", "test"};
    String[] exp1c = {".", "this", "is", "the", "junit", "test", "!"};
    String[] pr = {};

    ls.insert(1, "is");
    assertArrayEquals(exp1a, ls.getArray(pr).getA());

    ls.insert(0, ".");
    assertArrayEquals(exp1b, ls.getArray(pr).getA());

    ls.insert(6, "!");
    assertArrayEquals(exp1c, ls.getArray(pr).getA());
  }

  @Test
  public final void testLength() {
    assertEquals(4, ls.getLength());
  }

  @SuppressWarnings("unused")
  @Test
  public final void testRemove() {

    String[] exp1a = {"this", "junit", "test"};
    String[] exp1b = {"junit", "test"};
    String[] exp1c = {"junit"};

    String[] exp2a = {null, null, null, null, null};
    String[] exp2b = {"junit", "test"};
    String[] exp2c = {"junit"};
    String[] pr = {};

    ls.remove(1);
    assertArrayEquals(exp1a, ls.getArray(pr).getA());

    ls.remove(0);
    assertArrayEquals(exp1b, ls.getArray(pr).getA());

    ls.remove(1);
    assertArrayEquals(exp1c, ls.getArray(pr).getA());
  }

  @Test
  public final void testSet() {
    String[] exp1a = {"this", "the", "junit", "test"};
    String[] exp1b = {"there", "the", "junit", "test"};
    String[] exp1c = {"there", "the", "junit", "mod"};
    String[] pr = {};

    ls.set(1, "the");
    assertArrayEquals(exp1a, ls.getArray(pr).getA());

    ls.set(0, "there");
    assertArrayEquals(exp1b, ls.getArray(pr).getA());

    ls.set(3, "mod");
    assertArrayEquals(exp1c, ls.getArray(pr).getA());
  }

  @Test
  public final void testSetValuesListOfT() {
    ArrayList<String> exp1 = new ArrayList<>();
    String[] pr = {};
    exp1.add("one");
    exp1.add("two");
    exp1.add("tree");

    ls.setValues(exp1);

    assertArrayEquals(exp1.toArray(), ls.getArray(pr).getA());
  }

  @Test
  public final void testSetValuesTArray() {
    String[] exp1 = {"abc", "cde", "qwe"};
    String[] pr = {};

    ls.setValues(exp1);
    assertArrayEquals(exp1, ls.getArray(pr).getA());
  }
}
