package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;

public class WebSocketMessage implements WebResponse, WebMessage {
    private final String string;
    private final ByteBuffer byteBuffer;

    @Override
    public SendPort<WebSocketMessage> sender() {
        return null;
    }

    public WebSocketMessage(String str) {
        this.string = str;
        this.byteBuffer = null;
    }

    public WebSocketMessage(ByteBuffer bb) {
        this.string = null;
        this.byteBuffer = bb;
    }

    public String getString() {
        return string;
    }

    public boolean isString() {
        return (string != null);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }
}
