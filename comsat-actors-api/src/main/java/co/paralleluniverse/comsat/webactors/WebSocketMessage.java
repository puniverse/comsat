/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
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
