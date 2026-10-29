package de.jumpstart;

import jakarta.jms.*;

import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Executable demonstrations. All message operations below are direct Jakarta Messaging calls. */
public final class LabMain {
    private LabMain() {}

    public static void main(String[] args) {
        Output.configure();
        try {
            run(new Options(args));
        } catch (Exception e) {
            Errors.report(e);
            System.exit(2);
        }
    }

    public static void run(Options o) throws Exception {
        if (o.word(0, "help").equals("help")) {
            System.out.println(
                    "Lab: queue | s01 … s16, then produce | consume | subscribe | register |"
                            + " unsubscribe | browse | replay | request | reply | transfer.\n"
                            + "Also: doctor [build|runtime], ledger [show|reset --confirm]."
                            + " Options and examples: docs/scenarios.md");
            return;
        }
        var s = new Settings(o);
        if (o.word(0, "").equals("doctor")) {
            doctor(s, o);
            return;
        }
        if (o.word(0, "").equals("ledger")) {
            try (var ledger = new Ledger(s)) {
                if (o.word(1, "show").equals("reset")) {
                    if (!o.flag("confirm"))
                        throw new UsageException(
                                "Ledger reset requires --confirm and stopped workers.");
                    ledger.clear();
                }
                System.out.println("ledgerEntries=" + ledger.count());
            }
            return;
        }
        String scenario =
                o.get("scenario", o.word(0, "queue").equals("queue") ? "s01" : o.word(0, "s01"))
                        .toLowerCase(Locale.ROOT);
        if (!scenario.matches("s(0[1-9]|1[0-6])"))
            throw new UsageException("Scenario must be s01 through s16.");
        String action = o.word(1, "consume");
        if (action.equals("produce")) {
            produce(s, o, scenario);
            return;
        }
        try (var factory = s.factory();
                var connection = factory.createConnection(s.user(), s.password())) {
            boolean durable = scenario.equals("s04") || o.flag("durable");
            boolean shared = scenario.equals("s14") || o.flag("shared");
            String subscription =
                    s.name(
                            scenario,
                            Settings.token(o.get("subscription", "shipping"), "Subscription"));
            if (durable && !shared)
                connection.setClientID(
                        s.name(
                                scenario,
                                "client-"
                                        + Settings.token(
                                                o.get("subscription", "shipping"),
                                                "Subscription")));
            try (var session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                if (action.equals("unsubscribe")) {
                    session.unsubscribe(subscription);
                    System.out.println("Subscription removed: " + subscription);
                    return;
                }
                if (action.equals("cleanup")) {
                    cleanup(s, o, scenario, session);
                    return;
                }
                if (action.equals("browse")) {
                    browse(s, o, scenario, session);
                    return;
                }
                if (action.equals("request")) {
                    request(s, o, scenario, connection, session);
                    return;
                }
                if (action.equals("replay")) {
                    replay(s, o, scenario, connection, session);
                    return;
                }
                Destination destination =
                        destination(s, scenario, session, o.get("target", "incoming"));
                String selector = o.get("selector", null);
                MessageConsumer consumer;
                if (destination instanceof Topic topic) {
                    if (shared)
                        consumer =
                                session.createSharedDurableConsumer(topic, subscription, selector);
                    else if (durable)
                        consumer =
                                session.createDurableConsumer(topic, subscription, selector, false);
                    else consumer = session.createConsumer(topic, selector);
                } else consumer = session.createConsumer(destination, selector);
                try (consumer) {
                    connection.start();
                    System.out.println(
                            "READY scenario="
                                    + scenario
                                    + " worker="
                                    + o.get("worker", "worker")
                                    + " subscription="
                                    + subscription);
                    System.out.flush();
                    if (action.equals("register")) return;
                    if (!Set.of("consume", "subscribe", "transfer", "reply").contains(action))
                        throw new UsageException("Unknown action; run lab help.");
                    consume(s, o, scenario, action, session, consumer);
                }
            }
        }
    }

