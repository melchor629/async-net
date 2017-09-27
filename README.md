# async-net
A asynchronous network library, based on netty. Easy to use, asynchronous, for Java 7 (Android too).

## How to use it
It is published in the maven central repository, so you can easily use it in your projects:

Gradle
```
compile 'me.melchor9000:async-net:1.0.4'
```

Maven
```
<dependency>
    <groupId>me.melchor9000</groupId>
    <artifactId>async-net</artifactId>
    <version>1.0.4</version>
</dependency>
```

You can always compile by hand and import the `.jar` into your project. To compile, you will need [Gradle][1] and [netty][2] library.

## About the library and it's idea
To use this library, you need to follow this simple rules:

 - Every socket needs a Thread to work on. That's why every socket (and the DNS resolver) needs an `IOService`.
 - The acceptor (aka servers) can run in different Threads from the Sockets.
 - Every asynchronous operation can be converted to synchronous.
 - Every asynchronous operation return `Future`s.
 - Synchronous operations can throw Exceptions.

So, to understand how to use the library, I will give you a quick explanation of every important class:

 - `IOService`: Creates one or more threads (it's up to you) and some loops that listen for events
 that can occur and notify to the correspondent Sockets or listeners about this event
 - `Future`: The asynchronous tasks sometimes return a value, or in some cases you just want to wait something to complete.
 And here comes the Futures. Allows you to execute some code when the task is done (successfully or not)
 or wait for it (synchronously).
 - `Socket`: Abstract class where you can manage a Socket, send and receive data, connect to servers, wait for close.
 Read carefully its documentation, has a lot of functions. Also, watch for TCPSocket and UDPSocket, its implementations.
 - `Acceptor`: Helps you to make a server. In general a Server has two components: the socket that accept client sockets
 and client sockets. The acceptor only accepts connections and gives you the client sockets.
 - `SSL`: Also there's an implementation of SSL/TLS sockets and acceptors over TCPSocket and TCPAcceptor. Note that this
 implementations don't handle well with Android, but there's workarounds annotated in their documentations.
 - `DNSResolver`: As its name indicates, resolves domains into IPv4 and/or IPv6. Internally uses an UDPSocket. And the
 DNS servers is obtained through different methods that one of them must be provided. See its documentation for info
 about it.

## Examples
You can see [TestTCP.java][4], [TestUDP.java][5], [TestTCPServer.java][6] [TestSSL.java][7] as examples.

[TestTCP.java][4] is a Swing application that make HTTP `GET` requests
using the domain you put. Also you can add the path (like `melchor9000.me/eugl/`), but never start with `http://`. Is an example of how to use a TCP socket asynchronously, useful for GUI apps.

[TestUDP.java][5] is a simple `dig` command, shows you some information about a domain you choose. Demonstrates how to use an UDP
socket, not connected to any host, and in asynchronous mode too. Also, how to send and receive messages from and into Java Objects
with the help of `Serializable` class.

[TestTCPServer.java][6] is an simple echo server. Anything it receives, will return to the sender. Exposes the way to create a TCP acceptor and how to use synchronous calls to the library API. You can test the example with `telnet localhost 4321`.

## Why this library?
I came from programming net code in C++ with [Boost asio][3] library, that gives the programmer the ability to mix sync with async calls to sockets and I had the need to do the same in Java. I wanted to understand Java NIO but it is really complex for me in that moment.

So I decided to search a bit and I found [netty][2]. But this library at all is a kind complex but very powerful. And that wasn't my needs at all. I tried to make a library that is kind similar to [Boost asio][3]'s but more easy to use, and compatible with Android. And that's how appeared `async-net`.


 [1]: https://gradle.org
 [2]: http://netty.io
 [3]: http://www.boost.org/doc/libs/1_62_0/doc/html/boost_asio.html
 [4]: https://github.com/melchor629/async-net/blob/master/src/TestTCP.java
 [5]: https://github.com/melchor629/async-net/blob/master/src/TestUDP.java
 [6]: https://github.com/melchor629/async-net/blob/master/src/TestTCPServer.java
 [7]: https://github.com/melchor629/async-net/blob/master/src/TestSSL.java
