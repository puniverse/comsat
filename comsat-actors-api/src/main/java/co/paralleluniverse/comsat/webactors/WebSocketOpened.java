/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.actors.ActorRef;
import java.nio.ByteBuffer;

/**
 * A message sent by a virtual actor representing a WebSocket client indicating that a WebSocket has been opened.
 */
public class WebSocketOpened extends WebMessage {
    private final ActorRef<WebDataMessage> actor;

    public WebSocketOpened(ActorRef<WebDataMessage> actor) {
        this.actor = actor;
    }

    @Override
    public ActorRef<WebDataMessage> sender() {
        return actor;
    }

    /**
     * {@inheritDoc}
     * <p>This method returns {@code null}, as it has no body.
     *
     * @return {@code null}
     */
    @Override
    public String getStringBody() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>This method returns {@code null}, as it has no body.
     *
     * @return {@code null}
     */
    @Override
    public ByteBuffer getByteBufferBody() {
        return null;
    }
}
