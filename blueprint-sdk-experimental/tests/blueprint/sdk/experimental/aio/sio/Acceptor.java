/*
 * Copyright 2008 Sangmin Lee, all rights reserved.
 */
package blueprint.sdk.experimental.aio.sio;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * SIO Acceptor for comparison
 *
 * @author Sangmin Lee
 * @create 2008. 12. 3.
 */
@SuppressWarnings("WeakerAccess")
public class Acceptor extends Thread {
    private static final Logger L = Logger.getLogger(Acceptor.class);
    private final transient ServerSocket ssock;

    @SuppressWarnings("SameParameterValue")
    public Acceptor(final int port) throws IOException {
        super();
        ssock = new ServerSocket(port);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        while (true) {
            try {
                Socket sock = ssock.accept();
                new Session(sock).start();
            } catch (IOException e) {
                L.error(e);
                L.trace(e);
            }
        }
    }
}
