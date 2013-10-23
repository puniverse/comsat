package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;

class ServletWebSocketBinaryPort implements SendPort<ByteBuffer> {
    private final Session session;

    public ServletWebSocketBinaryPort(Session session) {
        this.session = session;
    }

    @Override
    public void send(ByteBuffer message) throws SuspendExecution, InterruptedException {
        session.getAsyncRemote().sendBinary(message); // TODO: use fiber async instead of servlet Async ?
    }

    @Override
    public boolean send(ByteBuffer message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    @Override
    public boolean trySend(ByteBuffer message) {
        session.getAsyncRemote().sendBinary(message);
        return true;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException ex) {
            Logger.getLogger(ServletWebSocketBinaryPort.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
