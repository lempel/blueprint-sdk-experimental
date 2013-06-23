/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.SocketChannelWrapper;


/**
 * Echo Protocol for Reference
 * 
 * @author Simon Lee
 * @since 2009. 2. 11.
 */
public class EchoProtocol extends Protocol {
	public EchoProtocol(final SocketChannelWrapper wrapper, final SelectorLoadBalancer selectorLB) {
		super(wrapper, selectorLB);
	}

	@Override
	public byte[] read(final ByteBuffer buffer) throws IOException, EOFException {
		byte[] result = null;

		int nRead = 0;
		if (wrapper.isValid()) {
			nRead = wrapper.read(buffer);
			if (nRead == -1) {
				throw new EOFException("nothing but EOF is received");
			} else if (nRead == 0) {
				readSelectorLB.register(wrapper, SelectionKey.OP_READ);
			} else if (nRead > 0) {
				buffer.flip();
				result = new byte[buffer.limit()];
				buffer.get(result);
				buffer.clear();
			}
		}

		return result == null ? new byte[0] : result;
	}

	@Override
	public void write(final ByteBuffer buffer) throws IOException {
		wrapper.write(buffer);
	}
}
