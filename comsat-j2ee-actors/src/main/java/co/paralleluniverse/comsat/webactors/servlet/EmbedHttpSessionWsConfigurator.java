package co.paralleluniverse.comsat.webactors.servlet;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

public class EmbedHttpSessionWsConfigurator extends Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // save the http session based on idea from
        // http://stackoverflow.com/questions/17936440/accessing-httpsession-from-httpservletrequest-in-a-web-socket-socketendpoint
        final Object httpSession = request.getHttpSession();
        if (httpSession != null)
            sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
        super.modifyHandshake(sec, request, response);
    }
}
