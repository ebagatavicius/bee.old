package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.IsCondition}.
 */
@SuppressWarnings("static-method")
public class TestIsCondition {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testAndConditionGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2", "field3");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.and();

    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1", select
        .getQuery());
    IsCondition clause01 = SqlUtils.and(SqlUtils.equals(SqlUtils.name("field1"), "Something val"));

    select.setWhere(clause01);
    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1 "
        + "WHERE field1 = 'Something val'", select.getQuery());

    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2", "field3");
    select1.addFrom("Table1");

    IsCondition clause1 =
        SqlUtils.and(SqlUtils.equals(SqlUtils.name("field1"), "Something val"), SqlUtils.equals(
            SqlUtils.name("field2"), "sv2"));
    select1.setWhere(clause1);
    assertEquals(
        "SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1 WHERE "
        + "field1 = 'Something val' AND field2 = 'sv2'", select1.getQuery());

    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2", "field3");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.and(null, null, null, null);
    select2.setWhere(clause2);
    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1",
        select2.getQuery());
  }

  @Test
  public final void testContainsGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.contains(SqlUtils.expression("field2"), "value");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE field2 LIKE '%value%' ESCAPE '|'",
        select.getQuery());

    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 =
        SqlUtils.contains(SqlUtils.expression("field2", " OR ", "field1"), "value");
    select3.setWhere(clause3);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE field2 OR field1 "
        + "LIKE '%value%' ESCAPE '|'", select3.getQuery());

    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");
//    try {
//      IsCondition clause1 = SqlUtils.contains(SqlUtils.expression("field2"), null);
//      select1.setWhere(clause1);
//
//      assertEquals(
//          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE field2 LIKE '%null%' ESCAPE '|'",
//          select1.getQuery());
//
//      IsCondition clause2 = SqlUtils.contains(SqlUtils.expression((Object) null), null);
//      fail("Runtime error not works. May be woeks NULL conversantion");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testContainsStringStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    select.setWhere(SqlUtils.contains("Table2", "field21", "value22"));

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 "
        + "LIKE '%value22%' ESCAPE '|'", select.getQuery());

//    final String query = "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE ";
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field1", "field2");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.contains("Table2", "field21", ""));
//      assertEquals(query + "Table2.field21 LIKE '%%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field1", "field2");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.contains("Table2", null, "field22"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field1", "field2");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.contains("Table2", "", "field22"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field1", "field2");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.contains(null, "field21", "val11"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field1", "field2");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.contains("", "field21", "val11"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testEndsWithIsExpressionString() {
    final String query = "SELECT Table1.field11, Table1.field12 FROM Table1 WHERE ";
    SqlSelect select;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    select = new SqlSelect();
    select.addFields("Table1", "field11", "field12");
    select.addFrom("Table1");
    select.setWhere(SqlUtils.endsWith(SqlUtils.name("field2"), "val"));

    assertEquals(query + "field2 LIKE '%val' ESCAPE '|'", select.getQuery());

//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith(SqlUtils.name("field2"), null));
//
//      assertEquals(query + "field2 LIKE '%null'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith(SqlUtils.name("field2"), ""));
//
//      assertEquals(query + "field2 LIKE '%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith(null, "val"));
//      fail("Exception not works " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testEndsWithStringStringString() {
    final String query = "SELECT Table1.field11, Table1.field12 FROM Table1 WHERE ";
    SqlSelect select;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    select = new SqlSelect();
    select.addFields("Table1", "field11", "field12");
    select.addFrom("Table1");
    select.setWhere(SqlUtils.endsWith("Table2", "field21", "val22"));

    assertEquals(query + "Table2.field21 LIKE '%val22' ESCAPE '|'", select.getQuery());

