---
layout: default
---

# Overview

Comsat is a set of open source libraries that integrate [Quasar](http://puniverse.github.io/quasar/) with various web or enterprise technologies (like HTTP services and database access). With Comsat, you can write web applications that are scalable and performant while, at the same time, are simple to code and maintain.

Comsat is not a web framework. In fact, it does not add new APIs at all (with one exception, Web Actors, mentioned later). It provides implementation to popular (and often, standard) APIs like Servlet, JAX-RS, and JDBC, that can be called within Quasar fibers. 

Comsat does provide one new API that you may choose to use: [Web Actors](manual/webactors.html). Web actors let you define a Quasar actor that receives and respnds to HTTP requests and web socket messages. The Web Actors API is rather minimal, and is intended to do one job and do it well: simplify two-way communication between your server and the client.

[Parallel Universe]: http://paralleluniverse.co

## News

### October 15, 2013

Quasar 0.3.0 has been released.

A [new spaceships demo](https://github.com/puniverse/spaceships-demo) showcases Quasar's (and SpaceBase's) abilities.

### July 19, 2013

Quasar/Pulsar 0.2.0 [has been released](http://blog.paralleluniverse.co/post/55876031297/quasar-pulsar-0-2-0-distributed-actors-supervisors).

### May 2, 2013

Introductory blog post: [Erlang (and Go) in Clojure (and Java), Lightweight Threads, Channels and Actors for the JVM](<http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar>). 

# Getting Started

### System requirements

Java 7 and is required to use Comsat.

### Using Leiningen {#lein}

Add the following dependency to Maven:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>ARTIFACT</artifactId>
    <version>0.1.0</version>
</dependency>
~~~

Where `ARTIFACT` is:

* `comsat-servlet` – Servlet integration for defining fiber-per-request servlets.
* `comsat-jersey-server` – Jersey server integration for defining REST services.
* `comsat-jax-rs-client` – JAX-RS client integration for calling HTTP services.
* `comsat-jdbc` – JDBC integration for using the JDBC API in fibers.
* `comsat-actors-api` – the Web Actors API
* `comsat-actors-servlet` – contains an implementation of Web Actors on top of Servlet and WebSocket (JSR-356) containers

### Activating Comsat

#### Using a Java agent

Then, the following must be added to the java command line (or use your favorite build tool to add this as a JVM argument):

~~~ sh
-javaagent:path-to-quasar-jar.jar
~~~


### In Tomcat


### In Jetty


### Building Quasar {#build}

Clone the repository:

    git clone git://github.com/puniverse/quasar.git quasar

and run:

    ./gradlew

Or, if you have gradle installed, run:

    gradle

# User Manual

## Comsat Integration

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

### Servlets

Comsat integrates with JavaEE servlets and enables you to write servlets that can scale to many concurrent visitors, even if servicing each requests takes a very long time, or requires calling many other services. Under the hood, Comsat does this by turning each servlet request into an asynchronous request, and then services each request on a separate [fiber](http://puniverse.github.io/quasar/manual/core.html#fibers). Calls to other web services or to a database are fiber- rather than thread-blocking. As a result, comsat can serve many thousands of concurrent requests with only a handful of OS threads. You, on the other hand, don't need to adopt a cumbersome asynchronous programming model. You can write the servlet code as you normally would, making synchronous (fiber-blocking) calls, provided that you use Comsat implementations.

To write a Comsat (fiber-per-request) servlet, simply extend [`FiberHttpServlet`]({{javadoc}}/fibers/servlet/FiberHttpServlet.html) rather than the usual `javax.servlet.HttpServlet`, and either annotate it with `@WebServlet` or declare it in `web.xml`. Note how the `service` and all the `doXXX` methods are [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (they all `throw SuspendExecution`).

You can deploy your servlet as you normally would, either as a WAR file, or in an embedded servlet container.

It is recommended that you then configure your servlet container to limit the number of threads in its thread pool to some small number, as all these threads do is create the fiber (which runs in the fiber thread pool) and return.

To learn about writing servlets, you can refer to the [Java Servlets tutorial](http://docs.oracle.com/javaee/7/tutorial/doc/servlets.htm).

### REST Services

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

{:.alert .alert-info}
**Note**: [Web Actors](webactors.html) are a great way to write REST services, as well as web-socket services, for interactive web applications.

### HTTP Client

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

To learn how to use the HTTP client, please refer to the [Jersey documentation](https://jersey.java.net/documentation/latest/user-guide.html#client), or the [JAX-RS client Javadoc](http://docs.oracle.com/javaee/7/api/javax/ws/rs/client/package-summary.html).

All of the JAX-RS API is supported, and blocking calls are fiber- rather than thread-blocking. If you want to execute several requests in parallel, you may use any of the "async" methods that return a `Future`:

~~~ java
Future response = resourceTarget.request("text/plain").header("Foo", "bar").async().get(String.class);
~~~

Calling `Future.get()` would also just block the fiber and not any OS thread. 

{:.alert .alert-info}
**Note**: A method that makes use of the API and runs in a fiber must be declared [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (normally by declaring `throws SuspendExecution`).

### JDBC

The `comsat-jdbc` project integrates the JDBC API with applications employing Quasar fibers (or actors). To use JDBC in Quasar fibers or actors, simply wrap your database driver's `DataSource` with [`FiberDataSource`]({{javadoc}}/fibers/jdbc/FiberDataSource.html), and use it to obtain connections which you may then freely use within fibers.

{:.alert .alert-info}
**Note**: A method that makes use of the API and runs in a fiber must be declared [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (normally by declaring `throws SuspendExecution`).

Normally, Comsat transforms asynchronous (callback based) API into fiber-blocking operations. JDBC, however, has no asynchronous API. comsat-jdbc simply runs the actual thread-blocking JDBC operations in a thread pool, and blocks the calling fiber until the operation has completed execution in the thread pool. As a result, you will not get any scalability benefits by calling your database in fibers (unlike, say, calling web services), because an OS thread will still block on every JDBC call. In practice, though, it matters little, as your database is likely to be a narrower bottleneck than the OS scheduler anyway.


XXXXXXXXXX Deployment in Servlet (JNDI?)


{:.alert .alert-warn}
**Note**: Your application may only may direct use of the Comsat JDBC data source, because methods calling the API must be declared suspendable (or run on regular threads). Database access frameworks (like various ORM solutions) that make use of JDBC cannot use this data source and be used in Quasar fibers. In the future, we will provide separate integration module for some popular database access libraries.

If you want to learn how to use JDBC, the [JDBC tutorial](http://docs.oracle.com/javase/tutorial/jdbc/basics/) is a good resource.

## Web Actors

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

Web Acotrs are [Quasar actors](http://puniverse.github.io/quasar/manual/actors.html) that receive and respond to messages from web clients. Web actors support HTTP, SSE and SSE (Server-Sent Events) messages, and are a convenient, efficient, and natural method for implementing the backend for interactive web applications.

### Deployment

WebActors are implemented on top of a web server. Currently, they can be deployed be deployed in any JavaEE 7 servlet container, but we are working on supporting deploying them on top of [Netty](http://netty.io/) and [Undertow](http://undertow.io/).

A web actor is attached to a web session. It can be spawned and attached manually (say, after the user logs in and the session is authenticated). The manual attachment API is container dependent: see [here](??????) for the API for JavaEE containers. A web actor can also be spawned and attached automatically by letting COMSAT spawn and attach a web actor to every newly created session. This method will be described below. Because a web actor consumes very few resources, spawning them automatically is sufficient in all but the most extreme circumstances.

For automatic deployment, all you have to do is define an actor class (one that extends `BasicActor` or `Actor`), and annotate it with the [`WebActor`]({{javadoc}}comsat/webactors/WebActor.html) annotation. For example:

~~~ java
@WebActor(name="chat", httpUrlPatterns="/chat", webSocketUrlPatterns="/chat/ws")
public class ChatActor extends BasicActor<WebMessage, Void> {
    @Override
    protected Void doRun() {
        // ...
    }
}
~~~

In this example, all HTTP requests to the `/chat` resource, as well as all websocket messages to `/chat/ws` will be received as messages by the actor. A new `ChatActor` will be spawned for every new HTTP session.


For details, see the [Javadoc]({{javadoc}}/comsat/webactors/WebActor.html).

### Basic Operation

A web actor should be able to receive messages of type [`WebMessage`]({{javadoc}}/comsat/webactors/WebMessage.html). `WebMessage` is the supertype of all messages that can be received from or sent to a web client. The class encapsulates a message body, that can be either text or binary, and a *sender*, which, following a common actor pattern, is the actor that sent the message.

For messages received from the web client, the *sender* is a virtual actor representing the web client. You can perform normal actor operations on it, like `watch` to detect actor death; their semantics depend on the specific type of the message.

A single web actor instance may handle HTTP requests, emit SSE events, and handle one or more WebSocket connections.

### HTTP (REST Services)

A web actor is attached to one or more HTTP resources (as specified by `@WebActor`'s `httpUrlPatterns` property), and an actor instance is associated with a single HTTP session. Every HTTP request to the resource, associated with the session, will be received by the actor as an [`HttpRequest`]({{javadoc}}/comsat/webactors/HttpRequest.html) message. The actor can then respond with an [`HttpResponse`]({{javadoc}}/comsat/webactors/HttpResponse.html) message, which it sends to the request's sender. 

All HTTP request messages to a specific web actor instance will come from the same sender. If you `watch` that sender actor, it will emit an `ExitMessage` (signifying its death), when the session is terminated.

When you responsd to an `HttpRequest` with an `HttpResponse`, by default, the request stream will close. If, however, you wish to send the response's body in parts (e.g., for SSE, discussed in the next section), you may call `HttpRequest.openChannel`, which will return a Quasar *channel* that can be used to send [`WebDataMessage`]({{javadoc}}comsat/webactors/WebDataMessage.html)s messages to the stream. The stream will flush after each `WebDataMessage`'s body has been written to it. If `openChannel` has been called, the HTTP response stream will be closed when `close` is called on the returned channel.

Please refer to the [Javadoc]({{javadoc}}/comsat/webactors/HttpRequest.html) for details.

### SSE

SSE, or [Server-Sent Events](http://dev.w3.org/html5/eventsource/), is an HTML5 stnadard, supported by most modern browsers, for pushing discrete messages to the web client, without it sending new HTTP requests for each one. A good tutorial by Eric Bidelman on SSE can be found [here](http://www.html5rocks.com/en/tutorials/eventsource/basics/). An SSE stream is initiated with an HTTP request; then, each event message is written to the response stream and flushed, only the messages need to be encoded according to the SSE standard. SSE also specifies that if the connection is closed, the client will attempt to reconnect with a new request after a timeout, that can be set by the server.

The [`SSE`]({{javadoc}}/comsat/webactors/SSE.html) class contains a set of static utility methods that encode the events according to the SSE standard, and ensure that the response headers are set correctly (in terms of character encoding, etc.).

Here's how SSE can be used with a web actor:

~~~ java
SendPort<WebDataMessage> sseChannel = request.openChannel();
request.sender().send(SSE.startSSE(request).build());

// ... send events
sseChannel.send(new WebDataMessage(self(), SSE.event("this is an SSE event!")));

// ... close the stream
sseChannel.close()
~~~

It might be convient (and elegant) to wrap the channel returned by `openStream` with a *mapping channel* (see the Quasar docs), that will transform a message class representing the event into an SSE-encoded `WebDataMessage`.

### WebSockets

[WebSocket](http://en.wikipedia.org/wiki/WebSocket) is a new web protocol for low(ish)-latency, bi-directional communication between the client and the server. Web sockets are extremely useful for interactive web applications, and they fit beautifully with COMSAT Web Actors.

A web actor may register itself to handle web socket connections by declaring which WebSocket URIs it is interesten in, in the `@WebActor` annotations `webSocketUrlPatterns` property. Such a web actor will handle all web socket sessions at those URIs associated with the actor instance's HTTP session (a web socket is also associated with an HTTP session).

When the client connects to a web socket, the web actor will receive a [`WebSocketOpened`]({{javadoc}}/comsat/webactors/WebSocketOpened.html) message, and each following message will be received as a [`WebDataMessage`]({{javadoc}}comsat/webactors/WebDataMessage.html). The actor can send messages to the client by replying to the sender with `WebDataMessage`s of its own. 

The virtual actor that's the *sender* of the messages received from the client represents the WebSocket session; i.e., each open web socket will have a different actor as the sender of the messages. That virtual actor dies when the web socket connection closes.

## Examples

{% capture examples %}https://github.com/{{site.github}}/tree/master/src/test/java/co/paralleluniverse/examples{% endcapture %}

