package me.melchor9000.net.resolver.serverLookup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AndroidServerProvider extends DNSServerProvider {

    private Object connectivityManager;

    public AndroidServerProvider(Object context) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> Context = context.getClass();
        Method getSystemService = Context.getMethod("getSystemService", String.class);
        this.connectivityManager = getSystemService.invoke(context, "connectivity");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<InetSocketAddress> getList() {
        try {
            Object[] networks = (Object[]) connectivityManager.getClass().getMethod("getAllNetworks").invoke(connectivityManager);
            List<InetSocketAddress> addresses = new ArrayList<>();
            for(Object network : networks) {
                Object linkProperties = connectivityManager.getClass().getMethod("getLinkProperties", network.getClass()).invoke(connectivityManager, network);
                if(linkProperties == null) continue;

                if(hasDefaultRoute(linkProperties)) {
                    int i = 0;
                    for(InetAddress address : (List<InetAddress>) linkProperties.getClass().getMethod("getDnsServers").invoke(linkProperties)) {
                        addresses.add(i++, new InetSocketAddress(address, 53));
                    }
                } else {
                    for(InetAddress address : (List<InetAddress>) linkProperties.getClass().getMethod("getDnsServers").invoke(linkProperties)) {
                        addresses.add(new InetSocketAddress(address, 53));
                    }
                }
            }

            if(addresses.isEmpty()) return null; else return addresses;
        } catch(IllegalAccessException | NoSuchMethodException | InvocationTargetException | NullPointerException e) {
            return null;
        }
    }

    private boolean hasDefaultRoute(Object linkProperties) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for(Object route : (Collection) linkProperties.getClass().getMethod("getRoutes").invoke(linkProperties)) {
            if((Boolean) route.getClass().getMethod("isDefaultRoute").invoke(route)) {
                return true;
            }
        }
        return false;
    }
}
