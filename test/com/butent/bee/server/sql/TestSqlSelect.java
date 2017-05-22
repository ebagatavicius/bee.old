package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst.SqlEngine;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlSelect}.
 */
@SuppressWarnings("static-method")
public class TestSqlSelect {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testAddAllFields() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addAllFields("Source_table");

    assertEquals("SELECT Source_table.* FROM from_table",
        select.getSqlString(builder));
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    builder = SqlBuilderFactory.getBuilder();
    assertEquals("SELECT [Source_table].* FROM [from_table]",
        select.getSqlString(builder));
    select.setLimit(30);
    assertEquals("SELECT TOP 30 [Source_table].* FROM [from_table]",
        select.getSqlString(builder));
    select.setOffset(10);
    select.addOrder("Table1", "field2");

    assertEquals(true,
        select.getSqlString(builder).contains("SELECT ["));
    assertEquals(
        true,
        select.getSqlString(builder)
            .contains(
                "].* FROM (SELECT TOP 40 ROW_NUMBER() OVER (ORDER BY [Table1].[field2]) AS ["));
    assertEquals(
        true,
        select.getSqlString(builder).contains(
            "], [Source_table].* FROM [from_table]) ["));
    assertEquals(true,
        select.getSqlString(builder).contains("] WHERE ["));

    SqlSelect select1 = new SqlSelect();
    select1.addFrom("Table1");
    select1.addAllFields("Table12");

    select = new SqlSelect();
    select.addFrom("from_table");
    select.addAllFields("Source_table");
    select.setLimit(9);
    select.addUnion(select1);
    select.addOrder("Table22", "field21");

    assertEquals(true, select.getSqlString(builder).contains("SELECT TOP 9 ["));

