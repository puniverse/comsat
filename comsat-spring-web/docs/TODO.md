First release
=============

Goals
-----

- Fiber-blocking Spring controllers support

TODO
----

- Test (trying to reuse existing Spring ones as much as possible)
- Mirrored protected API can be useful to use in subclasses but can be tricky to override as it could require forwarding (when it's referenced in the original class) and it's not apparent when there's
  such a need; it could be better to hide the parts requiring forwarding, to implement it in advance or at least document such a need in the JavaDocs, especially if subclassing is not going to be
  common. Think about it.

Maybe first release
===================

TODO
----

- Fiber-blocking Spring standalone (Tomcat-based) web app

- Evaluate dynamic code generation strategies
  - Doesn't seem very useful for the main servlets hierarchy as those are container-called classes (so there must be static, named ones for the "configuration file" use case)
  - Protected proxies are currently static classes in Spring packages: 4.1.2 jars are not sealed but in future...
  - Impls
    - Java provides interface-based proxies to a provided instance and can only proxy public methods
      - Doesn't seem very useful in case of class-based APIs like mirroring servlets
    - CGI provides subclassing proxies and can proxy non-final protected methods but requires building the instance itself
      - Very used, github but little docs, runs on ASM
      - Useful for forwarding
      - Less useful for protected methods proxying
        - But ideal would by "mirroring" wrapping proxy generation in the same package (runtime class generation doesn't even need to cope with jar sealing, unlike static classes)
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
- Fiber-blocking support for other (related) frameworks (like Spring Web Flow? Could be as easy as adding few suspendables)
- Fiber-blocking resource serving (by adapting `ResourceServlet`)
- Fiber-blocking view rendering (adapting `View` impls., including tiles?)
- Fiber-blocking support for non-servlet APIs? (portlet, ...)
- Fiber-blocking Spring HTTP client
- Fiber-blocking Websocket
- Fiber-blocking Spring "Remoting" C/S (e.g. JAX-WS and other HTTP technologies only?)