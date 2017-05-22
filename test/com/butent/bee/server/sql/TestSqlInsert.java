package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlInsert}.
 */
@SuppressWarnings("static-method")
public class TestSqlInsert {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testAddConstant() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlInsert insert = new SqlInsert("TableName");

    insert.addConstant("username", "Petras");
    insert.addConstant("pass", "slaptasPass");

    assertEquals(
        "INSERT INTO TableName (username,pass) VALUES ('Petras','slaptasPass')",
        insert.getSqlString(builder));
  }

  @Test
  public final void testAddFields() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlInsert insert = new SqlInsert("TableName");

    insert.addFields("field1", "field2", "field3");

    assertEquals(
        "INSERT INTO TableName (field1,field2,field3) VALUES ",
        insert.getSqlString(builder));
  }

  @Test
  public final void testGetFieldCount() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlInsert insert = new SqlInsert("TableName");

    insert.addFields("field1", "field2", "field3");
    assertEquals(3, insert.getFieldCount());

//    try {
//      insert.addFields("field4", null);
//      assertEquals(4, insert.getFieldCount());
//      fail("BeeRuntimeException not works");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//      System.out
//          .println("public final void getField(): " + e.getMessage());
//    } catch (Exception e) {
//      fail("Java runtime error. Need BeeRuntimeException !!!");
//    }
  }

  @Test
  public final void testGetSources() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlInsert insert = new SqlInsert("TableName");

    SqlSelect select = new SqlSelect();
    select.addFields("Table2", "field1", "field2");

    select.addFrom("Table1");
    select.addFrom("Table2");
    insert.setDataSource(select);

    Object[] rez = insert.getSources().toArray();
    Object[] expected = {"Table2", "TableName", "Table1"};

    assertArrayEquals(expected, rez);
  }

  @Test
  public final void testIsEmpty() {
    SqlInsert insert = new SqlInsert("Table1");
    assertTrue(insert.isEmpty());
    insert.addExpression("Field1", SqlUtils.expression("100"));
    assertFalse(insert.isEmpty());
  }

  @Test
  public final void testReset() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlInsert insert = new SqlInsert("TableName");

    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");
    IsCondition clause = SqlUtils.equals("Table1", "password", "12345");
    select.setWhere(clause);

    insert.addFields("field1", "field2", "field3");
    insert.reset();

    assertEquals(0, insert.getFieldCount());

    insert.setDataSource(select);
    insert.reset();
    assertEquals(null, insert.getDataSource());
  }
}
