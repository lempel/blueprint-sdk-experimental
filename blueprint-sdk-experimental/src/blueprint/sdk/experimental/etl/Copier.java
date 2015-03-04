/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.etl;

import blueprint.sdk.logger.Logger;

import java.sql.*;


/**
 * Copy table/index/sequence from source to target
 *
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
@SuppressWarnings("WeakerAccess")
public class Copier {
    private static final Logger LOGGER = Logger.getInstance();

    private final Connection srcCon;
    private final Connection targetCon;
    private final boolean createTable;
    private final boolean dropTable;

    /**
     * Constructor
     *
     * @param srcCon    source connection
     * @param targetCon target connection
     * @param create    set to create new object
     * @param drop      set to drop existing object
     */
    @SuppressWarnings("SameParameterValue")
    public Copier(final Connection srcCon, final Connection targetCon, boolean create, boolean drop) {
        this.srcCon = srcCon;
        this.targetCon = targetCon;
        this.createTable = create;
        this.dropTable = drop;
    }

    public Connection getSrcCon() {
        return srcCon;
    }

    public Connection getTargetCon() {
        return targetCon;
    }

    public boolean isCreateTable() {
        return createTable;
    }

    public boolean isDropTable() {
        return dropTable;
    }

    /**
     * copy tables
     *
     * @param databaseType     (see DatabaseType)
     * @param srcSchemaName source schema name
     * @param targetSchemaName target schema name
     * @param tableNames target tables
     * @throws SQLException many things can go wrong...
     */
    @SuppressWarnings("SameParameterValue")
    public void copyTables(final int databaseType, final String srcSchemaName, final String targetSchemaName,
                           final String... tableNames) throws SQLException {
        MetaDataHelper meta = new MetaDataHelper(getSrcCon());
        TableHandler targetHandler = new TableHandler(getTargetCon());

        // TODO implement Index, Sequence copy

        Statement srcStmt = getSrcCon().createStatement();

        for (String tableName : tableNames) {
            TableInfo tableInfo = meta.getTableInfo(srcSchemaName, tableName);
            ColumnInfo[] columns = tableInfo.getColumns();

            int columnCount = columns.length;
            if (columnCount > 0) {
                StringBuilder builder = new StringBuilder(1024);
                builder.append("insert into \"");
                builder.append(tableName);
                builder.append("\" values (");
                for (int i = 0; i < columnCount - 1; i++) {
                    builder.append("?,");
                }
                builder.append("?)");

                PreparedStatement targetStmt = getTargetCon().prepareStatement(builder.toString());

                if (isDropTable()) {
                    try {
                        targetHandler.dropTable(databaseType, targetSchemaName, tableName);
                    } catch (SQLException ignored) {
                        // table may not exists
                    }
                }
                if (isCreateTable()) {
                    targetHandler.createTable(databaseType, tableInfo);
                }

                ResultSet rset;
                if (srcSchemaName != null && srcSchemaName.length() > 0) {
                    rset = srcStmt.executeQuery("select * from " + srcSchemaName + ".\"" + tableName + "\"");
                } else {
                    rset = srcStmt.executeQuery("select * from \"" + tableName + "\"");
                }

                int rowCount = 0;
                while (rset.next()) {
                    for (ColumnInfo column : columns) {
                        int index = column.getIndex();
                        String name = column.getName();
                        int type = column.getType();

                        // TODO support more types
                        switch (type) {
                            case Types.DATE:
                                targetStmt.setDate(index, rset.getDate(name));
                                break;
                            case Types.DECIMAL:
                                targetStmt.setBigDecimal(index, rset.getBigDecimal(name));
                                break;
                            case Types.NUMERIC:
                            case Types.INTEGER:
                                targetStmt.setInt(index, rset.getInt(name));
                                break;
                            case Types.FLOAT:
                                targetStmt.setFloat(index, rset.getFloat(name));
                                break;
                            case Types.DOUBLE:
                                targetStmt.setDouble(index, rset.getDouble(name));
                                break;
                            case Types.TIME:
                                targetStmt.setTime(index, rset.getTime(name));
                                break;
                            case Types.TIMESTAMP:
                                targetStmt.setTimestamp(index, rset.getTimestamp(name));
                                break;
                            case Types.BLOB:
                                targetStmt.setBlob(index, rset.getBlob(name));
                                break;
                            case Types.CLOB:
                                targetStmt.setClob(index, rset.getClob(name));
                                break;
                            default:
                                targetStmt.setString(index, rset.getString(name));
                                break;
                        }
                    }

                    // TODO do more sophisticated exception handling
                    targetStmt.executeUpdate();

                    rowCount++;
                    if (rowCount % 100 == 0) {
                        getTargetCon().commit();
                    }
                }
            }

            LOGGER.info("table \"" + tableName + "\" copied from \"" + srcSchemaName + "\" to \"" + targetSchemaName
                    + "\"");
            getTargetCon().commit();
        }
    }
}
