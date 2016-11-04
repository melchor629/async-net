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

import me.melchor9000.net.*;
import me.melchor9000.net.resolver.*;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class TestUDP {

    //See https://tools.ietf.org/html/rfc1035
    public static void main(String... args) throws Exception {
        final IOService service = new IOService();
        final UDPSocket socket = new UDPSocket(service);

        String a;
        if(args.length == 0) {
            System.out.print("Write a domain to check: ");
            a = new Scanner(System.in).nextLine();
        } else {
            a = args[0];
        }

        final String domain = a;

        System.out.println("Requesting " + domain + "...");me.melchor9000.net.resolver.DNSResolverCache.dnsServers();
        System.out.println();
        socket.bind();

        DNSMessage message = new DNSMessage();
        final int tid = message.getId();
        message.setRecursionDesired(true);

        DNSQuery query = new DNSQuery();
        query.setName(domain);
        query.setType(1);
        query.setClass(1);
        message.addQuery(query);

        socket.sendAsyncTo(message, new InetSocketAddress("192.168.1.1", 53)).whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) {
                final DNSMessage message = new DNSMessage();
                message.setId(tid);
                socket.receiveAsyncFrom(message).whenDone(new Callback<Future<UDPSocket.Packet>>() {
                    @Override
                    public void call(Future<UDPSocket.Packet> arg) {
                        if(!arg.isSuccessful()) {
                            arg.cause().printStackTrace();
                            System.exit(-1);
                        }

                        System.out.printf("Transaction ID: 0x%x\n", message.getId());
                        //System.out.printf("Flags: 0x%x\n", flags = b.readShort());
                        System.out.printf("Questions: %d\n", message.getCountQueries());
                        System.out.printf("Answers RRs: %d\n", message.getCountAnswers());
                        System.out.printf("Authority RRs: %d\n", message.getCountAuthorities());
                        System.out.printf("Additional RRs: %d\n", message.getCountAdditionals());
                        System.out.println();

                        if(message.getOpcode() != 0) {
                            System.out.println("Error: " + message.getOpcodeAsString());
                            System.exit(message.getOpcode());
                        }

                        System.out.println("Queries");
                        for(DNSQuery query : message.getQueries()) {
                            System.out.println("  Name: " + query.getName());
                            System.out.println("  Type: " + query.getTypeAsString());
                            System.out.println("  Class: " + query.getClassAsString());
                            System.out.println();
                        }

                        System.out.println("Answers");
                        for(DNSResourceRecord record : message.getAnswers()) {
                            System.out.println("  Name: " + record.getName());
                            System.out.println("  Type: " + record.getTypeAsString());
                            System.out.println("  Class: " + record.getClassAsString());
                            System.out.println("  TTL: " + record.getTtl());
                            System.out.println("  Data: " + record.getData());
                            System.out.println();
                        }

                        System.exit(0); //No mola nada esto, pero bueno
                    }
                });
            }
        });
    }
}
