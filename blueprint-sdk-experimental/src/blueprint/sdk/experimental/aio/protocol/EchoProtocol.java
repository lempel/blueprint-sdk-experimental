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
 * @version $Revision$
 * @since 2009. 2. 11.
 * @last $Date$
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
