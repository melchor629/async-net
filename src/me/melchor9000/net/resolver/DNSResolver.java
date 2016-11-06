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

package me.melchor9000.net.resolver;

import me.melchor9000.net.*;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * DNS resolver for any domain.
 */
public class DNSResolver implements AutoCloseable, Callback<Socket> {
    private UDPSocket socket;
    private IOService service;
    private List<Request> requests;
    private int tries = 2;

    public DNSResolver(IOService service) {
        socket = new UDPSocket(service);
        this.service = service;
        this.requests = new ArrayList<>();
        socket.addOnDataReceivedListener(this);
    }

    /**
     * Resolves the domain {@code name} to IPv4 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolveV4
     * @return a {@link Future} representing the task
     */
    public Future<Iterable<InetAddress>> resolveAsyncV4(final String name) {
        @SuppressWarnings("unchecked")
        final FutureImpl<Iterable<InetAddress>> f[] = (FutureImpl<Iterable<InetAddress>>[]) Array.newInstance(FutureImpl.class, 1);
        final FutureImpl<Iterable<InetAddress>> future = f[0] = new FutureImpl<>(service, new Procedure() {
            @Override
            public void call() {
                requests.remove(requestByFuture(f[0]));
            }
        });

        Iterable<InetAddress> resolved = DNSResolverCache.getAddressesIPv4(name);
        if(resolved != null) {
            future.postSuccess(resolved);
        } else {
            final DNSMessage message = new DNSMessage();
            DNSQuery queryIPv4 = new DNSQuery();

            message.setRecursionDesired(true);
            queryIPv4.setName(name);
            queryIPv4.setType(DNSUtils.typeToInt("A"));
            queryIPv4.setClass(DNSUtils.classToInt("IN"));
            message.addQuery(queryIPv4);

            doRequest(future, message, 4);
        }
        return future;
    }

    /**
     * Resolves the domain {@code name} to IPv6 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolveV4
     * @return a {@link Future} representing the task
     */
    public Future<Iterable<InetAddress>> resolveAsyncV6(final String name) {
        @SuppressWarnings("unchecked")
        final FutureImpl<Iterable<InetAddress>> f[] = (FutureImpl<Iterable<InetAddress>>[]) Array.newInstance(FutureImpl.class, 1);
        final FutureImpl<Iterable<InetAddress>> future = f[0] = new FutureImpl<>(service, new Procedure() {
            @Override
            public void call() {
                requests.remove(requestByFuture(f[0]));
            }
        });

        Iterable<InetAddress> resolved = DNSResolverCache.getAddressesIPv6(name);
        if(resolved != null) {
            future.postSuccess(resolved);
        } else {
            final DNSMessage message = new DNSMessage();
            DNSQuery queryIPv6 = new DNSQuery();

            message.setRecursionDesired(true);
            queryIPv6.setName(name);
            queryIPv6.setType(DNSUtils.typeToInt("AAAA"));
            queryIPv6.setClass(DNSUtils.classToInt("IN"));
            message.addQuery(queryIPv6);

            doRequest(future, message, 6);
        }
        return future;
    }

    /**
     * Resolves the domain {@code name} to IPv4 and IPv6 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolveV4
     * @return a {@link Future} representing the task
     */
    public Future<Iterable<InetAddress>> resolveAsync(final String name) {
        @SuppressWarnings("unchecked")
        final FutureImpl<Iterable<InetAddress>> f[] = (FutureImpl<Iterable<InetAddress>>[]) Array.newInstance(FutureImpl.class, 1);
        final FutureImpl<Iterable<InetAddress>> future = f[0] = new FutureImpl<>(service, new Procedure() {
            @Override
            public void call() {
                requests.remove(requestByFuture(f[0]));
            }
        });

        if(DNSResolverCache.hasIPv4(name) && DNSResolverCache.hasIPv6(name)) {
            future.postSuccess(DNSResolverCache.getAddresses(name));
        } else if(!DNSResolverCache.hasIPv4(name)) {
            resolveAsyncV4(name).whenDone(new Callback<Future<Iterable<InetAddress>>>() {
                @Override
                public void call(Future<Iterable<InetAddress>> arg) {
                    if(arg.isSuccessful()) future.postSuccess(DNSResolverCache.getAddresses(name));
                    else future.postError(arg.cause());
                }
            });
        } else if(!DNSResolverCache.hasIPv6(name)) {
            resolveAsyncV6(name).whenDone(new Callback<Future<Iterable<InetAddress>>>() {
                @Override
                public void call(Future<Iterable<InetAddress>> arg) {
                    if(arg.isSuccessful()) future.postSuccess(DNSResolverCache.getAddresses(name));
                    else future.postError(arg.cause());
                }
            });
        } else {
            final boolean hasFinished[] = new boolean[] { false, false };
            resolveAsyncV4(name).whenDone(new Callback<Future<Iterable<InetAddress>>>() {
                @Override
                public void call(Future<Iterable<InetAddress>> arg) {
                    hasFinished[0] = true;
                    if(arg.isSuccessful() && hasFinished[1]) if(!future.isDone()) future.postSuccess(DNSResolverCache.getAddresses(name));
                    else future.postError(arg.cause());
                }
            });
            resolveAsyncV6(name).whenDone(new Callback<Future<Iterable<InetAddress>>>() {
                @Override
                public void call(Future<Iterable<InetAddress>> arg) {
                    hasFinished[1] = true;
                    if(arg.isSuccessful() && hasFinished[0]) if(!future.isDone()) future.postSuccess(DNSResolverCache.getAddresses(name));
                    else future.postError(arg.cause());
                }
            });
        }

        return future;
    }

