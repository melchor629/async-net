package me.melchor9000.net.resolver;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Stores requests in a cache and manages TTLs of it
 */
public class DNSResolverCache {
    private static Map<String, Set<Entry<InetAddress>>> cache = new TreeMap<>();
    private static Map<String, Entry<String>> alias = new TreeMap<>();

    public static List<InetSocketAddress> dnsServers() {
        Hashtable env = new Hashtable();
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
        } catch(Exception ignore) {}
        return Collections.emptyList();
    }

    static Iterable<InetAddress> getAddresses(String name) {
        clear(name);
        if(cache.containsKey(name)) {
            Set<InetAddress> out = new HashSet<>();
            for(Entry<InetAddress> e : cache.get(name)) {
                out.add(e.value);
            }
            return out;
        } else if(alias.containsKey(name)) {
            return getAddresses(alias.get(name).value);
        }
        return null;
    }

    static void addAEntry(String name, DNSResourceRecord record) {
        clear(name);
        if(!cache.containsKey(name)) cache.put(name, new HashSet<Entry<InetAddress>>());
        cache.get(name).add(new Entry<>((InetAddress) ((DNSA) record.getData()).getAddress(), record.getTtl()));
    }

    static void addAAAAEntry(String name, DNSResourceRecord record) {
        clear(name);
        if(!cache.containsKey(name)) cache.put(name, new TreeSet<Entry<InetAddress>>());
        cache.get(name).add(new Entry<>((InetAddress) ((DNSAAAA) record.getData()).getAddress(), record.getTtl()));
    }

    static void addCNAMEEntry(String name, DNSResourceRecord record) {
        alias.put(name, new Entry<>(((DNSCNAME) record.getData()).getCname(), record.getTtl()));
    }

    private static void clear(String name) {
        if(cache.containsKey(name)) {
            Iterator<Entry<InetAddress>> it = cache.get(name).iterator();
            while(it.hasNext()) {
                if(!it.next().isValid()) it.remove();
            }
        }

        if(alias.containsKey(name)) {
            if(!alias.get(name).isValid()) {
                alias.remove(name);
            }
        }
    }

    private static class Entry<Value> {
        private Value value;
        private long endLive;

        private Entry(Value value, long ttl) {
            this.value = value;
            endLive = (System.currentTimeMillis() / 1000 + ttl) * 1000;
        }

        private boolean isValid() {
            return System.currentTimeMillis() < endLive;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Entry) {
                @SuppressWarnings("unchecked") Entry<Value> e = (Entry<Value>) o;
                return e.value.equals(e) && e.endLive == endLive;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value.hashCode() * 7 + (int) endLive;
        }
    }
}
