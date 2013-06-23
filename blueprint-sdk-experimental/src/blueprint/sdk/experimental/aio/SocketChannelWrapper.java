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
package blueprint.sdk.experimental.aio;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import lempel.blueprint.base.concurrent.TimeoutHandler;
import bluerpint.sdk.util.Validator;
import bluerpint.sdk.util.jvm.shutdown.Terminatable;

/**
 * Provides EASIER way to handle SocketChannel.<br>
 * <b>This class is NOT THREAD SAFE.</b><br>
 * 
 * @author Simon Lee
 * @version $Revision$
 * @since 2008. 11. 25.
 * @last $Date$
 */
public class SocketChannelWrapper implements Terminatable {
	/** wrapee (SocketChannel) */
	private transient SocketChannel channel;

	private transient TimeoutHandler timeoutHandler = null;

	private transient Selector acceptSelector;
	// anyone need this?
	private transient Selector connectSelector;
	private transient Selector readSelector;
	private transient Selector writeSelector;

	private transient boolean terminated = false;

	public SocketChannelWrapper(SocketChannel channel) {
		this.channel = channel;
	}

	public void configureBlocking(boolean block) throws IOException {
		channel.configureBlocking(block);
	}

	/**
	 * @param sel
	 * @param ops
	 *            interested ops
	 * @return
	 */
	public SelectionKey register(Selector sel, int ops) {
		SelectionKey result = null;
		try {
			switch (ops) {
			case SelectionKey.OP_ACCEPT:
				// cancel from previous selecor
				if (Validator.isNotNull(acceptSelector)) {
					cancel(ops);
				}
				acceptSelector = sel;
				break;
			case SelectionKey.OP_CONNECT:
				// cancel from previous selecor
				if (Validator.isNotNull(connectSelector)) {
					cancel(ops);
				}
				connectSelector = sel;
				break;
			case SelectionKey.OP_READ:
				// cancel from previous selecor
				if (Validator.isNotNull(readSelector)) {
					cancel(ops);
				}
				readSelector = sel;
				break;
			case SelectionKey.OP_WRITE:
				// cancel from previous selecor
				if (Validator.isNotNull(writeSelector)) {
					cancel(ops);
				}
				writeSelector = sel;
				break;
			default:
				// should not happen
				break;
			}

			sel.wakeup();
			result = channel.register(sel, ops);
		} catch (CancelledKeyException ignored) {
		} catch (ClosedChannelException ignored) {
		}

		return result;
	}

	public void cancel(int ops) {
		Selector sel = null;

		switch (ops) {
		case SelectionKey.OP_ACCEPT:
			sel = acceptSelector;
			break;
		case SelectionKey.OP_CONNECT:
			sel = connectSelector;
			break;
		case SelectionKey.OP_READ:
			sel = readSelector;
			break;
		case SelectionKey.OP_WRITE:
			sel = writeSelector;
			break;
		default:
			// should not happen
			break;
		}

		if (Validator.isNotNull(sel)) {
			SelectionKey key = channel.keyFor(sel);

			if (Validator.isNotNull(key)) {
				key.cancel();
			}
		}
	}

	public void open(String address, int port) throws IOException {
		close();
		channel = SocketChannel.open(new InetSocketAddress(address, port));

		updateTimestamp();
	}

	public void connect(String address, int port) throws IOException {
		channel.connect(new InetSocketAddress(address, port));

		updateTimestamp();
	}

	public void close() throws IOException {
		if (Validator.isNotNull(channel)) {
			cancel(SelectionKey.OP_ACCEPT);
			cancel(SelectionKey.OP_CONNECT);
			cancel(SelectionKey.OP_READ);
			cancel(SelectionKey.OP_WRITE);
			channel.close();

			removeFromTimeoutHandler();
		}
	}

	/**
	 * <b>BEWARE: SocketChannel could be timed-out already</b>
	 * 
	 * @param _timeoutHandler
	 */
	public void setTimeoutHandler(TimeoutHandler _timeoutHandler) {
		timeoutHandler = _timeoutHandler;
		updateTimestamp();
	}

	/**
	 * update timestamp of TimeoutHandler (if not null).<br>
	 * Call right after every SUCCESSFUL read/write.<br>
	 */
	public void updateTimestamp() {
		if (Validator.isNotNull(timeoutHandler)) {
			timeoutHandler.updateTimestamp(this);
		}
	}

	public void removeFromTimeoutHandler() {
		if (Validator.isNotNull(timeoutHandler)) {
			timeoutHandler.remove(this);
		}
	}

	public int read(ByteBuffer dst) throws IOException {
		int result = channel.read(dst);
		if (result > 0) {
			updateTimestamp();
		}
		return result;
	}

	public long read(ByteBuffer[] dsts) throws IOException {
		long result = channel.read(dsts);
		if (result > 0) {
			updateTimestamp();
		}
		return result;
	}

	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		long result = channel.read(dsts, offset, length);
		if (result > 0) {
			updateTimestamp();
		}
		return result;
	}

	public void write(ByteBuffer src) throws IOException {
		int attempts = 0;
		SelectionKey key = null;

		try {
			while (src.hasRemaining()) {
				int len = channel.write(src);
				attempts++;

				if (len == -1) {
					throw new EOFException();
				} else if (len == 0) {
					if (writeSelector == null) {
						writeSelector = SelectorFactory.get();
					}

					key = register(writeSelector, SelectionKey.OP_WRITE);

					if (writeSelector.select(30 * 1000) == 0 && attempts > 2) {
						throw new IOException("Client disconnected");
					} else {
						attempts--;
					}
				} else {
					attempts = 0;
					updateTimestamp();
				}
			}
		} finally {
			if (Validator.isNotNull(key)) {
				key.cancel();
				key = null;
			}

			SelectorFactory.release(writeSelector);
		}
	}

	public long write(ByteBuffer[] srcs) throws IOException {
		long result = channel.write(srcs);
		if (result > 0) {
			updateTimestamp();
		}
		return result;
	}

	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		long result = channel.write(srcs, offset, length);
		if (result > 0) {
			updateTimestamp();
		}
		return result;
	}

	public boolean isValid() {
		return Validator.isValid(channel);
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void terminate() {
		try {
			close();
		} catch (IOException ignored) {
		}

		terminated = true;
	}

	@Override
	protected void finalize() throws Throwable {
		channel = null;
		timeoutHandler = null;

		super.finalize();
	}
}
