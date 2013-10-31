---
layout: default
title: Comsat Integration
weight: 1
---

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

## Servlets



## REST Services

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

## JDBC

The `comsat-jdbc` project integrates the JDBC API with applications employing Quasar fibers (or actors). To use JDBC in Quasar fibers or actors, simply wrap your database driver's `DataSource` with [`FiberDataSource`]({{javadoc}}/fibers/jdbc/FiberDataSource.html), and use it to obtain connections which you may then freely use within fibers.

Normally, Comsat transforms asynchronous (callback based) API into fiber-blocking operations. JDBC, however, has no asynchronous API. comsat-jdbc simply runs the actual thread-blocking JDBC operations in a thread pool, and blocks the calling fiber until the operation has completed execution in the thread pool. As a result, you will not get any scalability benefits by calling your database in fibers (unlike, say, calling web services), because an OS thread will still block on every JDBC call. In practice, though, it matters little, as your database is likely to be a narrower bottleneck than the OS scheduler anyway.

{:.alert .alert-warning}
**Note**: Your application may only may direct use of the Comsat JDBC data source, because methods calling the API must be declared suspendable (or run on regular threads). Database access frameworks (like various ORM solutions) that make use of JDBC cannot use this data source and be used in Quasar fibers. In the future, we will provide separate integration module for some popular database access libraries.

If you want to learn how to use JDBC, the [JDBC tutorial](http://docs.oracle.com/javase/tutorial/jdbc/basics/) is a good resource.