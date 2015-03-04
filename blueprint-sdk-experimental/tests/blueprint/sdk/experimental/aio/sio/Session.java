/*
 * Copyright 2008 Sangmin Lee, all rights reserved.
 */
package blueprint.sdk.experimental.aio.sio;

import org.apache.log4j.Logger;
import uka.transport.MemoryOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Client Session for comparison
 *
 * @author Sangmin Lee
 * @create 2008. 12. 3.
 */
@SuppressWarnings("WeakerAccess")
public class Session extends Thread {
    private static final Logger L = Logger.getLogger(Session.class);

    private transient final Socket sock;
    private transient final DataInputStream dis;
    private transient final DataOutputStream dos;

    public Session(final Socket sock) throws IOException {
        super();
        this.sock = sock;
        this.sock.setSoTimeout(5000);
        dis = new DataInputStream(sock.getInputStream());
        dos = new DataOutputStream(sock.getOutputStream());
    }

    public void run() {
        MemoryOutputStream mos = new MemoryOutputStream();
        for (; ; ) {
            try {
                byte[] data = new byte[dis.available()];
                if (data.length == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    dis.readFully(data);
                    mos.write(data);

                    if (isEOF(mos.getBuffer())) {
                        String msg = "OK. I got It. - " + System.currentTimeMillis();
                        dos.write(msg.getBytes());

                        try {
                            sock.close();
                        } catch (IOException ignored) {
                        }

                        return;
                    }
                }
            } catch (IOException e) {
                L.error(e);
                L.trace(e);
            } finally {
                try {
                    mos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * check EOF of HTTP
     *
     * @param data
     * @return
     */
    protected boolean isEOF(final byte[] data) {
        boolean result = false;
        int length = data.length;

        if (length >= 2 && (data[length - 2] == 0x0a && data[length - 1] == 0x0a)) {
            result = true;
        }
        if (length >= 4
                && (data[length - 4] == 0x0d && data[length - 3] == 0x0a && data[length - 2] == 0x0d && data[length - 1] == 0x0a)) {
            result = true;
        }

        return result;
    }
}
