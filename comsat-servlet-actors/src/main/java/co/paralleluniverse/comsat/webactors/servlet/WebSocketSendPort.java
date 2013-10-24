package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.comsat.webactors.WebSocketMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;

public class WebSocketSendPort implements SendPort<WebSocketMessage> {
    private final Session session;

    public WebSocketSendPort(Session session) {
        this.session = session;
    }

    @Override
    public void send(WebSocketMessage message) throws SuspendExecution, InterruptedException {
        trySend(message);
    }

    @Override
    public boolean send(WebSocketMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return trySend(message);
    }

    @Override
    public boolean trySend(WebSocketMessage message) {
        if (message.isString())
            session.getAsyncRemote().sendText(message.getString()); // TODO: use fiber async instead of servlet Async ?
        else
            session.getAsyncRemote().sendBinary(message.getByteBuffer());
        return true;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException ex) {
            Logger.getLogger(WebSocketSendPort.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
