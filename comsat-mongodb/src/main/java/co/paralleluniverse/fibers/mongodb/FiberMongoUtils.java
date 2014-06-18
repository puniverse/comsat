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
import com.allanbank.mongodb.Callback;

/**
 * Utility classes for 
 * @author dev
 */
public class FiberMongoUtils {

    /**
     * Builds a new Async Java Mongo Driver callback that will set a future upon completion or failure
     * @param <T>
     * @param future
     * @return
     */
    public static <T> Callback<T> callbackSettingFuture(final SettableFuture<T> future) {
        return new Callback<T>() {

            @Override
            public void callback(T v) {
                future.set(v);
            }

            @Override
            public void exception(Throwable thrwbl) {
                future.setException(thrwbl);
            }
        };
    }
}
