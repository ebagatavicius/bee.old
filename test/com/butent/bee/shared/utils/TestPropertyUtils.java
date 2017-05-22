package com.butent.bee.shared.utils;

import com.butent.bee.shared.time.DateTime;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tests {@link com.butent.bee.shared.utils.PropertyUtils}.
 */
@SuppressWarnings("static-method")
public class TestPropertyUtils {

  private List<Property> propList = new ArrayList<>();
  private List<ExtendedProperty> propExtList = new ArrayList<>();
  private Collection<ExtendedProperty> propExtColl = new ArrayList<>();
  private Collection<ExtendedProperty> propExtColl2 = new ArrayList<>();
  private Collection<Property> propList1 = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    propList.add(new Property("NAME1", "VALUE1"));
    propList.add(new Property("NAME2", "VALUE2"));
    propList.add(new Property("NAME3", "VALUE3"));

    propList1.add(new Property("NAME1", "VALUE1"));
    propList1.add(new Property("NAME2", "VALUE2"));
    propList1.add(new Property("NAME3", "VALUE3"));

    ExtendedProperty ex1 = new ExtendedProperty("NAME1", "SUB1", "VALUE1");
    ex1.setDate(new DateTime(1298362388227L));

    ExtendedProperty ex2 = new ExtendedProperty("NAME2", "SUB2", "VALUE2");
    ex2.setDate(new DateTime(1298362388227L));

    ExtendedProperty ex3 = new ExtendedProperty("NAME3", "SUB3", "VALUE3");
    ex3.setDate(new DateTime(1298362388446L));

    ExtendedProperty ex4 = new ExtendedProperty("NAME2", "SUB2", "VALUE3");
    ex4.setDate(new DateTime(1298362388446L));

    ExtendedProperty ex5 = new ExtendedProperty("NAME2", "SUB3", "VALUE3");
    ex5.setDate(new DateTime(1298362388446L));

    propExtList.add(ex1);
    propExtList.add(ex2);
    propExtList.add(ex3);

    propExtColl.add(ex1);
    propExtColl.add(ex2);
    propExtColl.add(ex3);
    propExtColl.add(null);

    propExtColl2.add(ex1);
    propExtColl2.add(ex2);
    propExtColl2.add(ex3);
    propExtColl2.add(ex4);
    propExtColl2.add(ex5);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testAddChildren() {
    assertEquals(1, PropertyUtils.addChildren(propExtColl, "name2", "sub1", "value1"));
    assertEquals(2, PropertyUtils.addChildren(propExtColl, "name2", "sub1", "value1", "sub2",
        "value2"));
    assertEquals(2, PropertyUtils.addChildren(propExtColl, "name2", 1, "value1", "sub2", "value2"));
    assertEquals(2, PropertyUtils.addChildren(propExtColl, "name2", "sub1", 1, "sub2", "value2"));
    assertEquals(2, PropertyUtils.addChildren(propExtColl, "name2", null, "value1", "sub2",
        "value2"));
    assertEquals(1, PropertyUtils.addChildren(propExtColl, "name2", "sub1", null, "sub2",
        "value2"));
  }

  @Test
  public final void testAddExtendedCollectionOfExtendedPropertyStringObject() {
    assertEquals(true, PropertyUtils.addExtended(propExtList, "name1", "value1"));
    assertEquals(false, PropertyUtils.addExtended(propExtList, null, "value1"));
    assertEquals(false, PropertyUtils.addExtended(propExtList, "name1", null));
    assertEquals(false, PropertyUtils.addExtended(propExtList, null, null));
    assertEquals(true, PropertyUtils.addExtended(propExtList, "name1", 5));
    assertEquals(false, PropertyUtils.addExtended(propExtList, "", "               "));
    assertEquals(true, PropertyUtils.addExtended(propExtList, "               ",
        "               "));
    assertEquals(false, PropertyUtils.addExtended(propExtList, "                            ",
        ""));
    assertEquals(false, PropertyUtils.addExtended(propExtList, "", ""));
  }

