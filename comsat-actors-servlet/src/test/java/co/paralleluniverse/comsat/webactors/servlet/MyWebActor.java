/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.actors.*;
import co.paralleluniverse.comsat.webactors.*;
import static co.paralleluniverse.comsat.webactors.HttpResponse.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.base.Function;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebActor(httpUrlPatterns = {"/*"}, webSocketUrlPatterns = {"/ws"})
public class MyWebActor extends BasicActor<Object, Void> {
    // There is one actor for each client
    private static final Set<ActorRef<Object>> actors = Collections.newSetFromMap(new ConcurrentHashMap<ActorRef<Object>, Boolean>());

    // The client representation of this actor
    private SendPort<WebDataMessage> peer;

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        actors.add(self());
        try {
            for (;;) {
                Object message = receive();
                if (message instanceof HttpRequest) {
                    HttpRequest msg = (HttpRequest) message;
                    switch (msg.getRequestURI()) {
                        case "/":
                            msg.getFrom().send(ok(self(), msg, "httpResponse")
                                    .setContentType("text/html").build());
                            break;
                        case "/ssepublish":
                            postMessage(new WebDataMessage(self(), msg.getStringBody()));
                            msg.getFrom().send(ok(self(), msg, "").build());
                            break;
                        case "/ssechannel":
                            msg.getFrom().send(SSE.startSSE(self(), msg).build());
                            break;

                    }
                } // -------- WebSocket/SSE opened -------- 
                else if (message instanceof WebStreamOpened) {
                    WebStreamOpened msg = (WebStreamOpened) message;
                    watch(msg.getFrom()); // will call handleLifecycleMessage with ExitMessage when the session ends

                    SendPort<WebDataMessage> p = msg.getFrom();
                    if (msg instanceof HttpStreamOpened)
                        p = wrapAsSSE(p);
                    this.peer = p;

//                    p.send(new WebDataMessage(self(), "Welcome. " + actors.size() + " listeners"));
                } // -------- WebSocket message received -------- 
                else if (message instanceof WebDataMessage) {
                    postMessage((WebDataMessage) message);
                }
            }
        } finally {
            actors.remove(self());
        }
    }

    private SendPort<WebDataMessage> wrapAsSSE(SendPort<WebDataMessage> actor) {
        return Channels.mapSend(actor, new Function<WebDataMessage, WebDataMessage>() {
            @Override
            public WebDataMessage apply(WebDataMessage f) {
                return new WebDataMessage(f.getFrom(), SSE.event(f.getStringBody()));
            }
        });
    }

    private void postMessage(final WebDataMessage webDataMessage) throws InterruptedException, SuspendExecution {
        if (peer != null)
            peer.send(webDataMessage);
        if (webDataMessage.getFrom().equals(peer))
            for (SendPort actor : actors)
                if (actor != self())
                    actor.send(webDataMessage);
    }

    @Override
    protected Object handleLifecycleMessage(LifecycleMessage m) {
        // while listeners might contain an SSE actor wrapped with Channels.map, the wrapped SendPort maintains the original actors hashCode and equals behavior
        if (m instanceof ExitMessage)
            actors.remove(((ExitMessage) m).getActor());
        return super.handleLifecycleMessage(m);
    }
}
