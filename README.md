# *COMSAT*<br/>Scalable, Concurrent Web Apps
[![Build Status](http://img.shields.io/travis/puniverse/comsat.svg?style=flat)](https://travis-ci.org/puniverse/comsat) [![Dependency Status](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f/badge.png?style=flat)](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f) [![Version](http://img.shields.io/badge/version-0.7.0-blue.svg?style=flat)](https://github.com/puniverse/comsat/releases) [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![License](http://img.shields.io/badge/license-LGPL-blue.svg?style=flat)](https://www.gnu.org/licenses/lgpl.html)

## Getting started

Add the following Maven/Gradle dependencies:

| Feature                                                                                                                   | Artifact
|---------------------------------------------------------------------------------------------------------------------------|--------------------------
| Servlet integration for defining fiber-per-request servlets.                                                              | `co.paralleluniverse:comsat-servlet:0.7.0`
| A fiber-blocking Clojure [Ring](https://github.com/ring-clojure/ring) adapter based on Jetty 9.3.                         | `co.paralleluniverse:comsat-ring-jetty9:0.7.0`
| [HTTP Kit](http://www.http-kit.org/client.html)-based fiber-blocking HTTP client.                                         | `co.paralleluniverse:comsat-httpkit:0.7.0`
| [Jersey server](https://jersey.java.net/) integration for defining REST services.                                         | `co.paralleluniverse:comsat-jersey-server:0.7.0`
| [Dropwizard](http://dropwizard.io/) integration including Jersey, ApacheHttpClient and JDBI.                              | `co.paralleluniverse:comsat-dropwizard:0.7.0`
| [Spring Framework](http://projects.spring.io/spring-framework/) Web MVC fiber-blocking controller methods integration.    | `co.paralleluniverse:comsat-spring-webmvc:0.7.0`
| [Spring Boot](http://projects.spring.io/spring-boot/) auto-configuration support for Web MVC controllers.                 | `co.paralleluniverse:comsat-spring-boot:0.7.0`
| [Spring Security](http://projects.spring.io/spring-security/) configuration support for fibers.                           | `co.paralleluniverse:comsat-spring-security:0.7.0`
| [JAX-RS client](https://jersey.java.net/documentation/latest/client.html) integration for HTTP calls with fibers.         | `co.paralleluniverse:comsat-jax-rs-client:0.7.0`
| [ApacheHttpClient](http://hc.apache.org/httpcomponents-client-ga/) integration for HTTP calls with fibers.                | `co.paralleluniverse:comsat-httpclient:0.7.0`
| [JDBI](http://jdbi.org/) integration with fibers.                                                                         | `co.paralleluniverse:comsat-jdbi:0.7.0`
| JDBC integration with fibers.                                                                                             | `co.paralleluniverse:comsat-jdbc:0.7.0`
| [jOOQ](http://www.jooq.org/) integration with fibers.                                                                     | `co.paralleluniverse:comsat-jooq:0.7.0`
| MongoDB fiber-blocking integration for the [Allanbank API](http://www.allanbank.com/mongodb-async-driver/index.html).     | `co.paralleluniverse:comsat-mongodb-allanbank:0.7.0`
| [OkHttp](https://github.com/square/okhttp) HTTP+SPDY client integration.                                                  | `co.paralleluniverse:comsat-okhttp:0.7.0`
| The Web Actors API.                                                                                                       | `co.paralleluniverse:comsat-actors-api:0.7.0`
| Deploy HTTP, SSE and WebSocket Web Actors as [Undertow](http://undertow.io/) handlers.                                    | `co.paralleluniverse:comsat-actors-undertow:0.7.0`
| Deploy HTTP, SSE and WebSocket Web Actors as [Netty](http://netty.io/) handlers.                                          | `co.paralleluniverse:comsat-actors-netty:0.7.0`
| Deploy HTTP, SSE and WebSocket Web Actors in J2EE 7 Servlet and WebSocket (JSR-356) embedded and standalone containers.   | `co.paralleluniverse:comsat-actors-servlet:0.7.0`
| Use Comsat in the Tomcat servlet container without the java agent.                                                        | `co.paralleluniverse:comsat-tomcat-loader:0.7.0[:jdk8]` (for JDK 8 optionally add the `jdk8` classifier)
| Use Comsat in the Jetty servlet container without the java agent.                                                         | `co.paralleluniverse:comsat-jetty-loader:0.7.0[:jdk8]` (for JDK 8 optionally add the `jdk8` classifier)
| [Spring Framework](http://projects.spring.io/spring-framework/) Web integration allows using fiber-blocking controllers.  | `co.paralleluniverse:comsat-spring-web:0.7.0`
| [Apache Kafka](http://kafka.apache.org/) producer integration module.                                                     | `co.paralleluniverse:comsat-kafka:0.7.0`
| [Apache Shiro](http://shiro.apache.org/) realms integration module.                                                       | `co.paralleluniverse:comsat-shiro:0.7.0`

Or, build and install from sources (after installing [Gradle](http://www.gradle.org) locally) in your local maven repository with:

```
gradle install
```

The full testsuite can be run with `gradle build`.

## Usage

* [Documentation](http://docs.paralleluniverse.co/comsat/)
* [Javadoc](http://docs.paralleluniverse.co/comsat/javadoc)

A [Gradle template](https://github.com/puniverse/comsat-gradle-template) project and a [Maven archetype](https://github.com/puniverse/comsat-mvn-archetype) using various integration modules and featuring setup with both Dropwizard and standalone Tomcat are also available for jumpstart and study. Both have a `without-comsat` branch which is useful to clearly see the (minimal, if any) porting effort required (branches comparison works very well for this purporse).

There's a [Comsat-Ring Clojure Leiningen template](https://github.com/puniverse/comsat-ring-template) as well which includes an `auto-instrument` branch that doesn't need any explicit suspendable-marking code (`suspendable!`, `defsfn`, `sfn` etc.) thanks to [Pulsar's new auto-instrumentation feature](http://docs.paralleluniverse.co/pulsar/#automatic-instrumentation).

You can also have a look at [additional examples](https://github.com/puniverse/comsat-examples).

Finally there are several regularly updated third-party bootstrap projects: [Comsat + Dropwizard + jOOQ](https://github.com/circlespainter/comsat-jooq-gradle-template), [Comsat Web Actors Stock Quotes (ported from Akka)](https://github.com/circlespainter/quasar-stocks), [Spring MVC + Tomcat standalone servlet container](https://github.com/circlespainter/spring4-mvc-gradle-annotation-hello-world).

## Getting help

Questions and suggestions are welcome at this [forum/mailing list](https://groups.google.com/forum/#!forum/comsat-user).

## Contributions (including Pull Requests)

Please have a look at some brief [information for contributors](https://github.com/puniverse/comsat/blob/master/CONTRIBUTING.md).

## License

COMSAT is free software published under the following license:

```
Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.

This program and the accompanying materials are dual-licensed under
either the terms of the Eclipse Public License v1.0 as published by
the Eclipse Foundation

  or (per the licensee's choosing)

under the terms of the GNU Lesser General Public License version 3.0
as published by the Free Software Foundation.
```
