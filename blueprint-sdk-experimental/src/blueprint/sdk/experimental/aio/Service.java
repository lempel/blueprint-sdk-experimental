/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio;

import blueprint.sdk.core.concurrent.TimeoutHandler;
import blueprint.sdk.logger.Logger;
import blueprint.sdk.util.Terminatable;
import blueprint.sdk.util.Validator;
import lempel.blueprint.base.io.IpFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
 * @author Sangmin Lee
 * @since 2008. 11. 25.
 */
@SuppressWarnings("WeakerAccess")
public class Service implements Runnable, Terminatable {
    private static final Logger LOGGER = Logger.getInstance();

    private final String serviceName;
    private transient final Proactor proactor;
    private final IpFilter ipFilter;
    private SocketAddress address;
    private transient ServerSocketChannel serverChannel;
    private transient TimeoutHandler timeoutHandler = null;
    private boolean runFlag = true;
    private boolean terminated = false;

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
     * @param bindAddress address to bind
     * @param bindPort port to bind
     * @param reuseAddress true: reuse address
     * @param clientTimeout connection timeout
     * @throws IOException
     */
    @SuppressWarnings("SameParameterValue")
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
     * @param channel channel to accept
     */
    public void accept(final SocketChannel channel) {
        proactor.accept(channel);
    }

    public void run() {
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

        runFlag = false;

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
