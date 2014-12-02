First release
=============

Goals
-----

- Fiber-blocking Spring controllers support
- Spring Boot integration: auto-configuring Comsat's fiber-enabled Spring Web MVC integration (instead of standard Spring Web MVC)

Analysis
--------

Approaches and shortcomings are described here:
https://github.com/circlespainter/servlet3-filter-async-test#dispatching-requests-for-async-execution-in-spring-boot-with-or-without-actuator.

1. Starting the fiber dispatch in a special outmost (i.e. first) filter: best approach, least expensive, useful in general and covers the whole request but
   unfortunately both Jetty and Tomcat have bugs that have been reported but are difficult to circumvent.
2. Starting the fiber dispatch in a DispatcherServlet: requires patching the controller method handling as well, since Spring's bookeeping of the async
   processing (necessary to process triggers and filters when it is complete as the Servlet 3 spec offers no support for that) is tailor-made for its own
   async support. This approach limits the fiber execution scope to the servlet and is the most expensive implement.
3. New `HandlerMethod` implementation dispatching normal blocking controller methods to Deferred that will be completed in fibers. This approach seems to
   work very similarly (and as ok as) the normal Spring Web async handling (which is not perfect anyway) and is not terribly expensive. Unfortunately
   Spring's basic implementation is currently difficult to reuse, so some copy&paste is needed (3 improvement issues have been opened on that subject).

The chosen approach is 3. Some previous tests of the idea (and shortcomings) are available here https://github.com/circlespainter/spring-boot-async-test.

TODO
----

- Undestand better problematic tests
- Understand why MockMvc has problems with async
- Verify more thoroughly that Spring-supported async return types (`Future`, `DeferredResult`, `Callable`) are indeed working in new fiber method handler
- Separate spring-only and spring-boot parts as they have different dependencies (or rename project to make it apparent it
  is a spring-boot-web integration and not just a spring-web one)
- Understand why Spring error dispatches after async don't work if they are async themselves (and allow it if possible)

SIDE TODO
---------

- [QUASAR] Quasar-core agent-based instrumentation on servlet container Jetty (runner) doesn't work as ASM is missing (presumably because runtime dependency
  are in webapp classpath, not container classpath)
- [QUASAR] Quasar-core agent-based instrumentation on servlet container Jetty (runner) warns that the agent has not been loaded (presumably because runtime
  dependency are in webapp classpath, not container classpath)
- [QUASAR] Didn't get any meaningful error when park()ing in non-fiber; feasible to improve?

- [COMSAT] Tomcat 8.0.15 JDK8 loader seems not to work because of classpath linking problems (missing methods)
- [COMSAT] Tomcat 7.0.57 JDK7 loader seems not to work as some quasar classes and ASM are missing
- [COMSAT] Docs Jersey Server: both in `web.xml` and programmatic configuration, async must be set to true
- [COMSAT] Instrumentation for servlet containers: quasar jar alone doesn't work as it misses dependencies. Comsat loader jars seem perfect instead,
  only they need quasar's manifest.
- [COMSAT] Publish JavaDocs 0.2.0
- [COMSAT] `gradle install` blocks on installing a war
- [COMSAT] Publish the servlet-container-based template as an example
- [COMSAT] Annotation-based framework to generate dynamically forwarders (through cglib) and protected proxies (through ASM or Javassist);
  idea: `@Mirrors(Class)` on class, `@Proxy` and `@Rewrite` on features

Maybe first release
===================

TODO
----

- Evaluate dynamic code generation strategies
  - Doesn't seem very useful for the main servlets hierarchy as those are container-called classes (so there must be static, named ones for the "configuration
    file" use case)
  - Protected proxies are currently static classes in Spring packages: 4.1.2 jars are not sealed but in future...
  - Impls
    - Java provides interface-based proxies to a provided instance and can only proxy public methods
      - Doesn't seem very useful in case of class-based APIs like mirroring servlets
    - CGI provides subclassing proxies and can proxy non-final protected methods but requires building the instance itself
      - Very used, github but little docs, runs on ASM
      - Useful for forwarding
      - Less useful for protected methods proxying
        - But ideal would by "mirroring" wrapping proxy generation in the same package (runtime class generation doesn't even need to cope with jar sealing,
          unlike static classes)
    - Javassist provides source- and bytecode-level APIs for class generation and modification
      - Homey website, Japanese author, seems maintained on github by JBoss
      - Source-level API has limitations (the library seems to include some partial compiler in order to support it)
      - Useful for all uses, does not provide exact use case but it's quite high-level if using source-level API
    - ASM: similar to Javassist but more thorough docs and more used, seems only to support bytecode-level though
      - Useful for all uses, does not provide exact use case and it's a bit low-level
  - Especially useful knowledge when writing assisted tools for Comsat APIs integration
    - Unluckily we're probably going to resort to instrumentation to implement fibers for quite some time
    - Language level processing (analysis and code generation) might be better when allowing user customization is needed

Future
======

Possible Goals
--------------

- Fiber-blocking multipart support (could be as easy as adding few suspendables)
- Fiber-blocking support for other (related) frameworks (like Spring Web Flow? Could be as easy as adding few suspendables; Spring-WS requires some work for
  MessageDispatcherServlet and other machinery)
- Fiber-blocking resource serving (by adapting `ResourceServlet`)
- Fiber-blocking view rendering (adapting `View` impls., including tiles?)
- Fiber-blocking support for non-servlet APIs? (portlet, ...)
- Fiber-blocking Spring HTTP client
- Fiber-blocking Websocket
- Fiber-blocking Spring "Remoting" C/S (e.g. JAX-WS and other HTTP technologies only?)
