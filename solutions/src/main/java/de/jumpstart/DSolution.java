package de.jumpstart;

import jakarta.jms.*;

import java.util.*;

/** Aufgabe D: eigenständige ausführbare Musterlösung. */
public final class DSolution {
    private DSolution() {}

    public static void run(Settings s, Options o) throws Exception {
        String scenario = "s06";
        String name = s.name(scenario, "orders.incoming");
        String action = o.word(1, "consume"),
                worker = Settings.token(o.get("worker", "worker"), "Worker");
        int count = o.number("count", 10, 1, 1000);
        try (var factory = s.factory();
                var connection = factory.createConnection(s.user(), s.password())) {

            try (var session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Destination destination = session.createQueue(name);

                if (action.equals("replay")) {
                    connection.start();
                    try (var input =
                                    session.createConsumer(
                                            session.createQueue(
                                                    (s.remote() ? s.name(scenario, "DLQ") : "DLQ")
                                                            + "::DLQ."
                                                            + name));
                            var output = session.createProducer(destination)) {
                        var failed = input.receive(o.number("timeout", 5000, 100, 60000));
                        if (failed == null) throw new UsageException("DLQ leer.");
                        var body = Messages.body(failed);
                        var fixed =
                                Messages.create(
                                        session,
                                        false,
                                        Messages.id(failed),
                                        body.path("orderId").asText(),
                                        body.path("amountCents").asLong(),
                                        "PlaceOrder",
                                        "EU",
                                        "standard");
                        output.send(fixed);
                        session.commit();
                        Messages.log(scenario, "repair", "REPLAY", fixed);
                    }
                    return;
                }

                if (action.equals("produce")) {
                    String batch = UUID.randomUUID().toString();
                    try (var producer = session.createProducer(destination)) {
                        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                        for (int i = 1; i <= count; i++) {
                            var message =
                                    Messages.create(
                                            session,
                                            false,
                                            o.get(
                                                    "id",
                                                    o.flag("duplicate") ? batch : batch + "-" + i),
                                            o.get(
                                                    "order",
                                                    "order-"
                                                            + (o.has("id") || o.flag("duplicate")
                                                                    ? 1
                                                                    : i)),
                                            1000,
                                            "PlaceOrder",
                                            i % 2 == 1 ? "EU" : "US",
                                            i == 2 ? "invalid" : "standard");
                            producer.send(message);
                            session.commit();
                            Messages.log(scenario, "producer", "SEND", message);
                        }
                    }
                    return;
                }
                if (!Set.of("consume", "subscribe", "register").contains(action))
                    throw new UsageException("Aktion nicht unterstützt.");
                try (var consumer = session.createConsumer(destination, o.get("selector", null))) {
                    connection.start();
                    System.out.println("READY worker=" + worker);
                    System.out.flush();
                    if (action.equals("register")) return;
                    for (int i = 0; o.flag("continuous") || i < count; i++) {
                        Message message = consumer.receive(o.number("timeout", 5000, 100, 60000));
                        if (message == null) {
                            if (o.flag("continuous")) continue;
                            throw new UsageException("Empfangsfrist abgelaufen.");
                        }
                        Messages.log(scenario, worker, "RECEIVE", message);

                        if (Messages.body(message)
                                .path("priorityClass")
                                .asText()
                                .equals("invalid")) {
                            session.rollback();
                            Messages.log(scenario, worker, "ROLLBACK", message);
                            continue;
                        }

                        int pause = o.number("sleep", 100, 0, 10000);
                        if (pause > 0) Thread.sleep(pause);

                        session.commit();
                        Messages.log(scenario, worker, "COMMIT", message);
                    }
                }
            }
        }
    }
}
