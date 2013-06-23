/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio.session;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.protocol.EchoProtocol;


/**
 * Echo Session
 * 
 * @author Simon Lee
 * @since 2008. 12. 1.
 */
public class EchoSession extends Session {
	public EchoSession(final SocketChannel channel, final ConcurrentHashMap<Integer, Session> map,
			final Integer bufferSize, final SelectorLoadBalancer selectorLB) throws IOException {
		super(channel, map, bufferSize, selectorLB);

		protocol = new EchoProtocol(wrapper, selectorLB);
	}

	public void process() throws IOException {
		// read
		byte[] request = protocol.read(readBuffer);

		// write
		// don't have call protocol.write but not prohibited.
		write(request);
	}
}
