---
layout: default
title: Welcome
---

Comsat is a set of open source libraries that integrate [Quasar](http://puniverse.github.io/quasar/) with various web or enterprise technologies (like HTTP services and database access). With Comsat, you can write web applications that are scalable and performant while, at the same time, are simple to code and maintain.

Comsat is not a web framework. In fact, it does not add new APIs at all (with one exception, Web Actors, mentioned later). It provides implementation to popular (and often, standard) APIs like Servlet, JAX-RS, and JDBC, that can be called within Quasar fibers. 

Comsat does provide one new API that you may choose to use: [Web Actors](manual/webactors.html). Web actors let you define a Quasar actor that receives and respnds to HTTP requests and web socket messages.

[Parallel Universe]: http://paralleluniverse.co

## License

    COMSAT
    Copyright © 2013 Parallel Universe
    
    This program and the accompanying materials are dual-licensed under
    either the terms of the Eclipse Public License v1.0 as published by
    the Eclipse Foundation
    
      or (per the licensee's choosing)  
    
    under the terms of the GNU Lesser General Public License version 3.0
    as published by the Free Software Foundation. 

## Acknowledgments

Parts of the documentation layout, icons and styles were taken from Google's [Polymer Project](http://www.polymer-project.org/).
