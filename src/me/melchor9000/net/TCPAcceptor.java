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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.lang.reflect.Array;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>
 *     Accepts TCP connections, and creates {@link TCPSocket} for any new one.
 * </p>
 * <p>
 *     When bound, any new connection is notified to the first {@link #acceptAsync()}
 *     or {@link #accept()} calls, passed to the listener or stored in a queue; all
 *     in this order. When the new connection is stored in the queue, a new call of
 *     {@link #acceptAsync()} or {@link #accept()} will return without blocking the
 *     first {@link TCPSocket} in the queue.
 * </p>
 * <p>
 *     The method {@link #pendingConnections()} will tell you how many sockets are
 *     waiting to be accepted, or how many {@code accept()} calls are waiting for
 *     new connections, if the value is negative.
 * </p>
 */
public class TCPAcceptor extends Acceptor<TCPSocket> {
    private ConcurrentLinkedQueue<FutureImpl<TCPSocket>> accepts;
    private ConcurrentLinkedQueue<TCPSocket> sockets;

    /**
     * Creates a TCP acceptor for server applications.
     * @param service {@link IOService} for the acceptor and the sockets
     */
    public TCPAcceptor(IOService service) {
        super(service);
        cnstr();
    }

    /**
     * Creates a TCP acceptor for server applications.
     * @param server {@link IOService} for the acceptor
     * @param worker {@link IOService} for the sockets
     */
    public TCPAcceptor(IOService server, IOService worker) {
        super(server, worker);
        cnstr();
    }

    private void cnstr() {
        bootstrap
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        TCPSocket socket = new TCPSocket(TCPAcceptor.this, ch);
                        if(!accepts.isEmpty()) {
                            accepts.poll().postSuccess(socket);
                        } else {
                            if(onConnection != null) {
                                onConnection.call(socket);
                            } else {
                                sockets.add(socket);
                            }
                        }
                    }
                });
        accepts = new ConcurrentLinkedQueue<>();
        sockets = new ConcurrentLinkedQueue<>();
    }

    @Override
    public Future<TCPSocket> acceptAsync() {
        checkSocketCreated("acceptAsync");
        @SuppressWarnings("unchecked") final FutureImpl<TCPSocket> a[] = (FutureImpl<TCPSocket>[]) Array.newInstance(FutureImpl.class, 1);
        a[0] = createFuture(new Procedure() {
            @Override
            public void call() {
                accepts.remove(a[0]);
            }
        });

        if(channel != null) {
            if(sockets.isEmpty()) accepts.add(a[0]);
            else a[0].postSuccess(sockets.poll());
        } else a[0].postError(new IllegalStateException("Socket is not listening"));
        return a[0];
    }

    @Override
    public void setOnConnectionListener(Callback<TCPSocket> cbk) {
        super.setOnConnectionListener(cbk);
        if(cbk != null) {
            while(!sockets.isEmpty()) {
                try {
                    cbk.call(sockets.poll());
                } catch (Exception ignore) {}
            }
        }
    }

    /**
     * @return if the value is positive, tells the number of pending
     * connections to be accepted; if negative, tells the number of
     * {@code accept()} calls that are waiting a new connection.
     */
    public int pendingConnections() {
        return sockets.size() - accepts.size();
    }
}
