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

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.SocketChannelWrapper;


/**
 * Provides basic functions and guidelines for Protocol implementation.<br>
 * <br>
 * Guide line:<br>
 * Use readSelectorLB.register(SocketChannelWrapper, int) for read operation.<br>
 * Check wrapper.isValid() before every read/write operations.<br>
 * 
 * @author Simon Lee
 * @since 2008. 12. 12.
 */
public abstract class Protocol {
	protected transient SocketChannelWrapper wrapper;
	protected transient SelectorLoadBalancer readSelectorLB;

	/**
	 * Constructor
	 * 
	 * @param wrapper
	 * @param readSelectorLB
	 *            SelectorLoadBalancer for OP_READ
	 */
	public Protocol(final SocketChannelWrapper wrapper, final SelectorLoadBalancer readSelectorLB) {
		this.wrapper = wrapper;
		this.readSelectorLB = readSelectorLB;
	}

	/**
	 * returns received byte[] only if read operation is successful.
	 * 
	 * @param buffer
	 * @return null or received byte[]
	 * @throws IOException
	 * @throws EOFException
	 *             nothing but EOF is received
	 */
	public abstract byte[] read(final ByteBuffer buffer) throws IOException, EOFException;

	public abstract void write(final ByteBuffer buffer) throws IOException;

	protected SocketChannelWrapper getWrapper() {
		return wrapper;
	}

	protected void setWrapper(final SocketChannelWrapper wrapper) {
		this.wrapper = wrapper;
	}

	protected SelectorLoadBalancer getReadSelectorLB() {
		return readSelectorLB;
	}

	protected void setReadSelectorLB(final SelectorLoadBalancer readSelectorLB) {
		this.readSelectorLB = readSelectorLB;
	}
}
