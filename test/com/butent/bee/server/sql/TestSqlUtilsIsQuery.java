package com.butent.bee.server.sql;

import com.google.common.collect.Lists;

import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.server.sql.SqlUtils}.
 */
@SuppressWarnings("static-method")
public class TestSqlUtilsIsQuery {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCreateForeignKey() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals("ALTER TABLE Table1 ADD CONSTRAINT name FOREIGN KEY (field1) "
        + "REFERENCES refTable (refField1) ON DELETE CASCADE",
        SqlUtils.createForeignKey("Table1", "name", Lists.newArrayList("field1"),
            "refTable", Lists.newArrayList("refField1"), SqlKeyword.DELETE).getQuery());

    assertEquals("ALTER TABLE Table1 ADD CONSTRAINT name FOREIGN KEY (field1) "
        + "REFERENCES refTable (refField1) ON DELETE SET NULL",
        SqlUtils.createForeignKey("Table1", "name", Lists.newArrayList("field1"),
            "refTable", Lists.newArrayList("refField1"), SqlKeyword.SET_NULL).getQuery());

    assertEquals("ALTER TABLE Table1 ADD CONSTRAINT name FOREIGN KEY (field1, field2) "
        + "REFERENCES refTable (refField1, refField2)",
        SqlUtils.createForeignKey("Table1", "name", Lists.newArrayList("field1", "field2"),
            "refTable", Lists.newArrayList("refField1", "refField2"), null).getQuery());
  }

  @Test
  public final void testCreateIndex() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    assertEquals("CREATE INDEX indexName ON Table1 (field1, field2)",
        SqlUtils.createIndex("Table1", "indexName", Lists.newArrayList("field1", "field2"), false)
            .getQuery());
    assertEquals("CREATE UNIQUE INDEX indexName ON Table1 (field1)", SqlUtils
        .createIndex("Table1", "indexName", Lists.newArrayList("field1"), true).getQuery());
  }

  @Test
  public final void testCreatePrimaryKey() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals(
        "ALTER TABLE Table1 ADD CONSTRAINT constraint_name PRIMARY KEY (User_ID, Username)",
        SqlUtils.createPrimaryKey("Table1", "constraint_name",
            Lists.newArrayList("User_ID", "Username")).getQuery());
    assertEquals(
        "ALTER TABLE Table1 ADD CONSTRAINT constraint_name UNIQUE (field)",
        SqlUtils.createUniqueKey("Table1", "constraint_name", Lists.newArrayList("field"))
            .getQuery());
  }

  @Test
  public final void testDBForeignKeys() {

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);

    assertEquals("SELECT t.table_name AS tblName, c.constraint_name AS keyName, r.table_name "
        + "AS fkRefTable FROM information_schema.referential_constraints c "
        + "INNER JOIN information_schema.table_constraints t "
        + "ON c.constraint_name = t.constraint_name "
        + "INNER JOIN information_schema.table_constraints r "
        + "ON c.unique_constraint_name = r.constraint_name WHERE c.constraint_catalog = 'MyDB' "
        + "AND t.table_catalog = 'MyDB' AND c.constraint_schema = 'MyDbSchema' "
        + "AND t.table_schema = 'MyDbSchema' AND t.table_name = 'Table1' "
        + "AND r.table_name = 'RefTable1'",
        SqlUtils.dbForeignKeys("MyDB", "MyDbSchema", "Table1", "RefTable1").getQuery());

    assertEquals("SELECT t.table_name AS tblName, c.constraint_name AS keyName, r.table_name "
        + "AS fkRefTable FROM information_schema.referential_constraints c "
        + "INNER JOIN information_schema.table_constraints t "
        + "ON c.constraint_name = t.constraint_name "
        + "INNER JOIN information_schema.table_constraints r "
        + "ON c.unique_constraint_name = r.constraint_name WHERE c.constraint_catalog = 'MyDB' "
        + "AND t.table_catalog = 'MyDB' AND c.constraint_schema = 'MyDbSchema' "
        + "AND t.table_schema = 'MyDbSchema' AND t.table_name = 'Table1'",
        SqlUtils.dbForeignKeys("MyDB", "MyDbSchema", "Table1", null).getQuery());

    assertEquals("SELECT t.table_name AS tblName, c.constraint_name AS keyName, r.table_name "
        + "AS fkRefTable FROM information_schema.referential_constraints c "
        + "INNER JOIN information_schema.table_constraints t "
        + "ON c.constraint_name = t.constraint_name "
        + "INNER JOIN information_schema.table_constraints r "
        + "ON c.unique_constraint_name = r.constraint_name WHERE c.constraint_catalog = 'MyDB' "
        + "AND t.table_catalog = 'MyDB' AND c.constraint_schema = 'MyDbSchema' "
        + "AND t.table_schema = 'MyDbSchema'",
        SqlUtils.dbForeignKeys("MyDB", "MyDbSchema", null, null).getQuery());

    assertEquals("SELECT t.table_name AS tblName, c.constraint_name AS keyName, r.table_name "
        + "AS fkRefTable FROM information_schema.referential_constraints c "
        + "INNER JOIN information_schema.table_constraints t "
        + "ON c.constraint_name = t.constraint_name "
        + "INNER JOIN information_schema.table_constraints r "
        + "ON c.unique_constraint_name = r.constraint_name WHERE c.constraint_catalog = 'MyDB' "
        + "AND t.table_catalog = 'MyDB'",
        SqlUtils.dbForeignKeys("MyDB", null, null, null).getQuery());

    assertEquals("SELECT t.table_name AS tblName, c.constraint_name AS keyName, r.table_name "
        + "AS fkRefTable FROM information_schema.referential_constraints c "
        + "INNER JOIN information_schema.table_constraints t "
        + "ON c.constraint_name = t.constraint_name "
        + "INNER JOIN information_schema.table_constraints r "
        + "ON c.unique_constraint_name = r.constraint_name",
        SqlUtils.dbForeignKeys(null, null, null, null).getQuery());

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);

    assertEquals("SELECT \"c\".\"TABLE_NAME\" AS \"tblName\", \"c\".\"CONSTRAINT_NAME\" "
        + "AS \"keyName\", \"r\".\"TABLE_NAME\" AS \"fkRefTable\" FROM \"ALL_CONSTRAINTS\" \"c\" "
        + "INNER JOIN \"ALL_CONSTRAINTS\" \"r\" "
        + "ON \"c\".\"R_CONSTRAINT_NAME\" = \"r\".\"CONSTRAINT_NAME\" "
        + "WHERE \"c\".\"CONSTRAINT_TYPE\" = 'R' AND \"c\".\"OWNER\" = 'MyDbSchema' "
        + "AND \"r\".\"OWNER\" = 'MyDbSchema' AND \"c\".\"TABLE_NAME\" = 'Table1' "
        + "AND \"r\".\"TABLE_NAME\" = 'RefTable1'",
        SqlUtils.dbForeignKeys("MyDB", "MyDbSchema", "Table1", "RefTable1").getQuery());

    assertEquals("SELECT \"c\".\"TABLE_NAME\" AS \"tblName\", \"c\".\"CONSTRAINT_NAME\" "
        + "AS \"keyName\", \"r\".\"TABLE_NAME\" AS \"fkRefTable\" FROM \"ALL_CONSTRAINTS\" \"c\" "
        + "INNER JOIN \"ALL_CONSTRAINTS\" \"r\" "
        + "ON \"c\".\"R_CONSTRAINT_NAME\" = \"r\".\"CONSTRAINT_NAME\" "
        + "WHERE \"c\".\"CONSTRAINT_TYPE\" = 'R' AND \"c\".\"OWNER\" = 'MyDbSchema' "
        + "AND \"r\".\"OWNER\" = 'MyDbSchema' AND \"c\".\"TABLE_NAME\" = 'Table1'",
        SqlUtils.dbForeignKeys("MyDB", "MyDbSchema", "Table1", null).getQuery());

    assertEquals("SELECT \"c\".\"TABLE_NAME\" AS \"tblName\", \"c\".\"CONSTRAINT_NAME\" "
        + "AS \"keyName\", \"r\".\"TABLE_NAME\" AS \"fkRefTable\" FROM \"ALL_CONSTRAINTS\" \"c\" "
        + "INNER JOIN \"ALL_CONSTRAINTS\" \"r\" "
        + "ON \"c\".\"R_CONSTRAINT_NAME\" = \"r\".\"CONSTRAINT_NAME\" "
        + "WHERE \"c\".\"CONSTRAINT_TYPE\" = 'R' AND \"c\".\"OWNER\" = 'MyDbSchema' "
        + "AND \"r\".\"OWNER\" = 'MyDbSchema'",
        SqlUtils.dbForeignKeys("MyDB", "MyDbSchema", null, null).getQuery());

    assertEquals("SELECT \"c\".\"TABLE_NAME\" AS \"tblName\", \"c\".\"CONSTRAINT_NAME\" "
        + "AS \"keyName\", \"r\".\"TABLE_NAME\" AS \"fkRefTable\" FROM \"ALL_CONSTRAINTS\" \"c\" "
        + "INNER JOIN \"ALL_CONSTRAINTS\" \"r\" "
        + "ON \"c\".\"R_CONSTRAINT_NAME\" = \"r\".\"CONSTRAINT_NAME\" "
        + "WHERE \"c\".\"CONSTRAINT_TYPE\" = 'R'",
        SqlUtils.dbForeignKeys("MyDB", null, null, null).getQuery());

    assertEquals("SELECT \"c\".\"TABLE_NAME\" AS \"tblName\", \"c\".\"CONSTRAINT_NAME\" "
        + "AS \"keyName\", \"r\".\"TABLE_NAME\" AS \"fkRefTable\" FROM \"ALL_CONSTRAINTS\" \"c\" "
        + "INNER JOIN \"ALL_CONSTRAINTS\" \"r\" "
        + "ON \"c\".\"R_CONSTRAINT_NAME\" = \"r\".\"CONSTRAINT_NAME\" "
        + "WHERE \"c\".\"CONSTRAINT_TYPE\" = 'R'",
        SqlUtils.dbForeignKeys(null, null, null, null).getQuery());
  }

  @Test
  public final void testDbName() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals("", SqlUtils.dbName().getQuery());
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    assertEquals("SELECT current_database() as \"dbName\"", SqlUtils.dbName().getQuery());
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    assertEquals("SELECT db_name() AS [dbName]", SqlUtils.dbName().getQuery());
  }

  @Test
  public final void testDbSchema() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals("", SqlUtils.dbSchema().getQuery());
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    assertEquals("SELECT current_schema() as \"dbSchema\"", SqlUtils.dbSchema().getQuery());
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    assertEquals("SELECT schema_name() AS [dbSchema]", SqlUtils.dbSchema().getQuery());

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    assertEquals("SELECT sys_context('USERENV', 'CURRENT_SCHEMA') AS \"dbSchema\" FROM dual",
        SqlUtils.dbSchema().getQuery());
  }

  @Test
  public final void testDbTables() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals("SELECT t.table_name AS tblName, t.table_rows AS rowCount "
        + "FROM information_schema.tables t WHERE t.table_catalog = 'MyDB' "
        + "AND t.table_schema = 'MyDBSchema' AND t.table_name = 'table1'",
        SqlUtils.dbTables("MyDB", "MyDBSchema", "table1").getQuery());

    assertEquals("SELECT t.table_name AS tblName, t.table_rows AS rowCount "
        + "FROM information_schema.tables t WHERE t.table_catalog = 'MyDB' "
        + "AND t.table_schema = 'MyDBSchema'",
        SqlUtils.dbTables("MyDB", "MyDBSchema", null).getQuery());

    assertEquals("SELECT t.table_name AS tblName, t.table_rows AS rowCount "
        + "FROM information_schema.tables t WHERE t.table_catalog = 'MyDB'",
        SqlUtils.dbTables("MyDB", null, null).getQuery());

    assertEquals("SELECT t.table_name AS tblName, t.table_rows AS rowCount "
        + "FROM information_schema.tables t",
        SqlUtils.dbTables(null, null, null).getQuery());

    // tarp PostgreSqlBuilder DB_TABLES
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.POSTGRESQL);
    assertEquals(
        "SELECT \"t\".\"relname\" AS \"tblName\", \"t\".\"reltuples\" AS \"rowCount\" FROM"
            + " \"pg_class\" \"t\" INNER JOIN \"pg_namespace\" \"s\" ON \"t\".\"relnamespace\" = "
            + "\"s\".\"oid\" WHERE \"s\".\"nspname\" = \'MyDBSchema\' AND "
            + "\"t\".\"relname\" = \'table1\'",
        SqlUtils.dbTables("MyDB", "MyDBSchema", "table1").getQuery());

    assertEquals(
        "SELECT \"t\".\"relname\" AS \"tblName\", \"t\".\"reltuples\" AS \"rowCount\" FROM"
            + " \"pg_class\" \"t\" INNER JOIN \"pg_namespace\" \"s\" ON \"t\".\"relnamespace\" = "
            + "\"s\".\"oid\" WHERE \"s\".\"nspname\" = \'MyDBSchema\'",
        SqlUtils.dbTables("MyDB", "MyDBSchema", null).getQuery());

    assertEquals(
        "SELECT \"t\".\"relname\" AS \"tblName\", \"t\".\"reltuples\" AS \"rowCount\" FROM"
            + " \"pg_class\" \"t\" INNER JOIN \"pg_namespace\" \"s\" ON \"t\".\"relnamespace\" = "
            + "\"s\".\"oid\" WHERE 1 = 0",
        SqlUtils.dbTables("MyDB", null, null).getQuery());

    assertEquals(
        "SELECT \"t\".\"relname\" AS \"tblName\", \"t\".\"reltuples\" AS \"rowCount\" FROM"
            + " \"pg_class\" \"t\" INNER JOIN \"pg_namespace\" \"s\" ON \"t\".\"relnamespace\" = "
            + "\"s\".\"oid\" WHERE 1 = 0",
        SqlUtils.dbTables(null, null, null).getQuery());

    SqlBuilderFactory.setDefaultBuilder(SqlEngine.ORACLE);
    assertEquals("SELECT \"t\".\"TABLE_NAME\" AS \"tblName\", \"t\".\"NUM_ROWS\" AS "
        + "\"rowCount\" FROM \"ALL_TABLES\" \"t\" WHERE \"t\".\"OWNER\" = 'MyDBSchema' "
        + "AND \"t\".\"TABLE_NAME\" = 'table1'",
        SqlUtils.dbTables("MyDB", "MyDBSchema", "table1").getQuery());

    assertEquals("SELECT \"t\".\"TABLE_NAME\" AS \"tblName\", \"t\".\"NUM_ROWS\" AS \"rowCount\" "
        + "FROM \"ALL_TABLES\" \"t\" WHERE \"t\".\"OWNER\" = 'MyDBSchema'",
        SqlUtils.dbTables("MyDB", "MyDBSchema", null).getQuery());

    assertEquals("SELECT \"t\".\"TABLE_NAME\" AS \"tblName\", \"t\".\"NUM_ROWS\" AS \"rowCount\" "
        + "FROM \"ALL_TABLES\" \"t\"",
        SqlUtils.dbTables("MyDB", null, null).getQuery());

    assertEquals("SELECT \"t\".\"TABLE_NAME\" AS \"tblName\", \"t\".\"NUM_ROWS\" AS \"rowCount\" "
        + "FROM \"ALL_TABLES\" \"t\"",
        SqlUtils.dbTables(null, null, null).getQuery());
  }

  @Test
  public final void testDropForeignKey() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.GENERIC);
    assertEquals("ALTER TABLE Table1 DROP CONSTRAINT foreignkey_name",
        SqlUtils.dropForeignKey("Table1", "foreignkey_name").getQuery());

    // try {
    // assertEquals("ALTER TABLE Table1 DROP FOREIGN KEY", SqlUtils
    // .dropForeignKey("Table1", null).getQuery());
    // assertEquals("ALTER TABLE Table1 DROP FOREIGN KEY", SqlUtils
    // .dropForeignKey(null, null).getQuery());
    // fail("BeeRuntimeException not works");
    // } catch (BeeRuntimeException e) {
    // assertTrue(true);
    // System.out.println("public final void testCreateIndex():"
    // + e.getMessage());
    // } catch (Exception e) {
    // fail("Java runtime error. Need BeeRuntimeException !!!");
    // }
  }

  @Test
  public void testTemporaryName() {
    SqlBuilderFactory.setDefaultBuilder(SqlEngine.MSSQL);
    SqlBuilder builder = SqlBuilderFactory.getBuilder();
    SqlSelect select = new SqlSelect();
    select.setWhere(SqlUtils.contains(SqlUtils.constant(SqlUtils.temporaryName("temp value")),
        "5"));
    select.addFrom("Source_table");
    select.addFields("Source_table", "name");
    assertEquals("SELECT [Source_table].[name] FROM [Source_table] WHERE '#temp value' "
        + "LIKE '%5%' ESCAPE '|'",
        select.getSqlString(builder));
  }
}
