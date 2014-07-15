/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.embedded.containers;

import java.io.IOException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClients;

public abstract class AbstractEmbeddedServer implements EmbeddedServer {
    protected int port = 8080;
    protected int nThreads = 100;
    protected int maxConn = 1000;

    @Override
    public EmbeddedServer setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public EmbeddedServer setNumThreads(int nThreads) {
        this.nThreads = nThreads;
        return this;
    }

    @Override
    public EmbeddedServer setMaxConnections(int maxConn) {
        this.maxConn = maxConn;
        return this;
    }

    public static void waitUrlAvailable(final String url) throws InterruptedException, IOException {
        for (;;) {
            Thread.sleep(10);
            try {
                if (HttpClients.createDefault().execute(new HttpGet(url)).getStatusLine().getStatusCode() > -100)
                    break;
            } catch (HttpHostConnectException ex) {
            }
        }
    }
}
