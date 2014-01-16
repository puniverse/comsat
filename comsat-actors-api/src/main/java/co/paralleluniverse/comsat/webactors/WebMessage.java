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
import co.paralleluniverse.actors.behaviors.ActorMessage;
import co.paralleluniverse.actors.behaviors.FromMessage;
import java.nio.ByteBuffer;

/**
 * A message that is received from or can be set to a web client (via HTTP or WebSockets).
 * The message has either a {@link #getStringBody() text body} or a {@link #getByteBufferBody() binary} body (but not both).
 */
public abstract class WebMessage extends ActorMessage implements FromMessage {
    /**
     * The actor that sent this message. This can be a virtual actor representing the web client.
     */
    @Override
    public abstract ActorRef<? extends WebMessage> getFrom();

    /**
     * The message's text body, if it has one; {@code null} otherwise.
     */
    public abstract String getStringBody();

    /**
     * The message's binary body, if it has one; {@code null} otherwise.
     */
    public abstract ByteBuffer getByteBufferBody();

    @Override
    protected String contentString() {
        return super.contentString() + " from: " + getFrom();
    }
}
