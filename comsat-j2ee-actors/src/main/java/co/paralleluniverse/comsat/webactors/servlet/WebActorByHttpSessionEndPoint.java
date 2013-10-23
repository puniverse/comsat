package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.servlet.ServletWebActors;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public class WebActorByHttpSessionEndPoint extends Endpoint {
    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession!=null) {
            ActorRef<Object> actor = (ActorRef<Object>) httpSession.getAttribute("actor");
            ServletWebActors.attachWebSocket(session, actor);
        } else {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "session actor not found"));
            } catch (IOException ex) {
                Logger.getLogger(WebActorByHttpSessionEndPoint.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