    public static Destination destination(
            Settings s, String scenario, Session session, String target) throws JMSException {
        if (Set.of("dlq", "expiry").contains(target))
            return session.createQueue(errorQueue(s, scenario, target));
        return switch (scenario) {
            case "s03", "s04", "s07", "s14" ->
                    session.createTopic(s.name(scenario, "orders.events"));
            default ->
                    session.createQueue(
                            s.name(scenario, "orders." + Settings.token(target, "Target")));
        };
    }

    public static String errorQueue(Settings s, String scenario, String type) {
        String source = s.name(scenario, "orders.incoming");
        String address =
                s.remote()
                        ? s.name(scenario, type.equals("dlq") ? "DLQ" : "EXPIRY")
                        : (type.equals("dlq") ? "DLQ" : "EXPIRY");
        return address + "::" + (type.equals("dlq") ? "DLQ." : "EXP.") + source;
    }

    public static void produce(Settings s, Options o, String scenario) throws Exception {
        int count = o.number("count", 10, 1, 1000);
        String batch = UUID.randomUUID().toString();
        try (var factory = s.factory();
                var connection = factory.createConnection(s.user(), s.password());
                var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                var producer =
                        session.createProducer(
                                destination(s, scenario, session, o.get("target", "incoming")))) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setTimeToLive(o.number("ttl", scenario.equals("s11") ? 500 : 0, 0, 60000));
            producer.setDeliveryDelay(
                    o.number("delay", scenario.equals("s12") ? 1500 : 0, 0, 60000));
            int events = o.number("events", scenario.equals("s13") ? 3 : 1, 1, 100);
            for (int i = 1; i <= count; i++)
                for (int sequence = 1; sequence <= events; sequence++) {
                    String id =
                            o.get(
                                    "id",
                                    o.flag("duplicate") ? batch : batch + "-" + i + "-" + sequence);
                    String order =
                            o.get("order", "order-" + (o.has("id") || o.flag("duplicate") ? 1 : i));
                    String type =
                            o.get(
                                    "eventType",
                                    scenario.equals("s07") && i % 3 == 0
                                            ? "OrderCancelled"
                                            : "OrderCreated");
                    String priority =
                            o.get(
                                    "priorityClass",
                                    scenario.equals("s06") && i == 2 ? "invalid" : "standard");
                    var m =
                            Messages.create(
                                    session,
                                    Set.of("s03", "s04", "s07", "s13", "s14").contains(scenario),
                                    id,
                                    order,
                                    1000L + (o.has("id") || o.flag("duplicate") ? 1 : i),
                                    type,
                                    o.get("region", i % 2 == 1 ? "EU" : "US"),
                                    priority);
                    if (scenario.equals("s13")) {
                        m.setStringProperty("JMSXGroupID", order);
                        m.setIntProperty("sequence", sequence);
                    }
                    producer.send(m);
                    Messages.log(scenario, "producer", "SEND", m);
                }
        }
    }

    private static void consume(
            Settings s,
            Options o,
            String scenario,
            String action,
            Session session,
            MessageConsumer consumer)
            throws Exception {
        int limit = o.number("count", 10, 1, 10000),
                timeout = o.number("timeout", 5000, 100, 60000),
                sleep = o.number("sleep", 0, 0, 10000);
        String worker = Settings.token(o.get("worker", "worker"), "Worker");
        try (var ledger = scenario.equals("s09") ? new Ledger(s) : null) {
            int received = 0;
            while (o.flag("continuous") || received < limit) {
                Message m = consumer.receive(timeout);
                if (m == null) {
                    System.out.println("TIMEOUT scenario=" + scenario + " received=" + received);
                    if (o.flag("continuous")) continue;
                    if (received < limit) throw new UsageException("Receive timeout expired");
                }
                received++;
                Messages.log(scenario, worker, "RECEIVE", m);
                if (o.flag("crash") && !scenario.equals("s09")) {
                    System.out.println("CRASH_BEFORE_JMS_COMMIT");
                    System.out.flush();
                    Runtime.getRuntime().halt(17);
                }
                if (sleep > 0) Thread.sleep(sleep);
                if (action.equals("transfer")) {
                    try (var output =
                            session.createProducer(
                                    session.createQueue(s.name(scenario, "orders.outgoing")))) {
                        output.send(copy(session, m));
                    }
                }
                if (action.equals("reply")) {
                    if (m.getJMSReplyTo() == null)
                        throw new UsageException("Request requires JMSReplyTo.");
                    try (var output = session.createProducer(m.getJMSReplyTo())) {
                        var response = copy(session, m);
                        response.setJMSCorrelationID(m.getJMSMessageID());
                        output.send(response);
                    }
                }
                if (o.flag("rollback")
                        || scenario.equals("s06")
                                && Messages.body(m)
                                        .path("priorityClass")
                                        .asText()
                                        .equals("invalid")) {
                    session.rollback();
                    Messages.log(scenario, worker, "ROLLBACK", m);
                    continue;
                }
                if (ledger != null) {
                    var body = Messages.body(m);
                    boolean applied =
                            ledger.apply(
                                    Messages.id(m),
                                    body.path("orderId").asText(),
                                    body.path("amountCents").asLong());
                    System.out.println(
                            "ledgerEffect="
                                    + (applied ? "APPLIED" : "DUPLICATE")
                                    + " entries="
                                    + ledger.count());
                    System.out.flush();
                    if (o.flag("crash") && applied) {
                        System.out.println("CRASH_AFTER_DB_COMMIT");
                        System.out.flush();
                        Runtime.getRuntime().halt(17);
                    }
                }
                session.commit();
                Messages.log(scenario, worker, "COMMIT", m);
                if (scenario.equals("s13"))
                    System.out.println(
                            "group="
                                    + m.getStringProperty("JMSXGroupID")
                                    + " sequence="
                                    + m.getIntProperty("sequence")
                                    + " worker="
                                    + worker);
            }
        }
    }

    private static TextMessage copy(Session session, Message original) throws JMSException {
        var m = session.createTextMessage(((TextMessage) original).getText());
        for (String p : List.of("eventType", "region", "priorityClass"))
            m.setStringProperty(p, original.getStringProperty(p));
        return m;
    }

    private static void cleanup(Settings s, Options o, String scenario, Session session)
            throws Exception {
        if (!o.flag("confirm"))
            throw new UsageException("Cleanup requires --confirm; only empty destinations without consumers.");
        if (Set.of("s03", "s04", "s07", "s14").contains(scenario))
            throw new UsageException(
                    "Remove topic resources with unsubscribe and the same"
                            + " subscription identity.");
        String target = Settings.token(o.get("target", "incoming"), "Target");
        String queue =
                Set.of("dlq", "expiry").contains(target)
                        ? errorQueue(s, scenario, target).split("::")[1]
                        : s.name(scenario, "orders." + target);
        var core =
                ((org.apache.activemq.artemis.jms.client.ActiveMQSession) session).getCoreSession();
        var name = org.apache.activemq.artemis.api.core.SimpleString.of(queue);
        var info = core.queueQuery(name);
        if (!info.isExists()) {
            System.out.println("Destination does not exist.");
            return;
        }
        if (info.getConsumerCount() != 0 || info.getMessageCount() != 0)
            throw new UsageException(
                    "Cleanup refused: destination contains messages or has active consumers.");
        core.deleteQueue(name);
        System.out.println("Empty owned queue removed: " + queue);
    }

    private static void browse(Settings s, Options o, String scenario, Session session)
            throws Exception {
        String target = o.get("target", "incoming");
        jakarta.jms.Queue queue =
                session.createQueue(
                        Set.of("dlq", "expiry").contains(target)
                                ? errorQueue(s, scenario, target)
                                : s.name(scenario, "orders." + Settings.token(target, "Target")));
        try (var browser = session.createBrowser(queue)) {
            int n = 0;
            var rows = browser.getEnumeration();
            while (rows.hasMoreElements()) {
                Messages.log(scenario, "browser", "BROWSE", (Message) rows.nextElement());
                n++;
            }
            System.out.println(
                    "queue="
                            + queue.getQueueName()
                            + " visible="
                            + n
                            + " (snapshot; active deliveries excluded)");
        }
    }

    private static void replay(
            Settings s, Options o, String scenario, Connection connection, Session session)
            throws Exception {
        connection.start();
        try (var input =
                        session.createConsumer(
                                session.createQueue(errorQueue(s, scenario, "dlq")));
                var output =
                        session.createProducer(
                                session.createQueue(s.name(scenario, "orders.incoming")))) {
            for (int i = 0; i < o.number("count", 1, 1, 1000); i++) {
                Message m = input.receive(o.number("timeout", 5000, 100, 60000));
                if (m == null) throw new UsageException("DLQ empty or receive timeout expired");
                var b = Messages.body(m);
                var repaired =
                        Messages.create(
                                session,
                                false,
                                Messages.id(m),
                                b.path("orderId").asText(),
                                b.path("amountCents").asLong(),
                                b.path("eventType").asText(),
                                b.path("region").asText(),
                                "standard");
                output.send(repaired);
                session.commit();
                Messages.log(scenario, "repair", "REPLAY", repaired);
            }
        }
    }

    private static void request(
            Settings s, Options o, String scenario, Connection connection, Session session)
            throws Exception {
        TemporaryQueue reply = Replies.temporary(session, s.name(scenario, "reply"));
        try (var input = session.createConsumer(reply);
                var output =
                        session.createProducer(
                                session.createQueue(s.name(scenario, "orders.incoming")))) {
            var request =
                    Messages.create(
                            session,
                            false,
                            UUID.randomUUID().toString(),
                            "request-order",
                            1000,
                            "OrderCreated",
                            "EU",
                            "standard");
            request.setJMSReplyTo(reply);
            output.send(request);
            session.commit();
            connection.start();
            Message response = input.receive(o.number("timeout", 3000, 100, 60000));
            if (response == null) throw new UsageException("No reply within the timeout");
            if (!request.getJMSMessageID().equals(response.getJMSCorrelationID()))
                throw new UsageException("Incorrect correlation ID");
            Messages.log(scenario, "requester", "REPLY", response);
            session.commit();
            System.out.println("correlation=" + response.getJMSCorrelationID());
        } finally {
            reply.delete();
        }
    }

    private static void doctor(Settings s, Options o) throws Exception {
        System.out.println(
                "Java=" + Runtime.version() + " java.home=" + System.getProperty("java.home"));
        System.out.println(
                "Profile="
                        + (s.remote() ? "remote" : "local")
                        + " host="
                        + s.host()
                        + " port="
                        + s.port()
                        + " namespace="
                        + s.namespace());
        if (o.word(1, "runtime").equals("build")) {
            if (ToolProviderCheck.compilerMissing())
                throw new UsageException("A JDK is required to build.");
            if (!Files.isRegularFile(s.home.resolve(".mvn/wrapper/maven-wrapper.jar")))
                throw new UsageException("Maven Wrapper is missing.");
            System.out.println(
                    "Build prerequisites available; dependencies/proxy will be checked during"
                            + " the build.");
            return;
        }
        for (String artifact : List.of("broker", "lab"))
            if (!Files.isRegularFile(s.home.resolve(artifact + "/target/" + artifact + ".jar"))
                    && !Files.isRegularFile(s.home.resolve("lib/" + artifact + ".jar")))
                throw new UsageException("Build artifact is missing: run mvnw.cmd clean verify first.");
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(s.host(), s.port()), 1500);
        } catch (Exception e) {
            throw new UsageException(
                    "Broker port is unreachable. Start the broker in its own terminal; check profile,"
                            + " firewall and port.");
        }
        try (var factory = s.factory();
                var connection = factory.createConnection(s.user(), s.password())) {
            connection.start();
            System.out.println(
                    "TCP, "
                            + (s.ssl() ? "TLS and " : "")
                            + "authentication successful. Port is occupied by the broker.");
        }
    }

    private static final class ToolProviderCheck {
        static boolean compilerMissing() {
            return javax.tools.ToolProvider.getSystemJavaCompiler() == null;
        }
    }
}
