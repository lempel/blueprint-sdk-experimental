/*
 * Copyright 2008 Sangmin Lee, all rights reserved.
 */
package blueprint.sdk.experimental.aio;

import java.io.IOException;
import java.nio.channels.Selector;

import org.apache.log4j.Logger;

/**
 * Tests Selector Factory
 * 
 * @author Sangmin Lee
 * @create 2008. 12. 5.
 */
public class SelectorFactoryTest {
	private static final Logger logger = Logger.getLogger(SelectorFactoryTest.class);

	public static void main(final String[] args) throws IOException {
		long start;
		long end;

		int volume = 1000;
		for (int x = 0; x < 20; x++) {
			start = System.nanoTime();
			Selector[] sels = new Selector[volume];
			for (int i = 0; i < volume; i++) {
				sels[i] = SelectorFactory.get();
			}
			for (Selector sel : sels) {
				SelectorFactory.release(sel);
			}
			end = System.nanoTime();
			logger.debug(end - start + " : " + SelectorFactory.size());
		}
	}
}
