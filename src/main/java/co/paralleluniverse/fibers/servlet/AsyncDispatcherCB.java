/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;

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
            Log.getLogger(AsyncDispatcherCB.class).info("run");
            final Void run = super.run();
            Log.getLogger(AsyncDispatcherCB.class).info("afterrun");
            return run;
        } catch (InterruptedException e) {
            Log.getLogger(AsyncDispatcherCB.class).info(e);
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }

    @Override
    public void onComplete(AsyncEvent event) {
        Log.getLogger(AsyncDispatcherCB.class).info("completed");
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
        Log.getLogger(AsyncDispatcherCB.class).info("timeout");
        super.failed(new TimeoutException(), fiber);
    }

    @Override
    public void onError(AsyncEvent event) {
        Log.getLogger(AsyncDispatcherCB.class).info("error");
        super.failed(event.getThrowable(), fiber);
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
        event.getAsyncContext().addListener(this);
        Log.getLogger(AsyncDispatcherCB.class).info("startAsync");
        // do nothing;
    }
}
