/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.jdbc;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.util.concurrent.ExecutorService;

/**
 * @author circlespainter
 */
public abstract class JDBCFiberAsync<V, E extends Throwable> extends FiberAsync<V, E> {
    @Suspendable
    public static <V extends Object, E extends Exception> V exec(ExecutorService es, CheckedCallable<V, E> cc) throws E {
        try {
            return runBlocking(es, cc);
        } catch (final SuspendExecution se) {
            throw new AssertionError(se);
        } catch (final InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
