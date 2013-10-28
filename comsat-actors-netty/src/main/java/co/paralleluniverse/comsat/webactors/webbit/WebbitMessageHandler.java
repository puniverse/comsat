/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.comsat.webactors.webbit;

import org.webbitserver.WebSocketConnection;
public interface WebbitMessageHandler {

    public void onMessage(WebSocketConnection connection, String msg) throws Throwable;

    public void onMessage(WebSocketConnection connection, byte[] msg) throws Throwable;
    
}