//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith("Table2", null, "val22"));
//      fail("Exception not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith("Table2", "field21", null));
//
//      assertEquals(query + "Table2.field21 LIKE '%null'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith("Table2", "field21", ""));
//
//      assertEquals(query + "Table2.field21 LIKE '%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith("Table2", "", "val22"));
//      fail("Exception not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith(null, "field21", "val22"));
//      fail("Exception not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.endsWith("", "field21", "val22"));
//      fail("Exception not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testEqualStringStringOject() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.equals("Table1", "field1", "val");
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 = 'val'",
        select.getQuery());
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.equals("Table1", "field1", 25);
    select3.setWhere(clause3);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 = 25",
        select3
            .getQuery());
    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFrom("Table1");

    IsCondition clause4 = SqlUtils.equals("Table1", "field1", true);
    select4.setWhere(clause4);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 = 1", select4
        .getQuery());
    SqlSelect select5 = new SqlSelect();
    select5.addFields("Table1", "field1", "field2");
    select5.addFrom("Table1");

    IsCondition clause5 = SqlUtils.equals("Table1", "field1", null);
    select5.setWhere(clause5);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 IS NULL",
        select5.getQuery());
    SqlSelect select6 = new SqlSelect();
    select6.addFields("Table1", "field1", "field2");
    select6.addFrom("Table1");

    IsCondition clause6 = SqlUtils.equals("Table1", "field1", "");
    select6.setWhere(clause6);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 = ''",
        select6.getQuery());
//    try {
//      SqlUtils.equal(null, "field1", null);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      SqlUtils.equal("Table1", null, null);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlUtils.equal("", "field1", null);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//
//    try {
//      SqlUtils.equal("Table1", "  ", null);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testInListIsExpressionObjectArrGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.inList(SqlUtils.bitAnd("Table2", "field21", "val21"), "val1");
    select.setWhere(clause);
    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) = 'val1'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 =
        SqlUtils.inList(SqlUtils.bitAnd("Table2", "field21", "val21"), "val1", 1, null, false);
    select1.setWhere(clause1);
    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) "
        + "IN ('val1', 1, null, null)", select1.getQuery());

//    try {
//      IsCondition clause2 = SqlUtils.inList(null, (Object) null);
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException! " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.inList(SqlUtils.bitAnd("Table2", "field21", "val21"));
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException! " + e.getMessage());
//    }
  }

  @Test
  public final void testInListStringStringObjectArrGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");
    IsCondition clause = SqlUtils.inList("Table2", "field21", "val1");
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 = 'val1'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");
    IsCondition clause1 = SqlUtils.inList("Table2", "field21", "val1", false, -5, null, "");
    select1.setWhere(clause1);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 IN ('val1',"
        + " null, -5, null, '')", select1.getQuery());
  }

  @Test
  public final void testInStringStringSqlSelectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    SqlSelect inselect = new SqlSelect();
    inselect.addFields("Tablen", "fieldn1");
    inselect.addFrom("Tablen");
    inselect.setWhere(SqlUtils.equals(SqlUtils.expression("Fieldn"), "5"));

    IsCondition clause = SqlUtils.in("Table1", "\t \t field2  ", inselect);
    select.setWhere(clause);

    String[] b = {"Tablen"};
    Object[] a = clause.getSources().toArray();

    assertArrayEquals(inselect.getSources().toArray(), a);
    assertArrayEquals(b, a);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field2 IN "
        + "((SELECT Tablen.fieldn1 FROM Tablen WHERE Fieldn = '5'))", select.getQuery());
//    try {
//      IsCondition clause1 = SqlUtils.in("  Table1  ", "field2", null);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
//
//    try {
//      IsCondition clause1 = SqlUtils.in("Table1", null, inselect);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
//
//    try {
//      IsCondition clause1 = SqlUtils.in(null, "Field", inselect);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
//
//    try {
//      IsCondition clause1 = SqlUtils.in("  \t\t   ", "Field", inselect);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
//
//    try {
//      IsCondition clause1 = SqlUtils.in("  Table1   ", "", inselect);
//      fail("RuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException : " + e.getMessage());
//    }
  }

  @Test
  public final void testInStringStringStringStringIsConditionGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.in("Table1", "field1", "Table2", "field21", null);
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 IN "
        + "((SELECT DISTINCT Table2.field21 FROM Table2))", select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 =
        SqlUtils.in("Table1", "field1", "Table2", "field21",
            SqlUtils.and(SqlUtils.equals("Table1", "field2", "val1"), SqlUtils.equals("Table2",
                "field21", "val2")));
    select1.setWhere(clause1);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 IN "
        + "((SELECT DISTINCT Table2.field21 FROM Table2 WHERE Table1.field2 = 'val1' AND "
        + "Table2.field21 = 'val2'))", select1.getQuery());
