package co.paralleluniverse.fibers.ws.rs.client;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import javax.ws.rs.client.InvocationCallback;

abstract class AsyncRs<ResponseType> extends FiberAsync<ResponseType, InvocationCallback<ResponseType>, Void, RuntimeException> implements InvocationCallback<ResponseType> {
    private final Fiber fiber;
    
    public AsyncRs() {
        this.fiber = Fiber.currentFiber();
    }

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
        System.out.println("completed: "+response);
        super.completed(response, fiber);
    }

    @Override
    public void failed(Throwable throwable) {
        System.out.println("throwable: "+throwable);
        throwable.printStackTrace(System.out);
        super.failed(throwable, fiber);
    }
}
