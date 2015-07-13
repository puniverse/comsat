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
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.fibers.FiberAsync;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.channels.StreamSourceChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author circlespainter
 */
public class FiberReadChannelListener extends FiberAsync<ByteBuffer, IOException> implements ChannelListener<StreamSourceChannel> {

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final Pool<ByteBuffer> pool;
	private final StreamSourceChannel ch;

	public FiberReadChannelListener(Pool<ByteBuffer> pool, StreamSourceChannel ch) {
		assert pool != null;
		assert ch != null;
		this.pool = pool;
		this.ch = ch;
	}

	@Override
	protected void requestAsync() {
		Pooled<ByteBuffer> resource = pool.allocate();
		ByteBuffer buffer = resource.getResource();
		try {
			int r;
			do {
				r = ch.read(buffer);
				if (r == 0) {
					ch.getReadSetter().set(this);
					ch.resumeReads();
				} else if (r == -1) {
					asyncCompleted(ByteBuffer.wrap(baos.toByteArray()));
					IoUtils.safeClose(ch);
				} else {
					buffer.flip();
					byte[] b = new byte[buffer.remaining()];
					buffer.get(b);
					baos.write(b);
				}
			} while (r > 0);
		} catch (IOException e) {
			asyncFailed(e);
		} finally {
			resource.free();
		}
	}

	@Override
	public void handleEvent(StreamSourceChannel channel) {
		Pooled<ByteBuffer> resource = pool.allocate();
		ByteBuffer buffer = resource.getResource();
		try {
			int r;
			do {
				r = ch.read(buffer);
				if (r == 0) {
					return;
				} else if (r == -1) {
					asyncCompleted(ByteBuffer.wrap(baos.toByteArray()));
					IoUtils.safeClose(channel);
				} else {
					buffer.flip();
					byte[] b = new byte[buffer.remaining()];
					buffer.get(b);
					baos.write(b);
				}
			} while (r > 0);
		} catch (IOException e) {
			asyncFailed(e);
		} finally {
			resource.free();
		}
	}
}
