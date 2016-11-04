package me.melchor9000.net.resolver;

import me.melchor9000.net.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

/**
 * DNS resolver for any domain.
 */
public class DNSResolver implements AutoCloseable {
    private UDPSocket socket;

    public DNSResolver(IOService service) {
        socket = new UDPSocket(service);
    }

    public Future<Iterable<InetAddress>> resolveAsync(final String name) {
        final FutureImpl<Iterable<InetAddress>> future = new FutureImpl<>(service, null);

        Iterable<InetAddress> resolved = DNSResolverCache.getAddresses(name);
        if(resolved != null) {
            future.postSuccessSafe(resolved);
        } else {
            final DNSMessage message = new DNSMessage();
            final int tid = message.getId();
            DNSQuery queryIPv4 = new DNSQuery();
            DNSQuery queryIPv6 = new DNSQuery();

            message.setRecursionDesired(true);
            queryIPv4.setName(name);
            queryIPv4.setType(DNSUtils.typeToInt("A"));
            queryIPv4.setClass(DNSUtils.classToInt("IN"));
            queryIPv6.setName(name);
            queryIPv6.setType(DNSUtils.typeToInt("AAAA"));
            queryIPv6.setClass(DNSUtils.classToInt("IN"));
            message.addQuery(queryIPv4);
            //message.addQuery(queryIPv6);

            final Iterator<InetSocketAddress> dnsServers = DNSResolverCache.dnsServers().iterator();
            final Callback<Future<Void>> sendCbk = new Callback<Future<Void>>() {
                @Override
                public void call(Future<Void> arg) throws Exception {
                    if(arg.isSuccessful()) {
                        final Callback<Future<Void>> self = this;
                        final DNSMessage message2 = new DNSMessage();
                        message2.setId(tid);
                        socket.receiveAsyncFrom(message2).whenDone(new Callback<Future<UDPSocket.Packet>>() {
                            @Override
                            public void call(Future<UDPSocket.Packet> arg) throws Exception {
                                if(arg.isSuccessful()) {
                                    if(message2.getOpcode() != 0) {
                                        if(dnsServers.hasNext()) {
                                            socket.sendAsyncTo(message, dnsServers.next()).whenDone(self);
                                        } else {
                                            future.postErrorSafe(new UnknownHostException(name));
                                        }
                                    } else {
                                        addAllRecords(name, message2.getAnswers());
                                        addAllRecords(name, message2.getAuthorities());
                                        addAllRecords(name, message2.getAdditionals());
                                        future.postSuccessSafe(DNSResolverCache.getAddresses(name));
                                    }
                                } else {
                                    future.postErrorSafe(arg.cause());
                                }
                            }
                        });
                    } else {
                        future.postErrorSafe(arg.cause());
                    }
                }
            };

            if(!socket.isOpen()) socket.bind();
            socket.sendAsyncTo(message, dnsServers.next()).whenDone(sendCbk);
        }
        return future;
    }

    public Iterable<InetAddress> resolve(String name) throws ExecutionException, InterruptedException {
        return resolveAsync(name).sync().getValue();
    }

    @Override
    public void close() {
        if(socket.isOpen()) socket.close();
    }

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
