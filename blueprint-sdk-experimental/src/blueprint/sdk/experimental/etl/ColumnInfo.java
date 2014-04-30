/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.etl;

import java.sql.Types;

/**
 * Table column info
 * 
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
public class ColumnInfo {
	private int index = -1;
	private String name = null;
	private int type = -1;
	private int length = -1;
	private int digits = -1;
	private int radix = 10;
	private boolean nullable = true;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getDigits() {
		return digits;
	}

	public void setDigits(int digits) {
		this.digits = digits;
	}

	public int getRadix() {
		return radix;
	}

	public void setRadix(int radix) {
		this.radix = radix;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	/**
	 * returns actual type name (for DDL)
	 * 
	 * @param databaseType
	 * @param type
	 * @return
	 */
	public static String getTypeName(int databaseType, int type) {
		String result = "unknown";

		// TODO expand type support
		switch (type) {
		case Types.BIGINT:
			result = "BIGINT";
			break;
		case Types.BINARY:
			result = "BINARY";
			break;
		case Types.BOOLEAN:
			result = "BOOLEAN";
			break;
		case Types.CHAR:
			result = "CHARACTER";
			break;
		case Types.CLOB:
			result = "CLOB";
			break;
		case Types.DATE:
			result = "DATE";
			break;
		case Types.DECIMAL:
			result = "DECIMAL";
			break;
		case Types.DOUBLE:
			result = "DOUBLE";
			break;
		case Types.FLOAT:
			result = "FLOAT";
			break;
		case Types.INTEGER:
			switch (databaseType) {
			case DatabaseType.DB2:
				result = "INTEGER";
				break;
			case DatabaseType.MSSQL:
			default:
				result = "INT";
				break;
			}
			break;
		case Types.NUMERIC:
			result = "NUMERIC";
			break;
		case Types.TIME:
			result = "TIME";
			break;
		case Types.TIMESTAMP:
			result = "TIMESTAMP";
			break;
		case Types.VARCHAR:
			result = "VARCHAR";
			break;
		}

		return result;
	}
}