    /**
     * Resolves the domain {@code name} to IPv4 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolveV4
     * @return an {@link Iterable} object with the IP addresses or null
     */
    public Iterable<InetAddress> resolveV4(String name) {
        return resolveAsyncV4(name).sync().getValueNow();
    }

    /**
     * Resolves the domain {@code name} to IPv6 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolveV4
     * @return an {@link Iterable} object with the IP addresses or null
     */
    public Iterable<InetAddress> resolveV6(String name) {
        return resolveAsyncV6(name).sync().getValueNow();
    }

    /**
     * Resolves the domain {@code name} to IPv4 and IPv6 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolveV4
     * @return a {@link Future} representing the task
     */
    public Iterable<InetAddress> resolve(String name) {
        return resolveAsync(name).sync().getValueNow();
    }

    @Override
    public void close() {
        if(socket.isOpen()) socket.close();
    }

    /**
     * Closes the resolver asynchronously
     * @return a {@link Future} representing the close task
     */
    public Future<Void> closeAsync() {
        return socket.closeAsync();
    }

    private Request requestById(int id) {
        for(Request r : requests) {
            if(r.sentMessage.getId() == id) {
                return r;
            }
        }
        return null;
    }

    private Request requestByFuture(Future<Iterable<InetAddress>> id) {
        for(Request r : requests) {
            if(r.future == id) {
                return r;
            }
        }
        return null;
    }

    private void doRequest(final FutureImpl<Iterable<InetAddress>> future, final DNSMessage sentMessage, int type) {
        final Request r = new Request(future, sentMessage, DNSResolverCache.dnsServers().iterator(), type);
        requests.add(r);
        future.whenDone(new Callback<Future<Iterable<InetAddress>>>() {
            @Override
            public void call(Future<Iterable<InetAddress>> arg) {
                requests.remove(r);
            }
        });

        final Callback<Future<Void>> sendCbk = new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                r.tries--;
                if(!arg.isSuccessful() && !arg.isCancelled()) {
                    future.postError(arg.cause());
                }
            }
        };

        final Procedure timeoutProc = new Procedure() {
            @Override
            public void call() {
                if(!future.isDone()) {
                    if(r.tries > 0) {
                        socket.sendAsyncTo(sentMessage, r.currentServer).whenDone(sendCbk);
                        r.timeoutFuture = service.schedule(this, 1000);
                    } else {
                        if(r.it.hasNext()) {
                            socket.sendAsyncTo(sentMessage, r.currentServer = r.it.next()).whenDone(sendCbk);
                            r.timeoutFuture = service.schedule(this, 1000);
                        } else {
                            future.postError(new TimeoutException());
                        }
                    }
                }
            }
        };

        if(!socket.isOpen()) socket.bind();
        socket.sendAsyncTo(sentMessage, r.currentServer).whenDone(sendCbk);
        r.timeoutFuture = service.schedule(timeoutProc, 1000);
    }

    private void addAllRecords(String name, Iterable<DNSResourceRecord> a) {
        for(DNSResourceRecord record : a) {
            if(record.getTypeAsString().equals("A")) {
                DNSResolverCache.addAEntry(name, record);
            } else if(record.getTypeAsString().equals("CNAME")) {
                DNSResolverCache.addCNAMEEntry(name, record);
            } else if(record.getTypeAsString().equals("AAAA")) {
                DNSResolverCache.addAAAAEntry(name, record);
            }
        }
    }

    @Override
    public void call(Socket arg) {
        DNSMessage message = new DNSMessage();
        try {
            socket.receive(message);
        } catch(Throwable e) {
            return;
        }

        Request r = requestById(message.getId());
        if(r != null) {
            String name = r.sentMessage.getQueries().iterator().next().getName();
            if(r.timeoutFuture != null) r.timeoutFuture.cancel(true);
            if(message.getResponseCode() != 0) {
                if(message.getResponseCode() == 3) {
                    r.future.postError(new UnknownHostException(name));
                } else {
                    r.future.postError(new Error(DNSUtils.errorToString(message.getResponseCode())));
                }
            } else {
                addAllRecords(name, message.getAnswers());
                addAllRecords(name, message.getAuthorities());
                addAllRecords(name, message.getAdditionals());
                if(r.type == 6) {
                    r.future.postSuccess(DNSResolverCache.getAddressesIPv6(name));
                } else if(r.type == 4) {
                    r.future.postSuccess(DNSResolverCache.getAddressesIPv4(name));
                }
            }
        } else {
            throw new IllegalStateException("Received ID is not in requests ones: " + message.getId());
        }
    }

    private class Request {
        private FutureImpl<Iterable<InetAddress>> future;
        private Future<?> timeoutFuture;
        private DNSMessage sentMessage;
        private Iterator<InetSocketAddress> it;
        private InetSocketAddress currentServer;
        private int tries = DNSResolver.this.tries;
        private int type;

        private Request(FutureImpl<Iterable<InetAddress>> future, DNSMessage sentMessage, Iterator<InetSocketAddress> it, int type) {
            this.future = future;
            this.sentMessage = sentMessage;
            this.it = it;
            this.type = type;
            currentServer = it.next();
        }
    }
}