//    try {
//      IsCondition clause2 = SqlUtils.in("\t\t\r", "field1", "Table2", "field21", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in(null, "field1", "Table2", "field21", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in("Table1", "", "Table2", "field21", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in("Table1", null, "Table2", "field21", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in("Table1", "Field1", "", "field21", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in("Table1", "Field1", null, "field21", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in("Table1", "Field1", "Table2", "", null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
//    try {
//      IsCondition clause2 = SqlUtils.in("Table1", "Field1", "Table2", null, null);
//      fail("Exception not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testIsNotNullIsExpressionGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.notNull(SqlUtils.bitAnd("Table2", "field21", "val21"));
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) "
        + "IS NOT NULL", select.getQuery());
//    try {
//      IsCondition clause1 = SqlUtils.notNull(null);
//      fail("Runtime error not works!");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException !: " + e.getMessage());
//    }
  }

  @Test
  public final void testIsNotNullStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");
    IsCondition clause = SqlUtils.notNull("Table2", "field21");
    select.setWhere(clause);
    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 IS NOT NULL",
        select.getQuery());
  }

  @Test
  public final void testIsNullIsExpressionGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.isNull(SqlUtils.bitAnd("Table2", "field21", "val21"));
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) IS NULL",
        select.getQuery());
  }

  @Test
  public final void testIsNullStringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.isNull("Table2", "field21");
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 IS NULL",
        select.getQuery());
  }

  @Test
  public final void testJoinLessEqualStringStringStringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.joinLessEqual("Table1", "field1", "Table2", "field21");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 <= Table2.field21",
        select.getQuery());
  }

  @Test
  public final void testJoinLessStringStringStringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.joinLess("Table1", "field1", "Table2", "field21");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 < Table2.field21",
        select.getQuery());
  }

  @Test
  public final void testJoinMoreEqualStringStringStringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.joinMoreEqual("Table1", "field1 \t\n\r", "Table2", "field21");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 >= Table2.field21",
        select.getQuery());
  }

  @Test
  public final void testJoinMoreStringStringstringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.joinMore("Table1", "field1 \t\n\r", "Table2", "field21");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 > Table2.field21",
        select.getQuery());
  }

  @Test
  public final void testJoinNorEqualStringStringStringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.joinNotEqual("Table1", "field1 \t\n\r", "Table2", "field21");
    select.setWhere(clause);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 <> Table2.field21",
          select.getQuery());
    } catch (AssertionError e) {
//      e.printStackTrace();
      System.out.println("[INFO] TestIsCondition.testJoinNorEqualStringStringStringStringGeneric: "
          + "Runs alternative test 1");
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE "
          + "Table1.field1 != Table2.field21", select.getQuery());
    }
  }

  @Test
  public final void testJoinStringStrinStringStringGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.join("Table1", "field1", "Table2", "field21");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 = Table2.field21",
        select.getQuery());
  }

  @Test
  public final void testJoinUsingStringStringStringArr() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    // Phase1
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.joinUsing("Table1", "Table2 ", "field1");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table1.field1 = Table2.field1",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.joinUsing("Table1", "Table2 ", "field1", "field21");
    select1.setWhere(clause1);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE "
        + "Table1.field1 = Table2.field1 AND Table1.field21 = Table2.field21", select1.getQuery());
