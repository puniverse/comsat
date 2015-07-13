# *COMSAT*<br/>Scalable, Concurrent Web Apps
[![Build Status](http://img.shields.io/travis/puniverse/comsat.svg?style=flat)](https://travis-ci.org/puniverse/comsat) [![Dependency Status](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f/badge.png?style=flat)](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f) [![Version](http://img.shields.io/badge/version-0.4.0-blue.svg?style=flat)](https://github.com/puniverse/comsat/releases) [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![License](http://img.shields.io/badge/license-LGPL-blue.svg?style=flat)](https://www.gnu.org/licenses/lgpl.html)

## Getting started

In [Maven](https://maven.apache.org):

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>ARTIFACT</artifactId>
    <version>0.4.0</version>
</dependency>
```

The corresponding Gradle dependency is ```'co.paralleluniverse:ARTIFACT:0.4.0'```

Where `ARTIFACT` is:

* `comsat-servlet` – Servlet integration for defining fiber-per-request servlets.
* `comsat-ring-jetty9` - A fiber-blocking Clojure [Ring](https://github.com/ring-clojure/ring) adapter based on Jetty 9.2
* `comsat-httpkit` - [HTTP Kit](http://www.http-kit.org/client.html)-based fiber-blocking HTTP client.
* `comsat-jersey-server` – [Jersey server](https://jersey.java.net/) integration for defining REST services.
* `comsat-dropwizard` – [Dropwizard](http://dropwizard.io/) integration including Jersey, ApacheHttpClient and JDBI.
* `comsat-spring-webmvc` – [Spring Framework](http://projects.spring.io/spring-framework/) Web MVC fiber-blocking controller methods integration.
* `comsat-spring-boot` – [Spring Boot](http://projects.spring.io/spring-boot/) auto-configuration support for Web MVC controllers.
* `comsat-spring-security` – [Spring Security](http://projects.spring.io/spring-security/) configuration support for fibers.
* `comsat-jax-rs-client` – [JAX-RS client](https://jersey.java.net/documentation/latest/client.html) integration for calling HTTP services.
* `comsat-httpclient` – [ApacheHttpClient](http://hc.apache.org/httpcomponents-client-ga/) integration for calling HTTP services.
* `comsat-retrofit` – [Retrofit](http://square.github.io/retrofit/) integration.
* `comsat-jdbi` – [JDBI](http://jdbi.org/) integration for using the JDBI API in fibers.
* `comsat-jdbc` – JDBC integration for using the JDBC API in fibers.
* `comsat-jooq` – [jOOQ](http://www.jooq.org/) integration for using the jOOQ API in fibers.
* `comsat-mongodb-allanbank` – MongoDB integration for using the [allanbank API](http://www.allanbank.com/mongodb-async-driver/index.html)
* `comsat-okhttp` – [OkHttp](https://github.com/square/okhttp) HTTP+SPDY client integration.
* `comsat-actors-api` – the Web Actors API
* `comsat-actors-servlet` – Enables deploying HTTP, SSE and WebSocket Web Actors in J2EE 7 Servlet and WebSocket (JSR-356) embedded and standalone containers
* `comsat-tomcat-loader` – Enables using Comsat in Tomcat container without the need of javaAgent
* `comsat-spring-web` – [Spring Framework](http://projects.spring.io/spring-framework/) Web integration allows using fiber-blocking controllers

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

## License

COMSAT is free software published under the following license:

```
Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.

This program and the accompanying materials are dual-licensed under
either the terms of the Eclipse Public License v1.0 as published by
the Eclipse Foundation

  or (per the licensee's choosing)

under the terms of the GNU Lesser General Public License version 3.0
as published by the Free Software Foundation.
```
