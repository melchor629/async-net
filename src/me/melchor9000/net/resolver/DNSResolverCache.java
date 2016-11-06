package me.melchor9000.net.resolver;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Stores requests in a cache4 and manages TTLs of it
 */
public class DNSResolverCache {
    private static Map<String, Set<Entry<InetAddress>>> cache4 = new TreeMap<>();
    private static Map<String, Set<Entry<InetAddress>>> cache6 = new TreeMap<>();
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
        Set<InetAddress> ret = null;
        Set<InetAddress> v4 = getAddresses(name, cache4);
        Set<InetAddress> v6 = getAddresses(name, cache6);
        if(v4 != null) {
            ret = v4;
            if(v6 != null) {
                ret.addAll(v6);
            }
        } else if(v6 != null) {
            ret = v6;
        }

        return ret;
    }

    static Iterable<InetAddress> getAddressesIPv4(String name) {
        clear(name);
        return getAddresses(name, cache4);
    }

    static Iterable<InetAddress> getAddressesIPv6(String name) {
        clear(name);
        return getAddresses(name, cache6);
    }

    static boolean hasIPv4(String name) {
        clear(name);
        return cache4.containsKey(name) || (alias.containsKey(name) && hasIPv4(alias.get(name).value));
    }

    static boolean hasIPv6(String name) {
        clear(name);
        return cache6.containsKey(name) || (alias.containsKey(name) && hasIPv6(alias.get(name).value));
    }

    private static Set<InetAddress> getAddresses(String name, Map<String, Set<Entry<InetAddress>>> cache) {
        if(cache.containsKey(name)) {
            Set<InetAddress> out = new HashSet<>();
            for(Entry<InetAddress> e : cache.get(name)) {
                out.add(e.value);
            }
            return out;
        } else if(alias.containsKey(name)) {
            return getAddresses(alias.get(name).value, cache);
        }
        return null;
    }

    static void addAEntry(String name, DNSResourceRecord record) {
        clear(name);
        if(!cache4.containsKey(name)) cache4.put(name, new HashSet<Entry<InetAddress>>());
        cache4.get(name).add(new Entry<>((InetAddress) ((DNSA) record.getData()).getAddress(), record.getTtl()));
    }

    static void addAAAAEntry(String name, DNSResourceRecord record) {
        clear(name);
        if(!cache6.containsKey(name)) cache6.put(name, new HashSet<Entry<InetAddress>>());
        cache6.get(name).add(new Entry<>((InetAddress) ((DNSAAAA) record.getData()).getAddress(), record.getTtl()));
    }

    static void addCNAMEEntry(String name, DNSResourceRecord record) {
        alias.put(name, new Entry<>(((DNSCNAME) record.getData()).getCname(), record.getTtl()));
    }

    private static void clear(String name) {
        clear(name, cache4);
        clear(name, cache6);

        if(alias.containsKey(name)) {
            if(!alias.get(name).isValid()) {
                alias.remove(name);
            }
        }
    }

    private static void clear(String name, Map<String, Set<Entry<InetAddress>>> cache) {
        if(cache.containsKey(name)) {
            Iterator<Entry<InetAddress>> it = cache.get(name).iterator();
            while(it.hasNext()) {
                if(!it.next().isValid()) it.remove();
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
