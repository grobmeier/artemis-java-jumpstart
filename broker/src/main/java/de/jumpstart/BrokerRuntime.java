package de.jumpstart;

import org.apache.activemq.artemis.api.core.*;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.*;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.*;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManagerImpl;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public final class BrokerRuntime implements AutoCloseable {
    private final EmbeddedActiveMQ embedded;
    private final DataArea area;

    public BrokerRuntime(Settings s) throws Exception {
        area = new DataArea(s);
        EmbeddedActiveMQ candidate = null;
        try {
            area.initialize();
            String bind = s.value("bind", s.remote() ? "" : "127.0.0.1");
            if (bind.isBlank() || (!s.remote() && !InetAddress.getByName(bind).isLoopbackAddress()))
                throw new UsageException(
                        "Remote-Bind muss explizit sein; lokal ist nur Loopback erlaubt.");
            try (var probe = new ServerSocket()) {
                probe.bind(new InetSocketAddress(bind, s.port()));
            } catch (BindException e) {
                throw new UsageException(
                        "Port ist belegt; anderen Port wählen oder eigenen Broker geordnet"
                                + " stoppen.");
            }
            var security = new SecurityConfiguration();
            Map<String, Set<Role>> roles = new HashMap<>();
            if (s.remote()) {
                Properties users = new Properties();
                Path usersFile = s.home.resolve(s.value("usersFile", "config/users.properties"));
                try (Reader in = Files.newBufferedReader(usersFile)) {
                    users.load(in);
                }
                for (String user : users.stringPropertyNames())
                    if (user.endsWith(".password")) {
                        String name = user.substring(0, user.length() - 9);
                        String ns =
                                Settings.token(
                                        users.getProperty(name + ".namespace", ""),
                                        "Autorisierter Namespace");
                        if (users.getProperty(user).isBlank())
                            throw new UsageException("Leeres Trainingspasswort verweigert.");
                        security.addUser(name, users.getProperty(user));
                        security.addRole(name, ns);
                        grant(roles, ns, ns);
                        for (String group : users.getProperty(name + ".groups", "").split(","))
                            if (!group.isBlank())
                                grant(roles, Settings.token(group.trim(), "Gruppenbereich"), ns);
                    }
                if (roles.isEmpty())
                    throw new UsageException("Keine individuellen Trainingsnutzer konfiguriert.");
            } else {
                security.addUser(s.user(), s.password());
                security.addRole(s.user(), "local");
                roles.put("#", Set.of(trainingRole("local")));
            }
            Map<String, Object> transport = new HashMap<>();
            transport.put("host", bind);
            transport.put("port", s.port());
            transport.put("protocols", "CORE");
            transport.put("useEpoll", false);
            transport.put("useKQueue", false);
            transport.put("remotingThreads", 2);
            if (s.remote()) {
                String store = s.value("keyStore", "");
                String password = s.value("keyStorePassword", "");
                if (store.isBlank() || password.isBlank())
                    throw new UsageException(
                            "Remote-Broker benötigt TLS-Schlüsselspeicher und Passwort.");
                transport.put("sslEnabled", true);
                transport.put("keyStorePath", s.home.resolve(store).toString());
                transport.put("keyStorePassword", password);
                transport.put("keyStoreType", "PKCS12");
            }
            var config = new ConfigurationImpl();
            config.setName("java-jumpstart")
                    .setPersistenceEnabled(true)
                    .setSecurityEnabled(true)
                    .setJMXManagementEnabled(false)
                    .setJournalType(JournalType.NIO)
                    .setJournalDirectory(area.data.resolve("journal").toString())
                    .setBindingsDirectory(area.data.resolve("bindings").toString())
                    .setPagingDirectory(area.data.resolve("paging").toString())
                    .setLargeMessagesDirectory(area.data.resolve("large-messages").toString())
                    .setJournalDatasync(true)
                    .setJournalSyncTransactional(true)
                    .setJournalSyncNonTransactional(true)
                    .setMessageExpiryScanPeriod(100)
                    .setConnectionTtlCheckInterval(500)
                    .setThreadPoolMaxSize(8)
                    .setScheduledThreadPoolMaxSize(2)
                    .setGlobalMaxSize(64L * 1024 * 1024);
            config.addAcceptorConfiguration(
                    new TransportConfiguration(
                            "org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory",
                            transport));
            config.setSecurityRoles(roles);
            List<String> namespaces =
                    s.remote()
                            ? roles.keySet().stream()
                                    .map(k -> k.substring(0, k.length() - 2))
                                    .toList()
                            : List.of("*");
            for (String namespace : namespaces)
                for (String scenario :
                        List.of(
                                "s01", "s02", "s03", "s04", "s05", "s06", "s07", "s08", "s09",
                                "s10", "s11", "s12", "s13", "s14", "s15", "s16")) {
                    // Capture namespace from the matching address in auto-created, per-address
                    // error queues.
                    var setting =
                            new AddressSettings()
                                    .setAutoCreateAddresses(true)
                                    .setAutoCreateQueues(true)
                                    .setAutoDeleteQueues(false)
                                    .setAutoDeleteAddresses(false)
                                    .setDeadLetterAddress(
                                            SimpleString.of(
                                                    s.remote()
                                                            ? namespace + "." + scenario + ".DLQ"
                                                            : "DLQ"))
                                    .setAutoCreateDeadLetterResources(true)
                                    .setDeadLetterQueuePrefix(SimpleString.of("DLQ."))
                                    .setExpiryAddress(
                                            SimpleString.of(
                                                    s.remote()
                                                            ? namespace + "." + scenario + ".EXPIRY"
                                                            : "EXPIRY"))
                                    .setAutoCreateExpiryResources(true)
                                    .setExpiryQueuePrefix(SimpleString.of("EXP."))
                                    .setMaxDeliveryAttempts(3)
                                    .setRedeliveryDelay(500)
                                    .setRedeliveryMultiplier(1.0)
                                    .setMaxSizeBytes(1024 * 1024)
                                    .setPageSizeBytes(256 * 1024)
                                    .setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE);
                    config.addAddressSetting(namespace + "." + scenario + ".#", setting);
                }
            // Remote error addresses themselves live inside each authorized namespace.
            config.addAddressSetting(
                    "#",
                    new AddressSettings()
                            .setAutoCreateAddresses(true)
                            .setAutoCreateQueues(true)
                            .setMaxSizeBytes(1024 * 1024)
                            .setPageSizeBytes(256 * 1024)
                            .setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE));
            candidate =
                    new EmbeddedActiveMQ()
                            .setConfiguration(config)
                            .setSecurityManager(new ActiveMQSecurityManagerImpl(security));
            candidate.start();
            embedded = candidate;
        } catch (Exception e) {
            if (candidate != null)
                try {
                    candidate.stop();
                } catch (Exception ignored) {
                }
            area.close();
            throw e;
        }
    }

    private static void grant(Map<String, Set<Role>> roles, String namespace, String role) {
        roles.computeIfAbsent(namespace + ".#", k -> new HashSet<>()).add(trainingRole(role));
    }

    private static Role trainingRole(String name) {
        return new Role(name, true, true, true, true, true, true, false, true, true, true);
    }

    public ActiveMQServer server() {
        return embedded.getActiveMQServer();
    }

    public void status() throws Exception {
        for (var name : server().getActiveMQServerControl().getQueueNames()) {
            var q = server().locateQueue(name);
            if (q == null) continue; // A concurrent targeted cleanup may have removed this queue.
            System.out.printf(
                    "queue=%s depth=%d consumers=%d delivering=%d%n",
                    name, q.getMessageCount(), q.getConsumerCount(), q.getDeliveringCount());
        }
    }

    @Override
    public void close() throws Exception {
        try {
            embedded.stop();
        } finally {
            area.close();
        }
    }
}
