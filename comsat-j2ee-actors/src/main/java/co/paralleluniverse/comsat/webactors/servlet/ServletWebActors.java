package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.comsat.webactors.WebSocketBinaryMessage;
import co.paralleluniverse.comsat.webactors.WebSocketStringMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class ServletWebActors {
    public static void attachHttpSession(HttpSession session, ActorRef<Object> actor) {
        session.setAttribute("actor", actor);
    }

    public static void attachWebSocket(final Session session, final ActorRef<Object> actor) {
        if (session.getUserProperties().containsKey(WebActor.ACTOR_KEY))
            throw new RuntimeException("Session is already attached to an actor.");
        session.getUserProperties().put(WebActor.ACTOR_KEY, actor);
        // TODO: register the handler in order to enable detach
        final ServletWebSocketBinaryPort binaryPort = new ServletWebSocketBinaryPort(session);
        final ServletWebSocketStringPort stringPort = new ServletWebSocketStringPort(session);
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(final ByteBuffer message) {
                try {
                    actor.send(new WebSocketBinaryMessage() {
                        @Override
                        public ByteBuffer getMessage() {
                            return message;
                        }

                        @Override
                        public SendPort<String> getSenderStringPort() {
                            return stringPort;
                        }

                        @Override
                        public SendPort<ByteBuffer> getSenderBinaryPort() {
                            return binaryPort;
                        }
                    });
                } catch (SuspendExecution ex) {
                    Logger.getLogger(WebActor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(final String message) {
                try {
                    actor.send(new WebSocketStringMessage() {
                        @Override
                        public String getMessage() {
                            return message;
                        }

                        @Override
                        public SendPort<String> getSenderStringPort() {
                            return stringPort;
                        }

                        @Override
                        public SendPort<ByteBuffer> getSenderBinaryPort() {
                            return binaryPort;
                        }
                    });
                } catch (SuspendExecution ex) {
                    Logger.getLogger(WebActor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

    }

    public static void detachWebSocket(final Session session) {
        WebActor get = (WebActor) session.getUserProperties().get(WebActor.ACTOR_KEY);
        if (get != null) {
            session.getUserProperties().remove(WebActor.ACTOR_KEY);
//            session.removeMessageHandler(null);
        }
    }

    public static boolean isHttpAttached(HttpSession session) {
        return (session.getAttribute("actor") != null);
    }
}