//    try {
//      IsCondition clause2 = SqlUtils.joinUsing("Table1", "Table2");
//      fail("Exception not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("need BeeRuntimeException ! " + e.getMessage());
//    }
  }

  @Test
  public final void testLessEqualIsExpressionObject() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.lessEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), "value");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <= 'value'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.lessEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), 10);
    select1.setWhere(clause1);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <= 10",
          select1.getQuery());
    } catch (AssertionError e) {
      System.out.println("Alternatyvus testas ");
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <= 10.0",
          select1.getQuery());
    }
    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.lessEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), false);
    select2.setWhere(clause2);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <= null",
        select2.getQuery());
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.lessEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), null);
    select3.setWhere(clause3);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <= null",
        select3.getQuery());
  }

  @Test
  public final void testLessEqualStringStringObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.lessEqual("Table2", "field21", "value1");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <= 'value1'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.lessEqual("Table2", "field21", 5L);
    select1.setWhere(clause1);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <= 5",
          select1.getQuery());
    } catch (AssertionError e) {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <= 5.0",
          select1.getQuery());
    }

    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.lessEqual("Table2", "field21", true);
    select3.setWhere(clause3);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <= 1",
        select3.getQuery());
    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFrom("Table1");

    IsCondition clause4 = SqlUtils.lessEqual("Table2", "field21", null);
    select4.setWhere(clause4);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <= null",
        select4.getQuery());
  }

  @Test
  public final void testLessExpressionObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.less(SqlUtils.bitAnd("Table2", "field21", "val21"), "value");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) < 'value'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.less(SqlUtils.bitAnd("Table2", "field21", "val21"), 10E1);
    select1.setWhere(clause1);
    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) < 100.0",
          select1.getQuery());
    } catch (AssertionError e) {
      System.out
          .println("[INFO]TestIscondition.testLessExpressionObjectGeneric: Alternative test 1");
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) < 100",
          select1.getQuery());
    }
    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.less(SqlUtils.bitAnd("Table2", "field21", "val21"), false);
    select2.setWhere(clause2);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) < null",
        select2.getQuery());
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.less(SqlUtils.bitAnd("Table2", "field21", "val21"), null);
    select3.setWhere(clause3);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) < null",
        select3.getQuery());
  }

  @Test
  public final void testLessStringStringObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.less("Table2", "field21", "value1");
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 < 'value1'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.less("Table2", "field21", 5L);
    select1.setWhere(clause1);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 < 5",
          select1.getQuery());
    } catch (AssertionError e) {
      System.out.println("TestIsCondition.testLessStringStringObjectGeneric: Alternative test1");
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 < 5.0",
          select1.getQuery());
    }

    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.less("Table2", "field21", true);
    select3.setWhere(clause3);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 < 1",
        select3.getQuery());

    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFrom("Table1");

    IsCondition clause4 = SqlUtils.less("Table2", "field21", null);
    select4.setWhere(clause4);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 < null",
        select4.getQuery());
  }

  @Test
  public void testLikeStringStringString() {
    final String query = "SELECT Table1.field11, Table1.field12 FROM Table1 WHERE ";
    SqlSelect select;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    select = new SqlSelect();
    select.addFields("Table1", "field11", "field12");
    select.addFrom("Table1");

    select.setWhere(SqlUtils.matches("Table2", "field21", "val22"));

    assertEquals(query + "Table2.field21 LIKE 'val22' ESCAPE '|'", select.getQuery());

//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//
//      select.setWhere(SqlUtils.matches("Table2", "field21", null));
//
//      assertEquals(query + "Table2.field21 LIKE null", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//
//      select.setWhere(SqlUtils.matches("Table2", "field21", ""));
//
//      assertEquals(query + "Table2.field21 LIKE '' ESCAPE '|'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//
//      select.setWhere(SqlUtils.matches("Table2", "", "val22"));
//
//      fail("Exceptions not work: " + select.getQuery());
//
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.Exception, need BeeRuntimeException" + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//
//      select.setWhere(SqlUtils.matches("Table2", null, "val22"));
//
//      fail("Exceptions not work: " + select.getQuery());
//
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.Exception, need BeeRuntimeException" + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//
//      select.setWhere(SqlUtils.matches("", "field21", "val22"));
//
//      fail("Exceptions not work: " + select.getQuery());
//
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.Exception, need BeeRuntimeException" + e.getMessage());
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//
//      select.setWhere(SqlUtils.matches(null, "field21", "val22"));
//
//      fail("Exceptions not work: " + select.getQuery());
//
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.Exception, need BeeRuntimeException" + e.getMessage());
//    }
  }

  @Test
  public final void testMoreEqualIsExpressionObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.moreEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), "value");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) >= 'value'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.moreEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), 10);
    select1.setWhere(clause1);
    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) >= 10",
          select1.getQuery());
    } catch (AssertionError e) {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) >= 10.0",
          select1.getQuery());
    }
    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.moreEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), false);
    select2.setWhere(clause2);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) >= null",
        select2.getQuery());
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.moreEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), null);
    select3.setWhere(clause3);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) >= null",
        select3.getQuery());
  }

  @Test
  public final void testMoreEqualStringStringObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.moreEqual("Table2", "field21", "value1");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 >= 'value1'",
        select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.moreEqual("Table2", "field21", 5L);
    select1.setWhere(clause1);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 >= 5",
          select1.getQuery());
    } catch (AssertionError e) {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21>=5.0",
          select1.getQuery());
    }
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.moreEqual("Table2", "field21", true);
    select3.setWhere(clause3);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 >= 1",
        select3.getQuery());
    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFrom("Table1");

    IsCondition clause4 = SqlUtils.moreEqual("Table2", "field21", null);
    select4.setWhere(clause4);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 >= null",
        select4.getQuery());
  }

  @Test
  public final void testMoreIsExpressionObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.more(SqlUtils.bitAnd("Table2", "field21", "val21"), "value");
    select.setWhere(clause);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) > 'value'",
        select.getQuery());

    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.more(SqlUtils.bitAnd("Table2", "field21", "val21"), 10);
    select1.setWhere(clause1);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) > 10",
          select1.getQuery());
    } catch (AssertionError e) {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) > 10.0",
          select1.getQuery());
    }

    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.more(SqlUtils.bitAnd("Table2", "field21", "val21"), false);
    select2.setWhere(clause2);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) > null",
        select2.getQuery());

    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.more(SqlUtils.bitAnd("Table2", "field21", "val21"), null);
    select3.setWhere(clause3);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) > null",
        select3.getQuery());
  }

  @Test
  public final void testMoreStringStringObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.more("Table2", "field21", "value1");
    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 > 'value1'",
        select.getQuery());

    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.more("Table2", "field21", 5L);
    select1.setWhere(clause1);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 > 5",
          select1.getQuery());
    } catch (AssertionError e) {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 > 5.0",
          select1.getQuery());
    }
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.more("Table2", "field21", true);
    select3.setWhere(clause3);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 > 1",
        select3.getQuery());
    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFrom("Table1");

    IsCondition clause4 = SqlUtils.more("Table2", "field21", null);
    select4.setWhere(clause4);

    assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 > null",
        select4.getQuery());
  }

  @Test
  public final void testNotEqualIsExpressionObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.notEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), "value");
    select.setWhere(clause);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE "
          + "(Table2.field21 & val21) <> 'value'", select.getQuery());
    } catch (AssertionError e) {
      System.out.println("[INFO] TestIsCondition.testNotEqualIsExpressionObjectGeneric: "
          + "Alternative test 1");
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE "
          + "(Table2.field21 & val21) != 'value'", select.getQuery());
    }
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.notEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), 10);
    select1.setWhere(clause1);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21&val21) <> 10",
          select1.getQuery());
    } catch (AssertionError e) {
      System.out
          .println("TestIsCondition.testNotEqualIsExpressionObjectGeneric: Alternative test 2");
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) != 10",
          select1.getQuery());
    }
    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.notEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), false);
    select2.setWhere(clause2);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <> 0",
          select2.getQuery());
    } catch (AssertionError e) {
      System.out
          .println("TestIsCondition.testNotEqualIsExpressionObjectGeneric: Alternative test 3");
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) != null",
          select2.getQuery());
    }
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.notEqual(SqlUtils.bitAnd("Table2", "field21", "val21"), null);
    select3.setWhere(clause3);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) <> null",
          select3.getQuery());
    } catch (AssertionError e) {
      System.out
          .println("TestIsCondition.testNotEqualIsExpressionObjectGeneric: Alternative test 4");
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.field21 & val21) "
          + "IS NOT NULL",
          select3.getQuery());
    }
  }

  @Test
  public final void testNotEqualStringStringObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    // Phase1
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.notEqual("Table2", "field21", "value1");
    select.setWhere(clause);

    try {
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <> 'value1'",
          select.getQuery());
    } catch (AssertionError e) {
      System.out.println("[INFO] TestIsCondition.testNotEqualStringStringObjectGeneric(): "
          + "Alternative test 1");
      assertEquals(
          "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 != 'value1'",
          select.getQuery());
    }
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.notEqual("Table2", "field21", 5L);
    select1.setWhere(clause1);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <> 5",
          select1.getQuery());
    } catch (AssertionError e) {
      System.out.println("[INFO] TestIsCondition.testNotEqualStringStringObjectGeneric(): "
          + "Alternative test 2");
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 != 5",
          select1.getQuery());
    }
    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    IsCondition clause3 = SqlUtils.notEqual("Table2", "field21", true);
    select3.setWhere(clause3);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <> 1",
          select3.getQuery());
    } catch (AssertionError e) {
      System.out.println("[INFO] TestIsCondition.testNotEqualStringStringObjectGeneric(): "
          + "Alternative test 3");
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 != 1",
          select3.getQuery());
    }

    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFrom("Table1");

    IsCondition clause4 = SqlUtils.notEqual("Table2", "field21", null);
    select4.setWhere(clause4);

    try {
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 <> null",
          select4.getQuery());
    } catch (AssertionError e) {
      System.out.println("[INFO] TestIsCondition.testNotEqualStringStringObjectGeneric(): "
          + "Alternative test 4");
      assertEquals("SELECT Table1.field1, Table1.field2 FROM Table1 WHERE Table2.field21 "
          + "IS NOT NULL",
          select4.getQuery());
    }
  }

  @Test
  public final void testNotIsCondition() {
    final String query = "SELECT Table1.field11, Table1.field12 FROM Table1 WHERE ";
    SqlSelect select;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    select = new SqlSelect();
    select.addFields("Table1", "field11", "field12");
    select.addFrom("Table1");
    select.setWhere(SqlUtils.not(SqlUtils.isNull("Table2", "field21")));

    assertEquals(query + "NOT(Table2.field21 IS NULL)", select.getQuery());

//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.not(null));
//      fail("Exceptions not works" + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception, need BeeRuntimeException " + e.getMessage());
//    }
  }

  @Test
  public final void testOrIsConditionArrGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2", "field3");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.or();

    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1", select
        .getQuery());
    IsCondition clause01 = SqlUtils.or(SqlUtils.equals(SqlUtils.name("field1"), "Something val"));

    select.setWhere(clause01);
    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1 WHERE "
        + "(field1 = 'Something val')", select.getQuery());
    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2", "field3");
    select1.addFrom("Table1");

    IsCondition clause1 =
        SqlUtils.or(SqlUtils.equals(SqlUtils.name("field1"), "Something val"), SqlUtils.equals(
            SqlUtils.name("field2"), "sv2"));
    select1.setWhere(clause1);
    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1 WHERE "
        + "(field1 = 'Something val' OR field2 = 'sv2')", select1.getQuery());

    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2", "field3");
    select2.addFrom("Table1");

    IsCondition clause2 = SqlUtils.or(null, null);
    select2.setWhere(clause2);
    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1",
        select2.getQuery());
  }

  @Test
  public final void testSqlFalseGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2", "field3");
    select.addFrom("Table1");

    IsCondition clause = SqlUtils.sqlFalse();

    select.setWhere(clause);

    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1 WHERE 1 = 0",
        select
            .getQuery());
  }

  @Test
  public final void testSqlTrueGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2", "field3");
    select.addFrom("Table1");
    IsCondition clause = SqlUtils.sqlTrue();

    select.setWhere(clause);
    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3 FROM Table1 WHERE 1 = 1",
        select
            .getQuery());
  }

  @Test
  public final void testStartsWithIsExpressionString() {
    final String query = "SELECT Table1.field11, Table1.field12 FROM Table1 WHERE ";
    SqlSelect select;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    select = new SqlSelect();
    select.addFields("Table1", "field11", "field12");
    select.addFrom("Table1");
    select.setWhere(SqlUtils.startsWith(SqlUtils.name("Table2"), "val2"));

    assertEquals(query + "Table2 LIKE 'val2%' ESCAPE '|'", select.getQuery());

//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith(SqlUtils.name("Table2"), ""));
//
//      assertEquals(query + "Table2 LIKE '%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith(SqlUtils.name("Table2"), null));
//
//      assertEquals(query + "Table2 LIKE 'null%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith(null, "val2"));
//
//      fail("Exceptions not work: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.Exception, need BeeRuntimeException: " + e.getMessage());
//    }
  }

  @Test
  public final void testStartsWithStringString() {
    final String query = "SELECT Table1.field11, Table1.field12 FROM Table1 WHERE ";
    SqlSelect select;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    select = new SqlSelect();
    select.addFields("Table1", "field11", "field12");
    select.addFrom("Table1");
    select.setWhere(SqlUtils.startsWith("Table2", "field21", "val2"));
    assertEquals(query + "Table2.field21 LIKE 'val2%' ESCAPE '|'", select.getQuery());

//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith("Table2", "", "val2"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception, need BeeRuntimeException");
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith("Table2", "field21", ""));
//      assertEquals(query + "Table2.field21 LIKE '%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith("Table2", "field21", null));
//      assertEquals(query + "Table2.field21 LIKE 'null%'", select.getQuery());
//
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith("Table2", null, "val2"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception, need BeeRuntimeException");
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith("", "field21", "val2"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception, need BeeRuntimeException");
//    }
//
//    try {
//      select = new SqlSelect();
//      select.addFields("Table1", "field11", "field12");
//      select.addFrom("Table1");
//      select.setWhere(SqlUtils.startsWith(null, "field21", "val2"));
//      fail("Exceptions not works: " + select.getQuery());
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail("java.lang.exception, need BeeRuntimeException");
//    }
  }
}
