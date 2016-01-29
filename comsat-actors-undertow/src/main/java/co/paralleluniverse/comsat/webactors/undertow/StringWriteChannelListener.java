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
 * Adapted from Undertow's `io.undertow.util.StringWriteChannelListener` class.
 * JBoss, Home of Professional Open Source. Copyright (C) 2014 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * Distributed under the Apache License Version 2.0.
 */
package co.paralleluniverse.comsat.webactors.undertow;

import io.undertow.UndertowLogger;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author Stuart Douglas
 * @author circlespainter
 */
public final class StringWriteChannelListener implements ChannelListener<StreamSinkChannel> {
    private ByteBuffer buffer;

    public StringWriteChannelListener(String string) {
        this(string, Charset.defaultCharset());
    }

    public StringWriteChannelListener(final String string, Charset charset) {
        buffer = ByteBuffer.wrap(string.getBytes(charset));
    }

    public StringWriteChannelListener(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public final void setup(final StreamSinkChannel channel) {
        partialWrite(channel, true);
    }

    @Override
    public final void handleEvent(final StreamSinkChannel channel) {
        partialWrite(channel, false);
    }

    private void partialWrite(StreamSinkChannel c, boolean setup) {
        if (buffer == null)
            return;

        try {
            int writeCount;
            do {
                writeCount = c.write(buffer);
            } while (buffer.hasRemaining() && writeCount > 0);
            if (buffer.hasRemaining()) {
                if (setup)
                    c.getWriteSetter().set(this);
                c.resumeWrites();
            } else {
                writeDone(c);
            }
        } catch (final Throwable t) {
            handleError(c, "Error in 'partialWrite(setup = " + setup + ")'", t);
            if (!(t instanceof IOException))
                throw new RuntimeException(t);
        }
    }

    private void writeDone(final StreamSinkChannel c) {
        try {
            cleanup();
            c.shutdownWrites();

            if (!c.flush()) {
                c.getWriteSetter().set (
                    ChannelListeners.flushingChannelListener (
                        new ChannelListener<StreamSinkChannel>() {
                            @Override
                            public final void handleEvent(StreamSinkChannel o) {
                                IoUtils.safeClose(c);
                            }
                        },
                        ChannelListeners.closingChannelExceptionHandler()
                    )
                );
                c.resumeWrites();
            }
        } catch (final Throwable t) {
            handleError(c, "Error in 'writeDone'", t);
            if (!(t instanceof IOException))
                throw new RuntimeException(t);
        }
    }

    private void handleError(StreamSinkChannel c, String m, Throwable t) {
        if (t instanceof IOException)
            UndertowLogger.REQUEST_IO_LOGGER.ioException((IOException) t);
        else
            UndertowLogger.REQUEST_IO_LOGGER.error(m, t);
        cleanup();
        c.getWriteSetter().set(null);
        IoUtils.safeClose(c);
    }

    private void cleanup() {
        if (buffer != null) {
            buffer = null;
        }
    }
}
