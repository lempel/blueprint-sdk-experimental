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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.SocketChannelWrapper;
import blueprint.sdk.experimental.aio.protocol.Protocol;
import blueprint.sdk.util.Terminatable;
import blueprint.sdk.util.Validator;

/**
 * Provides basic functions and guidelines for client session implementation.<br>
 * <br>
 * Guidelines:<br>
 * You need an implementation of Protocol.<br>
 * Instantiate Protocol manually at constructor or process method.<br>
 * If 'Protocol.read()' returns -1, generally it means you gotta terminate
 * current session.<br>
 * If you are willing to override terminate() method, make sure to call
 * 'map.remove(this)'.<br>
 * 
 * @author Sangmin Lee
 * @since 2008. 11. 27.
 */
public abstract class Session implements Terminatable {
	protected final transient SocketChannelWrapper wrapper;
	protected final ConcurrentHashMap<Integer, Session> sessionMap;
	protected final transient SelectorLoadBalancer readSelectorLB;

	protected Protocol protocol;
	protected ByteBuffer readBuffer;

	private transient boolean terminated = false;

	/**
	 * Constructor
	 * 
	 * @param channel
	 * @param sessionMap
	 *            Map of sessions
	 * @param readBufferSize
	 *            read buffer size in byte
	 * @param readSelectorLB
	 *            SelectorLoadBalancer for OP_READ ops
	 * @throws IOException
	 *             Can't create SocketChannelWrapper
	 */
	public Session(final SocketChannel channel, final ConcurrentHashMap<Integer, Session> sessionMap,
			final Integer readBufferSize, final SelectorLoadBalancer readSelectorLB) throws IOException {
		wrapper = new SocketChannelWrapper(channel);
		this.sessionMap = sessionMap;

		readBuffer = ByteBuffer.allocate(readBufferSize);
		this.readSelectorLB = readSelectorLB;
	}

	public boolean isValid() {
		return wrapper.isValid();
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void terminate() {
		if (Validator.isNotNull(wrapper)) {
			wrapper.terminate();
		}

		if (Validator.isNotNull(sessionMap)) {
			sessionMap.remove(this);
		}

		if (Validator.isNotNull(readBuffer)) {
			readBuffer.clear();
			readBuffer = null;
		}

		terminated = true;
	}

	/**
	 * Process a client<br>
	 * <b>BEWARE: client could be timed-out already</b><br>
	 * 
	 * @throws IOException
	 */
	public abstract void process() throws IOException;

	public byte[] read(final ByteBuffer buffer) throws IOException {
		return protocol.read(buffer);
	}

	public void write(byte[] data) throws IOException {
		protocol.write(ByteBuffer.wrap(data));
	}

	public SocketChannelWrapper getWrapper() {
		return wrapper;
	}

	public ConcurrentHashMap<Integer, Session> getSessionMap() {
		return sessionMap;
	}

	public ByteBuffer getReadBuffer() {
		return readBuffer;
	}
}
