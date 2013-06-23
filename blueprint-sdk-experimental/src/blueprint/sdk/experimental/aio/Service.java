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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import lempel.blueprint.base.concurrent.TimeoutHandler;
import lempel.blueprint.base.io.IpFilter;
import blueprint.sdk.logger.Logger;
import bluerpint.sdk.util.Validator;
import bluerpint.sdk.util.jvm.shutdown.Terminatable;

/**
 * A Service<br>
 * Accept & processes own Clients<br>
 * <br>
 * example:<br>
 * Proactor p1 = new Proactor(Reactor.class, 3, 1, SimpleHttpSession.class,
 * 1024);<br>
 * Service s1 = new Service("service 1", p1);<br>
 * s1.getIpFilter().allow("127.0.0.1");<br>
 * s1.bind("localhost", 1112, true, 5);<br>
 * 
 * @author Simon Lee
 * @version $Revision$
 * @since 2008. 11. 25.
 * @last $Date$
 */
public class Service implements Runnable, Terminatable {
	private static final Logger LOGGER = Logger.getInstance();

	private final String serviceName;
	private transient final Proactor proactor;

	private SocketAddress address;
	private transient ServerSocketChannel serverChannel;
	private transient TimeoutHandler timeoutHandler = null;
	private final IpFilter ipFilter;

	private transient boolean terminated = false;

	public Service(final String serviceName, final Proactor proactor) {
		LOGGER.info(this, "creating service [" + serviceName + "]");

		this.serviceName = serviceName;
		this.proactor = proactor;
		ipFilter = new IpFilter();

		LOGGER.info(this, "service [" + serviceName + "] created");
	}

	/**
	 * bind ServerSocketChannel & start service
	 * 
	 * @param bindAddress
	 * @param bindPort
	 * @param reuseAddress
	 * @param clientTimeout
	 * @throws IOException
	 */
	public void bind(final String bindAddress, final int bindPort, final boolean reuseAddress, final int clientTimeout)
			throws IOException {
		LOGGER.info(this, "binding service [" + serviceName + "] to [" + address + "]");

		if (clientTimeout > 0) {
			timeoutHandler = TimeoutHandler.newTimeoutHandler(clientTimeout, 1);
			proactor.setTimeoutHandler(timeoutHandler);
		}

		if (Validator.isNotEmpty(bindAddress) || "*".equals(bindAddress)) {
			address = new InetSocketAddress(bindPort);
		} else {
			address = new InetSocketAddress(InetAddress.getByName(bindAddress), bindPort);
		}

		// blocking mode
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(true);
		serverChannel.socket().setReuseAddress(reuseAddress);
		serverChannel.socket().bind(address);

		Thread thr = new Thread(this);
		thr.setDaemon(true);
		thr.start();

		LOGGER.info(this, "service [" + serviceName + "] is now bound to [" + address + "] and started");
	}

	/**
	 * Accept a client
	 * 
	 * @param channel
	 */
	public void accept(final SocketChannel channel) {
		proactor.accept(channel);
	}

	public void run() {
		boolean runFlag = true;

		LOGGER.info(this, "service [" + serviceName + "] started");

		while (runFlag) {
			try {
				// blocking mode
				Socket sock = serverChannel.socket().accept();
				if (sock != null) {
					if (ipFilter.isAllowed(sock.getInetAddress().getHostAddress())) {
						accept(sock.getChannel());
					} else {
						try {
							sock.close();
						} catch (Exception ignored) {
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("can't accept client - " + e);
				LOGGER.trace(e);
			}
		}

		terminated = true;
	}

	public boolean isValid() {
		boolean result = false;
		if (serverChannel.isOpen() && proactor.isValid()) {
			result = true;
		}
		return result;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void terminate() {
		LOGGER.info(this, "terminating service [" + serviceName + "]");

		try {
			serverChannel.close();
		} catch (IOException ignored) {
		}
		proactor.terminate();
		timeoutHandler.terminate();

		LOGGER.info(this, "service [" + serviceName + "] is terminated");
	}

	public String getServiceName() {
		return serviceName;
	}

	public ServerSocketChannel getServerChannel() {
		return serverChannel;
	}

	public Proactor getProactor() {
		return proactor;
	}

	public IpFilter getIpFilter() {
		return ipFilter;
	}
}
