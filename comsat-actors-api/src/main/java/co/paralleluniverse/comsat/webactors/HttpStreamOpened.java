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
import co.paralleluniverse.actors.behaviors.IdMessage;
import java.nio.ByteBuffer;

/**
 * A message sent by a virtual actor representing an open HTTP stream signifying that an HTTP stream has been opened.
 */
public class HttpStreamOpened extends WebMessage implements IdMessage {
    private final ActorRef<WebDataMessage> actor;
    private final HttpResponse response;

    public HttpStreamOpened(ActorRef<WebDataMessage> actor, HttpResponse response) {
        this.actor = actor;
        this.response = response;
    }

    /**
     * {@inheritDoc }
     * <p/>
     * Returns the actor to be used for sending {@link WebDataMessage}s over the stream.
     */
    @Override
    public ActorRef<WebDataMessage> getFrom() {
        return actor;
    }

    /**
     * {@inheritDoc }
     * <p/>
     * Returns the {@link HttpResponse} passed to the constructor.
     */
    @Override
    public Object getId() {
        return response;
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
