package com.poker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PokerSystemApplication {

    public static void main(String[] args) {
        // On macOS the JDK auto-populates socksProxyHost from the system network proxy settings.
        // The PostgreSQL JDBC driver honors socksProxyHost and would route the local DB connection
        // through that SOCKS proxy, causing "UnknownHostException: 127.0.0.1". Clearing these
        // properties makes the local database connection go direct.
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.setProperty("java.net.useSystemProxies", "false");
        SpringApplication.run(PokerSystemApplication.class, args);
    }
}
