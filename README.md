# *COMSAT*<br/>Scalable, Concurrent Web Apps
[![Build Status](http://img.shields.io/travis/puniverse/comsat.svg?style=flat)](https://travis-ci.org/puniverse/comsat) [![Dependency Status](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f/badge.png?style=flat)](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f) [![Version](http://img.shields.io/badge/version-0.3.0-blue.svg?style=flat)](https://github.com/puniverse/comsat/releases) [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![License](http://img.shields.io/badge/license-LGPL-blue.svg?style=flat)](https://www.gnu.org/licenses/lgpl.html)

## Getting started

In Maven:

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>ARTIFACT</artifactId>
    <version>0.4.0-SNAPSHOT</version>
</dependency>
```

Where `ARTIFACT` is:

* `comsat-servlet` – Servlet integration for defining fiber-per-request servlets.
* `comsat-ring-jetty9` - A fiber-blocking Clojure [Ring](https://github.com/ring-clojure/ring) adapter based on Jetty 9.2
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
* `comsat-actors-servlet` – Enables HTTP and WebSocket (JSR-356) usage through Web Actors API
* `comsat-tomcat-loader` – Enables using Comsat in Tomcat container without the need of javaAgent
* `comsat-spring-web` – [Spring Framework](http://projects.spring.io/spring-framework/) Web integration allows using fiber-blocking controllers

Or, build from sources by running:

```
./gradlew
```

## Usage

* [Documentation](http://docs.paralleluniverse.co/comsat/)
* [Javadoc](http://docs.paralleluniverse.co/comsat/javadoc)

You can also study the examples [here](https://github.com/puniverse/comsat-examples).

## Getting help

Questions and suggestions are welcome at this [forum/mailing list](https://groups.google.com/forum/#!forum/comsat-user).

## License

COMSAT is free software published under the following license:

```
Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.

This program and the accompanying materials are dual-licensed under
either the terms of the Eclipse Public License v1.0 as published by
the Eclipse Foundation

  or (per the licensee's choosing)

under the terms of the GNU Lesser General Public License version 3.0
as published by the Free Software Foundation.
```

[![githalytics.com alpha](https://cruel-carlota.gopagoda.com/d376531837c3513ea73279fdbee7d48b "githalytics.com")](http://githalytics.com/puniverse/quasar)
