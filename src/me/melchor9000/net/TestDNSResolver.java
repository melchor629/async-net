package me.melchor9000.net;

import me.melchor9000.net.resolver.*;

import java.net.InetAddress;

/**
 * Test {@link DNSResolver}
 */
public class TestDNSResolver {
    public static void main(String... args) throws Exception {
        IOService service = new IOService();
        try(DNSResolver resolver = new DNSResolver(service)) {
            System.out.println("www.google.com");
            for(InetAddress address : resolver.resolve("www.google.com")) {
                System.out.print("  ");
                System.out.println(address);
            }

            System.out.println("\nwww.twitter.com");
            for(InetAddress address : resolver.resolve("www.twitter.com")) {
                System.out.print("  ");
                System.out.println(address);
            }

            System.out.println("\nmelchor9000.me");
            for(InetAddress address : resolver.resolve("melchor9000.me")) {
                System.out.print("  ");
                System.out.println(address);
            }

        } finally {
            service.cancel();
        }
    }
}
