package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlDelete}.
 */
@SuppressWarnings("static-method")
public class TestSqlDelete {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testGetSources() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlDelete delete = new SqlDelete("Target_table");
    delete.setWhere(SqlUtils.sqlFalse());

    Object[] arr = delete.getSources().toArray();
    Object[] rez = {"Target_table"};

    assertArrayEquals(rez, arr);
  }

  @Test
  public final void testGetSqlString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlDelete delete = new SqlDelete("Target_table");

    delete.setWhere(SqlUtils.equals(SqlUtils.name("username"), "root"));

    assertEquals(
        "DELETE FROM Target_table WHERE username = 'root'",
        delete.getSqlString(builder));
  }

  @Test
  public final void testGetSqlStringAlias() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlDelete delete = new SqlDelete("Target_table");

    delete.setWhere(SqlUtils.equals(SqlUtils.name("username"), "root"));

    assertEquals(
        "DELETE FROM Target_table WHERE username = 'root'",
        delete.getSqlString(builder));
  }

  @Test
  public final void testIsEmpty() {
//    try {
//      SqlDelete del = new SqlDelete("\n \t \r");
//      fail("Exceptions not work");
//    } catch (BeeRuntimeException e) {
//      assertTrue(true);
//    } catch (Exception e) {
//      fail("Need BeeRuntimeException: " + e.getMessage());
//    }
    SqlDelete del = new SqlDelete("Table1");
    assertTrue(del.isEmpty());

    del.setWhere(SqlUtils.isNull("Table1", "Filed1"));
    assertFalse(del.isEmpty());
  }

}
