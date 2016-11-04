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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>TCP Socket for connexions to servers.</p>
 * <p>
 *     This implementation creates a TCP Socket for client only connections.
 *     To use the Socket you must connect to a remote endpoint using one of
 *     the available {@code connect} or {@code connectAsync} methods available.
 *     It is not recommended to call any {@code bind} methods because it will
 *     replace the current socket (if it is connected) with a new not connected.
 * </p>
 * <p>
 *     Receive operations will return when there is some data, that is, it will not
 *     ensure read an exact quantity of bytes. Receive operations stores all unread
 *     data into an intermediary <i>Buffer</i>. When it is full, new data gets discarded.
 *     Internal buffer, normally, has capacity for 146KB (not KiB). To know if there's bytes
 *     to read, you can call {@link #readableBytes()}.
 * </p>
 * <p>
 *     Send operations will send directly to the remote endpoint, flushing anything pending to send.
 *     This also depends on if Nagle's Algorithm is enabled or not.
 * </p>
 * <p>
 *     A TCP Connection can shutdown the output or the input or both streams. This can also be
 *     accomplished with {@link #shutdownOutput()}, {@link #shutdownOutputAsync()},
 *     {@link #shutdownInput()}, {@link #shutdownInputAsync()}, {@link #shutdown()} or
 *     {@link #shutdownAsync()}.
 * </p>
 * <p>
 *     When closed, all write and read operations will fail. {@link #isOpen()} will return null.
 *     Is possible to be notified when the socket is closed, by you or by the remote endpoint,
 *     using {@link #onClose()}, or the synchronous version {@link #waitUntilClose()}.
 * </p>
 */
public class TCPSocket extends Socket {
    private SocketChannel socket;
    private ByteBuf readBuffer;
    private ConcurrentLinkedQueue<ReadOperation> readOperations;
    private ReadManager readManager;
    private volatile boolean isClosed = false;

    /**
     * Creates a new TCP Socket
     * @param service {@link IOService} to attach this socket
     */
    public TCPSocket(IOService service) {
        super(service);
        bootstrap
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(readManager);
                    }
                });
        readBuffer = ByteBufAllocator.DEFAULT.directBuffer(1460, 1460 * 100).retain();
        readOperations = new ConcurrentLinkedQueue<>();
        readManager = new ReadManager();
    }

    TCPSocket(TCPAcceptor acceptor, SocketChannel socket) {
        super(acceptor.service);
        channel = this.socket = socket;
        readBuffer = ByteBufAllocator.DEFAULT.directBuffer(1460, 1460 * 100).retain();
        readOperations = new ConcurrentLinkedQueue<>();
        readManager = new ReadManager();
        socket.pipeline().addLast(readManager);
    }

    @Override
    public void bind(SocketAddress address) {
        super.bind(address);
        channelCreated();
    }

    @Override
    public void connect(SocketAddress address) throws InterruptedException {
        super.connect(address);
        channelCreated();
    }

    @Override
    public Future<Void> connectAsync(SocketAddress endpoint) {
        return super.connectAsync(endpoint).whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                channelCreated();
            }
        });
    }

    @Override
    public long receive(ByteBuf data, int bytes) throws Throwable {
        checkSocketCreated("receive");
        return isClosed ? -1 : receiveAsync(data, bytes).getValue();
    }

    @Override
    public Future<Long> receiveAsync(ByteBuf data, int bytes) {
        checkSocketCreated("receiveAsync");
        final ReadOperation op[] = new ReadOperation[1];
        final FutureImpl<Long> future = createFuture(new Procedure() {
            @Override
            public void call() {
                readOperations.remove(op[0]);
            }
        });

        if(!isClosed) {
            readOperations.add(op[0] = new ReadOperation(future, bytes, data));
            if(readManager.hasEnoughData()) {
                try {
                    readManager.checkAndSendData();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            service.post(new Procedure() {
                @Override
                public void call() {
                    try {
                        future.postError(new IOException("End of stream"));
                    } catch(Exception e) {
                        System.out.println("Captured exception on Future.postError()");
                        e.printStackTrace();
                    }
                }
            });
        }
        return future;
    }

    /**
     * Ends the output stream of this connection, sending before any data pending
     * to send. After shutdown, any attempt to send anything will fail with an
     * {@link java.io.IOException}.
     */
    public void shutdownOutput() {
        checkSocketCreated("shutdownOutput");
        socket.shutdownOutput().syncUninterruptibly();
    }

    /**
     * Ends the output stream of this connection, sending before any data pending
     * to send. After shutdown, any attempt to send anything will fail with an
     * {@link java.io.IOException}.
     * @return {@link Future} of this task
     */
    public Future<Void> shutdownOutputAsync() {
        checkSocketCreated("shutdownOutputAsync");
        return createFuture(socket.shutdownOutput());
    }

    /**
     * Ends the input stream of this connection, discarding any pending data to be
     * acknowledged. Any attempt to receive some data will fail with an {@link IOException}
     * or returning -1.
     * {@link java.io.IOException}.
     */
    public void shutdownInput() {
        checkSocketCreated("shutdownInput");
        socket.shutdownInput().syncUninterruptibly();
    }

    /**
     * Ends the input stream of this connection, discarding any pending data to be
     * acknowledged. Any attempt to receive some data will fail with an {@link IOException}
     * or returning -1.
     * {@link java.io.IOException}.
     * @return {@link Future} of this task
     */
    public Future<Void> shutdownInputAsync() {
        checkSocketCreated("shutdownInputAsync");
        return createFuture(socket.shutdownInput());
    }

    /**
     * Ends the input and output stream of this connection, but not closes
     * the connection.
     * @see #shutdownInput() About shutdown the input stream
     * @see #shutdownOutput() About shutdown the Output stream
     */
    public void shutdown() {
        checkSocketCreated("shutdown");
        socket.shutdown().syncUninterruptibly();
    }

    /**
     * Ends the input and output stream of this connection, but not closes
     * the connection.
     * @return {@link Future} of this task
     * @see #shutdownInputAsync() About shutdown the input stream
     * @see #shutdownOutputAsync() About shutdown the Output stream
     */
    public Future<Void> shutdownAsync() {
        checkSocketCreated("shutdownAsync");
        return createFuture(socket.shutdown());
    }

    /**
     * @return the number of bytes that can be read, or -1 if EOF
     */
    public int readableBytes() {
        return isClosed ? -1 : readBuffer.readableBytes();
    }

    /**
     * Waits until this socket is closed. Any interruption won't affect
     * the wait.
     */
    public void waitUntilClose() {
        socket.closeFuture().syncUninterruptibly();
    }

    /**
     * Returns a {@link Future} that will complete when this socket is
     * closed.
     * @return a {@link Future} for the task
     */
    public Future<Void> onClose() {
        checkSocketCreated("onClose");
        return createFuture(socket.closeFuture());
    }

    @Override
    public void close() {
        super.close();
        readBuffer.release();
    }

    @Override
    public Future<Void> closeAsync() {
        return super.closeAsync().whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                readBuffer.release();
            }
        });
    }

    private class ReadManager extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buff = (ByteBuf) msg;
            try {
                bytesRead += buff.readableBytes();
                if(readBuffer.writerIndex() < readBuffer.maxCapacity() && readBuffer.maxCapacity() - readBuffer.writerIndex() <= buff.readableBytes()) {
                    System.out.println("Discarded " + (buff.readableBytes() - readBuffer.writableBytes()) + " bytes");
                    readBuffer.writeBytes(buff, 0, readBuffer.writableBytes());
                    checkAndSendData();
                } else if(readBuffer.maxCapacity() - readBuffer.writerIndex() > buff.readableBytes()) {
                    readBuffer.writeBytes(buff);
                    checkAndSendData();
                } else {
                    System.out.println("oberflou");
                }
            } finally {
                buff.release();
                fireReceivedData();
            }
        }
