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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Socket from any protocol which has the basic read and write
 * operations.
 */
public abstract class Socket implements AutoCloseable {
    protected final IOService service;
    protected Channel channel;
    protected Bootstrap bootstrap;
    protected long bytesRead, bytesWrote;
    private List<Callback<Socket>> readNotifications;

    Socket(IOService service) {
        this.service = service;
        this.bootstrap = new Bootstrap().group(service.group);
        readNotifications = new ArrayList<>();
    }

    Socket(IOService service, Channel channel) {
        this.service = service;
        this.channel = channel;
        readNotifications = new ArrayList<>();
    }

    /**
     * Closes the socket after the current operation is done. It can
     * throw an {@link Throwable} if an error occurs. This operation
     * blocks the thread.
     */
    @Override
    public void close() {
        checkSocketCreated("close");
        channel.close().syncUninterruptibly();
    }

    /**
     * Closes the socket after the current operation is done, and then
     * calls {@code cbk} with the result, either successful or failure.
     * @return Future about the task
     */
    public Future<Void> closeAsync() {
        checkSocketCreated("closeAsync");
        return createFuture(channel.close());
    }

    /**
     * Binds the socket to a local address. Depending on the implementation,
     * this could fail.
     * @param local address to bind
     */
    public void bind(SocketAddress local)  {
        channel = bootstrap.bind(local).syncUninterruptibly().channel();
        bootstrap = null;
    }

    /**
     * Binds the socket to any port in the default interface without
     * connecting to some remote endpoint. Depending on the implementation,
     * this could fail.
     */
    public void bind() {
        channel = bootstrap.bind(0).syncUninterruptibly().channel();
        bootstrap = null;
    }

    /**
     * Binds the socket to a random port and connects to the remote
     * endpoint.
     * @param endpoint remote endpoint
     * @throws InterruptedException When this {@link Thread} is interrupted
     */
    public void connect(SocketAddress endpoint) throws InterruptedException  {
        channel = bootstrap.connect(endpoint).sync().channel();
        bootstrap = null;
    }

