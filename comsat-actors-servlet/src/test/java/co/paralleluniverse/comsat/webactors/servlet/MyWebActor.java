package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import static co.paralleluniverse.comsat.webactors.Cookie.*;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import static co.paralleluniverse.comsat.webactors.HttpResponse.*;
import co.paralleluniverse.comsat.webactors.HttpStreamOpened;
import co.paralleluniverse.comsat.webactors.SSE;
import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.comsat.webactors.WebStreamOpened;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.base.Function;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

@WebActor(httpUrlPatterns = {"", "/send1", "/send2", "/sse"}, webSocketUrlPatterns = {"/ws"})
public class MyWebActor extends BasicActor<Object, Void> {

    private boolean initialized;
    private final List<SendPort<WebDataMessage>> listeners = new ArrayList<>();
    private int i;

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        if (!initialized) { // necessary for hot code swapping, as this method might be called again after swap
            this.i = 1;
            this.initialized = true;
        }

        for (;;) {
            Object message = receive(5000, TimeUnit.MILLISECONDS);
            if (message instanceof HttpRequest) {
                HttpRequest msg = (HttpRequest) message;
                // -------- plain HTTP request -------- 
                if (!msg.getRequestURI().endsWith("/sse")) {
                    msg.getFrom().send(ok(self(), msg, "httpResponse")
//                            .setContentType("text/html")
                            .addCookie(cookie("userCookie", "value").build()).build());
                } // -------- request for SSE -------- 
                else {
                    msg.getFrom().send(SSE.startSSE(self(), msg).build());
                }
            } // -------- WebSocket/SSE opened -------- 
            else if (message instanceof WebStreamOpened) {
                WebStreamOpened msg = (WebStreamOpened) message;
                watch(msg.getFrom()); // will call handleLifecycleMessage with ExitMessage when the session ends

                SendPort<WebDataMessage> p = msg.getFrom();
                if (msg instanceof HttpStreamOpened)
                    p = wrapAsSSE(p);
                listeners.add(p);
                p.send(new WebDataMessage(self(), "Welcome. " + listeners.size() + " listeners"));
            } // -------- WebSocket message received -------- 
            else if (message instanceof WebDataMessage) {
                WebDataMessage msg = (WebDataMessage) message;
                if (!msg.isBinary()) {
                    for (SendPort listener : listeners)
                        listener.send(new WebDataMessage(self(), "local counter:" + i + " data:" + msg.getStringBody().toUpperCase()));
                }
            } // -------- Timeout -------- 
            else if (message == null) {
                for (SendPort listener : listeners)
                    listener.send(new WebDataMessage(self(), "local counter:" + i + " no data. "));
            }
            i++;

            checkCodeSwap();
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

    @Override
    protected Object handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ExitMessage) {
            // while listeners might contain an SSE actor wrapped with Channels.map, the wrapped SendPort maintains the original actors hashCode and equals behavior
            ExitMessage em = (ExitMessage) m;
            System.out.println("Actor " + em.getActor() + " has died.");
            boolean res = listeners.remove(em.getActor());
            System.out.println((res ? "Successfuly" : "Unsuccessfuly") + " removed listener for actor " + em.getActor());
        }
        return super.handleLifecycleMessage(m);
    }
}
