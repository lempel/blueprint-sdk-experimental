/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.etl;

import java.sql.*;
import java.util.ArrayList;

/**
 * handles database meta data
 *
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
@SuppressWarnings("WeakerAccess")
public class MetaDataHelper {
    protected DatabaseMetaData metaData;

    /**
     * Constructor
     *
     * @param con connection to use
     * @throws SQLException Can't get meta data from connection
     */
    public MetaDataHelper(final Connection con) throws SQLException {
        metaData = con.getMetaData();
    }

    public DatabaseMetaData getMetaData() {
        return metaData;
    }

    /**
     * get all table names
     *
     * @param schemaName target schema name
     * @return table names
     * @throws SQLException Can't access tables
     */
    @SuppressWarnings("SameParameterValue")
    public String[] getTableNames(final String schemaName) throws SQLException {
        ArrayList<String> names = new ArrayList<>();

        ResultSet rset = metaData.getTables(null, schemaName, null, null);
        while (rset.next()) {
            // TODO handle other table types

            // process 'TABLE'
            if ("TABLE".equals(rset.getString("TABLE_TYPE"))) {
                names.add(rset.getString("TABLE_NAME"));
            }
        }

        String[] result = new String[names.size()];
        return names.toArray(result);
    }

    /**
     * get all columns names & types
     *
     * @param schemaName target schema name
     * @param tableName target table name
     * @return column info
     * @throws SQLException Can't access columns
     */
    public ColumnInfo[] getColumns(final String schemaName, final String tableName) throws SQLException {
        ArrayList<ColumnInfo> columns = new ArrayList<>();

        ResultSet rset = metaData.getColumns(null, schemaName, tableName, null);
        int index = 1;
        while (rset.next()) {
            ColumnInfo column = new ColumnInfo();
            column.setIndex(index++);
            column.setName(rset.getString("COLUMN_NAME"));
            column.setType(rset.getInt("DATA_TYPE"));
            column.setNullable("YES".equals(rset.getString("IS_NULLABLE")));

            if (column.getType() == Types.CHAR || column.getType() == Types.VARCHAR) {
                column.setLength(rset.getInt("CHAR_OCTET_LENGTH"));
            } else if (column.getType() == Types.DECIMAL) {
                column.setLength(rset.getInt("COLUMN_SIZE"));
                column.setDigits(rset.getInt("DECIMAL_DIGITS"));
            } else if (column.getType() == Types.FLOAT || column.getType() == Types.DOUBLE) {
                column.setDigits(rset.getInt("DECIMAL_DIGITS"));
                column.setRadix(rset.getInt("NUM_PREC_RADIX"));
            }

            columns.add(column);
        }

        ColumnInfo[] result = new ColumnInfo[columns.size()];
        return columns.toArray(result);
    }

    /**
     * get table info
     *
     * @param schemaName target schema name
     * @param tableName target table name
     * @return table info
     * @throws SQLException Can't access table
     */
    public TableInfo getTableInfo(final String schemaName, final String tableName) throws SQLException {
        TableInfo result = new TableInfo();
        result.setSchemaName(schemaName);
        result.setTableName(tableName);
        result.setColumns(getColumns(schemaName, tableName));

        ArrayList<String> keys = new ArrayList<>();
        ResultSet rset = metaData.getPrimaryKeys(null, schemaName, tableName);
        while (rset.next()) {
            keys.add(rset.getString("COLUMN_NAME"));
        }
        String[] keyNames = new String[keys.size()];
        keys.toArray(keyNames);
        result.setKeys(keyNames);

        return result;
    }
}
