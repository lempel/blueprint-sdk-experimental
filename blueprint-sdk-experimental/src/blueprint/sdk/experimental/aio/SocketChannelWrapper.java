/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
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

import blueprint.sdk.core.concurrent.TimeoutHandler;
import blueprint.sdk.util.Validator;
import blueprint.sdk.util.jvm.shutdown.Terminatable;

/**
 * Provides EASIER way to handle SocketChannel.<br>
 * <b>This class is NOT THREAD SAFE.</b><br>
 * 
 * @author Simon Lee
 * @since 2008. 11. 25.
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
