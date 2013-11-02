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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;

class WebSocketChannel implements SendPort<WebDataMessage> {
    private final Session session;

    public WebSocketChannel(Session session) {
        this.session = session;
    }

    @Override
    public void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
        trySend(message);
    }

    @Override
    public boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return trySend(message);
    }

    @Override
    public boolean trySend(WebDataMessage message) {
        if (!session.isOpen())
            return false;
        if (!message.isBinary())
            session.getAsyncRemote().sendText(message.getStringBody()); // TODO: use fiber async instead of servlet Async ?
        else
            session.getAsyncRemote().sendBinary(message.getByteBufferBody());
        return true;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException ex) {
            Logger.getLogger(WebSocketChannel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
