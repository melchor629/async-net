/*
    async-net: A basic asynchronous network library, based on netty
    Copyright (C) 2016  melchor629 (melchor9000@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package me.melchor9000.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    Acceptor(@NotNull IOService service) {
        this.service = service;
        bootstrap = new ServerBootstrap().group(service.group);
    }

    Acceptor(@NotNull IOService serverService, @NotNull IOService workerService) {
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
    public @NotNull Future<Void> closeAsync() {
        checkSocketCreated("closeAsync");
        return createFuture(channel.close());
    }

    /**
     * Waits until the acceptor is closed. Cannot be interrupted.
     */
    public void waitForClose() {
        checkSocketCreated("waitForClose");
        channel.closeFuture().syncUninterruptibly();
    }

    /**
     * Returns a {@link Future} that it results when the server is closed
     * @return a {@link Future} with the close task
     */
    public @NotNull Future<Void> onClose() {
        checkSocketCreated("onClose");
        return createFuture(channel.closeFuture());
    }

    /**
     * Binds the acceptor to the {@link SocketAddress}.
     * @param address address to bind
     * @throws InterruptedException if it is interrupted
     */
    public void bind(@NotNull SocketAddress address) throws InterruptedException {
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
    public void bind(@NotNull InetAddress address, int port) throws InterruptedException {
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
    public abstract @NotNull Future<SocketType> acceptAsync();

    /**
     * Waits until someone is connected to the acceptor, and returns its
     * {@code Socket}.
     * @return the {@link Socket} of the connection
     * @throws Exception if something bad happened
     */
    public @NotNull SocketType accept() throws Exception {
        return acceptAsync().sync().getValue();
    }

    /**
     * Instead of using {@link #accept()} or {@link #acceptAsync()}, it is
     * possible to use a listener to receive any new connection to the
     * acceptor. It is complementary of the before methods, so if one of the
     * before methods are called, the new connection will be returned in this
     * calls, not in the listener.
     * @param cbk a listener that will be called when a new connection occurs
     */
    public void setOnConnectionListener(@Nullable Callback<SocketType> cbk) {
        onConnection = cbk;
    }

    /**
     * Sets an option for the acceptor
     * @param option option
     * @param value value to set
     * @param <T> type of the value for the option
     * @return true if could be changed
     */
    public <T> boolean setOption(@NotNull ChannelOption<T> option, @NotNull T value) {
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
    public <T> void setChildOption(@NotNull ChannelOption<T> option, @NotNull T value) {
        if(bootstrap != null)
            bootstrap.option(option, value);
        else
            throw new IllegalStateException("Cannot set child options when the server is listening");
    }

    /**
     * Call this method to check if the socket is created, if it's not,
     * then throws a {@link SocketNotCreated} exception.
     * @param method method that was called
     */
    protected void checkSocketCreated(String method) {
        if(channel == null) throw new SocketNotCreated("Cannot call " + method + " before creating the Socket", null);
    }

    protected @NotNull <ReturnType> FutureImpl<ReturnType> createFuture(@NotNull Procedure whenCancelled) {
        return new FutureImpl<>(service, whenCancelled);
    }

    protected @NotNull <ReturnType> Future<ReturnType> createFuture(@NotNull io.netty.util.concurrent.Future<ReturnType> n) {
        return new NettyFuture<>(n, service, null);
    }
}
