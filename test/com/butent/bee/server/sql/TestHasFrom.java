package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.HasFrom}.
 */
@SuppressWarnings("static-method")
public class TestHasFrom {

  @Before
  public void setUp() throws Exception {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testAddFromFullSqlSelectStringIsCondition() {
    SqlSelect select = new SqlSelect();
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table2", "field21");
    select1.addFrom("Table2");

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromFull(select1, "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals("SELECT Table1.field1 FROM Table1 FULL JOIN (SELECT Table2.field21 FROM Table2) "
        + "Lentele2 ON Table2.Field21 = 'val21'", select.getQuery());
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromFull((SqlSelect) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromFullStringIsCondition() {
    SqlSelect select = new SqlSelect();
    String[] src1 = {"Table2", "Table1"};
    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromFull("Table2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 FULL JOIN Table2 ON Table2.Field21 = 'val21'",
        select.getQuery());

    Object[] a = select.getSources().toArray();
    assertArrayEquals(src1, a);
  }

  @Test
  public final void testAddFromFullStringStringIsCondition() {
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromFull("Table2", "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 FULL JOIN Table2 Lentele2 ON Table2.Field21 = 'val21'",
        select.getQuery());

    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1");
    select1.addFrom("Table1");
    select1.addFromFull("Table2", "",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 FULL JOIN Table2 ON Table2.Field21 = 'val21'",
        select1.getQuery());

//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromFull("", "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromFull((String) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromInnerSqlSelectStringIsCondition() {
    SqlSelect select = new SqlSelect();
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table2", "field21");
    select1.addFrom("Table2");

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromInner(select1, "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals("SELECT Table1.field1 FROM Table1 INNER JOIN (SELECT Table2.field21 FROM Table2) "
        + "Lentele2 ON Table2.Field21 = 'val21'", select.getQuery());
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromInner((SqlSelect) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromInnerStringIsCondition() {

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromInner("Table2", SqlUtils.isNull("Table2", "field22"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 INNER JOIN Table2 ON Table2.field22 IS NULL",
        select.getQuery());
  }

  @Test
  public final void testAddFromInnerStringStringIsCondition() {
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromInner("Table2", "Tab1",
        SqlUtils.isNull("Table2", "field22"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 INNER JOIN Table2 Tab1 ON Table2.field22 IS NULL",
        select.getQuery());

    SqlSelect select1 = new SqlSelect();

    select1.addFields("Table1", "field1");
    select1.addFrom("Table1");
    select1.addFromInner("Table2", SqlUtils.isNull("Table2", "field22"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 INNER JOIN Table2 ON Table2.field22 IS NULL",
        select1.getQuery());
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromInner("", "Tab1", SqlUtils.isNull("Table2", "field22"));
//      fail("Exceptions not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException" + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromInner((String) null, "Tab1",
//          SqlUtils.isNull("Table2", "field22"));
//      fail("Exceptions not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException" + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromInner("Table1", "Tab1", null);
//      fail("Exceptions not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException" + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromLeftSqlSelectStringIsCondition() {
    SqlSelect select = new SqlSelect();
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table2", "field21");
    select1.addFrom("Table2");

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromLeft(select1, "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals("SELECT Table1.field1 FROM Table1 LEFT JOIN (SELECT Table2.field21 FROM Table2) "
        + "Lentele2 ON Table2.Field21 = 'val21'", select.getQuery());
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromLeft((SqlSelect) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromLeftStringIsCondition() {
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromLeft("Table2", SqlUtils.isNull("Table2", "field22"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 LEFT JOIN Table2 ON Table2.field22 IS NULL",
        select.getQuery());
  }

  @Test
  public final void testAddFromLeftStringStringIsCondition() {
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromLeft("Table2", "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 LEFT JOIN Table2 Lentele2 ON Table2.Field21 = 'val21'",
        select.getQuery());

    SqlSelect select1 = new SqlSelect();

    select1.addFields("Table1", "field1");
    select1.addFrom("Table1");
    select1.addFromLeft("Table2", "",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 LEFT JOIN Table2 ON Table2.Field21 = 'val21'",
        select1.getQuery());

//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromLeft("", "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromLeft((String) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromLeft("Table2", "Lentele2", null);
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromRightSqlSelectStringIsCondition() {
    SqlSelect select = new SqlSelect();
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table2", "field21");
    select1.addFrom("Table2");

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromRight(select1, "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals("SELECT Table1.field1 FROM Table1 RIGHT JOIN (SELECT Table2.field21 FROM Table2) "
        + "Lentele2 ON Table2.Field21 = 'val21'", select.getQuery());

//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromRight((SqlSelect) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromRightStringIsCondition() {
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromRight("Table2", SqlUtils.isNull("Table2", "field22"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 RIGHT JOIN Table2 ON Table2.field22 IS NULL",
        select.getQuery());
  }

  @Test
  public final void testAddFromRightStringStringIsCondition() {
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFromRight("Table2", "Lentele2",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 RIGHT JOIN Table2 Lentele2 ON Table2.Field21 = 'val21'",
        select.getQuery());

    SqlSelect select1 = new SqlSelect();

    select1.addFields("Table1", "field1");
    select1.addFrom("Table1");
    select1.addFromRight("Table2", "",
        SqlUtils.equals("Table2", "Field21", "val21"));

    assertEquals(
        "SELECT Table1.field1 FROM Table1 RIGHT JOIN Table2 ON Table2.Field21 = 'val21'",
        select1.getQuery());

//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromRight("", "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromRight((String) null, "Lentele2",
//          SqlUtils.equal("Table2", "Field21", "val21"));
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFromRight("Table2", "Lentele2", null);
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromSqlSelectString() {
    SqlSelect select1 = new SqlSelect();
    SqlSelect select2 = new SqlSelect();
    SqlSelect select3 = new SqlSelect();

    String[] excp1 = {"SELECT Table2.field21, Table2.field22 FROM Table2"};
    String[] excp1a = {"Lentele1"};
    String[] excp2 = {"SELECT Table2.field21, Table2.field22 FROM Table2",
        "SELECT Table3.field31 FROM Table3"};
    String[] excp2a = {"Lentele1", "Lentele2"};

    select2.addFields("Table2", "field21", "field22");
    select2.addFrom("Table2");

    select3.addFields("Table3", "field31");
    select3.addFrom("Table3");

    select1.addFrom(select2, "Lentele1");
    Object[] a = select1.getFrom().toArray();

    for (int i = 0; i < a.length; i++) {
      assertEquals(excp1[i],
          ((SqlSelect) ((IsFrom) a[i]).getSource()).getQuery());
      assertEquals(excp1a[i], ((IsFrom) a[i]).getAlias());
    }

    select1.addFrom(select3, "Lentele2");

    a = select1.getFrom().toArray();

    for (int i = 0; i < a.length; i++) {
      assertEquals(excp2[i],
          ((SqlSelect) ((IsFrom) a[i]).getSource()).getQuery());
      assertEquals(excp2a[i], ((IsFrom) a[i]).getAlias());
    }
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFrom((SqlSelect) null, "ds");
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      SqlSelect sel2 = new SqlSelect();
//      sel1.addFrom(sel2, "ds");
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromString() {
    SqlSelect select = new SqlSelect();
    String[] excp1 = {"Table1"};
    String[] excp2 = {"Table1", "Table2"};

    select.addFrom("Table1");
    Object[] a = select.getFrom().toArray();

    for (int i = 0; i < a.length; i++) {
      assertEquals(excp1[i], ((IsFrom) a[i]).getSource());
    }

    select.addFrom("Table2");

    a = select.getFrom().toArray();

    for (int i = 0; i < a.length; i++) {
      assertEquals(excp2[i], ((IsFrom) a[i]).getSource());
    }
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFrom("");
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
//    try {
//      SqlSelect sel1 = new SqlSelect();
//      sel1.addFrom((String) null);
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
  }

  @Test
  public final void testAddFromStringString() {
    SqlSelect select = new SqlSelect();
    String[] excp1 = {"Table1"};
    String[] excp1a = {"Lentele1"};
    select.addFrom("Table1", "Lentele1");
    Object[] a = select.getFrom().toArray();

    for (int i = 0; i < a.length; i++) {
      assertEquals(excp1[i], ((IsFrom) a[i]).getSource());
      assertEquals(excp1a[i], ((IsFrom) a[i]).getAlias());
    }

    select.addFrom("Table2", "Lentele2");
    String[] excp2 = {"Table1", "Table2"};
    String[] excp2a = {"Lentele1", "Lentele2"};

    a = select.getFrom().toArray();

    for (int i = 0; i < a.length; i++) {
      assertEquals(excp2[i], ((IsFrom) a[i]).getSource());
      assertEquals(excp2a[i], ((IsFrom) a[i]).getAlias());
    }
    SqlSelect sel1 = new SqlSelect();
    sel1.addFrom("Table1", "");
    sel1.addFields("Table1", "field1");

    assertEquals("SELECT Table1.field1 FROM Table1", sel1.getQuery());

//    try {
//      SqlSelect sel2 = new SqlSelect();
//      sel2.addFrom("", "alias");
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    SqlSelect sel2 = new SqlSelect();
//    sel2.addFrom("Table1", null);
//    sel2.addFields("Table1", "field1");
//    assertEquals("SELECT Table1.field1 FROM Table1", sel2.getQuery());
//
//    try {
//      SqlSelect sel3 = new SqlSelect();
//      sel3.addFrom((String) null, "alias");
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @SuppressWarnings("rawtypes")
  @Test
  public final void testGetSources() {
    HasFrom select = new SqlSelect();

    String[] src = {"Table2", "Table1"};

    ((SqlSelect) select).addFields("Table1", "field1");
    select.addFrom("Table1");
    select.addFrom("Table2");
    Object[] a = select.getSources().toArray();
    assertArrayEquals(src, a);
  }

}
