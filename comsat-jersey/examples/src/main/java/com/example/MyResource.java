package com.example;

import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getIt() throws SuspendExecution, InterruptedException {
        System.out.println("=== bef: "+Thread.currentThread() +" "+
                Fiber.currentFiber() +" "+ ThreadUtil.getThreadLocalsString());
        Strand.sleep(100);
        System.out.println("=== aft: "+Thread.currentThread() +" "+
                Fiber.currentFiber() +" "+ ThreadUtil.getThreadLocalsString());
        URL url = Resources.getResource("index.html");
        try {
            String text = Resources.toString(url, Charsets.UTF_8);
            return text;
        } catch (IOException ex) {
            return "error " + ex;
        }
    }

    @POST
    @Path("/add")
    public Response addUser(
            @FormParam("name") String name,
            @FormParam("age") int age) throws SuspendExecution, InterruptedException {
        Strand.sleep(100);

        return Response.status(200)
                .entity("addUser is called, name : " + name + ", age : " + age)
                .build();

    }
}