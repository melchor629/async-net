package me.melchor9000.net.resolver.serverLookup;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class OracleJREServerProvider extends DNSServerProvider {

    @Override
    public List<InetSocketAddress> getList() {

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx;
        try {
            ictx = new InitialDirContext(env);
            String dnsServers = (String) ictx.getEnvironment().get("java.naming.provider.url");
            List<InetSocketAddress> addresses = new ArrayList<>();
            for(String dnsUrl : dnsServers.split(" ")) {
                try {
                    dnsUrl = dnsUrl.replace("dns://", "");
                    String parts[] = dnsUrl.split(":");
                    addresses.add(new InetSocketAddress(InetAddress.getByName(parts[0]), parts.length == 2 ? Integer.parseInt(parts[1]) : 53));
                } catch(Exception ignore) {}
            }
            return addresses;
        } catch(Exception ignore) { }
        return null;
    }

}
