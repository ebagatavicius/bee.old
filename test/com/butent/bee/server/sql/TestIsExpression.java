package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Tests {@link com.butent.bee.server.sql.IsExpression}.
 */
@SuppressWarnings("static-method")
public class TestIsExpression {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public final void sqlCaseIsExpressionObjectArr() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect sql;

    sql = new SqlSelect();
    sql.addFields("Table1", "field11", "field12");
    sql.addFrom("Table1");

    sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), SqlUtils.constant("pair1"),
        "pair2", "pair3"), "Name1");

    assertEquals("SELECT Table1.field11, Table1.field12, CASE field21 WHEN 'pair1' "
        + "THEN 'pair2' ELSE 'pair3' END AS Name1 FROM Table1", sql.getQuery());

    sql = new SqlSelect();
    sql.addFields("Table1", "field11", "field12");
    sql.addFrom("Table1");

    sql.addExpr(
        SqlUtils.sqlCase(SqlUtils.name("field21"), SqlUtils.constant("pair1"),
            SqlUtils.name("field22"), "pair3"), "Name1");
    assertEquals("SELECT Table1.field11, Table1.field12, CASE field21 WHEN 'pair1' "
        + "THEN field22 ELSE 'pair3' END AS Name1 FROM Table1", sql.getQuery());

    sql = new SqlSelect();
    sql.addFields("Table1", "field11", "field12");
    sql.addFrom("Table1");

    sql.addExpr(
        SqlUtils.sqlCase(SqlUtils.name("field21"), SqlUtils.constant("pair1"),
            SqlUtils.constant("field22"), "pair3"), "Name1");
    assertEquals("SELECT Table1.field11, Table1.field12, CASE field21 WHEN 'pair1' "
        + "THEN 'field22' ELSE 'pair3' END AS Name1 FROM Table1", sql.getQuery());

    sql = new SqlSelect();
    sql.addFields("Table1", "field11", "field12");
    sql.addFrom("Table1");

    sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), SqlUtils.constant("pair1"),
        "pair2", SqlUtils.constant("pair3"), "pair4", "pair5"), "Name1");

    assertEquals("SELECT Table1.field11, Table1.field12, CASE field21 WHEN 'pair1' "
            + "THEN 'pair2' WHEN 'pair3' THEN 'pair4' ELSE 'pair5' END AS Name1 FROM Table1",
        sql.getQuery());

    sql = new SqlSelect();
    sql.addFields("Table1", "field11", "field12");
    sql.addFrom("Table1");

    sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), SqlUtils.constant("pair1"),
        "pair2", SqlUtils.constant("pair3"), "pair4", null), "Name1");

    assertEquals("SELECT Table1.field11, Table1.field12, CASE field21 WHEN 'pair1' "
            + "THEN 'pair2' WHEN 'pair3' THEN 'pair4' ELSE null END AS Name1 FROM Table1",
        sql.getQuery());

    try {
      sql = new SqlSelect();
      sql.addFields("Table1", "field11", "field12");
      sql.addFrom("Table1");

      sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), SqlUtils.constant("pair1"),
          "pair2", SqlUtils.constant("pair3"), "pair4", ""), "Name1");

      assertEquals("SELECT Table1.field11, Table1.field12, CASE field21 WHEN 'pair1' "
              + "THEN 'pair2' WHEN 'pair3' THEN 'pair4' ELSE '' END AS Name1 FROM Table1",
          sql.getQuery());
    } catch (BeeRuntimeException e) {
      assertTrue(false);
    } catch (Exception e) {
      e.printStackTrace();
      fail("java.lang.Exception, need BeeRuntimeException "
          + e.getMessage());
    }

