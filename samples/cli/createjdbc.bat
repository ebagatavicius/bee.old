set AS_ADMIN=c:\glassfish4\glassfish\bin\asadmin.bat

call %AS_ADMIN% create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource --restype javax.sql.DataSource --property serverName=localhost:user=postgres:password=butent:databaseName=bee PgPool
call %AS_ADMIN% create-jdbc-resource --connectionpoolid PgPool jdbc/BeeDS
call %AS_ADMIN% ping-connection-pool PgPool