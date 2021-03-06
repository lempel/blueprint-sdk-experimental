/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.florist.db;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Connection Helper for automated connection close<br>
 * <br>
 * Usage:<br>
 * 1. JSP<br>
 * ConnectionHelper helper = new ConnectionHelper(request);<br>
 * <br>
 * 2. Servlet<bR>
 * public void doGet(HttpServletRequest request, HttpServletResponse response)<br>
 * throws ServletException, IOException {<br>
 * ConnectionHelper helper = new ConnectionHelper(request);<br>
 * // do what you have to do<br>
 * }<br>
 *
 * @author Sangmin Lee
 * @since 2009. 3. 11.
 */
@SuppressWarnings("WeakerAccess")
public class ConnectionHelper {
    /**
     * key for ConnectionHelper's List
     */
    public static final String PARAM_NAME = ConnectionHelper.class.getName();

    protected final List<Connection> connections = new ArrayList<>(100);

    @SuppressWarnings("unchecked")
    public ConnectionHelper(final HttpServletRequest req) {
        Object attr = req.getAttribute(PARAM_NAME);
        if (!(attr instanceof List)) {
            req.setAttribute(PARAM_NAME, new ArrayList<ConnectionHelper>(10));
        }
        List<ConnectionHelper> helpers = (List<ConnectionHelper>) req.getAttribute(PARAM_NAME);
        helpers.add(this);
    }

    protected static DataSource getDataSource(final String dbmsId) throws SQLException {
        DataSource src;
        try {
            Context initContext = new InitialContext();
            src = (DataSource) initContext.lookup(dbmsId);
        } catch (NamingException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
        return src;
    }

    public Connection getConnection(final String dbmsId) throws SQLException {
        Connection result;

        DataSource bds = getDataSource(dbmsId);
        result = bds.getConnection();
        connections.add(result);

        return result;
    }

    public void close() {
        while (!connections.isEmpty()) {
            Connection con = connections.remove(0);
            try {
                con.close();
            } catch (SQLException ignored) {
            }
        }
    }

    protected void finalize() throws Throwable {
        close();

        super.finalize();
    }
}
