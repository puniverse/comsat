package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.WebActor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public class WebActorEndPoint extends Endpoint {
    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        ActorRef<Object> actor = getHttpSessionActor(config);
        if (actor != null) {
            ServletWebActors.attachWebSocket(session, actor);
        } else {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "session actor not found"));
            } catch (IOException ex) {
                Logger.getLogger(WebActorEndPoint.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private ActorRef<Object> getHttpSessionActor(EndpointConfig config) {
        HttpSession httpSession = getHttpSession(config);
        if (httpSession == null)
            throw new RuntimeException("HttpSession hasn't been embedded by the EndPoint Configurator.");
        return (ActorRef<Object>) httpSession.getAttribute(WebActor.ACTOR_KEY);
    }

    private HttpSession getHttpSession(EndpointConfig config) {
        return (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
    }
}
