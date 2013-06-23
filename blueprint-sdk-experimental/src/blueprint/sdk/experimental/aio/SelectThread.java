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

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import blueprint.sdk.logger.Logger;
import bluerpint.sdk.util.jvm.shutdown.Terminatable;

/**
 * Polls a Selector & invokes a task
 * 
 * @author Simon Lee
 * @version $Revision$
 * @since 2008. 11. 26.
 * @last $Date$
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
