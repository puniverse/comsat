/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.jersey;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

// snippet REST resource example
@Singleton
@Path("/service")
public class TestResource {
    @GET
    @Produces("text/plain")
    @Suspendable  // <------------- FIBER
    public String get(@QueryParam("sleep") int sleep) throws IOException, SuspendExecution, InterruptedException {
        Fiber.sleep(sleep); // <--- you may use fiber blocking calls here
        return "sleep was "+sleep;
    }
    // snippet_exclude_begin
    @POST
    @Produces("text/plain")
    @Suspendable  // <------------- FIBER
    public String post(@QueryParam("sleep") int sleep) throws IOException, SuspendExecution, InterruptedException {
        return get(sleep);
    }
    // snippet_exclude_end
}
// end of snippet
