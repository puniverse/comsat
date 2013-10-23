package co.paralleluniverse.comsat.webactors.servlet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Builder;

//@WebListener
public class WebSocketEndpointRegistry implements ServletContextListener {
    public static void registerEndpoint(ServletContext sc, ServerEndpointConfig.Builder sec) {
        getConfigsList(sc, true).add(sec);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        List<ServerEndpointConfig.Builder> secs = getConfigsList(sc, false);

        final ServerContainer scon = (ServerContainer) sc.getAttribute("javax.websocket.server.ServerContainer");
        if (scon != null & secs != null) {
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

    private static List<Builder> getConfigsList(ServletContext sc, final boolean create) {
        List<ServerEndpointConfig.Builder> secs = (List<ServerEndpointConfig.Builder>) sc.getAttribute("secs");
        if (secs == null & create) {
            secs = new CopyOnWriteArrayList<>();
            sc.setAttribute("secs", secs);
            sc.addListener(WebSocketEndpointRegistry.class);
        }
        return secs;
    }
}
