Goal
----

Supporting (most used types of?) fiber-blocking Spring controllers (not considering views)

TODO
----

* `FiberHttpServletBean` and `FiberFrameworkServlet`: consider delegating HTTP-serving methods to original impls. and enlisting them through `META-INF/suspendables` instead of re-implementing them
* Finish mirroring `DispatcherSevlet`
* Mirror HandlerAdapter hierarchy?
  * It could be possible to instrument the existing ones through `META-INF/suspendables` as there's no need to make them implement an existing base class
    * Check that proxy-based (JDK / cglib) Spring AOP doesn't interfer with this or other parts
* Mirror ResourceServlet?
* Other servlets (or APIs) to be mirrored?
* Test (trying to reuse existing Spring ones as much as possible)