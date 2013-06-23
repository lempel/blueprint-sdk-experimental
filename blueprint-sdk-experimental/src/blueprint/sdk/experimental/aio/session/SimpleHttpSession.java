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
import blueprint.sdk.experimental.aio.protocol.SimpleHttpProtocol;
import bluerpint.sdk.util.CharsetUtil;


/**
 * HTTP Session
 * 
 * @author Simon Lee
 * @since 2008. 12. 1.
 */
public class SimpleHttpSession extends Session {
	public SimpleHttpSession(final SocketChannel channel, final ConcurrentHashMap<Integer, Session> map,
			final Integer bufferSize, final SelectorLoadBalancer selectorLB) throws IOException {
		super(channel, map, bufferSize, selectorLB);

		protocol = new SimpleHttpProtocol(wrapper, selectorLB);
	}

	public void process() throws IOException {
		try {
			byte[] data = read(readBuffer);

			StringBuilder builder = new StringBuilder(data.length + 20);
			builder.append("<html><body>Your request is:<br>");
			builder.append(new String(data));
			builder.append("</body></html>\n");

			write(builder.toString().getBytes(CharsetUtil.getDefaultEncoding()));
		} catch (IOException ex) {
			terminate();
		}
	}
}
