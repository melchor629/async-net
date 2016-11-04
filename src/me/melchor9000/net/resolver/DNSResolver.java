package me.melchor9000.net.resolver;

import me.melchor9000.net.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * DNS resolver for any domain.
 */
public class DNSResolver implements AutoCloseable {
    private UDPSocket socket;
    private IOService service;

    public DNSResolver(IOService service) {
        socket = new UDPSocket(service);
        this.service = service;
    }

    /**
     * Resolves the domain {@code name} to IPv4 and (if possible) IPv6 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolve
     * @return a {@link Future} representing the task
     */
    public Future<Iterable<InetAddress>> resolveAsync(final String name) {
        final FutureImpl<Iterable<InetAddress>> future = new FutureImpl<>(service, null);

        Iterable<InetAddress> resolved = DNSResolverCache.getAddresses(name);
        if(resolved != null) {
            future.postSuccess(resolved);
        } else {
            final DNSMessage message = new DNSMessage();
            final DNSMessage message4 = new DNSMessage();
            final int tid = message.getId();
            DNSQuery queryIPv4 = new DNSQuery();
            DNSQuery queryIPv6 = new DNSQuery();

            message.setRecursionDesired(true);
            message4.setRecursionDesired(true);
            queryIPv4.setName(name);
            queryIPv4.setType(DNSUtils.typeToInt("A"));
            queryIPv4.setClass(DNSUtils.classToInt("IN"));
            queryIPv6.setName(name);
            queryIPv6.setType(DNSUtils.typeToInt("AAAA"));
            queryIPv6.setClass(DNSUtils.classToInt("IN"));
            message.addQuery(queryIPv4);
            message.addQuery(queryIPv6);
            message4.addQuery(queryIPv4);
            message4.setId(tid);

            final Iterator<InetSocketAddress> dnsServers = DNSResolverCache.dnsServers().iterator();
            final InetSocketAddress currentServer[] = new InetSocketAddress[1];
            final boolean hasRepeatedRequestOnlyWithIPv4[] = new boolean[1];
            final Callback<Future<Void>> sendCbk = new Callback<Future<Void>>() {
                @Override
                public void call(Future<Void> arg) {
                    if(arg.isSuccessful()) {
                        final Callback<Future<Void>> self = this;
                        final DNSMessage message2 = new DNSMessage();
                        message2.setId(tid);
                        socket.receiveAsyncFrom(message2).whenDone(new Callback<Future<UDPSocket.Packet>>() {
                            @Override
                            public void call(Future<UDPSocket.Packet> arg) {
                                if(arg.isSuccessful()) {
                                    if(message2.getOpcode() != 0) {
                                        if(dnsServers.hasNext()) {
                                            hasRepeatedRequestOnlyWithIPv4[0] = false;
                                            socket.sendAsyncTo(message, currentServer[0] = dnsServers.next()).whenDone(self);
                                        } else {
                                            future.postError(new UnknownHostException(name));
                                        }
                                    } else {
                                        addAllRecords(name, message2.getAnswers());
                                        addAllRecords(name, message2.getAuthorities());
                                        addAllRecords(name, message2.getAdditionals());
                                        future.postSuccess(DNSResolverCache.getAddresses(name));
                                    }
                                } else if(!(arg.cause() instanceof CancellationException)) {
                                    future.postError(arg.cause());
                                } else {
                                    if(hasRepeatedRequestOnlyWithIPv4[0]) {
                                        hasRepeatedRequestOnlyWithIPv4[0] = false;
                                        socket.sendAsyncTo(message, currentServer[0] = dnsServers.next()).whenDone(self);
                                    } else {
                                        socket.sendAsyncTo(message4, currentServer[0]).whenDone(self);
                                        hasRepeatedRequestOnlyWithIPv4[0] = true;
                                    }
                                }
                            }
                        }).setTimeout(1000);
                    } else {
                        future.postError(arg.cause());
                    }
                }
            };

            if(!socket.isOpen()) socket.bind();
            socket.sendAsyncTo(message, currentServer[0] = dnsServers.next()).whenDone(sendCbk);
        }
        return future;
    }

    /**
     * Resolves the domain {@code name} to IPv4 and (if possible) IPv6 addresses.
     * Will try using any of the configured DNS servers in the OS.
     * @param name domain name to resolve
     * @return an {@link Iterable} object with the IP addresses or null
     */
    public Iterable<InetAddress> resolve(String name) throws ExecutionException, InterruptedException {
        return resolveAsync(name).sync().getValue();
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
}
