---
layout: default
title: Getting Started
weight: 1
---

## System requirements

Java 7 and is required to use Comsat.

## Using Leiningen {#lein}

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

## Activating Comsat

### Using a Java agent

Then, the following must be added to the java command line (or use your favorite build tool to add this as a JVM argument):

~~~ sh
-javaagent:path-to-quasar-jar.jar
~~~


## In Tomcat


## In Jetty


## Building Quasar {#build}

Clone the repository:

    git clone git://github.com/puniverse/quasar.git quasar

and run:

    ./gradlew

Or, if you have gradle installed, run:

    gradle



