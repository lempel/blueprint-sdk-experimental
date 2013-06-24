/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio;

import java.nio.channels.Selector;
import java.util.List;

import blueprint.sdk.util.Terminatable;

/**
 * Load Balancer for Selector list
 * 
 * @author Simon Lee
 * @since 2008. 12. 5.
 */
public class SelectorLoadBalancer implements Terminatable {
	private transient final List<Selector> selectors;

	private transient boolean terminated = false;

	public SelectorLoadBalancer(final List<Selector> selectors) {
		this.selectors = selectors;
	}

	/**
	 * @param wrapper
	 * @param ops
	 *            interested ops
	 */
	public void register(final SocketChannelWrapper wrapper, final int ops) {
		synchronized (this) {
			if (selectors.isEmpty()) {
				Selector sel = selectors.remove(0);
				wrapper.register(sel, ops);
				selectors.add(sel);
			}
		}
	}

	public boolean isValid() {
		return (selectors.isEmpty()) ? false : true;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void terminate() {
		while (!selectors.isEmpty()) {
			SelectorFactory.release(selectors.remove(0));
		}
		
		terminated = true;
	}
}
