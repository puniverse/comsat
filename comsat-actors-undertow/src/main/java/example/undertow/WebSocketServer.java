package example.undertow;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.handler.WebSocketConnectionCallback;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class WebSocketServer {
    public static void main(final String[] args) {
        Undertow.builder().addListener(8080, "localhost").setHandler(path().
                addPath("/myapp", websocket(new WebSocketConnectionCallback() {
                    @Override
                    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                        System.out.println("**** onConnect ********");
                        channel.getReceiveSetter().set(new AbstractReceiveListener() {
                            @Override
                            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                System.out.println("**** onFullTextMessage ********");
                                WebSockets.sendText(message.getData(), channel, null);
                            }
                        });
                    }
                })).addPath("/", new HttpHandler() {
                    
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                })).build().start();
    }
}
