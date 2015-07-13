/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.FiberAsync;
import io.undertow.UndertowLogger;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author Stuart Douglas
 * @author circlespainter
 */
public class FiberWriteChannelListener extends FiberAsync<Void, IOException> implements ChannelListener<StreamSinkChannel> {

	private final ByteBuffer buffer;
	private final StreamSinkChannel channel;

	public FiberWriteChannelListener(String string, StreamSinkChannel channel) {
		this(string, Charset.defaultCharset(), channel);
	}

	public FiberWriteChannelListener(String string, Charset charset, StreamSinkChannel channel) {
		buffer = ByteBuffer.wrap(string.getBytes(charset));
		this.channel = channel;
	}

	public FiberWriteChannelListener(ByteBuffer buf, StreamSinkChannel channel) {
		buffer = buf;
		this.channel = channel;
	}

	@Override
	protected void requestAsync() {
		try {
			int c;
			do {
				c = channel.write(buffer);
			} while (buffer.hasRemaining() && c > 0);

			if (buffer.hasRemaining()) {
				channel.getWriteSetter().set(this);
				channel.resumeWrites();
			} else {
				writeDone(channel);
			}
		} catch (IOException e) {
			handleError(channel, e);
		}
	}

	@Override
	public void handleEvent(final StreamSinkChannel channel) {
		try {
			int c;
			do {
				c = channel.write(buffer);
			} while (buffer.hasRemaining() && c > 0);

			if (buffer.hasRemaining()) {
				channel.resumeWrites();
			} else {
				writeDone(channel);
			}
		} catch (IOException e) {
			handleError(channel, e);
		}
	}

	protected void handleError(StreamSinkChannel channel, IOException e) {
		UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
		IoUtils.safeClose(channel);
		asyncFailed(e);
	}

	protected void writeDone(final StreamSinkChannel channel) {
		try {
			channel.shutdownWrites();

			if (!channel.flush()) {
				channel.getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
					@Override
					public void handleEvent(StreamSinkChannel o) {
						IoUtils.safeClose(channel);
					}
				}, ChannelListeners.closingChannelExceptionHandler()));
				channel.resumeWrites();
			}

			asyncCompleted(null);
		} catch (IOException e) {
			handleError(channel, e);
		}
	}
}
