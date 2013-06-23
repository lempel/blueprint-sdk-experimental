/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import bluerpint.sdk.util.Validator;


/**
 * Selector Factory to recycle Selectors<br>
 * Opening a Selector cost a lot. So recycle as many as possible.<br>
 * 
 * @author Simon Lee
 * @since 2008. 12. 5.
 */
public class SelectorFactory {
	private transient static LinkedList<Selector> pool = new LinkedList<Selector>();

	/**
	 * @return
	 * @throws IOException
	 *             thrown by Selector
	 */
	public static Selector get() throws IOException {
		Selector result;
		try {
			result = pool.removeFirst();
		} catch (NoSuchElementException e) {
			result = Selector.open();
		}

		return result;
	}

	public static void release(final Selector selector) {
		if (Validator.isValid(selector)) {
			Set<SelectionKey> keys = selector.keys();

			for (SelectionKey key : keys) {
				key.cancel();
				keys.remove(key);
			}

			pool.addFirst(selector);
		}
	}

	public static int size() {
		return pool.size();
	}
}
