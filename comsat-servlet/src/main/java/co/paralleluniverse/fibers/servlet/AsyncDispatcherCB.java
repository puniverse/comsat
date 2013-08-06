/**
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
package co.paralleluniverse.fibers.servlet;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

/**
 *
 * @author eitan
 */
abstract  class AsyncDispatcherCB extends FiberAsync<Void, AsyncListener, Void, ServletException> implements AsyncListener {
    private final Fiber fiber;

    public AsyncDispatcherCB() {
        this.fiber = Fiber.currentFiber();
    }

    @Override
    public Void run() throws ServletException, SuspendExecution {
        try {
            final Void run = super.run();
            return run;
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onComplete(AsyncEvent event) {
        ServletResponse response = event.getAsyncContext().getResponse();
        ServletResponse original = ((ServletResponseWrapper)response).getResponse();
        try {
            original.getWriter().print(response.toString());
        } catch (IOException ex) {
            Logger.getLogger(AsyncDispatcherCB.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.completed(null, fiber);
    }

    @Override
    public void onTimeout(AsyncEvent event) {
        super.failed(new TimeoutException(), fiber);
    }

    @Override
    public void onError(AsyncEvent event) {
        super.failed(event.getThrowable(), fiber);
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
        event.getAsyncContext().addListener(this);
    }
}
