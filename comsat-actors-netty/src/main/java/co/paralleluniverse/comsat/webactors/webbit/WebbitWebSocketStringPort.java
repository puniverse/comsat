package co.paralleluniverse.comsat.webactors.webbit;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.webbitserver.WebSocketConnection;

class WebbitWebSocketStringPort implements SendPort<String> {
    private final WebSocketConnection connection;

    public WebbitWebSocketStringPort(WebSocketConnection connection) {
        this.connection = connection;
    }

    @Override
    public void send(String message) throws SuspendExecution, InterruptedException {
        connection.send(message);
    }

    @Override
    public boolean send(String message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    @Override
    public boolean trySend(String message) {
        try {
            send(message);
        } catch (SuspendExecution | InterruptedException ex) {
            Logger.getLogger(WebbitWebSocketStringPort.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    @Override
    public void close() {
        connection.close();
    }
}
