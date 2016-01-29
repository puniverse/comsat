/*
 * COMSAT
 * Copyright (c) 2016, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
/*
 * Adapted from Undertow's `io.undertow.util.StringReadChannelListener` class.
 * JBoss, Home of Professional Open Source. Copyright (C) 2014 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * Distributed under the Apache License Version 2.0.
 */
package co.paralleluniverse.comsat.webactors.undertow;

import io.undertow.UndertowLogger;
import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Stuart Douglas
 * @author circlespainter
 */
public abstract class ByteArrayReadChannelListener implements ChannelListener<StreamSourceChannel> {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private ByteBufferPool bufferPool;

    public ByteArrayReadChannelListener(final ByteBufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public final void setup(final StreamSourceChannel c) {
        partialRead(c, true);
    }

    @Override
    public final void handleEvent(final StreamSourceChannel c) {
        partialRead(c, false);
    }

    protected abstract void byteArrayDone(byte[] bs);

    protected abstract void error(IOException e);

    private void partialRead(StreamSourceChannel c, boolean setup) {
        if (baos == null || bufferPool == null) // Already dead by exception
            return;

        PooledByteBuffer r = null;
        ByteBuffer b;
        try {
            r = bufferPool.allocate();
            b = r.getBuffer();
            int readCount;
            do {
                readCount = c.read(b);
                if (readCount == 0) {
                    if (!setup)
                        return;

                    c.getReadSetter().set(this);
                    c.resumeReads();
                } else if (readCount == -1) {
                    final byte[] result = baos.toByteArray();
                    cleanup(c);
                    byteArrayDone(result);
                } else {
                    b.flip();
                    while (b.hasRemaining())
                        baos.write(b.get());
                }
            } while (readCount > 0);
        } catch (final IOException e) {
            cleanup(c);
            error(e);
        } catch (final Throwable t) {
            cleanup(c);
            UndertowLogger.REQUEST_IO_LOGGER.error("Error in partial read", t);
            throw new RuntimeException(t);
        } finally {
            if (r != null)
                r.close();
        }
    }

    private void cleanup(StreamSourceChannel c) {
        this.baos = null;
        this.bufferPool = null;
        c.getReadSetter().set(null);
        IoUtils.safeClose(c);
    }
}
