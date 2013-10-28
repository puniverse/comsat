package co.paralleluniverse.comsat.webactors.webbit;

import co.paralleluniverse.comsat.webactors.WebSocketMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.TimeUnit;
import org.webbitserver.WebSocketConnection;

class WebbitWebSocketPort implements SendPort<WebSocketMessage> {

    public WebbitWebSocketPort(WebSocketConnection connection) {
        this.connection = connection;
    }

    private final WebSocketConnection connection;

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
            connection.send(message.getString());
        else 
            connection.send(message.getByteBuffer().array());
        return true;
    }

    @Override
    public void close() {
        connection.close();
    }
}
