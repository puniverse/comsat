Goal
----

Supporting (most used types of?) fiber-blocking Spring controllers (not considering views)

TODO
----

* Mirror HandlerAdapter hierarchy?
  * It could be possible to instrument the existing ones through `META-INF/suspendables` as there's no need to make them implement an existing base class
* Mirror ResourceServlet?
* Other servlets (or APIs) to be mirrored?
* Test (trying to reuse existing Spring ones as much as possible)