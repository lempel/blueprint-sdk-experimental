/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.florist.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;


import org.apache.commons.dbcp.BasicDataSource;

import blueprint.sdk.logger.Logger;

/**
 * Create/Destroy JDBC Connection Pools<br>
 * Currently supports jakarta commons DBCP only<br>
 * Even though general JNDI DataSources can be used with blueprint.florist<br>
 * Add this ServletContextListener to web.xml<br>
 * 
 * @author Simon Lee
 * @since 2009. 3. 12.
 */
public class ConnectionListener implements ServletContextListener {
	public static final Logger LOGGER = Logger.getInstance();

	public Map<String, DataSource> boundDsrs = new HashMap<String, DataSource>();

	public void contextDestroyed(final ServletContextEvent arg0) {
		try {
			Context initContext = new InitialContext();

			Iterator<String> iter = boundDsrs.keySet().iterator();
			while (iter.hasNext()) {
				try {
					String key = iter.next();
					DataSource dsr = (DataSource) boundDsrs.get(key);
					initContext.unbind(key);
					// Close all BasicDataSources created by this class.
					// No need to close JNDI DataSources. It's not this class's
					// responsibility.
					if (dsr instanceof BasicDataSource) {
						((BasicDataSource) dsr).close();
					}
				} catch (SQLException e) {
					LOGGER.trace(e);
				}
			}

			boundDsrs.clear();
		} catch (NamingException e) {
			LOGGER.trace(e);
		}
	}

	public void contextInitialized(final ServletContextEvent arg0) {
		Properties poolProp = new Properties();
		try {
			Context initContext = new InitialContext();

			// load pool properties file (from class path)
			poolProp.load(ConnectionListener.class.getResourceAsStream("/jdbc_pools.properties"));
			StringTokenizer stk = new StringTokenizer(poolProp.getProperty("PROP_LIST"));

			// process all properties files list in pool properties (from class
			// path)
			while (stk.hasMoreTokens()) {
				try {
					String propName = stk.nextToken();
					LOGGER.info(this, "loading jdbc properties - " + propName);

					Properties prop = new Properties();
					prop.load(ConnectionListener.class.getResourceAsStream(propName));

					DataSource dsr;
					if (prop.containsKey("JNDI_NAME")) {
						// lookup DataSource from JNDI
						// FIXME JNDI support is not tested yet. SPI or Factory
						// is needed here.
						LOGGER.warning(this, "JNDI DataSource support needs more hands on!");
						dsr = (DataSource) initContext.lookup(prop.getProperty("JNDI_NAME"));
					} else {
						// create new BasicDataSource
						BasicDataSource bds = new BasicDataSource();
						bds.setMaxActive(Integer.parseInt(prop.getProperty("MAX_ACTIVE")));
						bds.setMaxIdle(Integer.parseInt(prop.getProperty("MAX_IDLE")));
						bds.setMaxWait(Integer.parseInt(prop.getProperty("MAX_WAIT")));
						bds.setInitialSize(Integer.parseInt(prop.getProperty("INITIAL")));
						bds.setDriverClassName(prop.getProperty("CLASS_NAME"));
						bds.setUrl(prop.getProperty("URL"));
						bds.setUsername(prop.getProperty("USER"));
						bds.setPassword(prop.getProperty("PASSWORD"));
						bds.setValidationQuery(prop.getProperty("VALIDATION"));
						dsr = bds;
					}
					boundDsrs.put(prop.getProperty("POOL_NAME"), dsr);
					initContext.bind(prop.getProperty("POOL_NAME"), dsr);
				} catch (RuntimeException e) {
					LOGGER.trace(e);
				}
			}
		} catch (IOException e) {
			LOGGER.trace(e);
		} catch (NamingException e) {
			LOGGER.trace(e);
		}
	}
}