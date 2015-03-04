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
 * database type constants
 *
 * @author Sangmin Lee
 * @since 2009. 9. 7.
 */
@SuppressWarnings("WeakerAccess")
public class DatabaseType {
    public static final int UNKNOWN = -1;
    public static final int ORACLE = 0;
    public static final int MYSQL = 1;
    public static final int POSTGRES = 2;
    public static final int MSSQL = 3;
    public static final int DB2 = 4;
}