    assertEquals(true, select.getSqlString(builder).contains("].* FROM (SELECT [Source_table].* "
        + "FROM [from_table] UNION ALL (SELECT [Table12].* FROM [Table1]) ORDER BY [field21]) ["));
  }

  @Test
  public final void testAddAvgExprString() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addAvg(SqlUtils.name("salary"), "atlygio_vidurkis");
    assertEquals("SELECT AVG(salary) AS atlygio_vidurkis FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddAvgStringString() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addAvg("from_table", "salary");
    assertEquals("SELECT AVG(from_table.salary) AS salary FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddAvgStringStringString() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addAvg("from_table", "salary", "atlygio_vidurkis");
    assertEquals(
        "SELECT AVG(from_table.salary) AS atlygio_vidurkis FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddConstant() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addConstant("name", "const_alias");
    assertEquals("SELECT 'name' AS const_alias FROM from_table",
        select.getSqlString(builder));

    select.addConstant(true, "const_alias2");
    assertEquals(
        "SELECT 'name' AS const_alias, 1 AS const_alias2 FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddCountExprString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");

    select.addCount(SqlUtils.name("name"), "name_alias");

    assertEquals("SELECT COUNT(name) AS name_alias FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddCountString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addCount("count_alias");

    assertEquals("SELECT COUNT(*) AS count_alias FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddEmptyBoolean() {
    SqlSelect sql;
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    sql = new SqlSelect();
    sql.addFields("Table1", "field11");
    sql.addFrom("Table1");

    sql.addEmptyBoolean("bool1");

    assertEquals(
        "SELECT Table1.field11, CAST(null AS BIT) AS bool1 FROM Table1",
        sql.getQuery());
  }

  @Test
  public final void testAddExprStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("from_table");
    select.addExpr(SqlUtils.expression("kazkas"), "expr_alias");

    assertEquals("SELECT kazkas AS expr_alias FROM from_table",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddMaxExprString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addMax(SqlUtils.expression("salary"), "highest_salary");

    assertEquals("SELECT MAX(salary) AS highest_salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddMaxStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addMax("employees", "salary");

    assertEquals("SELECT MAX(employees.salary) AS salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddMaxStringStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addMax("employees", "salary", "highest_salary");

    assertEquals(
        "SELECT MAX(employees.salary) AS highest_salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddMinExprString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addMin(SqlUtils.expression("salary"), "lowest_salary");

    assertEquals("SELECT MIN(salary) AS lowest_salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddMinStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addMin("employees", "salary");

    assertEquals("SELECT MIN(employees.salary) AS salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddMinStringStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addMin("employees", "salary", "lowest_salary");

    assertEquals(
        "SELECT MIN(employees.salary) AS lowest_salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddOrder() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addFields("employees", "name");
    select.addOrder("employees", "salary", "hours");

    assertEquals(
        "SELECT employees.name FROM employees ORDER BY employees.salary, employees.hours",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddOrderDesc() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addFields("employees", "name");
    select.addOrderDesc("employees", "salary", "hours");

    assertEquals(
        "SELECT employees.name FROM employees ORDER BY employees.salary DESC, employees.hours DESC",
        select.getSqlString(builder));
  }

  @Test
  public final void testAddUnion() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addField("Table", "name", "vardai");
    select.addFrom("employees_Butent_Kaunas");

    SqlSelect select2 = new SqlSelect();
    select2.addField("Table2", "name2", "vardai2");
    select2.addFrom("employees_Butent_Vilnius");

    select.addUnion(select2);

    assertEquals("SELECT Table.name AS vardai FROM employees_Butent_Kaunas UNION ALL "
        + "(SELECT Table2.name2 AS vardai2 FROM employees_Butent_Vilnius)",
        select.getSqlString(builder));
  }

  @Test
  public final void testCopyOf() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();

    select.addFields("Table1", "field1", "field2");
    select.addFrom("Table1");

    SqlSelect cpselect = select.copyOf();

    assertEquals(select.getQuery(), cpselect.getQuery());
    String excpected = select.getQuery();

    select.addFrom("Table2");
    assertEquals(excpected, cpselect.getQuery());

    cpselect = select.copyOf();
    assertEquals(select.getQuery(), cpselect.getQuery());

    excpected = select.getQuery();
    select.addGroup("Table2", "field1", "field2");
    assertEquals(excpected, cpselect.getQuery());
    cpselect = select.copyOf();
    assertEquals(select.getQuery(), cpselect.getQuery());

    excpected = select.getQuery();
    select.addOrder("Table1", "field3", "field4");
    assertEquals(excpected, cpselect.getQuery());
    cpselect = select.copyOf();
    assertEquals(select.getQuery(), cpselect.getQuery());

    excpected = select.getQuery();
    select.addUnion(cpselect);
    assertEquals(excpected, cpselect.getQuery());
    cpselect = select.copyOf();
    assertEquals(select.getQuery(), cpselect.getQuery());
  }

  @Test
  public final void testGetSources() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addField("Table", "name", "vardai");
    select.addFrom("employees_Butent_Kaunas");

    SqlSelect select2 = new SqlSelect();
    select2.addField("Table2", "name2", "vardai2");
    select2.addFrom("employees_Butent_Vilnius");
    select2.setWhere(SqlUtils.sqlTrue());

    select.addUnion(select2);
    select.addOrder("Table", "name");
    select.addGroup("Table", "name");
    select.setHaving(SqlUtils.contains(SqlUtils.expression("name"), "Petr"));
    select.setDistinctMode(false);

    Object[] a = select.getSources().toArray();
    Object[] rez = {"employees_Butent_Vilnius", "employees_Butent_Kaunas"};

    assertArrayEquals(rez, a);

    SqlSelect select3 = new SqlSelect();
    select3.addFrom("Table2");
    select3.addFields("Table2", "field21");
    select3.setWhere(SqlUtils.equals(SqlUtils.name("field31"), "val1"));
  }

  @Test
  public final void testIsEmpty() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    assertTrue(select.isEmpty());

    select.addFields("Table1", "field1");
    assertFalse(select.isEmpty());

    select.addFrom("Table1");
    assertFalse(select.isEmpty());
  }

  @Test
  public final void testReset() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlSelect select = new SqlSelect();
    select.addField("Table", "name", "vardai");
    select.addFrom("employees_Butent_Kaunas");

    SqlSelect select2 = new SqlSelect();
    select2.addField("Table2", "name2", "vardai2");
    select2.addFrom("employees_Butent_Vilnius");

    select.addUnion(select2);
    select.addOrder("Table", "name");
    select.addGroup("Table", "name");
    select.setHaving(SqlUtils.contains(SqlUtils.expression("name"), "Petr"));
    select.setDistinctMode(false);

    select.reset();

    assertEquals(0, select.getFields().size());
    assertEquals(0, select.getGroupBy().size());
    assertEquals(null, select.getHaving());
    assertEquals(0, select.getLimit());
    assertEquals(0, select.getOrderBy().size());
    assertEquals(0, select.getOffset());
  }

  @Test
  public final void testSqlLimitOffset() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select2 = new SqlSelect();
    select2.addFrom("table1");
    select2.addFields("table1", "field1");

    select2.setOffset(10);
    assertEquals("SELECT \"table1\".\"field1\" FROM \"table1\" OFFSET 10",
        select2.getSqlString(builder));

    select2.setLimit(10);
    assertEquals(
        "SELECT \"table1\".\"field1\" FROM \"table1\" LIMIT 10 OFFSET 10",
        select2.getSqlString(builder));

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    builder = SqlBuilderFactory.getBuilder();

    SqlSelect select3 = new SqlSelect();
    select3.addFrom("table1");
    select3.addFields("table1", "field1");

    select3.setOffset(10);
    assertEquals(true,
        select3.getSqlString(builder).contains("SELECT "));
    assertEquals(
        true,
        select3.getSqlString(builder).contains(
            ".* FROM (SELECT ROWNUM AS "));
    assertEquals(true, select3.getSqlString(builder).contains(", "));
    assertEquals(
        true,
        select3.getSqlString(builder)
            .contains(
                ".* FROM (SELECT \"table1\".\"field1\" FROM \"table1\") "));
    assertEquals(true,
        select3.getSqlString(builder).contains(" > 10"));

    select3.setLimit(10);
    assertEquals(true,
        select3.getSqlString(builder).contains("SELECT "));
    assertEquals(
        true,
        select3.getSqlString(builder).contains(
            ".* FROM (SELECT /*+ FIRST_ROWS(20) */ ROWNUM AS "));
    assertEquals(true, select3.getSqlString(builder).contains(", "));
    assertEquals(
        true,
        select3.getSqlString(builder)
            .contains(
                ".* FROM (SELECT \"table1\".\"field1\" FROM \"table1\") "));
    assertEquals(
        true,
        select3.getSqlString(builder).contains(
            " WHERE ROWNUM <= 20) "));
    assertEquals(true,
        select3.getSqlString(builder).contains(" WHERE "));
    assertEquals(true,
        select3.getSqlString(builder).contains(" > 10"));
  }

  @Test
  public final void testSumMinExprString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addSum(SqlUtils.expression("salary"), "all_salaries");

    assertEquals("SELECT SUM(salary) AS all_salaries FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testSumMinStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addSum("employees", "salary");

    assertEquals("SELECT SUM(employees.salary) AS salary FROM employees",
        select.getSqlString(builder));
  }

  @Test
  public final void testSumMinStringStringString() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();

    SqlSelect select = new SqlSelect();
    select.addFrom("employees");
    select.addSum("employees", "salary", "all_salaries");
    select.setWhere(SqlUtils.notNull("employees", "salary"));

    assertEquals("SELECT SUM(employees.salary) AS all_salaries FROM employees "
        + "WHERE employees.salary IS NOT NULL",
        select.getSqlString(builder));
  }
}