/*    try {
      sql = new SqlSelect();
      sql.addFields("Table1", "field11", "field12");
      sql.addFrom("Table1");

      sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), "pair1",
          "pair2", "pair3", "pair4"), "Name1");

      fail("Exception not work: " + sql.getQuery());

    } catch (BeeRuntimeException e) {
      assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("java.lang.Exception, need BeeRuntimeException "
          + e.getMessage());
    }*/

    /*
     * try { sql = new SqlSelect(); sql.addFields("Table1", "field11", "field12");
     * sql.addFrom("Table1");
     *
     * sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), "pair1", "pair2",
     * SqlUtils.constant("field22"), "pair4", "pair5"), "Name1");
     *
     * fail("Exception not works: " + sql.getQuery()); } catch (BeeRuntimeException e) {
     * assertTrue(true); } catch (Exception e) { e.printStackTrace();
     * fail("java.lang.Exception need BeeRumtimeException: " + e.getMessage()); }
     */

    try {
      sql = new SqlSelect();
      sql.addFields("Table1", "field11", "field12");
      sql.addFrom("Table1");

      sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), "pair1",
          "pair2"), "Name1");

      //fail("Exception not work: " + sql.getQuery());

    } catch (BeeRuntimeException e) {
      assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("java.lang.Exception, need BeeRuntimeException "
          + e.getMessage());
    }

    try {
      sql = new SqlSelect();
      sql.addFields("Table1", "field11", "field12");
      sql.addFrom("Table1");

      sql.addExpr(SqlUtils.sqlCase(SqlUtils.name("field21"), "pair1"),
          "Name1");

      fail("Exception not work: " + sql.getQuery());

    } catch (BeeRuntimeException e) {
      assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("java.lang.Exception, need BeeRuntimeException "
          + e.getMessage());
    }

    try {
      sql = new SqlSelect();
      sql.addFields("Table1", "field11", "field12");
      sql.addFrom("Table1");

      sql.addExpr(SqlUtils.sqlCase(null, "pair1", "pair2", "pair3",
          "pair4", ""), "Name1");

      assertEquals("SELECT Table1.field11, Table1.field12, CASE WHEN 'pair1' "
              + "THEN 'pair2' WHEN 'pair3' THEN 'pair4' ELSE '' END AS Name1 FROM Table1",
          sql.getQuery());

    } catch (BeeRuntimeException e) {
      assertTrue(true);
    } catch (Exception e) {
      e.printStackTrace();
      fail("java.lang.Exception, need BeeRuntimeException "
          + e.getMessage());
    }
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testBitAndStringStringObjectGeneric() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2", "field3");
    select.addFrom("Table1");

    IsExpression clause = SqlUtils.bitAnd("Table1", "field1", "val1");
    // SqlUtils.and(SqlUtils.equal(SqlUtils.name("field1"),
    // "Something val"));

    select.addExpr(clause, "expr1");

    assertEquals("SELECT Table1.field1, Table1.field2, Table1.field3, "
        + "(Table1.field1 & val1) AS expr1 FROM Table1", select.getQuery());
  }

  @Test
  public final void testBitAndStringStringObjectOracle() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1", "field2", "field3");
    select.addFrom("Table1");
    IsExpression clause = SqlUtils.bitAnd("Table1", "field1", "val1");
    // SqlUtils.and(SqlUtils.equal(SqlUtils.name("field1"),
    // "Something val"));

    select.addExpr(clause, "expr1");

    assertEquals("SELECT \"Table1\".\"field1\", \"Table1\".\"field2\", \"Table1\".\"field3\", "
        + "BITAND(\"Table1\".\"field1\", val1) AS \"expr1\" FROM \"Table1\"", select.getQuery());
  }

  @Test
  public final void testCast() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DOUBLE, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS DOUBLE) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.CHAR, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS CHAR(5)) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DECIMAL, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS NUMERIC(5, 10)) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.BOOLEAN, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS BIT) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.INTEGER, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS INTEGER) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATE, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS BIGINT) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.LONG, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS BIGINT) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATETIME, 5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS BIGINT) AS TB1 FROM Table1",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.STRING, -5, 10), "TB1");

    assertEquals(
        "SELECT Table1.field1, CAST(Table1.field2 AS VARCHAR(-5)) AS TB1 FROM Table1",
        s.getQuery());

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DOUBLE, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS FLOAT) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.CHAR, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS CHAR(5)) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DECIMAL, 5, 10), "TB1");

    assertEquals("SELECT [Table1].[field1], CAST([Table1].[field2] AS NUMERIC(5, 10)) AS [TB1] "
        + "FROM [Table1]", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.BOOLEAN, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS BIT) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.INTEGER, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS INTEGER) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATE, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS BIGINT) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.LONG, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS BIGINT) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATETIME, 5, 10), "TB1");

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS BIGINT) AS [TB1] FROM [Table1]",
        s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.STRING, -5, 10), "TB1"); // praleidziam

    assertEquals(
        "SELECT [Table1].[field1], CAST([Table1].[field2] AS VARCHAR(-5)) AS [TB1] FROM [Table1]",
        s.getQuery());

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DOUBLE, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS BINARY_DOUBLE) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.CHAR, 5, 10), "TB1");

    assertEquals(
        "SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NCHAR(5)) AS \"TB1\" "
            + "FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DECIMAL, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(5, 10)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.BOOLEAN, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(1)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.INTEGER, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(10)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATE, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(19)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.LONG, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(19)) AS "
        + "\"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATETIME, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(19)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.STRING, -5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NVARCHAR2(-5)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    /* PODTGRE SQL */

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DOUBLE, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS DOUBLE PRECISION) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.CHAR, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS CHAR(5)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DECIMAL, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(5, 10)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.BOOLEAN, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS NUMERIC(1)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.INTEGER, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS INTEGER) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATE, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS BIGINT) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.LONG, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS BIGINT) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.DATETIME, 5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS BIGINT) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());

    s = new SqlSelect();
    s.addFields("Table1", "field1");
    s.addFrom("Table1");
    s.addExpr(SqlUtils.cast(SqlUtils.field("Table1", "field2"),
        SqlDataType.STRING, -5, 10), "TB1");

    assertEquals("SELECT \"Table1\".\"field1\", CAST(\"Table1\".\"field2\" AS VARCHAR(-5)) "
        + "AS \"TB1\" FROM \"Table1\"", s.getQuery());
  }

  @Test
  public void testComplexExpressionGeneric() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    IsExpression ce = SqlUtils.expression(1, "string", 5.0);
    assertEquals("1string5.0",
        ce.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select = new SqlSelect();
    select.addField("users", "username", "vardas");
    select.addFrom("users");

    IsExpression ce2 = SqlUtils.expression(select);
    assertEquals("SELECT users.username AS vardas FROM users",
        ce2.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select2 = new SqlSelect();
    select2.addField("phones", "phone_names", "tel_vardai");
    select2.addFrom("phones");
    IsExpression ce3 = SqlUtils.expression(select2);
    assertEquals("SELECT phones.phone_names AS tel_vardai FROM phones",
        ce3.getSqlString(SqlBuilderFactory.getBuilder()));

    select.addFrom(select2, "alias2");

    IsExpression ce4 = SqlUtils.expression(select);
    assertEquals("SELECT users.username AS vardas FROM users, (SELECT phones.phone_names "
        + "AS tel_vardai FROM phones) alias2", ce4.getSqlString(SqlBuilderFactory.getBuilder()));

    select2 = select2.reset();
    select2.addField("JUnit", "method_names", "klases");
    select2.setDistinctMode(true);
    select2.addFrom("JUnit");
    IsExpression ce5 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT DISTINCT JUnit.method_names AS klases FROM phones, JUnit",
        ce5.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public void testComplexExpressionMsSql() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    IsExpression ce = SqlUtils.expression(1, "string", 5.0);
    assertEquals("1string5.0",
        ce.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select = new SqlSelect();
    select.addField("users", "username", "vardas");
    select.addFrom("users");

    IsExpression ce2 = SqlUtils.expression(select);
    assertEquals("SELECT [users].[username] AS [vardas] FROM [users]",
        ce2.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select2 = new SqlSelect();
    select2.addField("phones", "phone_names", "tel_vardai");
    select2.addFrom("phones");
    IsExpression ce3 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT [phones].[phone_names] AS [tel_vardai] FROM [phones]",
        ce3.getSqlString(SqlBuilderFactory.getBuilder()));

    select.addFrom(select2, "alias2");

    IsExpression ce4 = SqlUtils.expression(select);
    assertEquals("SELECT [users].[username] AS [vardas] FROM [users], "
            + "(SELECT [phones].[phone_names] AS [tel_vardai] FROM [phones]) [alias2]",
        ce4.getSqlString(SqlBuilderFactory.getBuilder()));

    select2 = select2.reset();
    select2.addField("JUnit", "method_names", "klases");
    select2.setDistinctMode(true);
    select2.addFrom("JUnit");
    IsExpression ce5 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT DISTINCT [JUnit].[method_names] AS [klases] FROM [phones], [JUnit]",
        ce5.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public void testComplexExpressionOracle() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    IsExpression ce = SqlUtils.expression(1, "string", 5.0);
    assertEquals("1string5.0",
        ce.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select = new SqlSelect();
    select.addField("users", "username", "vardas");
    select.addFrom("users");

    IsExpression ce2 = SqlUtils.expression(select);
    assertEquals(
        "SELECT \"users\".\"username\" AS \"vardas\" FROM \"users\"",
        ce2.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select2 = new SqlSelect();
    select2.addField("phones", "phone_names", "tel_vardai");
    select2.addFrom("phones");
    IsExpression ce3 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT \"phones\".\"phone_names\" AS \"tel_vardai\" FROM \"phones\"",
        ce3.getSqlString(SqlBuilderFactory.getBuilder()));

    select.addFrom(select2, "alias2");

    IsExpression ce4 = SqlUtils.expression(select);
    assertEquals("SELECT \"users\".\"username\" AS \"vardas\" FROM \"users\", "
            + "(SELECT \"phones\".\"phone_names\" AS \"tel_vardai\" FROM \"phones\") \"alias2\"",
        ce4.getSqlString(SqlBuilderFactory.getBuilder()));

    select2 = select2.reset();
    select2.addField("JUnit", "method_names", "klases");
    select2.setDistinctMode(true);
    select2.addFrom("JUnit");
    IsExpression ce5 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT DISTINCT \"JUnit\".\"method_names\" AS \"klases\" FROM \"phones\", \"JUnit\"",
        ce5.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public void testComplexExpressionPGSql() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    IsExpression ce = SqlUtils.expression(1, "string", 5.0);
    assertEquals("1string5.0",
        ce.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select = new SqlSelect();
    select.addField("users", "username", "vardas");
    select.addFrom("users");

    IsExpression ce2 = SqlUtils.expression(select);
    assertEquals(
        "SELECT \"users\".\"username\" AS \"vardas\" FROM \"users\"",
        ce2.getSqlString(SqlBuilderFactory.getBuilder()));

    SqlSelect select2 = new SqlSelect();
    select2.addField("phones", "phone_names", "tel_vardai");
    select2.addFrom("phones");
    IsExpression ce3 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT \"phones\".\"phone_names\" AS \"tel_vardai\" FROM \"phones\"",
        ce3.getSqlString(SqlBuilderFactory.getBuilder()));

    select.addFrom(select2, "alias2");

    IsExpression ce4 = SqlUtils.expression(select);
    assertEquals("SELECT \"users\".\"username\" AS \"vardas\" FROM \"users\", "
            + "(SELECT \"phones\".\"phone_names\" AS \"tel_vardai\" FROM \"phones\") \"alias2\"",
        ce4.getSqlString(SqlBuilderFactory.getBuilder()));

    select2 = select2.reset();
    select2.addField("JUnit", "method_names", "klases");
    select2.setDistinctMode(true);
    select2.addFrom("JUnit");
    IsExpression ce5 = SqlUtils.expression(select2);
    assertEquals(
        "SELECT DISTINCT \"JUnit\".\"method_names\" AS \"klases\" FROM \"phones\", \"JUnit\"",
        ce5.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public void testConstantExpressionGeneric() {
    JustDate jd = new JustDate(1298362388227L);
    DateTime dt = new DateTime(1298362388227L);

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    IsExpression ce = SqlUtils.constant(null);
    assertEquals("null",
        ce.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ce2 = SqlUtils.constant(false);
    assertEquals("null",
        ce2.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ce3 = SqlUtils.constant(jd);
    assertEquals(BeeUtils.toString(jd.getTime()),
        ce3.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ce5 = SqlUtils.constant(dt);
    assertEquals("1298362388227",
        ce5.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ce6 = SqlUtils.constant(5);
    assertEquals("5",
        ce6.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ce7 = SqlUtils.constant(" 'from' ");
    assertEquals("' ''from'' '",
        ce7.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ce8 = SqlUtils.constant(true);
    assertEquals("1",
        ce8.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public void testConstantExpressionMsSql() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    IsExpression ce7 = SqlUtils.constant(" 'from' ");
    assertEquals("' ''from'' '",
        ce7.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @SuppressWarnings("unused")
  @Test
  public void testConstantExpressionOracle() {
    Date d = new Date(1298362388227L);
    JustDate jd = new JustDate(1298362388227L);
    DateTime dt = new DateTime(1298362388227L);
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    IsExpression ce7 = SqlUtils.constant(" \from' ");
    assertEquals("' \from'' '",
        ce7.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public void testConstantExpressionPostGre() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    IsExpression ce7 = SqlUtils.constant(" \\from\\ ");
    assertEquals("' \\from\\ '",
        ce7.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public final void testFields() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    IsExpression[] expressions = SqlUtils.fields("Table1", "field1",
        "field2");
    String[] expected = {"Table1.field1", "Table1.field2"};

    for (int i = 0; i < expressions.length; i++) {
      assertEquals(expected[i].toString(),
          expressions[i].getSqlString(builder));
    }
  }

  @Test
  public void testNameExpression() {
    IsExpression ie = SqlUtils.name("A longer name");
    assertEquals("A longer name", ie.getValue());

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals("A longer name",
        ie.getSqlString(SqlBuilderFactory.getBuilder()));
    IsExpression ie2 = SqlUtils.name("A.longer.name");
    assertEquals("A.longer.name",
        ie2.getSqlString(SqlBuilderFactory.getBuilder()));
    IsExpression ie3 = SqlUtils.name("select.*.from.name");
    assertEquals("select.*.from.name",
        ie3.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression iemssql = SqlUtils.name("A longer name");
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    assertEquals("[A longer name]",
        iemssql.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ie2mssql = SqlUtils.name("A.longer.name");
    assertEquals("[A].[longer].[name]",
        ie2mssql.getSqlString(SqlBuilderFactory.getBuilder()));
    IsExpression ie3mssql = SqlUtils.name("select.*.from.name");
    assertEquals("[select].*.[from].[name]",
        ie3mssql.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ie4mssql = SqlUtils
        .name("Select * from table where name=\"tester\" or table1.type=table.*.concret.type");
    assertEquals(
        "[Select * from table where name=\"tester\" or table1].[type=table].*.[concret].[type]",
        ie4mssql.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ieoracle = SqlUtils.name("A longer name");
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    assertEquals("\"A longer name\"",
        ieoracle.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ie2oracle = SqlUtils.name("A.longer.name");
    assertEquals("\"A\".\"longer\".\"name\"",
        ie2oracle.getSqlString(SqlBuilderFactory.getBuilder()));
    IsExpression ie3oracle = SqlUtils.name("select.*.from.name");
    assertEquals("\"select\".*.\"from\".\"name\"",
        ie3oracle.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ie4oracle = SqlUtils
        .name("Select * from table where name=\"tester\" or table1.type=table.*.concret.type");
    assertEquals("\"Select * from table where name=\"tester\" or "
            + "table1\".\"type=table\".*.\"concret\".\"type\"",
        ie4oracle.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression iepg = SqlUtils.name("A longer name");
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    assertEquals("\"A longer name\"",
        iepg.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ie2pg = SqlUtils.name("A.longer.name");
    assertEquals("\"A\".\"longer\".\"name\"",
        ie2pg.getSqlString(SqlBuilderFactory.getBuilder()));
    IsExpression ie3pg = SqlUtils.name("select.*.from.name");
    assertEquals("\"select\".*.\"from\".\"name\"",
        ie3pg.getSqlString(SqlBuilderFactory.getBuilder()));

    IsExpression ie4pg = SqlUtils
        .name("Select * from table where name=\"tester\" or table1.type=table.*.concret.type");
    assertEquals("\"Select * from table where name=\"tester\" or "
            + "table1\".\"type=table\".*.\"concret\".\"type\"",
        ie4pg.getSqlString(SqlBuilderFactory.getBuilder()));
  }

  @Test
  public final void testSqlIf() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addFields("Table1", "field1");
    select.addFrom("Table1");

    IsCondition cond1 = SqlUtils.equals(SqlUtils.name("value1"),
        "pirmadienis");

    IsExpression expr = SqlUtils.sqlIf(cond1,
        SqlUtils.field("Table2", "field1"),
        SqlUtils.field("Table3", "field1"));
    select.addExpr(expr, "expr1");

    assertEquals("SELECT Table1.field1, CASE WHEN value1 = 'pirmadienis' THEN Table2.field1 ELSE "
            + "Table3.field1 END AS expr1 FROM Table1",
        select.getQuery());
  }

  @Test
  public final void testTemporaryName() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    SqlSelect select1 = new SqlSelect();
    select1.addFields("Table1", "field1", "field2");
    select1.addFrom("Table1");

    IsCondition clause1 = SqlUtils.less(SqlUtils.bitAnd("Table2",
        SqlUtils.temporaryName("tempName1"), "val21"), 10E1);
    select1.setWhere(clause1);

    assertEquals(
        "SELECT Table1.field1, Table1.field2 FROM Table1 WHERE (Table2.tempName1 & val21) < 100",
        select1.getQuery());
  }

  @Test
  public final void testTemporaryNameRandomParams() {
    // ----------------------------------------------------------------------
    String str = SqlUtils.temporaryName("");
    for (int i = 0; i < 100; i++) {
      assertEquals(true,
          str.compareTo("aaa") >= 0 && str.compareTo("zzz") <= 0);
    }
  }
}
