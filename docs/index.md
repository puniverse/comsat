---
layout: default
title: Comsat
description: "Comsat integrates lightweight threads and actors with the JVM web stack."
---

# Overview

COMSAT (or Comsat) is a set of open source libraries that integrate [Quasar](http://puniverse.github.io/quasar/) with various web or enterprise technologies (like HTTP services and database access). With Comsat, you can write web applications that are scalable and performant and, at the same time, are simple to code and maintain.

Comsat is not a web framework. In fact, it does not add new APIs at all (with one exception, Web Actors, mentioned later). It provides implementation to popular (and often, standard) APIs like Servlet, JAX-RS, and JDBC, that can be used efficiently within Quasar fibers.

Comsat does provide one new API that you may choose to use: Web Actors. Web actors let you define a Quasar actor that receives and responds to HTTP requests and web socket messages. The Web Actors API is rather minimal, and is intended to do one job and do it well: simplify two-way communication between your server and the client.

[Parallel Universe]: http://paralleluniverse.co

## News

### TBBD

COMSAT [0.5.0](https://github.com/puniverse/comsat/releases/tag/v0.5.0) has been released.

### July 1, 2015

COMSAT [0.4.0](https://github.com/puniverse/comsat/releases/tag/v0.4.0) has been released.

### December 23, 2014

COMSAT [0.3.0](https://github.com/puniverse/comsat/releases/tag/v0.3.0) has been released.

### July 23, 2014

COMSAT [0.2.0](https://github.com/puniverse/comsat/releases/tag/v0.2.0) has been released.

### January 22, 2014

COMSAT [0.1.0](https://github.com/puniverse/comsat/releases/tag/v0.1.0) has been released.

# Getting Started

### System requirements

Java 7 is required to use COMSAT.

### Using Comsat with [Maven](https://maven.apache.org) and [Gradle](http://www.gradle.org) {#maven}

First, you need the `quasar-core` dependency. With Maven:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-core</artifactId>
    <version>0.7.2</version>
</dependency>
~~~

or, for JDK8:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-core</artifactId>
    <version>0.7.2</version>
    <classifier>jdk8</classifier>
</dependency>
~~~

The corresponding Gradle dependencies are respectively `co.paralleluniverse:quasar-core:0.7.2` or, for JDK8, `co.paralleluniverse:quasar-core:0.7.2@jdk8`.

Then add the Comsat module relevant to your needs (Maven):

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>ARTIFACT</artifactId>
    <version>0.4.0</version>
</dependency>
~~~

The corresponding Gradle dependency is `co.paralleluniverse:ARTIFACT:0.4.0`.

`ARTIFACT` will be one of:

* `comsat-servlet` – Servlet integration for defining fiber-per-request servlets.
* `comsat-ring-jetty9` - A fiber-blocking Clojure [Ring](https://github.com/ring-clojure/ring) adapter based on Jetty 9.2
* `comsat-httpkit` - [HTTP Kit](http://www.http-kit.org/client.html)-based fiber-blocking HTTP client.
* `comsat-jersey-server` – [Jersey server](https://jersey.java.net/) integration for defining REST services.
* `comsat-dropwizard` – [Dropwizard](http://dropwizard.io/) integration including Jersey, ApacheHttpClient and JDBI.
* `comsat-spring-webmvc` – [Spring Framework](http://projects.spring.io/spring-framework/) Web MVC fiber-blocking controller methods integration.
* `comsat-spring-security` – [Spring Security](http://projects.spring.io/spring-security/) configuration support for fibers.
* `comsat-spring-boot` – [Spring Boot](http://projects.spring.io/spring-boot/) auto-configuration support for Web MVC controllers.
* `comsat-spring-boot-security` – auto-configuration support with security for Web MVC controllers.
* `comsat-jax-rs-client` – [JAX-RS client](https://jersey.java.net/documentation/latest/client.html) integration for calling HTTP services.
* `comsat-httpclient` – [ApacheHttpClient](http://hc.apache.org/httpcomponents-client-ga/) integration for calling HTTP services.
* `comsat-retrofit` – [Retrofit](http://square.github.io/retrofit/) integration.
* `comsat-jdbi` – [JDBI](http://jdbi.org/) integration for using the JDBI API in fibers.
* `comsat-jdbc` – JDBC integration for using the JDBC API in fibers.
* `comsat-jooq` – [jOOQ](http://www.jooq.org/) integration for using the jOOQ API in fibers.
* `comsat-mongodb-allanbank` – MongoDB integration for using the [allanbank API](http://www.allanbank.com/mongodb-async-driver/index.html)
* `comsat-okhttp` – [OkHttp](https://github.com/square/okhttp) HTTP+SPDY client integration.
* `comsat-actors-api` – The Web Actors API
* `comsat-actors-undertow` – Deploy HTTP, SSE and WebSocket Web Actors as [Undertow](http://undertow.io/) handlers
* `comsat-actors-netty` – Deploy HTTP, SSE and WebSocket Web Actors as [Netty](http://netty.io/) handlers
* `comsat-actors-servlet` – Deploy HTTP, SSE and WebSocket Web Actors in J2EE 7 Servlet and WebSocket (JSR-356) embedded and standalone containers
* `comsat-tomcat-loader` – Enables using Comsat in a Tomcat container without the need of javaAgent
* `comsat-jetty-loader` – Enables using Comsat in a Jetty container without the need of javaAgent

## Examples

A [Gradle template](https://github.com/puniverse/comsat-gradle-template) project and a [Maven archetype](https://github.com/puniverse/comsat-mvn-archetype) using various integration modules and featuring setup with both Dropwizard and standalone Tomcat are available for jumpstart and study. Both have a `without-comsat` branch which is useful to clearly see the (minimal, if any) porting effort required (branches comparison works very well for this purporse).

There's a [Comsat-Ring Clojure Leiningen template](https://github.com/puniverse/comsat-ring-template) as well which includes an `auto-instrument` branch that doesn't need any explicit suspendable-marking code (`suspendable!`, `defsfn`, `sfn` etc.) thanks to [Pulsar's new auto-instrumentation feature](http://docs.paralleluniverse.co/pulsar/#automatic-instrumentation).

This GitHub project contains examples covering most of the COMSAT functionality: [puniverse/comsat-examples](https://github.com/puniverse/comsat-examples).

Finally there are several regularly updated third-party bootstrap projects: [Comsat + Dropwizard + jOOQ](https://github.com/circlespainter/comsat-jooq-gradle-template), [Comsat Web Actors Stock Quotes (ported from Akka)](https://github.com/circlespainter/quasar-stocks), [Spring MVC + Tomcat standalone servlet container](https://github.com/circlespainter/spring4-mvc-gradle-annotation-hello-world).

### Enabling Comsat

Comsat runs code in [Quasar](http://docs.paralleluniverse.co/quasar/) fibers, which rely on bytecode instrumentation. This instrumentation is done in one of three ways: via a Java agent that must be loaded into the Servlet container; with a custom class-loader available for Tomcat and Jetty; or at compilation time.

AOT instrumentation is an advanced topic explained in the [Quasar documentation](http://docs.paralleluniverse.co/quasar/index.html#instrumentation).

When using AOT instrumentation alone, all of your fiber-blocking dependencies will need to have been AOT-compiled already. Please note that some Comsat modules, such as `comast-jersey-server`, rely on dynamic instrumentation of third-party libraries and so they cannot be used with AOT instrumentation alone.

#### The Java Agent

To use the Java agent, the following must be added to the java command line (or use your favorite build tool to add this as a JVM argument) when launching the process:

~~~ sh
-javaagent:path-to-quasar-jar.jar
~~~

Java agent instrumentation works with standalone Java applications and embedded Servlet containers but at present it cannot be used with standalone Servlet containers.

#### In Tomcat

If you're using Tomcat as your embedded or standalone Servlet container, a custom class-loader is available for use instead of the Java Agent. You'll need to put `comsat-tomcat-loader-{{site.version}}.jar` (or, for JDK8, `comsat-tomcat-loader-{{site.version}}-jdk8.jar`) into Tomcat's `common/lib` directory.

Then, include the following in your webapp's `META-INF/context.xml`:

~~~ xml
{% include_snippet loader ./comsat-test-war/src/main/webapp/META-INF/context.xml %}
~~~

The Tomcat instrumenting class-loader has been verified to work with Tomcat 7.0.62 and Tomcat 8.0.23 standalone Servlet containers.

#### In Jetty

If you're using Jetty as your embedded Servlet container, you have the option to use a custom class-loader instead of the Java agent. You'll need to put `comsat-jetty-loader-{{site.version}}.jar` (or, for JDK8, `comsat-jetty-loader-{{site.version}}-jdk8.jar`) into Jetty's `lib` directory.

Then, include a `<Set name="classLoader">` tag in your webapp's context xml:

~~~ xml
{% include_snippet context xml ./comsat-jetty-loader/src/test/resources/webapps/dep.xml %}
~~~

### Building Comsat {#build}

Install [Gradle](http://www.gradle.org), then clone the repository:

    git clone https://github.com/puniverse/comsat.git

and finally run:

    gradle install

The full testsuite can be run with `gradle build`.

# User Manual

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

## Comsat Integration

### Servlets

Comsat supports the Servlet 3.x specification (Java EE 6) and enables you to write servlets that can scale to many concurrent visitors, even if servicing each requests takes a very long time, or requires calling many other services. Under the hood, Comsat does this by turning each servlet request into an asynchronous request, and then services each on a separate [fiber](http://puniverse.github.io/quasar/manual/core.html#fibers). Calls to other web services or to a database are fiber- rather than thread-blocking. As a result, Comsat can serve many thousands of concurrent requests with only a handful of OS threads. You, on the other hand, don't need to adopt a cumbersome asynchronous programming model. You can write the servlet code as you normally would, making synchronous (fiber-blocking) calls, provided that you use Comsat implementations.

To write a Comsat (fiber-per-request) servlet, simply extend [`FiberHttpServlet`]({{javadoc}}/fibers/servlet/FiberHttpServlet.html) rather than the usual `javax.servlet.HttpServlet`, and either annotate it with `@WebServlet`, declare it in `web.xml` or use the programmatic [initializer API](http://docs.oracle.com/javaee/6/api/javax/servlet/ServletContainerInitializer.html). Note how the `service` and all the `doXXX` methods are [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) since they're annotated with `@Suspendable`, although they don't declare throwing `SuspendExecution` in order to retain full servlet API compatibility.

You can deploy your servlet as you normally would, either as a WAR file (remember to enable async support for it), or in an embedded servlet container.

It is recommended that you then configure your servlet container to limit the number of threads in its thread pool to some small number, as all these threads do is create the fiber (which runs in the fiber thread pool) and return.

Example:

~~~ java
{% include_snippet FiberHttpServlet example ./comsat-servlet/src/test/java/co/paralleluniverse/fibers/servlet/FiberHttpServletTest.java %}
~~~
Then you can simply add it as a regular servlet to you favorite servlet containter, e.g. for embedded Jetty:

~~~ java
{% include_snippet servlet registration ./comsat-servlet/src/test/java/co/paralleluniverse/fibers/servlet/FiberHttpServletTest.java %}
~~~

To learn about writing servlets, you can refer to the [Java Servlets tutorial](http://docs.oracle.com/javaee/6/tutorial/doc/bnafd.html).

### REST Services

You can easily create Comsat REST services with the [JAX-RS API](https://jax-rs-spec.java.net/nonav/2.0/apidocs/index.html), the standard Java REST service API. Comsat integrates with [Jersey](https://jersey.java.net/), the reference JAX-RS implementation.

All you need to do in order to enjoy Comsat's scalabilty, is replace the line

~~~ xml
<servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
~~~

in your `web.xml` file, which is how you would normally use Jersey in a Servlet container, with:

~~~ xml
<servlet-class>co.paralleluniverse.fibers.jersey.ServletContainer</servlet-class>
<async-supported>true</async-supported>
~~~

Your resource methods (the ones you annotate with `@GET`, `@PUT`, `@POST` etc.) can now be made suspendable by declaring `throws SuspendExecution`. Comsat would then run each request in a fiber. Your resource methods are free to use Comsat's JDBC implementation, or Comsat's JAX-RS client.

Here is an example of REST resource declaration:

~~~ java
{% include_snippet REST resource example ./comsat-jersey-server/src/test/java/co/paralleluniverse/fibers/jersey/TestResource.java %}
~~~

And then initialization of the jersey container:

~~~ java
{% include_snippet jersey registration ./comsat-jersey-server/src/test/java/co/paralleluniverse/fibers/jersey/FiberServletContainerTest.java %}
~~~
To learn about writing REST services with JAX-RS, please refer to the [Jersey User Guide](https://jersey.java.net/documentation/latest/user-guide.html).

{:.alert .alert-info}
**Note**: [Web Actors](webactors.html) are a great way to write REST services, as well as web-socket services, for interactive web applications.

### Clojure Ring

The Comsat Ring adapter is a fiber-blocking adapter based on Jetty 9: it will make your Ring handler run in a fiber rather than in a thread, boosting efficiency without requiring handler logic changes.

Comsat Ring is based on Pulsar, so it is necessary that the handler's fiber-blocking logic and all functions calling it are declared suspendable through either the `sfn` / `defsfn` macros or the `suspendable!` call (please refer to [Pulsar docs](http://docs.paralleluniverse.co/pulsar/#fibers) for details). This often means declaring suspendable the handler itself and middlewares applied to it. You can avoid making suspendable the resulting handler passed to the adapter though, as latter will do it for you.

So rather than:

~~~ clojure
(ns myapp.core
  (:use ring.adapter.jetty))

(defn- hello-world [request]
  (Thread/sleep 100)
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn run [] (run-jetty hello-world {:port 8080}))
~~~

Just setup Pulsar as described in the [docs](http://docs.paralleluniverse.co/pulsar/#lein), remembering to add the `[co.paralleluniverse/comsat-ring-jetty9 "{{site.version}}"]` dependency, and change your `use` or `require` clauses slightly:

~~~ clojure
(ns myapp.core
  (:use co.paralleluniverse.fiber.ring.jetty9)
  (:import (co.paralleluniverse.fibers Fiber))
  (:require [co.paralleluniverse.pulsar.core :as pc]))

(pc/defsfn hello-world [request]
  (Fiber/sleep 100)
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn run [] (run-jetty hello-world {:port 8080}))
~~~

Your handler is now running inside fibers rather than threads.

### Clojure HTTP Kit Client

[HTTP Kit](http://www.http-kit.org/) is a minimalist, efficient, Ring-compatible HTTP client/server for Clojure that supports async operation. The client API is an async subset of [clj-http](https://github.com/dakrone/clj-http) and the `comsat-httpkit` integration converts it back to straightforward fiber-blocking `clj-http` through the `await` function. Being only 18 lines of code, this integration also shows how easy it is to integrate Clojure async APIs with Pulsar.

Just add `comsat-httpkit` to your dependencies and `require` the `co.paralleluniverse.fiber.httpkit.client` namespace. You can then use `request`, `get`, `delete`, `head`, `post`, `put`, `options`, `patch` inside your fibers as you would use `clj-http` (currently limited to the `clj-http` features supported by HTTP Kit):

~~~ clojure
(ns myapp
  (:require
    [co.paralleluniverse.fiber.httpkit.client :refer :all]
    [co.paralleluniverse.pulsar.core :refer [fiber]]))

(defn -main []
  (println @(fiber (get "http://google.com"))))
~~~

Have also a look at the testsuite ported from HTTP Kit.

### HTTP Clients

#### Apache Http Client
The fiber blocking version of the Apache Http Client can be used with `FiberHttpClientBuilder`.
For example:

~~~ java
{% include_snippet client configuration ./comsat-httpclient/src/test/java/co/paralleluniverse/fibers/httpclient/FiberHttpClientBuilderTest.java %}
~~~

After that you can call the regular API from fibers:

~~~ java
{% include_snippet http call ./comsat-httpclient/src/test/java/co/paralleluniverse/fibers/httpclient/FiberHttpClientBuilderTest.java %}
~~~

If you prefer to use the future API you should build a regular `HttpAsyncClient `and then wrap it with `FiberCloseableHttpAsyncClient.wrap`, for example

~~~ java
{% include_snippet client configuration ./comsat-httpclient/src/test/java/co/paralleluniverse/fibers/httpasyncclient/FiberHttpAsyncClientTest.java %}
~~~

Then you can use it as follows:

~~~ java
{% include_snippet future calls ./comsat-httpclient/src/test/java/co/paralleluniverse/fibers/httpasyncclient/FiberHttpAsyncClientTest.java %}
~~~

#### Jersey Http Client

Comsat's integrated HTTP client is a JAX-RS client (specifically, a Jersey client). To create a client instance compatible with Quasar fibers, use the [`AsyncClientBuilder`]({{javadoc}}/fibers/ws/rs/client/AsyncClientBuilder.html) class:

~~~ java
Client client = AsyncClientBuilder.newClient();
~~~

You can also pass a configuration:

~~~ java
Client client = AsyncClientBuilder.newClient(config);
~~~

or use the builder API:

~~~ java
{% include_snippet client creation ./comsat-jax-rs-client/src/test/java/co/paralleluniverse/fibers/ws/rs/client/AsyncClientBuilderTest.java %}
~~~

Then the usage is like the regular API, for example:

~~~ java
{% include_snippet http call ./comsat-jax-rs-client/src/test/java/co/paralleluniverse/fibers/ws/rs/client/AsyncClientBuilderTest.java %}
~~~

To learn how to use the HTTP client, please refer to the [Jersey documentation](https://jersey.java.net/documentation/latest/user-guide.html#client), or the [JAX-RS client Javadoc](http://docs.oracle.com/javaee/7/api/javax/ws/rs/client/package-summary.html).

All of the JAX-RS API is supported, and blocking calls are fiber- rather than thread-blocking. If you want to execute several requests in parallel, you may use any of the "async" methods that return a `Future`:

~~~ java
Future response = resourceTarget.request("text/plain").header("Foo", "bar").async().get(String.class);
~~~

Calling `Future.get()` would also just block the fiber and not any OS thread.

{:.alert .alert-info}
**Note**: A method that makes use of the API and runs in a fiber must be declared [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (normally by declaring `throws SuspendExecution`).

{:.alert .alert-warn}
**Note**: the Jersey client's current implementation (since 2.5) has a significant disadvantage w.r.t ApacheHttpClient because it uses one thread per http call. Therefore it is not recommended until this is fixed.

#### Retrofit

[`Retrofit`](http://square.github.io/retrofit/) lets you access REST API through java interface. In order to use it from fibers you should first declare a `Suspendable` interface:

~~~ java
{% include_snippet interface ./comsat-retrofit/src/test/java/co/paralleluniverse/fibers/retrofit/FiberRestAdapterBuilderTest.java %}
~~~

This interface can then be registered with `FiberRestAdapterBuilder` and then used from fibers:

~~~ java
{% include_snippet registration ./comsat-retrofit/src/test/java/co/paralleluniverse/fibers/retrofit/FiberRestAdapterBuilderTest.java %}// ...
// usage from fiber context
{% include_snippet usage ./comsat-retrofit/src/test/java/co/paralleluniverse/fibers/retrofit/FiberRestAdapterBuilderTest.java %}
~~~

#### OkHttp

Comsat integrates with [OkHttp](https://github.com/square/okhttp), a modern HTTP+SPDY client and offers fiber-blocking `OkHttpClient` and `Call` implementation.

Build fiber-friendly, fully OkHttp-compatible `FiberOkHttpClient` and `FiberCall` as follows:

~~~ java
Request req = ...;
OkHttpClient client = new FiberOkHttpClient();
Call call = client.newCall(req);
~~~

OkHttp's `urlconnection` and `apache` modules are supported as well: just pass an `FiberOkHttpClient` instance when building `OkUrlFactory` and `OkApacheClient`:

~~~ java
OkUrlFactory factory = new OkUrlFactory(new FiberOkHttpClient());
OkApacheClient client = new OkApacheClient(new FiberOkHttpClient());
~~~

### DB Access

#### JDBC

The `comsat-jdbc` project makes the JDBC API more efficient when using Quasar fibers (or fiber-backed actors). To use JDBC in fibers, simply wrap your database driver's `DataSource` with [`FiberDataSource`]({{javadoc}}/fibers/jdbc/FiberDataSource.html), and use it to obtain connections. For example:

~~~ java
{% include_snippet DataSource wrapping ./comsat-jdbc/src/test/java/co/paralleluniverse/fibers/jdbc/FiberDataSourceTest.java %}
~~~

Then the DataSource can be used with the regular API from fibers, For example:

~~~ java
{% include_snippet DataSource usage ./comsat-jdbc/src/test/java/co/paralleluniverse/fibers/jdbc/FiberDataSourceTest.java %}// ...
// usage in fiber context
{% include_snippet connection usage ./comsat-jdbc/src/test/java/co/paralleluniverse/fibers/jdbc/FiberConnectionTest.java %}
~~~

{:.alert .alert-info}
**Note**: A method that makes use of the API and runs in a fiber must be declared [suspendable](http://puniverse.github.io/quasar/manual/core.html#fibers) (normally by declaring `throws SuspendExecution`).

Normally, Comsat transforms asynchronous (callback based) API into fiber-blocking operations. JDBC, however, has no asynchronous API. `comsat-jdbc` simply runs the actual thread-blocking JDBC operations in a thread pool, and blocks the calling fiber until the operation has completed execution in the thread pool. As a result, you will not get any scalability benefits by calling your database in fibers (unlike, say, calling web services), because an OS thread will still block on every JDBC call. In practice, though, it matters little, as your database is likely to be a narrower bottleneck than the OS scheduler anyway.

{:.alert .alert-warn}
**Note**: Your application may only may direct use of the Comsat JDBC data source, because methods calling the API must be declared suspendable (or run on regular threads). Database access frameworks (like various ORM solutions) that make use of JDBC cannot use this data source and be used in Quasar fibers. In the future, we will provide separate integration module for some popular database access libraries.

If you want to learn how to use JDBC, the [JDBC tutorial](http://docs.oracle.com/javase/tutorial/jdbc/basics/) is a good resource.

#### JDBC Deployment Via JNDI

Servlets often make use of JDBC data sources exposed through JNDI. If you do that, you can declare a COMSAT (i.e. a fiber-aware) JDBC data source through JNDI that will wrap your native data source. To do so, use the `co.paralleluniverse.fibers.jdbc.FiberDataSourceFactory` DataSource factory, and pass in the number of threads you'd like COMSAT to use in the JDBC worker pool.

In order to do that first you have to include `comsat-jdbc-{{site.version}}.jar` in your container's runtime classpath, by putting it into the container's `lib` directory.

If you're using `TOMCAT`, the following example is a snippet of `META-INF/context.xml` that will declare a DataSource under the `jdbc/fiberdb` name, which wraps a native DB declared under the `jdbc/globalds` name:

~~~ xml
<Context path="/">
...
{% include_snippet fiber ds ./comsat-test-war/src/main/webapp/META-INF/context.xml %}</Context>
~~~

In order to do the same thing with `Jetty`, you have to include similar definition in your `WEB-INF/jetty-env.xml`:

~~~ xml
{% include_snippet fiber ds ./comsat-test-war/src/main/webapp/WEB-INF/jetty-env.xml %}
~~~

#### JDBI

To use the powerful API of [JDBI](http://jdbi.org/) to access databases you first have to create an `IDBI` instance using the `FiberDBI` class:

~~~ java
{% include_snippet creation ./comsat-jdbi/src/test/java/co/paralleluniverse/fibers/jdbi/FiberFluentAPITest.java %}
~~~

The created instance can be used both with the Fluent API as well as with the `SqlObject` API. First the fluent API example:

~~~ java
{% include_snippet usage ./comsat-jdbi/src/test/java/co/paralleluniverse/fibers/jdbi/FiberFluentAPITest.java %}
~~~

As for the `SqlObject` API, declare first a `Suspendable` interface. Here is an example:

~~~ java
{% include_snippet interface ./comsat-jdbi/src/test/java/co/paralleluniverse/fibers/jdbi/FiberSqlObjectAPITest.java %}
~~~

The interface now can be registered and used as usual from fibers:

~~~ java
{% include_snippet registration ./comsat-jdbi/src/test/java/co/paralleluniverse/fibers/jdbi/FiberSqlObjectAPITest.java %}//...
// usage in fiber context
{% include_snippet usage ./comsat-jdbi/src/test/java/co/paralleluniverse/fibers/jdbi/FiberSqlObjectAPITest.java %}
~~~

#### jOOQ

[JOOQ](http://www.jooq.org/) is a comprehensive solution to access SQL databases. In order to use jOOQ from fibers, all you have to do is to provide a connection originated from `FiberDataSource`, for example:

~~~ java
{% include_snippet creation ./comsat-jooq/src/test/java/co/paralleluniverse/fibers/jooq/JooqContextTest.java %}// ...
// mapper definition
{% include_snippet mapper ./comsat-jooq/src/test/java/co/paralleluniverse/fibers/jooq/JooqContextTest.java %}// ...
// usage in fiber context
{% include_snippet usage ./comsat-jooq/src/test/java/co/paralleluniverse/fibers/jooq/JooqContextTest.java %}
~~~

#### MongoDB

Comsat integrates with MongoDB and offers a fiber-blocking [allanbank API](http://www.allanbank.com/mongodb-async-driver/index.html).

This is how you get a fiber-friendly `MongoDatabase` instance, which you can then use regularly from fibers:

~~~ java
MongoClient mongoClient = FiberMongoFactory.createClient( "mongodb://localhost:" + port + "/test?maxConnectionCount=10" ).asSerializedClient();
MongoDatabase mongoDb = mongoClient.getDatabase("mydb");
~~~

### Dropwizard

[Dropwizard](http://dropwizard.io/) is a Java framework for developing ops-friendly, high-performance, RESTful web services.

Only few changes are needed in order to use dropwizard with fibers.

First the YAML configuration file:

~~~ yaml
{% include_snippet server ./comsat-dropwizard/src/test/resources/server.yml %}
~~~

The number of concurrent threads needed for the `comsat-dropwizard` container will be low even if the number of concurrent connection is high because threads will just hand the established connections to newly created fibers. 50 to 200 threads will be enough but you should increase the queue size. You also need to configure an adequate `requestLog` appender (or disable it). Next is the `httpClient` configuration:

~~~ yaml
{% include_snippet httpclient ./comsat-dropwizard/src/test/resources/server.yml %}
~~~

You should also increase `maxConnections`. The DB configuration will look like this:

~~~ yaml
{% include_snippet db ./comsat-dropwizard/src/test/resources/server.yml %}
~~~

The `driverClass` will be `co.paralleluniverse.fibers.jdbc.FiberDriver`. The `url` will be your real datasource's with the addition of the 'fiber:' prefix, and as usual you should include the DB driver in your runtime classpath.

As for the code:

~~~ java
{% include_snippet app ./comsat-dropwizard/src/test/java/co/paralleluniverse/fibers/dropwizard/MyDropwizardApp.java %}
~~~

Instead of extending the regular `io.dropwizard.Application` class, you should extend the Comsat's `FiberApplication`. Your regular `run` function should be named `fiberRun` instead. The creation of the HTTP client should be through `FiberHttpClientBuilder` and the creation of `jdbi` should be through `FiberDBIFactory`.

### Spring

[Spring Framework](http://projects.spring.io/spring-framework/) is a popular Dependency Injection engine; it integrates with many enterprise Java tools and libraries and complements them with uniform and easy-to-use APIs.

[Spring Boot](http://projects.spring.io/spring-boot/) adds fast project bootstrap facilities, convention over configuration, auto-configuration based on classpath (and other conditions) and embedded Tomcat and Jetty containers integration. It also provides [Actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready), a set of ready-to-use facilities for production environments like auditing, health-checks, metrics and JMX monitoring/management through JMX's native protocol, HTTP, SSH or telnet.

[Spring Security](http://projects.spring.io/spring-security/) is a comprehensive Java security framework encompassing authentication and authorization for traditional and web applications, and the de-facto standard for securing Spring-based projects.

Comsat provides the ability to write fiber-blocking Spring Web MVC controllers with (optional) Spring Boot auto-configuration and (still optional) Spring Security context inheritance for fibers.

#### Fiber-blocking Spring Web MVC controllers

Adding support for fiber-blocking Spring Web MVC controllers is as easy as replacing in your Spring configuration class the `@EnableWebMvc` annotation with `@Import(FiberWebMvcConfigurationSupport.class)`, for example:

~~~ java
{% include_snippet import ./comsat-spring/comsat-spring-boot/comsat-spring-boot-sample-traditional/src/main/java/comsat/sample/traditional/SampleTraditionalApplication.java %}
~~~

...And declaring your controller methods as suspendable, as you would normally do with any fiber-blocking method:

~~~ java
{% include_snippet suspendable ./comsat-spring/comsat-spring-boot/comsat-spring-boot-sample-actuator/src/main/java/comsat/sample/actuator/SampleController.java %}
~~~

Spring Web MVC controller methods that have not been annotated (nor otherwise instrumented) to be suspendable will be invoked in thread-blocking mode rather than fiber-blocking.

#### Spring Security support

By default, Spring Security stores the server-side security context for the current user in a Java `ThreadLocal`. For suspendable Spring Web MVC controllers to inherit the security context, the strategy Spring uses mut be reconfigured to use Java's `InheritableThreadLocal` instead (please be aware that this is JVM-level global setting).

This is as easy as adding an `@Import` for the `co.paralleluniverse.springframework.security.config.FiberSecurityContextHolderConfig` configuration class:

~~~ java
{% include_snippet import ./comsat-spring/comsat-spring-boot/comsat-spring-boot-sample-actuator/src/main/java/comsat/sample/actuator/SampleActuatorApplication.java %}
~~~

At present there is one small caveat to consider when using Spring method security: as Spring will proxy secured methods so that all declared exceptions (including `SuspendExecution`) are catched individually, Quasar will refuse to instrument them. In this specific case `SuspendExecution` should not be declared but catched in the method body, and the method signature should be annotated with `@Suspendable` instead.

#### Spring Boot auto-configuration support

If you prefer using auto-configuration, it is enough to use the `FiberSpringBootApplication` or `FiberSecureSpringBootApplication` annotation instead, depending if you want to use Spring Security and its support for fibers:

~~~ java
{% include_snippet import ./comsat-spring/comsat-spring-boot/comsat-spring-boot-sample-web-groovy-templates/src/main/java/comsat/sample/ui/SampleGroovyTemplateApplication.java %}
~~~

## Web Actors

Web Actors are [Quasar actors](http://puniverse.github.io/quasar/manual/actors.html) that receive and respond to messages from web clients. Web actors support HTTP, WebSocket and SSE (Server-Sent Events) messages and are a convenient, efficient, and natural method to implement backends for interactive web applications.

Web Actors are deployed on a web server. Currently they can be deployed in any JavaEE 7 servlet container, as an [Underscore](http://underscore.io/) handler and as a [Netty](http://netty.io/) handler.

### Undertow deployment

Deploying web actors on top of Underscore is as easy as using one of two Underscore handlers: either `AutoWebActorHandler` or `WebActorHandler`.

`AutoWebActorHandler` will automatically scan the classpath for classes with the `@WebActor` annotation upon first use and will then instantiate and start the appropriate actor class (among detected ones) once per client session (or connection if there's no session, see below). Its constructor requires no arguments but optionally a user-specified classloader and/or a map containing per-class actor constructor parameters can be provided.

Here's an example server setup using `AutoWebActorHandler` without construction arguments (have a look at `comsat-actors-undertow`'s tests for more insight):

~~~ java
server = Undertow.builder()
        .addHttpListener(INET_PORT, "localhost")
        .setHandler(new AutoWebActorHandler().build();

server.start();
~~~

The way individual web actor references are assigned to individual HTTP exchanges is represented by the `WebActorHandler.Context` interface, which provides both the web actor reference and its implementation class in order to match incoming requests' URLs against its `@WebActor` annotation.

`WebActorHandler` delegates session lookup (or creation) to a developer-supplied `ContextProvider` which is the only required constructor argument; here's an example server setup using `WebActorHandler` and delegating all exchanges to a single actor (have a look at `comsat-actors-undertow`'s' tests for further insight):

~~~ java
final Actor actor = new MyWebActor();
final MActorRef<? extends WebMessage> actorRef = actor.spawn();
// ...
server = Undertow.builder()
    .addHttpListener(8080, "localhost")
    .setHandler(new WebActorHandler(new WebActorHandler.ContextProvider() {
        @Override
        public WebActorHandler.ActorContext get(HttpServerExchange xch) {
            return new WebActorHandler.DefaultContextImpl() {
                @Override
                public ActorRef<? extends WebMessage> getRef() {
                    return actorRef;
                }

                @Override
                public Class<? extends ActorImpl<? extends WebMessage>> getWebActorClass() {
                    return MyWebActor.class;
                }
            };
        }
    })).build();

server.start();
~~~

`WebActorHandler` needs Undertow's in-memory session handler only for SSE exchanges; by default session management is enabled for all exchanges but it can be disabled in non-SSE cases by setting the `co.paralleluniverse.comsat.webactors.undertow.WebActorHandler.HttpChannelAdapter.trackSessionOnlyForSSE` system property to `true`.

The actor context duration for the default implementation is 10 seconds but it can be configured through the `co.paralleluniverse.comsat.webactors.undertow.WebActorHandler.DefaultContextImpl.durationMillis` system property.

### Netty deployment

Deploying web actors on top of Netty is as easy as inserting one of two Netty handlers in your pipeline: either `AutoWebActorHandler` or `WebActorHandler`.

`AutoWebActorHandler` will automatically scan the classpath for classes with the `@WebActor` annotation upon first use and  will then instantiate and start the appropriate actor class (among detected ones) once per client session (or connection if there's no session, see below). Its constructor requires no arguments but optionally a user-specified classloader and/or a map containing per-class actor constructor parameters can be passed in.

The only other requirement is that your channel pipeline contains separate `HttpRequestDecoder` and `HttpResponseEncoder` instances rather than a single `HttpServerCodec` because the `HttpResponseEncoder` needs to be dynamically removed when an SSE exchange starts. If you prefer, as an alternative you can pass the name of your installed `HttpResponseEncoder` in `AutoWebActorHandler`'s constructor.

Here's an example server setup using `AutoWebActorHandler` without construction arguments (have a look at `comsat-actors-netty`'s tests for further insight):

~~~ java
final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
final ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpRequestDecoder());
            pipeline.addLast(new HttpResponseEncoder());
            pipeline.addLast(new HttpObjectAggregator(65536));

            pipeline.addLast(new AutoWebActorHandler());
        }
    });

final ChannelFuture ch = b.bind(INET_PORT).sync();
~~~

The way individual web actor references are assigned to individual HTTP exchanges is represented by the `WebActorHandler.Context` interface, which provides both the web actor reference and its implementation class in order to match incoming requests' URLs against its `@WebActor` annotation.

`WebActorHandler` delegates context lookup (or creation) to a developer-supplied `ContextProvider` which is is the only required constructor argument; here's an example server setup using `WebActorHandler` and delegating all exchanges to a single actor (have a look at `comsat-actors-netty`'s' tests for further insight):

~~~ java
final MyWebActor actor = new MyWebActor();
final MActorRef<? extends WebMessage> actorRef = actor.spawn();
// ...
final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
final ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpRequestDecoder());
            pipeline.addLast(new HttpResponseEncoder());
            pipeline.addLast(new HttpObjectAggregator(65536));

            pipeline.addLast(new WebActorHandler(new WebActorHandler.ActorContextProvider() {
                @Override
                public WebActorHandler.ActorContext get(ChannelHandlerContext ctx, FullHttpRequest req) {
                    return new WebActorHandler.DefaultActorContextImpl() {
                        @Override
                        public ActorRef<? extends WebMessage> getRef() {
                            return actorRef;
                        }
                    
                        @Override
                        public Class<? extends ActorImpl<? extends WebMessage>> getWebActorClass() {
                            return MyWebActor.class;
                        }
                    };
                }
            }));
        }
    });

final ChannelFuture ch = b.bind(8080).sync();
~~~

`WebActorHandler` needs cookie-based client session tracking only for SSE exchanges; by default it is enabled for all exchanges but it can be disabled in non-SSE cases through the `co.paralleluniverse.comsat.webactors.netty.WebActorHandler.HttpChannelAdapter.trackSessionOnlyForSSE` system property.

Session duration for the default implementation is 10 seconds but it can be configured through the `co.paralleluniverse.comsat.webactors.netty.WebActorHandler.DefaultContextImpl.durationMillis` system property.

### Servlet deployment

A web actor is attached to a servlet web session. It can be spawned and attached manually (say, after the user logs in and the session is authenticated). The manual attachment API unfortunately is container dependent. A web actor can also be spawned and attached automatically by letting COMSAT spawn and attach a web actor to every newly created session and this method will be described below. Because a web actor consumes very few resources, spawning them automatically is sufficient in all but the most extreme circumstances.

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

#### Embedded containers

If you use embedded container, you have to register `WebActorInitializer` as a `ServletContextListener` to your servlet container. It will scan and register the web actors according to the `@WebActor` annotation:

~~~ java
{% include_snippet WebActorInitializer ./comsat-actors-servlet/src/test/java/co/paralleluniverse/comsat/webactors/servlet/WebActorServletTest.java %}
~~~

Web actors may use websockets. In order to do that the container has to be configured to support it and unfortunately there's no standard mechanism for that yet. With Jetty you have to include the `javax-websocket-server-impl` jar and call the following method before you start the container:

~~~ java
WebSocketServerContainerInitializer.configureContext(context);
~~~

With Tomcat you have to include the `tomcat-embed-websocket` jar and register `ServletContainerInitilizer`:

~~~ java
context.addServletContainerInitializer(new WsSci(), null);
~~~

With Undertow you'll need the `undertow-websockets-jsr` jar; the setup is then a bit more involved as Undertow's `ServerWebSocketContainer` requires several construction arguments.

You can find an example for each of the servers above in the `comsat-test-utils` project [here](https://github.com/puniverse/comsat/tree/master/comsat-test-utils/src/main/java/co/paralleluniverse/embedded/containers): each embedded server utility class has an `enableWebsockets` method that performs the websockets setup.

For further details about the Web Actors API see the [Javadoc]({{javadoc}}/comsat/webactors/WebActor.html).

### Basic Operation

A web actor will receive messages of type [`WebMessage`]({{javadoc}}/comsat/webactors/WebMessage.html), which is the supertype of all messages that can be received from or sent to a web client. The class encapsulates a message body which can be either text or binary, and a *sender*, which, following a common actor pattern, is the actor that sent the message.

For messages received from the web client, the *sender* is a virtual actor representing the web client. You can perform normal actor operations on it, like `watch` to detect actor death; their semantics depend on the specific type of the message.

A single web actor instance may handle HTTP requests, emit SSE events, and handle one or more WebSocket connections.

### HTTP (REST Services)

A web actor is attached to one or more HTTP resources (as specified by `@WebActor`'s `httpUrlPatterns` property), and an actor instance is associated with a single HTTP session. Every HTTP request to the resource, associated with the session, will be received by the actor as an [`HttpRequest`]({{javadoc}}/comsat/webactors/HttpRequest.html) message. The actor can then respond with an [`HttpResponse`]({{javadoc}}/comsat/webactors/HttpResponse.html) message, which it sends to the request's sender.

All HTTP request messages to a specific web actor instance will come from the same sender. If you `watch` that sender actor, it will emit an `ExitMessage` (signifying its death), when the session is terminated.

When you respond to an `HttpRequest` with an `HttpResponse`, by default, the request stream will close. If, however, you wish to send the response's body in parts (e.g., for SSE, discussed in the next section), you may call `HttpRequest.openChannel`, which will return a Quasar *channel* that can be used to send [`WebDataMessage`]({{javadoc}}comsat/webactors/WebDataMessage.html)s messages to the stream. The stream will flush after each `WebDataMessage`'s body has been written to it. If `openChannel` has been called, the HTTP response stream will be closed when `close` is called on the returned channel.

Please refer to the [Javadoc]({{javadoc}}/comsat/webactors/HttpRequest.html) for details.

### SSE

SSE, or [Server-Sent Events](http://dev.w3.org/html5/eventsource/), is an HTML5 standard, supported by most modern browsers, for pushing discrete messages to the web client, without it sending new HTTP requests for each one. A good tutorial by Eric Bidelman on SSE can be found [here](http://www.html5rocks.com/en/tutorials/eventsource/basics/). An SSE stream is initiated with an HTTP request; then, each event message is written to the response stream and flushed, only the messages need to be encoded according to the SSE standard. SSE also specifies that if the connection is closed, the client will attempt to reconnect with a new request after a timeout, that can be set by the server.

The [`SSE`]({{javadoc}}/comsat/webactors/SSE.html) class contains a set of static utility methods that encode the events according to the SSE standard, and ensure that the response headers are set correctly (in terms of character encoding, etc.).

To start an SSE stream in response to an `HttpRequest`, do the following:

~~~ java
request.getFrom().send(new HttpResponse(self(), SSE.startSSE(request)));
~~~

This will set `HttpResponse`'s `startActor` flag, which will leave the response stream open and send back an [`HttpStreamOpened`]({{javadoc}}/comsat/webactors/HttpStreamOpened.html) message from a newly created actor representing the SSE stream. Once you receive the message, you send SSE events by sending `WebDataMessage`s to that actor:

~~~ java
sseActor.send(new WebDataMessage(self(), SSE.event("this is an SSE event!")));
~~~

To close the stream, you send a `co.paralleluniverse.actors.ShutdownMessage` to the SSE actor like so:

~~~ java
co.paralleluniverse.actors.ActorUtil.sendOrInterrupt(sseActor, new ShutdownMessage());
~~~

It might be convient (and elegant) to wrap the channel returned by `openStream` with a *mapping channel* (see the Quasar docs) that will transform a message class representing the event into an SSE-encoded `WebDataMessage`.

### WebSockets

[WebSocket](http://en.wikipedia.org/wiki/WebSocket) is a new web protocol for low(ish)-latency, bi-directional communication between the client and the server. Web sockets are extremely useful for interactive web applications, and they fit beautifully with COMSAT Web Actors.

A web actor may register itself to handle web socket connections by declaring which WebSocket URIs it is interesten in, in the `@WebActor` annotations `webSocketUrlPatterns` property. Such a web actor will handle all web socket sessions at those URIs associated with the actor instance's HTTP session (a web socket is also associated with an HTTP session).

When the client connects to a web socket, the web actor will receive a [`WebSocketOpened`]({{javadoc}}/comsat/webactors/WebSocketOpened.html) message, and each following message will be received as a [`WebDataMessage`]({{javadoc}}comsat/webactors/WebDataMessage.html). The actor can send messages to the client by replying to the sender with `WebDataMessage`s of its own.

The virtual actor that's the *sender* of the messages received from the client represents the WebSocket session; i.e., each open web socket will have a different actor as the sender of the messages. That virtual actor dies when the web socket connection closes.
