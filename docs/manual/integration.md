---
layout: default
title: Comsat Integration
weight: 1
---

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

## Servlets

Comsat integrates with JavaEE servlets and enables you to write servlets that can scale to many concurrent visitors, even if servicing each requests takes a very long time. Under the hood, Comsat does this by turning each servlet request into an asynchronous request, and then services each request on a separate [fiber](http://puniverse.github.io/quasar/manual/core.html#fibers). Calls to other web services or to a database are fiber- rather than thread-blocking. As a result, comsat can serve many thousands of concurrent requests with only a handful of OS threads. You, on the other hand, don't need to adopt a cumbersome asynchronous programming model. You can write the servlet code as you normally would, making synchronous (fiber-blocking) calls, provided that you use Comsat implementations.

To write a Comsat (fiber-per-request) servlet, simply extend [`FiberHttpServlet`]({{javadoc}}/fibers/servlet/FiberHttpServlet.html) rather than the usual `javax.servlet.HttpServlet`, and either annotate it with `@WebServlet` or declare it in `web.xml`. Note how the `service` and all the `doXXX` methods are [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (they all `throw SuspendExecution`).

You can deploy your servlet as you normally would, either as a WAR file, or in an embedded servlet container.

It is recommended that you then configure your servlet container to limit the number of threads in its thread pool to some small number, as all these threads do is create the fiber (which runs in the fiber thread pool) and return.

To learn about writing servlets, you can refer to the [Java Servlets tutorial](http://docs.oracle.com/javaee/7/tutorial/doc/servlets.htm).

## REST Services

You can easily create Comsat REST services with the [JAX-RS API](https://jax-rs-spec.java.net/nonav/2.0/apidocs/index.html), the standard Java REST service API. Comsat integrates with [Jersey](https://jersey.java.net/), the reference JAX-RS implementation. 

All you need to do in order to enjoy Comsat's scalabilty, is replace the line

~~~ xml
<servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
~~~

in your `web.xml` file, which is how you would normally use Jersey in a Servlet container, with:

~~~ xml
<servlet-class>co.paralleluniverse.fibers.jersey.ServletContainer</servlet-class>
~~~

Your resource methods (the ones you annotate with `@GET`, `@PUT`, `@POST` etc.) can now be made suspendable by declaring `throws SuspendExecution`. Comsat would then run each request in a fiber. Your resource methods are free to use Comsat's JDBC implementation, or Comsat's JAX-RS client.

To learn about writing REST services with JAX-RS, please refer to the [Jersey User Guide](https://jersey.java.net/documentation/latest/user-guide.html).

## HTTP Client

Comsat's integrated HTTP client is a JAX-RS client (specifically, Jersey client). To create a client instance compatible with Quasar fibers, use the [`AsyncClientBuilder`]({{javadoc}}/fibers/ws/rs/client/AsyncClientBuilder.html) class:

~~~ java
Client client = AsyncClientBuilder.newClient();
~~~

You can also pass a configuration

~~~ java
Client client = AsyncClientBuilder.newClient(config);
~~~

or use the builder API

~~~ java
Client client = AsyncClientBuilder.newBuilder()....build();
~~~

To learn how to use the HTTP client, please refer to the [Jersey documentation](https://jersey.java.net/documentation/latest/user-guide.html#client).

{:.alert .alert-info}
**Note**: A method that makes use of the API and runs in a fiber must be declared [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (normally by declaring `throws SuspendExecution`).

## JDBC

The `comsat-jdbc` project integrates the JDBC API with applications employing Quasar fibers (or actors). To use JDBC in Quasar fibers or actors, simply wrap your database driver's `DataSource` with [`FiberDataSource`]({{javadoc}}/fibers/jdbc/FiberDataSource.html), and use it to obtain connections which you may then freely use within fibers.

{:.alert .alert-info}
**Note**: A method that makes use of the API and runs in a fiber must be declared [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (normally by declaring `throws SuspendExecution`).

Normally, Comsat transforms asynchronous (callback based) API into fiber-blocking operations. JDBC, however, has no asynchronous API. comsat-jdbc simply runs the actual thread-blocking JDBC operations in a thread pool, and blocks the calling fiber until the operation has completed execution in the thread pool. As a result, you will not get any scalability benefits by calling your database in fibers (unlike, say, calling web services), because an OS thread will still block on every JDBC call. In practice, though, it matters little, as your database is likely to be a narrower bottleneck than the OS scheduler anyway.


XXXXXXXXXX Deployment in Servlet (JNDI?)


{:.alert .alert-warning}
**Note**: Your application may only may direct use of the Comsat JDBC data source, because methods calling the API must be declared suspendable (or run on regular threads). Database access frameworks (like various ORM solutions) that make use of JDBC cannot use this data source and be used in Quasar fibers. In the future, we will provide separate integration module for some popular database access libraries.

If you want to learn how to use JDBC, the [JDBC tutorial](http://docs.oracle.com/javase/tutorial/jdbc/basics/) is a good resource.