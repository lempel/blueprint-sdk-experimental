/*
 * Copied from blueprint-sdk (http://code.google.com/p/blueprint-sdk/).
 * Because of blueprint-sdk requires JDK 1.5 or higher.
 * Copyright 2009 Simon Lee, all rights reserved.
 */
/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 International - http://code.google.com/p/blueprint-sdk
 South Korea - http://lempel.egloos.com


 Background:

 blueprint-sdk is a java software development kit to protect other open source
 softwares' licenses. It's intended to provide light weight APIs for blueprints.
 Well... at least trying to.

 There are so many great open source projects now. Back in year 2000, there
 were not much to use. Even JDBC drivers were rare back then. Naturally, I have
 to implement many things by myself. Especially dynamic class loading, networking,
 scripting, logging and database interactions. It was time consuming. Now I can
 take my picks from open source projects.

 But I still need my own APIs. Most of my clients just don't understand open
 source licenses. They always want to have their own versions of open source
 projects but don't want to publish derivative works. They shouldn't use open
 source projects in the first place. So I need to have my own open source project
 to be free from derivation terms and also as a mediator between other open
 source projects and my client's requirements.

 Primary purpose of blueprint-sdk is not to violate other open source project's
 license terms.


 To commiters:

 License terms of the other software used by your source code should not be
 violated by using your source code. That's why blueprint-sdk is made for.
 Without that, all your contributions are welcomed and appreciated.
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
 * @version $Revision$
 * @since 2009. 3. 12.
 * @last $Date$
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