/*
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.channel().flush();
            ctx.read();
        }
*/
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
        }

        private void checkAndSendData() throws Exception {
            while(hasEnoughData()) {
                ReadOperation op = readOperations.poll();
                if(op.bytesToRead <= readBuffer.readableBytes()) {
                    readBuffer.readBytes(op.buffer, op.bytesToRead);
                    op.cbk.postSuccess((long) op.bytesToRead);
                } else {
                    long a = readBuffer.readableBytes();
                    readBuffer.readBytes(op.buffer, (int) a);
                    op.cbk.postSuccess(a);
                }
            }

            //Reset buffer positions
            if(readBuffer.readableBytes() == 0)
                readBuffer.readerIndex(0).writerIndex(0);
        }

        private boolean hasEnoughData() {
            return !readOperations.isEmpty() && readBuffer.readableBytes() != 0;
        }
    }

    private class ReadOperation {
        private FutureImpl<Long> cbk;
        private int bytesToRead;
        private ByteBuf buffer;

        private ReadOperation(FutureImpl<Long> cbk, int bytesToRead, ByteBuf buffer) {
            this.cbk = cbk;
            this.bytesToRead = bytesToRead;
            this.buffer = buffer;
        }
    }

    private void channelCreated() {
        socket = (SocketChannel) channel;
        if(socket != null) {
            socket.closeFuture().addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {
                @Override
                public void operationComplete(io.netty.util.concurrent.Future<? super Void> future) throws Exception {
                    isClosed = true;
                }
            });
        }
    }
}
