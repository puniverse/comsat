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
package co.paralleluniverse.fibers.okhttp;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author circlespainter
 */
public class FiberOkHttpTestUtil {
    public static RecordedResponse executeInFiberRecorded(final FiberOkHttpClient client, final Request request) throws InterruptedException, IOException, ExecutionException {
        final Response response = FiberOkHttpUtil.executeInFiber(client.newCall(request));
        return new RecordedResponse(request, response, null, response.body().string(), null);
    }

    private FiberOkHttpTestUtil() {
    }    
}
