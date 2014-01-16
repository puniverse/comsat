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

/**
 * A message sent to a web actor indicating that a new stream has been opened.
 * This message is sent as a result of the web actor replying to an {@link HttpRequest}
 * with an {@link HttpResponse} whose {@link HttpResponse.Builder#startActor() startActor} method has been called.
 * The actor sending this message writes all received {@link WebDataMessage} to the HTTP response stream that's been left
 * open after the {@link HttpResponse}.
 * <p/>
 * This is usually used by SSE.
 */
public class HttpStreamOpened extends WebStreamOpened implements IdMessage {
    private final HttpResponse response;

    public HttpStreamOpened(ActorRef<WebDataMessage> actor, HttpResponse response) {
        super(actor);
        this.response = response;
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
}
