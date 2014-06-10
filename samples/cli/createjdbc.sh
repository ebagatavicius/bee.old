#!/bin/sh

AS_ADMIN=asadmin
# AS_ADMIN=/opt/glassfish4/glassfish/bin/asadmin

$AS_ADMIN create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource --restype javax.sql.DataSource --property serverName=192.168.0.240:user=postgres:password=admin:databaseName=bee PgPool
$AS_ADMIN create-jdbc-resource --connectionpoolid PgPool jdbc/BeeDS
$AS_ADMIN ping-connection-pool PgPool