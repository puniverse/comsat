package co.paralleluniverse.comsat.webactors.webbit;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.WebDataMessage;
import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.WebSocketHandler;

public abstract class WebbitWebSocketHandler implements WebSocketHandler {
    static final String ACTOR_KEY = "co.paralleluniverse.actor";
    public static final String MH_KEY = "co.paralleluniverse.mh";

    @Override
    public final void onMessage(WebSocketConnection connection, String msg) throws Throwable {
        WebbitMessageHandler mh = (WebbitMessageHandler) connection.data(MH_KEY);
        if (mh != null)
            mh.onMessage(connection, msg);
    }

    @Override
    public final void onMessage(WebSocketConnection connection, byte[] msg) throws Throwable {
        WebbitMessageHandler mh = (WebbitMessageHandler) connection.data(MH_KEY);
        if (mh != null)
            mh.onMessage(connection, msg);
    }

    public void attachWebSocket(final WebSocketConnection connection, final ActorRef<Object> actor) {
        connection.data(ACTOR_KEY, actor);
        connection.data(MH_KEY, new WebbitMessageHandler() {
            final SendPort<String> stringPort = new WebbitWebSocketStringPort(connection);
            final SendPort<ByteBuffer> binaryPort = new WebbitWebSocketBinaryPort(connection);
            final SendPort<WebDataMessage> sp = new WebbitWebSocketPort(connection);

            @Override
            public void onMessage(WebSocketConnection conn, final String msg) throws Throwable {
                actor.send(new WebDataMessage(null, msg));
            }

            @Override
            public void onMessage(WebSocketConnection conn, final byte[] msg) throws Throwable {
                actor.send(new WebDataMessage(null, ByteBuffer.wrap(msg))); // TODO: copy
            }
        });
    }

    @Override
    public void onClose(WebSocketConnection connection) throws Throwable {
    }

    @Override
    public void onPing(WebSocketConnection connection, byte[] msg) throws Throwable {
    }

    @Override
    public void onPong(WebSocketConnection connection, byte[] msg) throws Throwable {
    }
}
