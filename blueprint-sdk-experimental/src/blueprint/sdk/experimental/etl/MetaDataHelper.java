/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.etl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

/**
 * handles database meta data
 * 
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
public class MetaDataHelper {
	protected DatabaseMetaData metaData;

	/**
	 * Constructor
	 * 
	 * @param con
	 * @throws SQLException
	 *             Can't get meta data from connection
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
	 * @param schemaName
	 * @return
	 * @throws SQLException
	 *             Can't access tables
	 */
	public String[] getTableNames(final String schemaName) throws SQLException {
		ArrayList<String> names = new ArrayList<String>();

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
	 * @param schemaName
	 * @param tableName
	 * @return
	 * @throws SQLException
	 *             Can't access columns
	 */
	public ColumnInfo[] getColumns(final String schemaName, final String tableName) throws SQLException {
		ArrayList<ColumnInfo> columns = new ArrayList<ColumnInfo>();

		ResultSet rset = metaData.getColumns(null, schemaName, tableName, null);
		int index = 1;
		while (rset.next()) {
			ColumnInfo column = new ColumnInfo();
			column.setIndex(index++);
			column.setName(rset.getString("COLUMN_NAME"));
			column.setType(rset.getInt("DATA_TYPE"));
			column.setNullable("YES".equals(rset.getString("IS_NULLABLE")) ? true : false);

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
	 * @param schemaName
	 * @param tableName
	 * @return
	 * @throws SQLException
	 *             Can't access table
	 */
	public TableInfo getTableInfo(final String schemaName, final String tableName) throws SQLException {
		TableInfo result = new TableInfo();
		result.setSchemaName(schemaName);
		result.setTableName(tableName);
		result.setColumns(getColumns(schemaName, tableName));

		ArrayList<String> keys = new ArrayList<String>();
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