  @Test
  public final void testAddPropertiesCollectionOfExtendedPropertyBooleanObjectArray() {

    assertEquals(1, PropertyUtils.addProperties(propExtColl, false, "name1", "value1"));
    assertEquals(2, PropertyUtils.addProperties(propExtColl, false, "name1", "value1", "name2",
        "value2"));
    assertEquals(1, PropertyUtils.addProperties(propExtColl, true, "name1", "sub1", "value1"));
    assertEquals(2, PropertyUtils.addProperties(propExtColl, true, "name1", "sub1", "value1",
        "name2", "sub1", "value2"));
    assertEquals(0, PropertyUtils.addProperties(propExtColl, false, "name1"));
    assertEquals(0, PropertyUtils.addProperties(propExtColl, false, 1, "value1"));
    assertEquals(3, PropertyUtils.addProperties(propExtColl, false, "name1", "value1", "name2",
        "value2", "name3", 2, 1, "value2"));
    assertEquals(2, PropertyUtils.addProperties(propExtColl, false, "name1", "value1", null, null,
        "name3", 2, 1, "value2"));
    assertEquals(2, PropertyUtils.addProperties(propExtColl, false, "name1", "value1", "name2",
        null, "name3", 2, 1, "value2"));
  }

  @Test
  public final void testAddPropertiesCollectionOfPropertyObjectArray() {
    assertEquals(1, PropertyUtils.addProperties(propList, "name1", "value1"));
    assertEquals(0, PropertyUtils.addProperties(propList, null, "value1"));
    assertEquals(0, PropertyUtils.addProperties(propList, "name1", null));
    assertEquals(0, PropertyUtils.addProperties(propList, null, null));
    assertEquals(1, PropertyUtils.addProperties(propList, "name1", 5));
    assertEquals(0, PropertyUtils.addProperties(propList, "", "               "));
    assertEquals(1, PropertyUtils.addProperties(propList, "               ", "               "));
    assertEquals(0, PropertyUtils.addProperties(propList, "                            ", ""));
    assertEquals(0, PropertyUtils.addProperties(propList, "", ""));
    assertEquals(1, PropertyUtils.addProperties(propList, "name1", "value1", "name2"));
    assertEquals(2, PropertyUtils.addProperties(propList, "name1", "value1", "name2", "value3"));
  }

  @Test
  public final void testAddProperty() {
    assertEquals(true, PropertyUtils.addProperty(propList1, "name1", "value1"));
    assertEquals(false, PropertyUtils.addProperty(propList1, null, "value1"));
    assertEquals(false, PropertyUtils.addProperty(propList1, "name1", null));
    assertEquals(false, PropertyUtils.addProperty(propList1, null, null));
    assertEquals(true, PropertyUtils.addProperty(propList1, "name1", 5));
    assertEquals(false, PropertyUtils.addProperty(propList1, "", "               "));
    assertEquals(true, PropertyUtils.addProperty(propList1, "               ", "               "));
    assertEquals(false, PropertyUtils.addProperty(propList1, "                            ", ""));
    assertEquals(false, PropertyUtils.addProperty(propList1, "", ""));
  }

  @Test
  public final void testAddSplit() {
    assertEquals(0, PropertyUtils.addSplit(propExtColl, "name1", "sub1", "", ";"));
    assertEquals(1, PropertyUtils.addSplit(propExtColl, "name1", "sub1", "value1", ";"));
    assertEquals(4, PropertyUtils.addSplit(propExtColl, "name1", "sub1", "value1, value2, value3",
        ", "));
    assertEquals(1, PropertyUtils.addSplit(propExtColl, "name1", "sub1", "value1,value2,value3",
        ", "));
    assertEquals(4, PropertyUtils.addSplit(propExtColl, "name1", "sub1", "value1,value2,value3",
        null));
  }

