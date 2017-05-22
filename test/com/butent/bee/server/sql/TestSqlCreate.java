package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlCreate}.
 */
@SuppressWarnings("static-method")
public class TestSqlCreate {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testIsEmpty() {
    // try {
    // SqlCreate create = new SqlCreate("\n \r \t");
    // fail("Exceptions not work");
    // } catch (BeeRuntimeException e) {
    // assertTrue(true);
    // } catch (Exception e) {
    // fail("Need BeeRuntimeEception : " + e.getMessage());
    // }

    SqlCreate create = new SqlCreate("table");
    assertTrue(create.isEmpty());
    assertFalse(create.hasField("table"));

    create.addField("Field1", SqlDataType.DOUBLE, 5, 100, false);

    assertFalse(create.isEmpty());
    assertFalse(create.hasField("table"));
    assertTrue(create.hasField("Field1"));
  }

  @Test
  public final void testSqlCreate() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlCreate create = new SqlCreate("Target", true);

    create.addBoolean("arIvykdyta", true);
    create.getSqlString(builder);
    assertEquals("CREATE TEMPORARY TABLE Target (arIvykdyta BIT NOT NULL)", create.getSqlString(
        builder));

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    builder = SqlBuilderFactory.getBuilder();

    create = new SqlCreate("Target", true);

    create.addBoolean("arIvykdyta", true);
    create.addField("field2", SqlDataType.INTEGER, 5, 6, false);
    create.getSqlString(builder);
    assertEquals("CREATE TABLE [Target] ([arIvykdyta] BIT NOT NULL, [field2] INTEGER)", create
        .getSqlString(builder));

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder2 = SqlBuilderFactory.getBuilder();

    SqlCreate create2 = new SqlCreate("Target", true);

    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    create2.setDataSource(select2);
    assertEquals(
        "CREATE TEMPORARY TABLE Target AS SELECT Table1.field1, Table1.field2 FROM Table1", create2
            .getSqlString(builder2));

    SqlBuilder builder3 = SqlBuilderFactory.getBuilder();

    SqlCreate create3 = new SqlCreate("Target", true);

    SqlSelect select3 = new SqlSelect();
    select3.addFields("Table1", "field1", "field2");
    select3.addFrom("Table1");

    create3.addBoolean("boolean field", true);
    create3.addChar("char", 25, true);
    create3.addDate("data", false);
    create3.addDateTime("datetime", false);
    create3.addDouble("double value", false);
    create3.addInteger("int field", true);
    create3.addLong("long field", false);
    create3.addDecimal("numeric field", 10, 10, false);
    create3.addString("string field", 7, false);

    assertEquals("CREATE TEMPORARY TABLE Target (boolean field BIT NOT NULL, "
        + "char CHAR(25) NOT NULL, data BIGINT, datetime BIGINT, double value DOUBLE, "
        + "int field INTEGER NOT NULL, long field BIGINT, numeric field NUMERIC(10, 10), "
        + "string field VARCHAR(7))",
        create3.getSqlString(builder3));

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    builder = SqlBuilderFactory.getBuilder();

    create = new SqlCreate("Target", true);

    create.addBoolean("arIvykdyta", true);
    create.addDouble("kaina", false);
    create.addDate("data", false);
    create.getSqlString(builder);
    assertEquals("CREATE TEMPORARY TABLE \"Target\" (\"arIvykdyta\" "
        + "NUMERIC(1) NOT NULL, \"kaina\" DOUBLE PRECISION, \"data\" BIGINT)",
        create.getSqlString(builder));

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    builder = SqlBuilderFactory.getBuilder();

    create = new SqlCreate("Target", true);

    create.addBoolean("arIvykdyta", true);
    create.addDouble("kaina", false);
    create.addDate("data", false);
    create.addInteger("kiek", false);
    create.addLong("millis", false);
    create.addDateTime("dateTime", false);
    create.addString("pav", 10, false);
    create.addChar("char field", 2, false);

    create.getSqlString(builder);
    assertEquals("CREATE TABLE \"Target\" (\"arIvykdyta\" "
        + "NUMERIC(1) NOT NULL, \"kaina\" BINARY_DOUBLE, \"data\" "
        + "NUMERIC(19), \"kiek\" NUMERIC(10), \"millis\" NUMERIC(19), \"dateTime\" "
        + "NUMERIC(19), \"pav\" NVARCHAR2(10), \"char field\" NCHAR(2))",
        create.getSqlString(builder));
  }

  @Test
  public final void testSqlCreateDataSource() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder4 = SqlBuilderFactory.getBuilder();

    SqlCreate create4 = new SqlCreate("Target");
    SqlCreate create5 = new SqlCreate("Target2");
    create5.addBoolean("bool2", false);

    SqlSelect select4 = new SqlSelect();
    select4.addFields("Table1", "field1", "field2");
    select4.addFields("Table1", "field1");
    select4.addFrom("Table1");

    create4.addBoolean("boolean field", true);

    create4.addDate("data", false);
    create4.addDateTime("datetime", false);
    create4.addDouble("double value", false);
    create4.addLong("long field", false);
    create4.addDecimal("numeric field", 10, 10, false);
    create4.addString("string field", 7, false);

    assertEquals("CREATE TEMPORARY TABLE Target (boolean field BIT NOT NULL, data BIGINT, "
        + "datetime BIGINT, double value DOUBLE, long field BIGINT, "
        + "numeric field NUMERIC(10, 10), string field VARCHAR(7))",
        create4.getSqlString(builder4));

    IsExpression expression = SqlUtils.name(create5.getField("bool2").getName());
    assertEquals("bool2", expression.getSqlString(builder4));
  }

  @Test
  public final void testSqlCreateGetSources() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlCreate create2 = new SqlCreate("Target", true);

    SqlSelect select2 = new SqlSelect();
    select2.addFields("Table1", "field1", "field2");
    select2.addFrom("Table1");

    create2.setDataSource(select2);
    String[] expectedStr = {"Table1"};
    for (int i = 0; i < create2.getSources().size(); i++) {
      assertEquals(expectedStr[i], create2.getSources().toArray()[i]);
    }
  }
}
