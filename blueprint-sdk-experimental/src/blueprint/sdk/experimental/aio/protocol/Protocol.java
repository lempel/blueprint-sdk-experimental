/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.aio.protocol;

import blueprint.sdk.experimental.aio.SelectorLoadBalancer;
import blueprint.sdk.experimental.aio.SocketChannelWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Provides basic functions and guidelines for Protocol implementation.<br>
 * <br>
 * Guide line:<br>
 * Use readSelectorLB.register(SocketChannelWrapper, int) for read operation.<br>
 * Check wrapper.isValid() before every read/write operations.<br>
 *
 * @author Sangmin Lee
 * @since 2008. 12. 12.
 */
public abstract class Protocol {
    @SuppressWarnings("WeakerAccess")
    protected transient SocketChannelWrapper wrapper;
    @SuppressWarnings("WeakerAccess")
    protected transient SelectorLoadBalancer readSelectorLB;

    /**
     * Constructor
     *
     * @param wrapper channel wrapper to use
     * @param readSelectorLB SelectorLoadBalancer for OP_READ
     */
    @SuppressWarnings("WeakerAccess")
    public Protocol(final SocketChannelWrapper wrapper, final SelectorLoadBalancer readSelectorLB) {
        this.wrapper = wrapper;
        this.readSelectorLB = readSelectorLB;
    }

    /**
     * returns received byte[] only if read operation is successful.
     *
     * @param buffer buffer to use
     * @return null or received byte[]
     * @throws java.io.IOException
     */
    public abstract byte[] read(final ByteBuffer buffer) throws IOException;

    /**
     * send data
     *
     * @param buffer data to send
     * @throws java.io.IOException
     */
    public abstract void write(final ByteBuffer buffer) throws IOException;

    protected SocketChannelWrapper getWrapper() {
        return wrapper;
    }

    protected void setWrapper(final SocketChannelWrapper wrapper) {
        this.wrapper = wrapper;
    }

    protected SelectorLoadBalancer getReadSelectorLB() {
        return readSelectorLB;
    }

    protected void setReadSelectorLB(final SelectorLoadBalancer readSelectorLB) {
        this.readSelectorLB = readSelectorLB;
    }
}
