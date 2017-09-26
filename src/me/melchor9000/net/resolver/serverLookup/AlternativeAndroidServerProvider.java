package me.melchor9000.net.resolver.serverLookup;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class AlternativeAndroidServerProvider extends DNSServerProvider {
    @Override
    protected List<InetSocketAddress> getList() {
        try {
            Class<?> SystemProperties =
                    Class.forName("android.os.SystemProperties");
            Method method = SystemProperties.getMethod("get", String.class);

            ArrayList<InetSocketAddress> servers = new ArrayList<>(5);

            for (String propKey : new String[] { "net.dns1", "net.dns2", "net.dns3", "net.dns4" }) {

                String value = (String) method.invoke(null, propKey);
                if (value == null) continue;
                if (value.length() == 0) continue;
                InetAddress ip = InetAddress.getByName(value);
                if (ip == null) continue;
                servers.add(new InetSocketAddress(ip, 53));
            }

            if (servers.size() > 0) {
                return servers;
            }
        } catch (Exception ignore) { }
        return null;
    }
}
