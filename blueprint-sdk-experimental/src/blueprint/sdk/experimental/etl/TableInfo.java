/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.etl;


/**
 * Table info
 * 
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
public class TableInfo {
	private String schemaName;
	private String tableName;
	private ColumnInfo[] columns;
	private String[] keys;

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	public ColumnInfo[] getColumns() {
		return columns;
	}

	public void setColumns(final ColumnInfo[] columns) {
		this.columns = columns;
	}

	public String[] getKeys() {
		return keys;
	}

	public void setKeys(final String[] keys) {
		this.keys = keys;
	}
}