  @Test
  public final void testAppendChildrenToExtended() {

    Collection<ExtendedProperty> expect = new ArrayList<>();

    ExtendedProperty ex1 = new ExtendedProperty("NAME1", "SUB1", "VALUE1");
    ex1.setDate(new DateTime(1298362388227L));

    ExtendedProperty ex2 = new ExtendedProperty("NAME2", "SUB2", "VALUE2");
    ex2.setDate(new DateTime(1298362388227L));

    ExtendedProperty ex3 = new ExtendedProperty("NAME3", "SUB3", "VALUE3");
    ex3.setDate(new DateTime(1298362388446L));

    ExtendedProperty ex4 = new ExtendedProperty("NAME2", "SUB1", "VALUE3");
    ex4.setSub("SUB2");
    ex4.setDate(new DateTime(1298362388446L));

    ExtendedProperty ex5 = new ExtendedProperty("NAME2", "SUB3", "VALUE3");
    ex5.setDate(new DateTime(1298362388446L));

    ExtendedProperty ex6 = new ExtendedProperty("NAME2", "NAME1", "VALUE1");
    ExtendedProperty ex7 = new ExtendedProperty("NAME2", "NAME2", "VALUE2");
    ExtendedProperty ex8 = new ExtendedProperty("NAME2", "NAME3", "VALUE3");

    expect.add(ex1);
    expect.add(ex2);
    expect.add(ex3);
    expect.add(ex4);
    expect.add(ex5);
    expect.add(ex6);
    expect.add(ex7);
    expect.add(ex8);

    PropertyUtils.appendChildrenToExtended(propExtColl2, "NAME2", propList);

    Object[] expected = expect.toArray();
    Object[] expected1 = propExtColl2.toArray();

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].toString(), expected1[i].toString());
    }
  }

  @Test
  public final void testAppendChildrenToProperties() {

    Collection<Property> propList11 = new ArrayList<>();
    Collection<Property> propList12 = new ArrayList<>();
    Collection<Property> propList13 = new ArrayList<>();
    Collection<Property> propList14 = new ArrayList<>();
    propList11.add(new Property("NAME1", "VALUE1"));
    propList11.add(new Property("NAME2", "VALUE2"));
    propList11.add(new Property("NAME3", "VALUE3"));
    propList12.add(new Property("NAME4", "VALUE4"));
    propList12.add(new Property("NAME5", "VALUE5"));
    propList12.add(new Property("NAME6", "VALUE6"));

    propList13.add(new Property("NAME1", "VALUE1"));
    propList13.add(new Property("NAME2", "VALUE2"));
    propList13.add(new Property("NAME3", "VALUE3"));
    propList13.add(new Property("NAME4", "VALUE4"));
    propList13.add(new Property("NAME5", "VALUE5"));
    propList13.add(new Property("NAME6", "VALUE6"));

    propList14.add(new Property("NAME1", "VALUE1"));
    propList14.add(new Property("NAME2", "VALUE2"));
    propList14.add(new Property("NAME3", "VALUE3"));
    propList14.add(new Property("root NAME4", "VALUE4"));
    propList14.add(new Property("root NAME5", "VALUE5"));

    Property prop2 = new Property("root NAME6", "VALUE5");
    prop2.setValue("VALUE6");

    propList14.add(prop2);

    PropertyUtils.appendChildrenToProperties(propList11, null, propList12);

    Object[] expected = propList11.toArray();
    Object[] expected1 = propList13.toArray();

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].toString(), expected1[i].toString());
    }
    propList11.clear();
    propList11.add(new Property("NAME1", "VALUE1"));
    propList11.add(new Property("NAME2", "VALUE2"));
    propList11.add(new Property("NAME3", "VALUE3"));

    PropertyUtils.appendChildrenToProperties(propList11, "root", propList12);

    Object[] expected2 = propList11.toArray();
    Object[] expected3 = propList14.toArray();

    for (int i = 0; i < expected2.length; i++) {
      assertEquals(expected2[i].toString(), expected3[i].toString());
    }
  }

  @Test
  public final void testAppendExtended() {

    Collection<ExtendedProperty> propList11 = new ArrayList<>();
    Collection<ExtendedProperty> propList12 = new ArrayList<>();
    Collection<ExtendedProperty> propList13 = new ArrayList<>();

    propList11.add(new ExtendedProperty("NAME1", "VALUE1"));
    propList11.add(new ExtendedProperty("NAME2", "VALUE2"));
    propList11.add(new ExtendedProperty("NAME3", "VALUE3"));

    propList12.add(new ExtendedProperty("NAME4", "VALUE4"));
    propList12.add(new ExtendedProperty("NAME5", "VALUE5"));
    propList12.add(new ExtendedProperty("NAME6", "VALUE6"));

    propList13.add(new ExtendedProperty("NAME1", "VALUE1"));
    propList13.add(new ExtendedProperty("NAME2", "VALUE2"));
    propList13.add(new ExtendedProperty("NAME3", "VALUE3"));
    propList13.add(new ExtendedProperty("NAME4", "VALUE4"));
    propList13.add(new ExtendedProperty("NAME5", "VALUE5"));
    propList13.add(new ExtendedProperty("NAME6", "VALUE6"));

    PropertyUtils.appendExtended(propList11, propList12);

    Object[] expected2 = propList11.toArray();
    Object[] expected3 = propList13.toArray();

    for (int i = 0; i < expected2.length; i++) {
      assertEquals(expected2[i].toString(), expected3[i].toString());
    }
  }

  @Test
  public final void testAppendWithPrefix() {
    Collection<ExtendedProperty> propList11 = new ArrayList<>();
    Collection<ExtendedProperty> propList12 = new ArrayList<>();
    Collection<ExtendedProperty> propList13 = new ArrayList<>();

    propList11.add(new ExtendedProperty("NAME1", "VALUE1"));
    propList11.add(new ExtendedProperty("NAME2", "VALUE2"));
    propList11.add(new ExtendedProperty("NAME3", "VALUE3"));

    propList12.add(new ExtendedProperty("NAME4", "VALUE4"));
    propList12.add(new ExtendedProperty("NAME5", "VALUE5"));

    propList12.add(new ExtendedProperty("NAME6", "VALUE6"));

    propList13.add(new ExtendedProperty("NAME1", "VALUE1"));
    propList13.add(new ExtendedProperty("NAME2", "VALUE2"));
    propList13.add(new ExtendedProperty("NAME3", "VALUE3"));
    propList13.add(new ExtendedProperty("# NAME4", "VALUE4"));
    propList13.add(new ExtendedProperty("# NAME5", "VALUE5"));
    propList13.add(new ExtendedProperty("# NAME6", "VALUE6"));

    PropertyUtils.appendWithPrefix(propList11, "#", propList12);

    Object[] expected2 = propList11.toArray();
    Object[] expected3 = propList13.toArray();

    for (int i = 0; i < expected2.length; i++) {
      assertEquals(expected2[i].toString(), expected3[i].toString());
    }
  }

  @Test
  public final void testCompareTo() {
    Property prop2 = new Property("root NAME6", "VALUE5");
    Property prop3 = new Property("root NAME6", "VALUE5");
    Property prop4 = new Property(null, null);

    assertEquals(-1, prop4.compareTo(prop3));
    assertEquals(0, prop2.compareTo(prop3));
    assertEquals(1, prop2.compareTo(prop4));
    assertEquals(1, prop4.compareTo(null));
    assertEquals(1, prop2.compareTo(null));
  }

  @Test
  public final void testCreatePropertiesObjectArray() {

    Collection<ExtendedProperty> propList11 = new ArrayList<>();
    Collection<ExtendedProperty> propList12 = new ArrayList<>();

    propList11.add(new ExtendedProperty("NAME1", "VALUE1"));
    propList12.add(new ExtendedProperty("NAME1", "VALUE1"));
    propList12.add(new ExtendedProperty("NAME2", "VALUE2"));

    Object[] expected1 = propList11.toArray();
    Object[] expected2 = propList12.toArray();

    for (int i = 0; i < expected1.length; i++) {
      assertEquals(expected1[i].toString(),
          PropertyUtils.createProperties("NAME1", "VALUE1", "NAME2").toArray()[i].toString());
    }
    for (int i = 0; i < expected2.length; i++) {
      assertEquals(expected2[i].toString(),
          PropertyUtils.createProperties("NAME1", "VALUE1",
              "NAME2", "VALUE2").toArray()[i].toString());
    }
  }

  @Test
  public final void testCreatePropertiesStringStringArray() {

    String[] mas = {"VALUE1", "VALUE2", "VALUE3"};

    Collection<ExtendedProperty> propList11 = new ArrayList<>();
    Collection<ExtendedProperty> propList12 = new ArrayList<>();

    propList11.add(new ExtendedProperty("AA: 1/3", "VALUE1"));
    propList11.add(new ExtendedProperty("AA: 2/3", "VALUE2"));
    propList11.add(new ExtendedProperty("AA: 3/3", "VALUE3"));

    propList12.add(new ExtendedProperty("1/3", "VALUE1"));
    propList12.add(new ExtendedProperty("2/3", "VALUE2"));
    propList12.add(new ExtendedProperty("3/3", "VALUE3"));

    Object[] expected1 = propList11.toArray();
    Object[] expected2 = propList12.toArray();

    for (int i = 0; i < expected1.length; i++) {
      assertEquals(expected1[i].toString(), PropertyUtils.createProperties("AA:", mas).toArray()[i]
          .toString());
    }
    for (int i = 0; i < expected2.length; i++) {
      assertEquals(expected2[i].toString(), PropertyUtils.createProperties(null, mas).toArray()[i]
          .toString());
    }
  }

  @Test
  public final void testExtendedToArray() {
    String[][] mas = {
        {"NAME1", "SUB1", "VALUE1", "10:13:08.227"},
        {"NAME2", "SUB2", "VALUE2", "10:13:08.227"},
        {"NAME3", "SUB3", "VALUE3", "10:13:08.446"}
    };
    assertArrayEquals(mas, PropertyUtils.extendedToArray(propExtList));
  }

  @Test
  public final void testPropertiesToArray() {
    String[][] mas = {
        {"NAME1", "VALUE1"}, {"NAME2", "VALUE2"}, {"NAME3", "VALUE3"}
    };
    assertArrayEquals(mas, PropertyUtils.propertiesToArray(propList));
  }

  @Test
  public final void testToString() {
    Property prop2 = new Property("root NAME6", "VALUE5");
    assertEquals("root NAME6=VALUE5", prop2.toString());
  }

  @Test
  public final void testToStringExt() {
    Property propExt2 = new ExtendedProperty("root NAME6", "SUB5", "VALUE5");
    assertEquals("root NAME6.SUB5=VALUE5", propExt2.toString());
  }

  @SuppressWarnings({"rawtypes", "unused" })
  @Test
  public final void testTransformStringString() {
    int index = -1;
    Method[] m = null;
    String s = "private static java.lang.String "
        + "com.butent.bee.shared.utils.PropertyUtils.transformString(java.lang.String)";

    try {
      Class c = PropertyUtils.class;
      m = c.getDeclaredMethods();
      for (int i = 0; i < m.length; i++) {
        if (m[i].toString().equals(s)) {
          index = i;
        }
      }
    } catch (SecurityException e) {
      System.err.println(e);
    }

    try {
      Class cls = Class.forName("com.butent.bee.shared.utils.PropertyUtils");
      Class[] partypes = new Class[1];
      partypes[0] = String.class;

      Method meth = m[index]; // cls.getMethod("private static java.lang.String transformString",
                              // partypes);
      System.out.println(meth.toString());
      meth.setAccessible(true);

      String[] arglist = new String[1];
      arglist[0] = new String("abc");

      assertEquals("abc", meth.invoke(null, (Object[]) arglist));
      arglist[0] = new String("");
      assertEquals("", meth.invoke(null, (Object[]) arglist));
      arglist[0] = new String("1F");
      assertEquals("1F", meth.invoke(null, (Object[]) arglist));
      arglist[0] = new String("   1F");
      assertEquals("   1F", meth.invoke(null, (Object[]) arglist));

      arglist[0] = new String(" ");
      assertEquals("[hex] 0020", meth.invoke(null, (Object[]) arglist));

      arglist[0] = new String("  ");
      assertEquals("[hex] 00200020", meth.invoke(null, (Object[]) arglist));

      arglist[0] = new String("                ");
      assertEquals("[whitespace] 16", meth.invoke(null, (Object[]) arglist));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
