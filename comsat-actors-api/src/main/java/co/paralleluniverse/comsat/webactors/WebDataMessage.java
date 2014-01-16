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
 * A message that can be received from or sent to a web client, and contains only data (and no metadata like headers).
 */
public class WebDataMessage extends WebMessage {
    private final ActorRef<WebDataMessage> sender;
    private final String string;
    private final ByteBuffer byteBuffer;

    /**
     * Constructs a {@code WebDataMessage} with a text body.
     * @param from the message sender
     * @param body the message body
     */
    public WebDataMessage(ActorRef<? super WebDataMessage> from, String body) {
        this.sender = (ActorRef<WebDataMessage>)from;
        this.string = body;
        this.byteBuffer = null;
    }

    /**
     * Constructs a {@code WebDataMessage} with a binary body.
     * @param from the message sender
     * @param body the message body
     */
    public WebDataMessage(ActorRef<? super WebDataMessage> from, ByteBuffer body) {
        this.sender = (ActorRef<WebDataMessage>)from;
        this.string = null;
        this.byteBuffer = body;
    }

    @Override
    public ActorRef<WebDataMessage> getFrom() {
        return sender;
    }

    /**
     * Whether this is a binary message or a text message.
     * @return {@code true} if this is a binary message; {@code false} if this is a text mesasge.
     */
    public boolean isBinary() {
        return (byteBuffer != null);
    }

    @Override
    public String getStringBody() {
        return string;
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        return byteBuffer;
    }

    @Override
    protected String contentString() {
        return super.contentString() + 
                " size: " + (string != null ? string.length() : byteBuffer != null ? byteBuffer.remaining() : 0) +
                (isBinary() ? "" : " data: " + string);
    }
}
