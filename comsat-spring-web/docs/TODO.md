First release
=============

Goals
-----

- Fiber-blocking Spring controllers support
- Spring Boot integration: auto-configuring Comsat's fiber-enabled Spring Web MVC integration (instead of standard Spring Web MVC)

TODO
----

- Automatic testsuite (try to reuse existing Spring & Spring Boot ones as much as possible)
  - Spring Boot samples (used as Spring Boot integration testsuite) are perfect, too bad they aren't releasing them in any artifact form:
    https://github.com/spring-projects/spring-boot/issues/1973 ; better to initially include them
  - More than 2/3 tests of Actuator Sample work, the ones failing all seem related to post-filtering not working, which seems somewhat
    related to the async servlet mode
  - Actually Spring has got its own Servlet 3.0 Async wrappers and listeners system (`AsyncWebRequest`, `StandardServletAsyncWebRequest`,
    `WebAsyncUtils`) that supposedly would make filters and servlet work generally in async mode, except parts of it (`DispatchServlet`,
    `RequestMappingHandlerAdapter` but possibly others too) assume it to be used not to generally manage async requests, but only to
    implement its own async controller method support (`WebAsyncManager`).
    - Trying to split the fiber task allocation between a priority servlet filter and the Dispatcher servlet. Requires support for `filterChain.doFilter()` in seperate thread which should be supported by servlet-3.0 compatible containers but seems to have problems at least under tomcat 8.0.15 (non circumventable) and jetty (maybe circumventable).
    - If everything else fails, rescoping to adapt the latter only, without starting async in the servlet. The baseline goal (fiber-blocking controller
      methods) would still be reached and this adaptation would need to be done anyway. Plus the request should be put async in the
      first filter in order to cover the entire processing and not so much in the `DispatchetServlet` alone.
- Currently working only with Quasar's synchronized methods instrumentation override because of:
  - Spring controller instrumentation call path blocker (in practice it only synchronizes if configured to do so):

```
[quasar] ERROR: while transforming org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter: Unable to instrument org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter#handleInternal(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView; because of synchronization
```

  - Fixing it would require copying and adapting (and then maintaining big code portion from `RequestMappingHandlerAdapter`, maybe better to relate with
    Spring guys to see if they can open things up a bit. Plus synchronization actually happens only when configured to to so (and default is false).
    - Check when https://jira.spring.io/browse/SPR-12460 is released
- Autoconfigure Quasar-classloader-empowered Tomcat and Jetty containers (i.e. write FiberEmbeddedServletContainerAutoConfiguration,
  FiberTomcatEmbeddedServletContainerFactory, FiberJettyEmbeddedServletContainerFactory reusing as much as possible existing ones)
    - Support both JDK7 and JDK8
  - Does dynamic instrumentation support flag for synchronized methods?

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

- Fiber-blocking Spring standalone (Tomcat-based) web app

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
