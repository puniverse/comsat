package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;

public interface WebSocketMessage extends WebMessage {
    public SendPort<String> getSenderStringPort();
    public SendPort<ByteBuffer> getSenderBinaryPort();
}
