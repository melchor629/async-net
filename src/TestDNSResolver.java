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

import me.melchor9000.net.IOService;
import me.melchor9000.net.Procedure;
import me.melchor9000.net.resolver.*;
import me.melchor9000.net.resolver.serverLookup.OracleJREServerProvider;

import java.net.InetAddress;

/**
 * Test {@link DNSResolver}
 */
public class TestDNSResolver {
    public static void main(String... args) throws Exception {
        IOService service = new IOService();
        service.schedule(new Procedure() {
            @Override
            public void call() {
                System.out.println("Paco");
            }
        }, 1000);
        try(DNSResolver resolver = new DNSResolver(service, new OracleJREServerProvider())) {
            System.out.println("www.google.com");
            for(InetAddress address : resolver.resolveV4("www.google.com")) {
                System.out.print("  ");
                System.out.println(address);
            }

            System.out.println("\nwww.twitter.com");
            for(InetAddress address : resolver.resolveV4("www.twitter.com")) {
                System.out.print("  ");
                System.out.println(address);
            }

            System.out.println("\nmelchor9000.me");
            for(InetAddress address : resolver.resolveV4("melchor9000.me")) {
                System.out.print("  ");
                System.out.println(address);
            }
        } finally {
            service.cancel();
        }
    }
}
