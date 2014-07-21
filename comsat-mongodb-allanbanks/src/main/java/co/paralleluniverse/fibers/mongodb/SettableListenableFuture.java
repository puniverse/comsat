/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.mongodb;

import co.paralleluniverse.strands.SettableFuture;
import com.allanbank.mongodb.ListenableFuture;
import com.allanbank.mongodb.client.FutureCallback;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Adds ListenableFuture support to SettableFuture through a delegate
 * @author circlespainter
 */
public class SettableListenableFuture<V> extends SettableFuture<V> implements ListenableFuture<V> {
    final private FutureCallback<V> delegate = new FutureCallback<>();

    @Override
    public boolean set(V value) {
        boolean ret = super.set(value);
        delegate.callback(value);
        return ret;
    }
    
    @Override
    public boolean setException (Throwable exception) {
        boolean ret = super.setException(exception);
        delegate.exception(exception);
        return ret;
    }
    
    @Override
    public void addListener(Runnable r, Executor exctr) throws RejectedExecutionException, IllegalArgumentException {
        delegate.addListener(r, exctr);
    }
    
}
