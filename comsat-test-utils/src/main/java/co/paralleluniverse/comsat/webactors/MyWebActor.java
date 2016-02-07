/*
 * COMSAT
 * Copyright (C) 2014-2016, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.actors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.base.Function;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MyWebActor extends BasicActor<WebMessage, Void> {
    // There is one actor for each client
    private static final Set<ActorRef<WebMessage>> actors =
        Collections.newSetFromMap(new ConcurrentHashMap<ActorRef<WebMessage>, Boolean>());

    // The client representation of this actor
    private SendPort<WebDataMessage> peer;

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        actors.add(self());
        try {
            //noinspection InfiniteLoopStatement
            for (;;) {
                final Object message = receive();
                if (message instanceof HttpRequest) {
                    final HttpRequest msg = (HttpRequest) message;
                    switch (msg.getRequestURI()) {
                        case "/":
                            msg.getFrom().send(HttpResponse.ok(self(), msg, "httpResponse").setContentType("text/html").build());
                            break;
                        case "/notfound":
                            msg.getFrom().send(HttpResponse.error(self(), msg, 404, "Not found").setContentType("text/plain").build());
                            break;
                        case "/die":
                            throw new RuntimeException("die");
                        case "/redirect":
                            msg.getFrom().send(HttpResponse.redirect(msg, "/foo").build());
                            break;
                        case "/ssepublish":
                            postMessage(new WebDataMessage(self(), msg.getStringBody()));
                            msg.getFrom().send(HttpResponse.ok(self(), msg, "").build());
                            break;
                        case "/ssechannel":
                            msg.getFrom().send(SSE.startSSE(self(), msg).build());
                            break;
                    }
                } // -------- WebSocket/SSE opened --------
                else if (message instanceof WebStreamOpened) {
                    final WebStreamOpened msg = (WebStreamOpened) message;
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
            public final WebDataMessage apply(WebDataMessage f) {
                return new WebDataMessage(f.getFrom(), SSE.event(f.getStringBody()));
            }
        });
    }

    private void postMessage(final WebDataMessage webDataMessage) throws InterruptedException, SuspendExecution {
        if (peer != null)
            peer.send(webDataMessage);
        if (webDataMessage.getFrom().equals(peer))
            for (final SendPort actor : actors)
                if (actor != self())
                    //noinspection unchecked
                    actor.send(webDataMessage);
    }

    @Override
    protected final WebMessage handleLifecycleMessage(LifecycleMessage m) {
        // while listeners might contain an SSE actor wrapped with Channels.map, the wrapped SendPort maintains the original actors hashCode and equals behavior
        if (m instanceof ExitMessage)
            actors.remove(((ExitMessage) m).getActor());
        return super.handleLifecycleMessage(m);
    }
}
