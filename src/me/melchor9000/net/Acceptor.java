package me.melchor9000.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Allows to accept connections from others hosts. Basic for a server.
 */
public abstract class Acceptor<SocketType extends Socket> implements AutoCloseable {
    protected final IOService service;
    protected Channel channel;
    protected ServerBootstrap bootstrap;
    protected Callback<SocketType> onConnection;

    Acceptor(IOService service) {
        this.service = service;
        bootstrap = new ServerBootstrap().group(service.group);
    }

    Acceptor(IOService serverService, IOService workerService) {
        this.service = workerService;
        bootstrap = new ServerBootstrap().group(serverService.group, workerService.group);
    }

    @Override
    public void close() throws Exception {
        channel.close().sync();
    }

    /**
     * Stops the acceptor asynchronously.
     * @return a {@link Future} for the close task
     */
    public Future<Void> closeAsync() {
        return new NettyFuture<>(channel.close());
    }

    /**
     * Waits until the acceptor is closed. Cannot be interrupted.
     */
    public void waitForClose() {
        channel.closeFuture().syncUninterruptibly();
    }

    /**
     * Returns a {@link Future} that it results when the server is closed
     * @return a {@link Future} with the close task
     */
    public Future<Void> onClose() {
        return new NettyFuture<>(channel.closeFuture());
    }

    /**
     * Binds the acceptor to the {@link SocketAddress}.
     * @param address address to bind
     * @throws InterruptedException if it is interrupted
     */
    public void bind(SocketAddress address) throws InterruptedException {
        if(bootstrap != null) {
            channel = bootstrap.bind(address).sync().channel();
            bootstrap = null;
        } else {
            channel.bind(address).sync();
        }
    }

    /**
     * Binds the acceptor to the {@link InetAddress} and port {@code port}.
     * @param address address
     * @param port port
     * @throws InterruptedException if it is interrupted
     */
    public void bind(InetAddress address, int port) throws InterruptedException {
        if(bootstrap != null) {
            channel = bootstrap.bind(address, port).sync().channel();
            bootstrap = null;
        } else {
            channel.bind(new InetSocketAddress(address, port)).sync();
        }
    }

    /**
     * Binds the acceptor to the port {@code port}.
     * @param port port
     * @throws InterruptedException if it is interrupted
     */
    public void bind(int port) throws InterruptedException {
        if(bootstrap != null) {
            channel = bootstrap.bind(port).sync().channel();
            bootstrap = null;
        } else {
            channel.bind(new InetSocketAddress(port)).sync();
        }
    }

    /**
     * Accepts a connection from any client and returns the {@code Socket}.
     * @return a {@link Future} with the accept task
     */
    public abstract Future<SocketType> acceptAsync();

    /**
     * Waits until someone is connected to the acceptor, and returns its
     * {@code Socket}.
     * @return the {@link Socket} of the connection
     * @throws Exception if something bad happened
     */
    public SocketType accept() throws Exception {
        return acceptAsync().sync().getValue();
    }

    /**
     * Instead of using {@link #accept()} or {@link #acceptAsync()}, it is
     * possible to use a listener to receive any new connection to the
     * acceptor. It is complementary of the before calls, so if one of the
     * before methods is called, the new connection will be returned in this
     * calls, not in the listener.
     * @param cbk a listener that will be called when a new connection occurs
     */
    public void setOnConnectionListener(Callback<SocketType> cbk) {
        onConnection = cbk;
    }

    /**
     * Sets an option for the acceptor
     * @param option option
     * @param value value to set
     * @param <T> type of the value for the option
     * @return true if could be changed
     */
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        if(bootstrap != null)
            bootstrap.option(option, value);
        else
            return channel.config().setOption(option, value);
        return true;
    }

    /**
     * Before calling any {@code bind()} methods, this can set some default
     * options for the new sockets created when a client is connected.
     * @param option option
     * @param value value to set
     * @param <T> type of the value for the option
     * @throws IllegalStateException If the server is already bound
     */
    public <T> void setChildOption(ChannelOption<T> option, T value) {
        if(bootstrap != null)
            bootstrap.option(option, value);
        else
            throw new IllegalStateException("Cannot set child options when the server is listening");
    }
}
