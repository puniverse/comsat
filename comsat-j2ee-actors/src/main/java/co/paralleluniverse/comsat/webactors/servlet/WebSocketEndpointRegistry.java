package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.comsat.webactors.servlet.EmbedHttpSessionWsConfigurator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

//@WebListener
public class WebSocketEndpointRegistry implements ServletContextListener {
    private static final List<ServerEndpointConfig.Builder> secs = new CopyOnWriteArrayList<>();

    public static void registerEndpoint(ServerEndpointConfig.Builder sec) {
        secs.add(sec);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        final ServerContainer scon = (ServerContainer) sc.getAttribute("javax.websocket.server.ServerContainer");
        if (scon != null) {
            for (ServerEndpointConfig.Builder sec : secs) {
                try {
                    scon.addEndpoint(sec.configurator(new EmbedHttpSessionWsConfigurator()).build());
                } catch (DeploymentException ex) {
                    Logger.getLogger(WebSocketEndpointRegistry.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
