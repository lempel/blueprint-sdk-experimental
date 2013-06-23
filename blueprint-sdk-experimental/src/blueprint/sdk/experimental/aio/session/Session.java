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
package blueprint.sdk.experimental.aio.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.SocketChannelWrapper;
import blueprint.sdk.experimental.aio.protocol.Protocol;
import bluerpint.sdk.util.Validator;
import bluerpint.sdk.util.jvm.shutdown.Terminatable;

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
 * @author Simon Lee
 * @version $Revision$
 * @since 2008. 11. 27.
 * @last $Date$
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
