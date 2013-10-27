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
package co.paralleluniverse.fibers.ws.rs.client;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import javax.ws.rs.client.InvocationCallback;

abstract class AsyncRs<ResponseType> extends FiberAsync<ResponseType, Void, RuntimeException> implements InvocationCallback<ResponseType> {
    @Override
    public ResponseType run() throws SuspendExecution {
        try {
            final ResponseType run = super.run();
            return run;
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void completed(ResponseType response) {
        asyncCompleted(response);
    }

    @Override
    public void failed(Throwable throwable) {
        asyncFailed(throwable);
    }
}
