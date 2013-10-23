package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;

class ServletWebSocketStringPort implements SendPort<String> {
    private final Session session;

    public ServletWebSocketStringPort(Session session) {
        this.session = session;
    }

    @Override
    public void send(String message) throws SuspendExecution, InterruptedException {
        session.getAsyncRemote().sendText(message); // TODO: use fiber async instead of servlet Async ?
    }

    @Override
    public boolean send(String message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    @Override
    public boolean trySend(String message) {
        session.getAsyncRemote().sendText(message);
        return true;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException ex) {
            Logger.getLogger(ServletWebSocketStringPort.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
