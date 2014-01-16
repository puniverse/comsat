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

/**
 * A message sent to a web actor indicating that a new WebSocket has been opened by the client.
 */
public class WebSocketOpened extends WebStreamOpened {
    public WebSocketOpened(ActorRef<WebDataMessage> actor) {
        super(actor);
    }
}
