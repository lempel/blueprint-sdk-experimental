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
import java.sql.DriverManager;

public class Test {
    public static void main(String[] args) throws Exception {
        // source database
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        Connection srcCon = DriverManager.getConnection("jdbc:db2://192.168.0.1:50000/DB1", "account", "password");

        // target database
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        Connection targetCon = DriverManager.getConnection("jdbc:db2://192.168.0.1:50000/DB2", "account", "password");
        targetCon.setAutoCommit(false);

        // copy all tables
        MetaDataHelper meta = new MetaDataHelper(srcCon);
        String[] tableNames = meta.getTableNames("DB1");
        Copier copier = new Copier(srcCon, targetCon, true, true);
        for (String tableName : tableNames) {
            copier.copyTables(DatabaseType.DB2, "DB1", "DB2", tableName);
        }

        // commit
        targetCon.commit();
    }
}