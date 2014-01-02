package co.paralleluniverse.comsat.webactors.webbit;

import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.TimeUnit;
import org.webbitserver.WebSocketConnection;

class WebbitWebSocketPort implements SendPort<WebDataMessage> {

    public WebbitWebSocketPort(WebSocketConnection connection) {
        this.connection = connection;
    }

    private final WebSocketConnection connection;

    @Override
    public void send(WebDataMessage message) throws SuspendExecution, InterruptedException {
        trySend(message);
    }

    @Override
    public boolean send(WebDataMessage message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return trySend(message);
    }

    @Override
    public boolean send(WebDataMessage message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return trySend(message);
    }

    @Override
    public boolean trySend(WebDataMessage message) {
        if (!message.isBinary())
            connection.send(message.getStringBody());
        else 
            connection.send(message.getByteBufferBody().array());
        return true;
    }

    @Override
    public void close() {
        connection.close();
    }
}
