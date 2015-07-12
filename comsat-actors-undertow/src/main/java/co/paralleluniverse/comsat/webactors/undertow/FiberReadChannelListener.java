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
import io.undertow.server.HttpServerExchange;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.Pooled;
import org.xnio.channels.StreamSourceChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author circlespainter
 */
public class FiberReadChannelListener extends FiberAsync<ByteBuffer, IOException> implements ChannelListener<StreamSourceChannel> {

  private final HttpServerExchange xch;
  private final List<ByteBuffer> bufs = new ArrayList<>(8);

  public FiberReadChannelListener(HttpServerExchange xch) {
    this.xch = xch;
  }

  @Override
  protected void requestAsync() {
    xch.getRequestChannel().getReadSetter().set(this);
  }

  @Override
  public void handleEvent(StreamSourceChannel channel) {
    Pooled<ByteBuffer> resource = xch.getConnection().getBufferPool().allocate();
    ByteBuffer buffer = resource.getResource();
    try {
      int r;
      do {
        r = channel.read(buffer);
        if (r == 0) {
          return;
        } else if (r == -1) {
          asyncCompleted(join(bufs));
          IoUtils.safeClose(channel);
        } else {
          buffer.flip();
          bufs.add(buffer.duplicate());
        }
      } while (r > 0);
    } catch (IOException e) {
      asyncFailed(e);
    } finally {
      resource.free();
    }
  }

  private ByteBuffer join(List<ByteBuffer> bufs) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (ByteBuffer buf : bufs) {
      byte[] b = new byte[buf.remaining()];
      buf.get(b);
    }
    return ByteBuffer.wrap(baos.toByteArray());
  }
}
