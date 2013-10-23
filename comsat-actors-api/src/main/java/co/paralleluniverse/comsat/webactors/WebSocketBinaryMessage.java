package co.paralleluniverse.comsat.webactors;

import java.nio.ByteBuffer;

public interface WebSocketBinaryMessage extends WebSocketMessage {
    public ByteBuffer getMessage();
}
