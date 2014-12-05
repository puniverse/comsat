Goals
-----

- Spring MVC: fiber-blocking Spring controllers support
- Spring Security: let fibers play nicely with security context
- Spring Boot: auto-configuring Comsat's fiber-enabled Spring Web MVC integration (instead of standard Spring Web MVC)

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

- User docs