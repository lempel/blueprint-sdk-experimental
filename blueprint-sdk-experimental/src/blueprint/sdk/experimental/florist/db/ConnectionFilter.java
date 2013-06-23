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
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * All JDBC Connections will be closed by this filter<br>
 * Add this Servlet Filter to web.xml<br>
 * 
 * @author Simon Lee
 * @since 2009. 3. 11.
 */
public class ConnectionFilter implements Filter {
	public void destroy() {
		// no-op
	}

	@SuppressWarnings("unchecked")
	public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
			throws IOException, ServletException {
		try {
			// do whatever need to do
			chain.doFilter(req, res);
		} finally {
			// extract ConnectionHelpers and close'em all
			Object attr = req.getAttribute(ConnectionHelper.PARAM_NAME);
			if (attr instanceof List) {
				List<ConnectionHelper> helpers = (List<ConnectionHelper>) req.getAttribute(ConnectionHelper.PARAM_NAME);
				while (!helpers.isEmpty()) {
					ConnectionHelper helper = helpers.remove(0);
					helper.close();
				}
			}
		}
	}

	public void init(final FilterConfig cfg) throws ServletException {
		// no-op
	}
}