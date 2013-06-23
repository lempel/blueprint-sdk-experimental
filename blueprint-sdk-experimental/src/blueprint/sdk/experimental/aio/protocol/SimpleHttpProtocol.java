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
 * Simple HTTP Protocol Implementation
 * 
 * @author Simon Lee
 * @version $Revision$
 * @since 2008. 12. 12.
 * @last $Date$
 */
public class SimpleHttpProtocol extends Protocol {
	public SimpleHttpProtocol(final SocketChannelWrapper wrapper, final SelectorLoadBalancer readSelectorLB) {
		super(wrapper, readSelectorLB);
	}

	public byte[] read(final ByteBuffer buffer) throws IOException, EOFException {
		byte[] result = null;

		if (wrapper.isValid()) {
			int nRead = wrapper.read(buffer);
			if (nRead == -1) {
				throw new EOFException("nothing but EOF is received");
			} else if (nRead == 0) {
				readSelectorLB.register(wrapper, SelectionKey.OP_READ);
			} else if (nRead > 0 && isEohReceived(buffer)) {
				buffer.flip();
				result = new byte[buffer.limit()];
				buffer.get(result);
				buffer.clear();

				// never read contents. this class is just for demo.
			}
		}

		return result;
	}

	public void write(final ByteBuffer buffer) throws IOException {
		wrapper.write(buffer);
	}

	/**
	 * Is end of header(two sequential new lines) received?
	 * 
	 * @param buffer
	 * @return
	 */
	protected static boolean isEohReceived(final ByteBuffer buffer) {
		int position = buffer.position();
		int eofLen = getEohLength(position);

		// get last 2 or 4 bytes
		byte[] data = new byte[eofLen];
		for (int i = 0; i < eofLen; i++) {
			data[i] = buffer.get(position - eofLen + i);
		}

		return isEoh(eofLen, data);
	}

	private static boolean isEoh(final int eofLen, final byte[] data) {
		boolean result = false;
		// check for two sequential new lines
		if (eofLen >= 2 && data[eofLen - 2] == 0x0a && data[eofLen - 1] == 0x0a) {
			result = true;
		} else if (eofLen >= 4 && data[eofLen - 4] == 0x0d && data[eofLen - 3] == 0x0a && data[eofLen - 2] == 0x0d
				&& data[eofLen - 1] == 0x0a) {
			result = true;
		}
		return result;
	}

	private static int getEohLength(final int position) {
		int process = 0;
		if (position >= 4) {
			process = 4;
		} else if (position >= 2) {
			process = 2;
		}

		return process;
	}
}
