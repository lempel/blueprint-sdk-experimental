/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import blueprint.sdk.logger.Logger;
import blueprint.sdk.util.Terminatable;

/**
 * Polls a Selector & invokes a task
 * 
 * @author Simon Lee
 * @since 2008. 11. 26.
 */
public abstract class SelectThread extends Thread implements Terminatable {
	private static final Logger LOGGER = Logger.getInstance();
	private transient final Selector selector;

	/** think time to prevent excessive CPU consumption (msec) */
	private int thinkTime = 10;
	private boolean running = false;
	private transient boolean terminated = false;

	public SelectThread(final Selector selector) {
		this(selector, 10);
	}

	/**
	 * Constructor
	 * 
	 * @param selector
	 * @param thinkTime
	 *            think time to prevent excessive CPU consumption (msec)
	 */
	public SelectThread(final Selector selector, final int thinkTime) {
		super();
		this.selector = selector;
		this.thinkTime = thinkTime;
		setName("SelectThread");
	}

	public void run() {
		running = true;

		LOGGER.debug(this, "select thread started");

		while (running) {
			boolean selected;
			try {
				selected = selector.selectNow() > 0 ? true : false;
			} catch (CancelledKeyException ignored) {
				selected = false;
			} catch (Exception e) {
				LOGGER.error(this, "select failed");
				LOGGER.trace(e);
				selected = false;
			}

			if (selected) {
				Set<SelectionKey> keysSet = selector.selectedKeys();
				Object[] keys = keysSet.toArray();
				keysSet.clear();
				for (Object key : keys) {
					try {
						process((SelectionKey) key);
					} catch (CancelledKeyException ignored) {
						selected = false;
					} catch (Exception e) {
						LOGGER.equals(e);
						LOGGER.trace(e);
					}
				}
			}

			// think time to prevent excessive CPU consumption
			try {
				Thread.sleep(thinkTime);
			} catch (InterruptedException ignored) {
			}
		}

		terminated = true;

		LOGGER.debug(this, "select thread stopped");
	}

	/**
	 * Handle selected key
	 * 
	 * @param key
	 */
	protected abstract void process(SelectionKey key);

	public Selector getSelector() {
		return selector;
	}

	public boolean isValid() {
		return running;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void terminate() {
		running = false;
	}
}
