package de.jumpstart;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public final class Settings {
    public final Path home;
    private final Properties file = new Properties();
    private final Options options;

    public Settings(Options options) throws IOException {
        this.options = options;
        home =
                Path.of(System.getProperty("lab.home", "."))
                        .toAbsolutePath()
                        .normalize()
                        .toRealPath();
        String conf =
                options.get(
                        "config",
                        System.getenv().getOrDefault("LAB_CONFIG", "config/local.properties"));
        Path p = home.resolve(conf);
        if (Files.exists(p))
            try (Reader in = Files.newBufferedReader(p)) {
                file.load(in);
            }
        else if (options.has("config") || System.getenv("LAB_CONFIG") != null)
            throw new UsageException("Configuration file is missing.");
        if (!Set.of("local", "remote").contains(value("profile", "local")))
            throw new UsageException("Profile must be local or remote.");
        token(namespace(), "Namespace");
        if (!host().matches("[A-Za-z0-9.:-]+")) throw new UsageException("Invalid host.");
        if (port() < 1 || port() > 65535) throw new UsageException("Port must be within 1–65535.");
        if (remote() && (!ssl() || !file.getProperty("profile", "").equals("remote")))
            throw new UsageException(
                    "Remote requires its own profile, TLS and explicit credentials; no"
                            + " demo fallbacks.");
        if (!remote() && !InetAddress.getByName(host()).isLoopbackAddress())
            throw new UsageException("A non-local host requires the remote profile.");
    }

    public String value(String key, String fallback) {
        String env = "LAB_" + key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return options.get(key, System.getenv().getOrDefault(env, file.getProperty(key, fallback)));
    }

    public boolean remote() {
        return value("profile", "local").equals("remote");
    }

    public boolean ssl() {
        return Boolean.parseBoolean(value("ssl", "false"));
    }

    public String host() {
        return value("host", "127.0.0.1");
    }

    public int port() {
        try {
            return Integer.parseInt(value("port", "61616"));
        } catch (NumberFormatException e) {
            throw new UsageException("Port must be an integer.");
        }
    }

    public String namespace() {
        return value("namespace", "training");
    }

    public String user() {
        return value("user", remote() ? "" : "lab");
    }

    public String password() {
        return value("password", remote() ? "" : "lab-local-only");
    }

    public Path data() {
        return home.resolve(value("dataDir", "runtime/broker")).normalize();
    }

    public String name(String scenario, String suffix) {
        return namespace() + "." + token(scenario, "Scenario") + "." + suffix;
    }

    public static String token(String s, String label) {
        if (!s.matches("[A-Za-z0-9_-]{1,64}"))
            throw new UsageException(label + ": only letters, digits, _ and - (1–64 characters).");
        return s;
    }

    public ActiveMQConnectionFactory factory() {
        if (remote()
                && (user().isBlank() || password().isBlank() || value("trustStore", "").isBlank()))
            throw new UsageException(
                    "A remote client requires individual credentials and a TLS truststore.");
        Map<String, Object> transport = new HashMap<>();
        transport.put("host", host());
        transport.put("port", port());
        transport.put("useEpoll", false);
        transport.put("useKQueue", false);
        transport.put("connect-timeout-millis", 3000);
        transport.put("sslEnabled", ssl());
        if (ssl()) {
            transport.put("verifyHost", true);
            transport.put("trustAll", false);
            transport.put("trustStorePath", home.resolve(value("trustStore", "")).toString());
            transport.put("trustStorePassword", value("trustStorePassword", ""));
            transport.put("trustStoreType", "PKCS12");
        }
        var t =
                new org.apache.activemq.artemis.api.core.TransportConfiguration(
                        "org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory",
                        transport);
        var f = new ActiveMQConnectionFactory(false, t);
        f.setInitialConnectAttempts(1);
        f.setReconnectAttempts(0);
        f.setCallTimeout(4000);
        f.setCallFailoverTimeout(4000);
        f.setClientFailureCheckPeriod(1000);
        f.setConnectionTTL(5000);
        f.setConsumerWindowSize(0);
        // Synchronous persistent sends with PAGE; avoid unbounded producer-credit waits.
        f.setProducerWindowSize(-1);
        f.setBlockOnDurableSend(true);
        f.setBlockOnAcknowledge(true);
        return f;
    }
}
