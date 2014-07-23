# COMSAT [![Build Status](https://travis-ci.org/puniverse/comsat.png?branch=master)](https://travis-ci.org/puniverse/comsat) [![Dependency Status](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f/badge.png)](https://www.versioneye.com/user/projects/52dfc913ec1375318800039f)<br/>Scalable, Concurrent Web Apps


## Getting started

In Maven:

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>ARTIFACT</artifactId>
    <version>0.2.0</version>
</dependency>
```

Where `ARTIFACT` is:

* `comsat-servlet` – Servlet integration for defining fiber-per-request servlets.
* `comsat-jersey-server` – Jersey server integration for defining REST services.
* `comsat-dropwizard` – Dropwizard integration including jersey, ApacheHttpClient and jdbi.
* `comsat-jax-rs-client` – JAX-RS client integration for calling HTTP services.
* `comsat-httpclient` – ApacheHttpClient integration for calling HTTP services.
* `comsat-retrofit` – Retrofit integration for calling HTTP services through nice interfaces.
* `comsat-jdbi` – JDBI integration for using the JDBI API in fibers.
* `comsat-jdbc` – JDBC integration for using the JDBC API in fibers.
* `comsat-jooq` – jOOQ integration for using the JOOQ API in fibers.
* `comsat-actors-api` – the Web Actors API
* `comsat-actors-servlet` – Enables WebSocket(JSR-356) usage through Web Actors API
* `comsat-tomcat-loader` – Enables using comsat in tomcat container without the need of javaAgent
* `comsat-jetty-loader` – Enables using comsat in jetty container without the need of javaAgent
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
