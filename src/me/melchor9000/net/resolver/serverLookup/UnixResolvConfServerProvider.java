package me.melchor9000.net.resolver.serverLookup;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnixResolvConfServerProvider extends DNSServerProvider {

    @Override
    protected List<InetSocketAddress> getList() {
        File file = new File("/etc/resolv.conf");
        if (!file.exists()) {
            return null;
        }

        List<InetSocketAddress> servers = new ArrayList<>();
        BufferedReader reader = null;
        final Pattern nameserverPattern = Pattern.compile("^nameserver\\s+(.*)$");
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while((line = reader.readLine()) != null) {
                Matcher matcher = nameserverPattern.matcher(line);
                if(matcher.matches()) {
                    servers.add(new InetSocketAddress(InetAddress.getByName(matcher.group(1).trim()), 53));
                }
            }
        } catch(IOException e) {
            return null;
        } finally {
            if(reader != null) try {
                reader.close();
            } catch(IOException ignore) { }
        }

        if (servers.isEmpty()) {
            return null;
        }

        return servers;
    }

}
