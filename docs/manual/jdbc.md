---
layout: default
title: JDBC
weight: 1
---

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

The `comsat-jdbc` project integrates the JDBC API with applications employing Quasar fibers (or actors).



[`FiberDataSource`]({{javadoc}}/fibers/jdbc/FiberDataSource.html)

## Fibers {#fibers}

Quasar's chief contribution is that of the lightweight thread, called *fiber* in Quasar.  
Fibers provide functionality similar to threads, and a similar API, but they're not managed by the OS. They are lightweight in terms of RAM (an idle fiber occupies ~400 bytes of RAM) and put a far lesser burden on the CPU when task-switching. You can have millions of fibers in an application. If you are familiar with Go, fibers are like goroutines. Fibers in Quasar are scheduled by one or more [ForkJoinPool](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html)s. 

Fibers are not meant to replace threads in all circumstances. A fiber should be used when its body (the code it executes) blocks very often waiting on other fibers (e.g. waiting for messages sent by other fibers on a channel, or waiting for the value of a dataflow-variable). For long-running computations that rarely block, traditional threads are preferable. Fortunately, as we shall see, fibers and threads interoperate very well.

Fibers are especially useful for replacing callback-ridden asynchronous code. They allow you to enjoy the scalability and performance benefits of asynchronous code while keeping the simple to use and understand threaded modedl.

### Using Fibers

A fiber is represented by the [`Fiber`]({{javadoc}}/fibers/Fiber.html) class. Similarly to a thread, you spawn a fiber like so:

~~~ java
new Fiber<V>() {
	@Override
	protected V run() throws SuspendExecution, InterruptedException {
        // your code
    }
}.start();
~~~