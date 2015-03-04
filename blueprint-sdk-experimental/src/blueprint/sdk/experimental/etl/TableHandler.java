/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.etl;

import blueprint.sdk.logger.LogLevel;
import blueprint.sdk.logger.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;


/**
 * table handler
 *
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
@SuppressWarnings("WeakerAccess")
public class TableHandler {
    protected static final Logger LOGGER = Logger.getInstance();

    protected Statement stmt;

    public TableHandler(final Connection con) throws SQLException {
        stmt = con.createStatement();
    }

    /**
     * @param databaseType (see DatabaseType)
     * @param schemaName target schema name
     * @param tableName target table name
     * @return true: dropped
     * @throws SQLException
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean dropTable(final int databaseType, final String schemaName, final String tableName)
            throws SQLException {
        String sql;

        // TODO support more database types
        switch (databaseType) {
            default:
                if (schemaName != null && schemaName.length() > 0) {
                    sql = "drop table " + schemaName + ".\"" + tableName + '\"';
                } else {
                    sql = "drop table \"" + tableName + '\"';
                }
                break;
        }

        return stmt.execute(sql);
    }

    /**
     * @param databaseType (see DatabaseType)
     * @param table table name to create
     * @return true: created
     * @throws SQLException
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean createTable(final int databaseType, final TableInfo table) throws SQLException {
        StringBuilder builder = new StringBuilder(1024);

        // TODO support more database types
        switch (databaseType) {
            default:
                builder.append("create table \"");
                if (table.getSchemaName() != null && table.getSchemaName().length() > 0) {
                    builder.append(table.getSchemaName()).append('.');
                }
                builder.append(table.getTableName()).append('\"');

                builder.append(" (\n");
                for (int c = 0; c < table.getColumns().length; c++) {
                    ColumnInfo column = table.getColumns()[c];
                    if (c > 0) {
                        builder.append(",\n");
                    }
                    builder.append('\"');
                    builder.append(column.getName());
                    builder.append("\" ");
                    builder.append(ColumnInfo.getTypeName(databaseType, column.getType()));
                    if (column.getType() == Types.CHAR || column.getType() == Types.VARCHAR) {
                        builder.append('(');
                        builder.append(column.getLength());
                        builder.append(')');
                    } else if (column.getType() == Types.DECIMAL) {
                        builder.append('(');
                        builder.append(column.getLength());
                        builder.append(',');
                        builder.append(column.getDigits());
                        builder.append(')');
                    } else if (column.getType() == Types.FLOAT || column.getType() == Types.DOUBLE) {
                        builder.append('(');
                        builder.append(column.getDigits());
                        builder.append(',');
                        builder.append(column.getRadix());
                        builder.append(')');
                    }
                    if (!column.isNullable()) {
                        builder.append(" NOT NULL");
                    }
                }

                if (table.getKeys() != null && table.getKeys().length > 0) {
                    builder.append("primary key (");

                    String[] keys = table.getKeys();
                    for (int i = 0; i < keys.length - 1; i++) {
                        builder.append('\"');
                        builder.append(keys[i]).append("\", ");
                    }
                    builder.append('\"');
                    builder.append(keys[keys.length - 1]);
                    builder.append('\"');

                    builder.append(")\n");
                }

                builder.append("\n)");
                break;
        }

        try {
            return stmt.execute(builder.toString());
        } catch (SQLException e) {
            LOGGER.println(LogLevel.SQL, "Can't create table - " + table.getTableName());
            LOGGER.println(LogLevel.SQL, builder.toString());
            throw e;
        }
    }
}
