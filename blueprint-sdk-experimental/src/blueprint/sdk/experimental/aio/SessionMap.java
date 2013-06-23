/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio;

import java.util.concurrent.ConcurrentHashMap;

import blueprint.sdk.experimental.aio.session.Session;


/**
 * Map of Sessions (to SocketChannel's hashcode)<br>
 * No specific purpose. Just for marking.<br>
 * 
 * @author Simon Lee
 * @since 2008. 11. 28.
 */
public class SessionMap extends ConcurrentHashMap<Integer, Session> {
	private static final long serialVersionUID = 8433280779020560086L;
}
