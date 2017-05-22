package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlUpdate}.
 */
public class TestSqlUpdate {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @SuppressWarnings("static-method")
  @Test
  public final void testGetSqlString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlUpdate update = new SqlUpdate("Source1");
    update.addConstant("field1", "value1");
    update.addConstant("field2", "value2");
    assertEquals("UPDATE Source1 SET field1='value1', field2='value2'", update.getSqlString(
        builder));

    update.reset();
    IsExpression expr = SqlUtils.field("Sourceexpr", "fieldexpr");
    update.addExpression("field1", expr);

    assertEquals("UPDATE Source1 SET field1=Sourceexpr.fieldexpr", update.getSqlString(builder));

    SqlUpdate update2 = new SqlUpdate("Source1");
    IsCondition where = SqlUtils.equals("Source1", "name", "John");
    update2.addConstant("name", "Petras");
    update2.setWhere(where);

    assertEquals("UPDATE Source1 SET name='Petras' WHERE Source1.name = 'John'", update2
        .getSqlString(builder));
  }

  @SuppressWarnings("static-method")
  @Test
  public final void testIsEmpty() {
    SqlUpdate update = new SqlUpdate("target");
    assertTrue(update.isEmpty()); // nes neturi BeeUtils nepalaiko isFrom, bet tikrina ar jis nera
                                  // NULL ;

    update.addExpression("Field1", SqlUtils.constant("hello"));

    assertFalse(update.isEmpty());

    update = new SqlUpdate("Table1");
    assertTrue(update.isEmpty());
  }
}
