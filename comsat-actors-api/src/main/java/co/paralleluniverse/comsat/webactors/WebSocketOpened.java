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
 *
 * @author pron
 */
public class WebSocketOpened implements WebMessage {
    private final ActorRef<WebDataMessage> actor;

    public WebSocketOpened(ActorRef<WebDataMessage> actor) {
        this.actor = actor;
    }
    
    @Override
    public ActorRef<WebDataMessage> sender() {
        return actor;
    }

    @Override
    public String getStringBody() {
        return null;
    }

    @Override
    public ByteBuffer getByteBufferBody() {
        return null;
    }
}
