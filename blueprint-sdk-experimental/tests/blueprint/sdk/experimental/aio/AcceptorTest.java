/*
 * Copyright 2008 Sangmin Lee, all rights reserved.
 */
package blueprint.sdk.experimental.aio;

import blueprint.sdk.experimental.aio.session.EchoSession;
import blueprint.sdk.experimental.aio.session.SimpleHttpSession;
import org.apache.log4j.Logger;

/**
 * Tests Acceptor
 *
 * @author Sangmin Lee
 * @create 2008. 11. 26.
 */
public class AcceptorTest {
    private static final Logger L = Logger.getLogger(AcceptorTest.class);

    public static void main(final String[] args) {

        try {
            Proactor proactor1 = new Proactor(Reactor.class, 3, 1, SimpleHttpSession.class, 1024);
            Service service1 = new Service("service 1", proactor1);
            service1.getIpFilter().allow("127.0.0.1");
            service1.bind("localhost", 1112, true, 5);

            Proactor proactor2 = new Proactor(Reactor.class, 3, 1, EchoSession.class, 1024);
            Service service2 = new Service("service 2", proactor2);
            service1.getIpFilter().allow("127.0.0.1");
            service2.bind("127.0.0.1", 1113, true, 5);
        } catch (Exception e) {
            L.trace(e);
        }
    }
}
