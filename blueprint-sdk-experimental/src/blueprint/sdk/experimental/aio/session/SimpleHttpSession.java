/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio.session;

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.protocol.SimpleHttpProtocol;
import blueprint.sdk.util.CharsetUtil;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;


/**
 * HTTP Session
 *
 * @author Sangmin Lee
 * @since 2008. 12. 1.
 */
public class SimpleHttpSession extends Session {
    public SimpleHttpSession(final SocketChannel channel, final ConcurrentHashMap<Integer, Session> map,
                             final Integer bufferSize, final SelectorLoadBalancer selectorLB) {
        super(channel, map, bufferSize, selectorLB);

        protocol = new SimpleHttpProtocol(wrapper, selectorLB);
    }

    public void process() throws IOException {
        try {
            byte[] data = read(readBuffer);

            write(("<html><body>Your request is:<br>" + new String(data) + "</body></html>\n").getBytes(CharsetUtil.getDefaultEncoding()));
        } catch (IOException ex) {
            terminate();
        }
    }
}
