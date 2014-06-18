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

import com.allanbank.mongodb.Callback;

import co.paralleluniverse.fibers.FiberAsync;

/**
 * Base class for async-to-fiber-blocking Async Java Mongo Driver transformations
 * 
 * @author circlespainter
 * @param <T>
 */
public abstract class FiberMongoCallback<T> extends FiberAsync<T, Throwable> implements Callback<T> {
    @Override
    public void callback(T success) {
        asyncCompleted(success);
    }

    @Override
    public void exception(Throwable failure) {
        asyncFailed(failure);
    }
}
