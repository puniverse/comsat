package co.paralleluniverse.comsat.webactors.webbit;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.webbitserver.WebSocketConnection;

class WebbitWebSocketBinaryPort implements SendPort<ByteBuffer> {
    private final WebSocketConnection connection;

    public WebbitWebSocketBinaryPort(WebSocketConnection connection) {
        this.connection = connection;
    }

    @Override
    public void send(ByteBuffer message) throws SuspendExecution, InterruptedException {
        connection.send(message.array());
    }

    @Override
    public boolean send(ByteBuffer message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    @Override
    public boolean send(ByteBuffer message, Timeout timeout) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    @Override
    public boolean trySend(ByteBuffer message) {
        try {
            send(message);
        } catch (SuspendExecution | InterruptedException ex) {
            Logger.getLogger(WebbitWebSocketBinaryPort.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public void close(Throwable t) {
        connection.close();
    }
}
