---
layout: default
title: Web Actors
weight: 2
---

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse/{% endcapture %}

Web Acotrs are [Quasar actors](http://puniverse.github.io/quasar/manual/actors.html) that receive and respond to messages from web clients. Web actors support HTTP, SSE and SSE (Server-Sent Events) messages, and are a convenient, efficient, and natural method for implementing the backend for interactive web applications.

## Deployment

WebActors are implemented on top of a web server. Currently, they can be deployed be deployed in any JavaEE 7 servlet container, but we are working on supporting deploying them on top of [Netty](http://netty.io/) and [Undertow](http://undertow.io/).

A web actor is attached to a web session. It can be spawned and attached manually (say, after the user logs in and the session is authenticated). The manual attachment API is container dependent: see [here](??????) for the API for JavaEE containers. A web actor can also be spawned and attached automatically by letting COMSAT spawn and attach a web actor to every newly created session. This method will be described below. Because a web actor consumes very few resources, spawning them automatically is sufficient in all but the most extreme circumstances.

For automatic deployment, all you have to do is define an actor class (one that extends `BasicActor` or `Actor`), and annotate it with the [`WebActor`]({{javadoc}}/comsat/webactors/WebActor.html) annotation. For example:

~~~ java
@WebActor(name="chat", httpUrlPatterns="/chat", webSocketUrlPatterns="/chat/ws")
public class ChatActor extends BasicActor<WebMessage, Void> {
	@Override
	protected Void doRun() {
		// ...
	}
}
~~~

In this example, all HTTP requests to the `/chat` resource, as well as all websocket messages to `/chat/ws` will be received as messages by the actor. A new `ChatActor` will be spawned for every new HTTP session.


For details, see the [Javadoc]({{javadoc}}/comsat/webactors/WebActor.html).

## Basic Operation

A web actor should be able to receive messages of type [`WebMessage`]({{javadoc}}/comsat/webactors/WebMessage.html). `WebMessage` is the supertype of all messages that can be received from or sent to a web client. The class encapsulates a message body, that can be either text or binary, and a *sender*, which, following a common actor pattern, is the actor that sent the message.

For messages received from the web client, the *sender* is a virtual actor representing the web client. You can perform normal actor operations on it, like `watch` to detect actor death; their semantics depend on the specific type of the message.

A single web actor instance may handle HTTP requests, emit SSE events, and handle one or more WebSocket connections.

## HTTP (REST Services)

A web actor is attached to one or more HTTP resources (as specified by `@WebActor`'s `httpUrlPatterns` property), and an actor instance is associated with a single HTTP session. Every HTTP request to the resource, associated with the session, will be received by the actor as an [`HttpRequest`]({{javadoc}}/comsat/webactors/HttpRequest.html) message. The actor can then respond with an [`HttpResponse`]({{javadoc}}/comsat/webactors/HttpResponse.html) message, which it sends to the request's sender. 

All HTTP request messages to a specific web actor instance will come from the same sender. If you `watch` that sender actor, it will emit an `ExitMessage` (signifying its death), when the session is terminated.

When you responsd to an `HttpRequest` with an `HttpResponse`, by default, the request stream will close. If, however, you wish to send the response's body in parts (e.g., for SSE, discussed in the next section), you may call `HttpRequest.openChannel`, which will return a Quasar *channel* that can be used to send [`WebDataMessage`]({{javadoc}}comsat/webactors/WebDataMessage.html)s messages to the stream. The stream will flush after each `WebDataMessage`'s body has been written to it. If `openChannel` has been called, the HTTP response stream will be closed when `close` is called on the returned channel.

Please refer to the [Javadoc]({{javadoc}}/comsat/webactors/HttpRequest.html) for details.

## SSE

SSE, or [Server-Sent Events](http://dev.w3.org/html5/eventsource/), is an HTML5 stnadard, supported by most modern browsers, for pushing discrete messages to the web client, without it sending new HTTP requests for each one. A good tutorial by Eric Bidelman on SSE can be found [here](http://www.html5rocks.com/en/tutorials/eventsource/basics/). An SSE stream is initiated with an HTTP request; then, each event message is written to the response stream and flushed, only the messages need to be encoded according to the SSE standard. SSE also specifies that if the connection is closed, the client will attempt to reconnect with a new request after a timeout, that can be set by the server.

The [`SSE`]({{javadoc}}/comsat/webactors/SSE.html) class contains a set of static utility methods that encode the events according to the SSE standard, and ensure that the response headers are set correctly (in terms of character encoding, etc.).

Here's how SSE can be used with a web actor:

~~~ java
SendPort<WebDataMessage> sseChannel = request.openChannel();
request.sender().send(SSE.startSSE(request).build());

// ... send events
sseChannel.send(new WebDataMessage(self(), SSE.event("this is an SSE event!")));

// ... close the stream
sseChannel.close()
~~~

It might be convient (and elegant) to wrap the channel returned by `openStream` with a *mapping channel* (see the Quasar docs), that will transform a message class representing the event into an SSE-encoded `WebDataMessage`.

## WebSockets

[WebSocket](http://en.wikipedia.org/wiki/WebSocket) is a new web protocol for low(ish)-latency, bi-directional communication between the client and the server. Web sockets are extremely useful for interactive web applications, and they fit beautifully with COMSAT Web Actors.

A web actor may register itself to handle web socket connections by declaring which WebSocket URIs it is interesten in, in the `@WebActor` annotations `webSocketUrlPatterns` property. Such a web actor will handle all web socket sessions at those URIs associated with the actor instance's HTTP session (a web socket is also associated with an HTTP session).

When the client connects to a web socket, the web actor will receive a [`WebSocketOpened`]({{javadoc}}/comsat/webactors/WebSocketOpened.html) message, and each following message will be received as a [`WebDataMessage`]({{javadoc}}comsat/webactors/WebDataMessage.html). The actor can send messages to the client by replying to the sender with `WebDataMessage`s of its own. 

The virtual actor that's the *sender* of the messages received from the client represents the WebSocket session; i.e., each open web socket will have a different actor as the sender of the messages. That virtual actor dies when the web socket connection closes.


