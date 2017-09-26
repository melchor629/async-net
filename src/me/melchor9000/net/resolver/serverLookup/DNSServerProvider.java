package me.melchor9000.net.resolver.serverLookup;

import me.melchor9000.net.resolver.DNSResolver;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows a {@link DNSResolver}  to search for dns servers from the
 * System. But also allows to implement different ways to do it, appart
 * from the default ones.
 * <br>
 * The basic implementations are:
 * <ul>
 *     <li>{@link OracleJREServerProvider} for Oracle JRE environments</li>
 *     <li>{@link AndroidServerProvider} for Android API 21 or higher, requires {@code ACCESS_NETWORK_STATE}</li>
 *     <li>{@link AlternativeAndroidServerProvider} for any Android, but more limited than the before</li>
 *     <li>{@link UnixResolvConfServerProvider} that uses {@code /etc/resolv.conf} to obtain DNS servers</li>
 * </ul>
 */
public abstract class DNSServerProvider {

    private List<InetSocketAddress> defaultList;

    public DNSServerProvider() {
        defaultList = new ArrayList<>();
        try {
            defaultList.add(new InetSocketAddress(Inet4Address.getByAddress(new byte[] { 84, (byte) 200, 69, 80 }), 53));
            defaultList.add(new InetSocketAddress(Inet6Address.getByAddress(new byte[] { 0x20, 0x01, 0x16, 0x08, 0x00, 0x10, 0x00, 0x25, 0, 0, 0, 0, (byte) 0x92, 0x49, (byte) 0xD6, (byte) 0x9B }), 53));
        } catch(UnknownHostException ignore) {}
    }

    public DNSServerProvider(List<InetSocketAddress> defaultList) {
        this.defaultList = defaultList;
    }

    protected abstract List<InetSocketAddress> getList();

    public List<InetSocketAddress> get() {
        List<InetSocketAddress> list = getList();
        if(list == null) return this.defaultList;
        else return list;
    }

}
