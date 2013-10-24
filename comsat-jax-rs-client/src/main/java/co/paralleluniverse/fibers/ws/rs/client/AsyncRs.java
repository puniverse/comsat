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
        System.out.println("completed: "+response);
        asyncCompleted(response);
    }

    @Override
    public void failed(Throwable throwable) {
        System.out.println("throwable: "+throwable);
        throwable.printStackTrace(System.out);
        asyncFailed(throwable);
    }
}
