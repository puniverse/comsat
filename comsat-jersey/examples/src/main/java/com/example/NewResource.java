package com.example;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("newresource")
public class NewResource {
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String doWorkResource(@QueryParam("sleep") int sleepTime) throws SuspendExecution {
        return doWork(sleepTime);
    }

    String doWork(int sleepTime) throws SuspendExecution {
        try {
            Strand.sleep(sleepTime);
            return "finished sleeping " + sleepTime;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}