    /**
     * Binds the socket to a random port and connects to the remote endpoint
     * asynchronously. Returns a {@link Future} where the task can be managed.
     * @param endpoint remote endpoint to connect
     * @return {@link Future} of the task
     */
    public Future<Void> connectAsync(SocketAddress endpoint) {
        return createFuture(bootstrap.connect(endpoint).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channel = future.channel();
                if(future.isSuccess()) {
                    bootstrap = null;
                }
            }
        }));
    }

    /**
     * Binds the socket to a random port and connects to the remote endpoint as
     * {@code address} and {@code port}.
     * @param address address of the remote endpoint
     * @param port port of the remote endpoint
     * @throws InterruptedException When this {@link Thread} is interrupted while waiting to connect
     */
    public void connect(InetAddress address, int port) throws InterruptedException {
        connect(new InetSocketAddress(address, port));
    }

    /**
     * Binds the socket to a random port and connects to the remote endpoint as
     * {@code hostName} and {@code port}.<br>
     * On <b>Android 4.0 or higher</b>, this method cannot be called from the
     * UI Thread.
     * @param hostName domain or IP address of the remote endpoint
     * @param port port of the remote endpoint
     * @throws InterruptedException When this {@link Thread} is interrupted while waiting to connect
     * @throws UnknownHostException If the hostName cannot be resolved
     */
    public void connect(String hostName, int port) throws UnknownHostException, InterruptedException {
        connect(InetAddress.getByName(hostName), port);
    }

    /**
     * Binds the socket to a random port and connects to the remote endpoint as
     * {@code address} and {@code port}.
     * @param address address of the remote endpoint
     * @param port port of the remote endpoint
     * @return {@link Future} of the task
     */
    public Future<Void> connectAsync(InetAddress address, int port) {
        return connectAsync(new InetSocketAddress(address, port));
    }

    /**
     * Binds the socket to a random port and connects to the remote endpoint as
     * {@code hostName} and {@code port}.
     * @param hostName domain or IP address of the remote endpoint
     * @param port port of the remote endpoint
     * @return {@link Future} of the task
     * @throws UnknownHostException If the hostName cannot be resolved
     */
    public Future<Void> connectAsync(String hostName, int port) throws UnknownHostException {
        return connectAsync(InetAddress.getByName(hostName), port);
    }

    /**
     * Receives some data from the socket and writes it into the {@link ByteBuf}
     * {@code data} a maximum of {@code bytes} bytes. This method don't read exactly
     * {@code bytes} bytes, only at most. To ensure read all the bytes, use one of the
     * {@link SocketUtil} methods.
     * @param data buffer where to write on all the data
     * @param bytes maximum number of bytes to read
     * @return number of bytes read currently
     * @throws Throwable if the receive operation fails, throws something
     */
    public abstract long receive(ByteBuf data, int bytes) throws Throwable;

    /**
     * Receives some data from the socket and writes it into the {@link ByteBuf}
     * {@code data} a maximum of {@code bytes} bytes. This method don't read exactly
     * {@code bytes} bytes, only at most. To ensure read all the bytes, use one of the
     * {@link SocketUtil} methods. This operation is done asynchronously, so returns a
     * {@link Future} for the task.
     * @param data buffer where to write on all the data
     * @param bytes maximum number of bytes to read
     * @return a {@link Future} representing this task
     */
    public abstract Future<Long> receiveAsync(ByteBuf data, int bytes);

    /**
     * Sends some data stored in the {@link ByteBuf} {@code data}, starting from its
     * current position with a size of {@code bytes}. Depending on the implementation
     * and its options, is possible that the data could not be sent in the moment, or
     * only a portion of it is sent.
     * @param data buffer with the data to be sent
     * @param bytes number of bytes to send
     * @return bytes sent
     * @throws InterruptedException If there's an interruption while sending the data
     */
    public long send(ByteBuf data, int bytes) throws InterruptedException {
        checkSocketCreated("send");
        ByteBuf buff = ByteBufAllocator.DEFAULT.buffer(bytes).retain();
        buff.writeBytes(data, 0, bytes);
        channel.writeAndFlush(buff).sync();
        buff.release();
        bytesWrote += bytes;
        return bytes;
    }

    /**
     * Sends some data stored in the {@link ByteBuf} {@code data}, starting from its
     * current position with a size of {@code bytes}. Depending on the implementation
     * and its options, is possible that the data could not be sent in the moment, or
     * only a portion of it is sent. This is an asynchronous operation, so returns a
     * {@link Future} representing the task.
     * @param data buffer with the data to be sent
     * @param bytes number of bytes to send
     * @return a {@link Future} representing this task
     */
    public Future<Void> sendAsync(ByteBuf data, final int bytes) {
        checkSocketCreated("sendAsync");
        final ByteBuf buff = ByteBufAllocator.DEFAULT.directBuffer(bytes).retain();
        buff.writeBytes(data, 0, bytes);
        return createFuture(channel.writeAndFlush(buff).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bytesWrote += bytes;
                buff.release();
            }
        }));
    }

    /**
     * Sends some data stored in the {@link ByteBuf} {@code data}, with the remaining
     * bytes of it. Depending on the implementation and its options, is possible that
     * the data could not be sent in the moment, or only a portion of it is sent. This
     * is an asynchronous operation, so returns a {@link Future} representing the task.
     * @param data buffer with the data to be sent
     * @return a {@link Future} representing this task
     */
    public Future<Void> sendAsync(ByteBuf data) {
        return sendAsync(data, data.readableBytes());
    }

    /**
     * Receives some data from the socket into the {@link ByteBuf}. This method
     * doesn't ensure to write exactly {@code data.remaining()} bytes. Use instead
     * {@link SocketUtil} methods.
     * @param data {@link ByteBuf} where to write the data
     * @return number of bytes read
     * @throws Throwable If something bad happens while receiving the data
     */
    public long receive(ByteBuf data) throws Throwable {
        return receive(data, data.writableBytes());
    }

    /**
     * Receives some data from the socket into the {@link ByteBuf}. This method
     * doesn't ensure to write exactly {@code data.remaining()} bytes. Use instead
     * {@link SocketUtil} methods. This is an asynchronous operation, so it returns
     * a {@link Future} about this task.
     * @param data {@link ByteBuf} where to write the data
     * @return a {@link Future} representing the task
     */
    public Future<Long> receiveAsync(ByteBuf data) {
        return receiveAsync(data, data.writableBytes());
    }

    /**
     * Sends some data stored in the {@link ByteBuf} {@code data}, starting from its
     * current position with a size of {@code data.remaining()} bytes. Depending on the implementation
     * and its options, is possible that the data could not be sent in the moment, or
     * only a portion of it is sent.
     * @param data {@link ByteBuf} with the data to be send
     * @return bytes sent
     * @throws InterruptedException if the send operation is interrupted
     */
    public long send(ByteBuf data) throws InterruptedException {
        return send(data, data.readableBytes());
    }

    /**
     * Sends the contents of the {@link String} using the default platform Charset,
     * without any extra characters. Depending on the implementation and its options,
     * is possible that the data could not be sent in the moment, or only a portion of
     * it is sent. That is an asynchronous operation, so it returns a {@link Future}
     * about this task.
     * @param data the {@link String} to send
     * @return a {@link Future} representing the task
     */
    public Future<Void> sendAsync(String data) {
        return sendAsync(Unpooled.wrappedBuffer(data.getBytes()));
    }

    /**
     * Sends the contents of the {@link String} using the default platform Charset,
     * without any extra characters. Depending on the implementation and its options,
     * is possible that the data could not be sent in the moment, or only a portion of
     * it is sent.
     * @param data the {@link String} to send
     * @return number of bytes sent
     * @throws InterruptedException if the send operation is interrupted
     */
    public long send(String data) throws InterruptedException {
        return send(Unpooled.wrappedBuffer(data.getBytes()));
    }

    /**
     * Changes an option of this socket with a new value. Returns true if
     * the option is changed.
     * @param type Option to change
     * @param value New value
     * @param <T> Type of the new value for the option
     * @return true if the option was set
     * @see ChannelOption
     */
    public <T> boolean setOption(ChannelOption<T> type, T value) {
        if(bootstrap != null)
            bootstrap.option(type, value);
        else
            return channel.config().setOption(type, value);
        return true;
    }

    /**
     * Gets the {@link SocketAddress} of the remote endpoint if this socket
     * is connected to one.
     * @return remote endpoint or {@code null} if not connected
     */
    public SocketAddress remoteEndpoint() {
        return channel != null ? channel.remoteAddress() : null;
    }

    /**
     * Gets the {@link SocketAddress} of the local socket, or returns
     * {@code null} if the socket is not created yet using one of the
     * connect or bind methods.
     * @return local endpoint
     */
    public SocketAddress localEndpoint() {
        return channel != null ? channel.localAddress() : null;
    }

    /**
     * @return true if the socket is open, false if it's closed
     */
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    /**
     * @return the number of bytes sent by this socket
     */
    public long sendBytes() {
        return bytesWrote;
    }

    /**
     * @return the number of bytes received by this socket
     */
    public long receivedBytes() {
        return bytesRead;
    }

    /**
     * Adds a {@link Callback} that will be called every time some data
     * is available to read. A receive operation called inside one of this
     * callbacks, will be added at the end of the read operation queue. If
     * there wasn't any receive operations before this one, a receive call
     * will not block and will return all possible data.
     * @param cbk listener for when some data is received
     */
    public void addOnDataReceivedListener(Callback<Socket> cbk) {
        readNotifications.add(cbk);
    }

    /**
     * Calls all methods that want to be notified when some data have
     * been received. Implementors must call always this method when
     * some data have been received.
     * @throws Exception some error
     */
    protected void fireReceivedData() throws Exception {
        for(Callback<Socket> cbk : readNotifications) cbk.call(this);
    }

    /**
     * Call this method to check if the socket is created, if it's not,
     * then throws a {@link SocketNotCreated} exception.
     * @param method method that was called
     */
    protected void checkSocketCreated(String method) {
        if(channel == null) throw new SocketNotCreated("Cannot call " + method + " before creating the Socket", this);
    }

    protected <ReturnType> FutureImpl<ReturnType> createFuture(Procedure whenCancelled) {
        return new FutureImpl<>(service, whenCancelled);
    }

    protected <ReturnType> Future<ReturnType> createFuture(io.netty.util.concurrent.Future<ReturnType> n) {
        return new NettyFuture<>(n, service, null);
    }
}
