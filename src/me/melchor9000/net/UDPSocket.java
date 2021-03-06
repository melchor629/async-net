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
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>UDP socket for any kind of operation with them.</p>
 * <p>
 *     This implementation creates a UDP socket that can act as
 *     server or client, or both. To use this socket you can either
 *     {@link #bind()} to a random port, {@link #bind(SocketAddress)} to
 *     a known port or {@code connect} to a remote endpoint. In UDP,
 *     <i>connect</i> is not defined, instead we use this <i>connection</i>
 *     to discard any message that doesn't come from the remote endpoint
 *     and send only to the remote endpoint. A not connected socket could
 *     send to any endpoint and receive from any endpoint, using the special
 *     {@code receiveFrom} and {@code sendTo} methods.
 * </p>
 * <p>
 *     Receive operations reads message by message, that's the essence of
 *     User Datagram Protocol (UDP). Any received packet is stored in a queue
 *     and every {@code receive} operation will read only one of this, if
 *     is possible. If the {@code buffer} not has enough capacity, will fail
 *     the receive operation. In this case, will throw a {@link NotEnoughSpaceForPacketException}
 *     with some useful information about the packet. In case of an asynchronous
 *     operation, you must read the packet inside the callback using a
 *     synchronous {@code receive} operation, otherwise, the packet will be
 *     discarded.
 * </p>
 * <p>
 *     Send operations also goes message by message. Every message is sent
 *     directly, without any intermediary buffers nor queue (except by the OS).
 * </p>
 * <p>
 *     Received messages from any sender ({@code receiveFrom} operations) will return
 *     a {@link Packet} which stores information about the Datagram received. The
 *     buffer inside it is your buffer.
 * </p>
 */
public class UDPSocket extends Socket {
    private DatagramChannel socket;
    private ConcurrentLinkedQueue<DatagramPacket> receivedPackets;
    private ConcurrentLinkedQueue<ReadOperation> readOperations;
    private ReadManager readManager;
    private volatile boolean canReadDirectly = false;

    /**
     * Create a UDP Socket
     * @param service {@link IOService} to attach this socket
     */
    public UDPSocket(IOService service) {
        this(service, null);
    }

    /**
     * Create a UDP Socket
     * @param service {@link IOService} to attach this socket
     * @param ip {@link ProtocolFamily}
     * @see java.net.StandardProtocolFamily
     */
    public UDPSocket(IOService service, ProtocolFamily ip) {
        super(service); //TODO protocol family
        bootstrap
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        ch.pipeline().addLast(readManager);
                    }
                });
        readOperations = new ConcurrentLinkedQueue<>();
        receivedPackets = new ConcurrentLinkedQueue<>();
        readManager = new ReadManager();
    }

    @Override
    public void bind(@NotNull SocketAddress local) {
        super.bind(local);
        socket = (DatagramChannel) channel;
    }

    @Override
    public void connect(@NotNull SocketAddress endpoint) throws InterruptedException {
        super.connect(endpoint);
        socket = (DatagramChannel) channel;
    }

    @NotNull
    @Override
    public Future<Void> connectAsync(@NotNull SocketAddress endpoint) {
        return super.connectAsync(endpoint).whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                socket = (DatagramChannel) channel;
            }
        });
    }

    /**
     * Sends the data contained in {@code data} with a length of {@code bytes}
     * to the remote endpoint {@code endpoint}.
     * @param data data to send
     * @param bytes number of bytes to send
     * @param endpoint remote endpoint
     * @return number of bytes sent
     */
    public long sendTo(ByteBuf data, int bytes, InetSocketAddress endpoint) {
        sendAsyncTo(data, bytes, endpoint).sync();
        return bytes;
    }

    /**
     * Sends the data contained in {@code data} with a length of
     * {@code data.remaining()} to the remote endpoint {@code endpoint}.
     * @param data data to send
     * @param endpoint remote endpoint
     * @return number of bytes sent
     */
    public long sendTo(ByteBuf data, InetSocketAddress endpoint) {
        return sendTo(data, data.readableBytes(), endpoint);
    }

    public Future<Void> sendAsyncTo(ByteBuf data, final int bytes, InetSocketAddress endpoint) {
        checkSocketCreated("sendAsyncTo");
        final ByteBuf buff = channel.alloc().directBuffer(bytes).retain();
        buff.writeBytes(data, bytes);
        return createFuture(channel.writeAndFlush(new DatagramPacket(buff, endpoint)).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                buff.release();
            }
        }));
    }

    public Future<Void> sendAsyncTo(ByteBuf data, InetSocketAddress endpoint) {
        return sendAsyncTo(data, data.readableBytes(), endpoint);
    }

    @Override
    public long receive(ByteBuf data, int bytes) throws Throwable {
        return receiveFrom(data, bytes).bytes;
    }

    public Packet receiveFrom(ByteBuf data, int bytes) throws Throwable {
        checkSocketCreated("receiveFrom");
        if(canReadDirectly) {
            if(receivedPackets.peek().content().readableBytes() > bytes)
                throw new NotEnoughSpaceForPacketException("Cannot write the message into the ByteBuf",
                        receivedPackets.peek().content().readableBytes(), receivedPackets.peek().sender());
            DatagramPacket packet = receivedPackets.poll();
            int bytes2 = packet.content().writableBytes();
            packet.content().writeBytes(data, packet.content().writableBytes());
            canReadDirectly = false;
            packet.release();
            return new Packet(data, packet.sender(), bytes2);
        } else {
            return receiveAsyncFrom(data, bytes).sync().getValueNow();
        }
    }

    public Packet receiveFrom(ByteBuf data) throws Throwable {
        return receiveFrom(data, data.writableBytes());
    }

    @NotNull
    @Override
    public Future<Long> receiveAsync(ByteBuf data, int bytes) {
        @SuppressWarnings("unchecked") final Future<Packet> leFuture[] = (FutureImpl<Packet>[]) Array.newInstance(FutureImpl.class, 1);
        final FutureImpl<Long> future = createFuture(new Procedure() {
            @Override
            public void call() {
                leFuture[0].cancel(true);
            }
        });

        leFuture[0] = receiveAsyncFrom(data, bytes).whenDone(new Callback<Future<Packet>>() {
            @Override
            public void call(Future<Packet> arg) {
                if(arg.isSuccessful()) future.postSuccess((long) arg.getValueNow().bytes);
                else future.postError(arg.cause());
            }
        });
        return future;
    }

    public Future<Packet> receiveAsyncFrom(ByteBuf data, int bytes) {
        checkSocketCreated("receiveAsyncFrom");
        final ReadOperation op[] = new ReadOperation[1];
        FutureImpl<Packet> future = createFuture(new Procedure() {
            @Override
            public void call() {
                readOperations.remove(op[0]);
            }
        });

        channel.read();
        readOperations.add(op[0] = new ReadOperation(future, bytes, data));
        if(!receivedPackets.isEmpty()) {
            try {
                readManager.checkAndSendData();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return future;
    }

    public Future<Packet> receiveAsyncFrom(ByteBuf data) {
        return receiveAsyncFrom(data, data.writableBytes());
    }

    public Future<Void> sendAsync(Serializable data) {
        return sendAsync(data.toByteBuf());
    }

    public long send(Serializable data) throws InterruptedException {
        return send(data.toByteBuf());
    }

    public Future<Void> sendAsyncTo(String data, InetSocketAddress remoteEndpoint) {
        return sendAsyncTo(Unpooled.wrappedBuffer(data.getBytes()), remoteEndpoint);
    }

    public Future<Void> sendAsyncTo(Serializable data, InetSocketAddress remoteEndpoint) {
        return sendAsyncTo(data.toByteBuf(), remoteEndpoint);
    }

    public long sendTo(String data, InetSocketAddress remoteEndpoint) throws InterruptedException {
        return sendTo(Unpooled.wrappedBuffer(data.getBytes()), remoteEndpoint);
    }

    public long sendTo(Serializable data, InetSocketAddress remoteEndpoint) throws InterruptedException {
        return sendTo(data.toByteBuf(), remoteEndpoint);
    }

    public <Type extends Serializable> Future<Type> receiveAsync(final Type data) {
        final ByteBuf b = Unpooled.buffer(1500).retain();
        @SuppressWarnings("unchecked") final Future<Long> leFuture[] = (FutureImpl<Long>[]) Array.newInstance(FutureImpl.class, 1);
        final FutureImpl<Type> future = createFuture(new Procedure() {
            @Override
            public void call() {
                leFuture[0].cancel(true);
            }
        });

        leFuture[0] = receiveAsync(b).whenDone(new Callback<Future<Long>>() {
            @Override
            public void call(Future<Long> arg) {
                try {
                    if(arg.isSuccessful()) {
                        try {
                            data.fromByteBuf(b);
                        } catch(DataNotRepresentsObject e) {
                            future.postError(e);
                        }
                        future.postSuccess(data);
                    } else {
                        future.postError(arg.cause());
                    }
                } finally {
                    b.release();
                }
            }
        });
        return future;
    }

    public <Type extends Serializable> Future<Packet> receiveAsyncFrom(final Type data) {
        final ByteBuf b = Unpooled.buffer(1500).retain();
        @SuppressWarnings("unchecked") final Future<Packet> leFuture[] = (FutureImpl<Packet>[]) Array.newInstance(FutureImpl.class, 1);
        final FutureImpl<Packet> future = createFuture(new Procedure() {
            @Override
            public void call() {
                leFuture[0].cancel(true);
            }
        });

        leFuture[0] = receiveAsyncFrom(b).whenDone(new Callback<Future<Packet>>() {
            @Override
            public void call(Future<Packet> arg) {
                try {
                    if(arg.isSuccessful()) {
                        try {
                            data.fromByteBuf(b);
                        } catch(DataNotRepresentsObject e) {
                            future.postError(e);
                        }
                        future.postSuccess(arg.getValueNow());
                    } else {
                        future.postError(arg.cause());
                    }
                } finally {
                    b.release();
                }
            }
        });
        return future;
    }

    public void receive(Serializable data) throws Throwable {
        ByteBuf b = Unpooled.buffer(1500);
        Packet p = receiveFrom(b);
        data.fromByteBuf(b);
    }

    public Packet receiveFrom(Serializable data) throws Throwable {
        ByteBuf b = Unpooled.buffer(1500);
        Packet p = receiveFrom(b);
        data.fromByteBuf(b);
        return p;
    }

    private class ReadManager extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            DatagramPacket message = (DatagramPacket) msg;
            bytesRead += message.content().readableBytes();
            receivedPackets.add(message);
            try {
                checkAndSendData();
            } finally {
                fireReceivedData();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
        }

        private void checkAndSendData() throws Exception {
            while(hasEnoughData()) {
                ReadOperation op = readOperations.poll();
                if(receivedPackets.peek().content().readableBytes() <= op.bytesToRead) {
                    DatagramPacket packet = receivedPackets.poll();
                    try {
                        int bytes = packet.content().readableBytes();
                        packet.content().readBytes(op.buffer, bytes);
                        op.cbk.postSuccess(new Packet(op.buffer, packet.sender(), bytes));
                    } finally {
                        packet.release();
                    }
                } else {
                    canReadDirectly = true;
                    op.cbk.postError(new NotEnoughSpaceForPacketException("Cannot write message into your buffer",
                            receivedPackets.peek().content().readableBytes(), receivedPackets.peek().sender()));
                    if(canReadDirectly) receivedPackets.poll().release();
                    canReadDirectly = false;
                }
            }
        }

        private boolean hasEnoughData() {
            return !readOperations.isEmpty() && !receivedPackets.isEmpty();
        }
    }

    private class ReadOperation {
        private FutureImpl<Packet> cbk;
        private int bytesToRead;
        private ByteBuf buffer;

        private ReadOperation(FutureImpl<Packet> cbk, int bytesToRead, ByteBuf buffer) {
            this.cbk = cbk;
            this.bytesToRead = bytesToRead;
            this.buffer = buffer;
        }
    }

    /**
     * Represents a Datagram of UDP
     */
    public class Packet {
        /**
         * Your {@link ByteBuf} of data
         */
        public final ByteBuf data;

        /**
         * The remote endpoint, where this datagram comes from
         */
        public final InetSocketAddress remoteEndpoint;

        /**
         * The number of bytes of valid data
         */
        public final int bytes;

        private Packet(ByteBuf data, InetSocketAddress remoteEndpoint, int bytes) {
            this.data = data;
            this.remoteEndpoint = remoteEndpoint;
            this.bytes = bytes;
        }
    }

    /**
     * When there isn't enough space to save a Datagram, this exception is raised to notify
     * the programmer to pass a more bigger buffer.
     */
    public static class NotEnoughSpaceForPacketException extends Exception {
        private long bytes;
        private InetSocketAddress remoteEndpoint;

        private NotEnoughSpaceForPacketException(String message, long bytes, InetSocketAddress end) {
            super(message);
            this.bytes = bytes;
            remoteEndpoint = end;
        }

        /**
         * @return the number of bytes of the received packet
         */
        public long getReceivedPacketSize() {
            return bytes;
        }

        /**
         * @return the remote endpoint, where the datagram comes from
         */
        public InetSocketAddress getRemoteEndpoint() {
            return remoteEndpoint;
        }
    }
}
