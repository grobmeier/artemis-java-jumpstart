package de.jumpstart;

import jakarta.jms.*;

import java.util.*;

/** Exercise E: independent executable worked solution. */
public final class ESolution {
    private ESolution() {}

    public static void run(Settings s, Options o) throws Exception {
        String scenario = "s09";
        String name = s.name(scenario, "orders.incoming");
        String action = o.word(1, "consume"),
                worker = Settings.token(o.get("worker", "worker"), "Worker");
        int count = o.number("count", 10, 1, 1000);
        try (var factory = s.factory();
                var connection = factory.createConnection(s.user(), s.password())) {

            try (var session = connection.createSession(true, Session.SESSION_TRANSACTED);
                    var ledger = new SolutionLedger(s)) {
                Destination destination = session.createQueue(name);

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
                                            "standard");
                            producer.send(message);
                            session.commit();
                            Messages.log(scenario, "producer", "SEND", message);
                        }
                    }
                    return;
                }
                if (!Set.of("consume", "subscribe", "register").contains(action))
                    throw new UsageException("Unsupported action.");
                try (var consumer = session.createConsumer(destination, o.get("selector", null))) {
                    connection.start();
                    System.out.println("READY worker=" + worker);
                    System.out.flush();
                    if (action.equals("register")) return;
                    for (int i = 0; o.flag("continuous") || i < count; i++) {
                        Message message = consumer.receive(o.number("timeout", 5000, 100, 300000));
                        if (message == null) {
                            if (o.flag("continuous")) continue;
                            throw new UsageException("Receive timeout expired.");
                        }
                        Messages.log(scenario, worker, "RECEIVE", message);

                        int pause = o.number("sleep", 100, 0, 10000);
                        if (pause > 0) Thread.sleep(pause);

                        var body = Messages.body(message);
                        boolean applied =
                                ledger.apply(
                                        Messages.id(message),
                                        body.path("orderId").asText(),
                                        body.path("amountCents").asLong());
                        System.out.println(
                                "ledgerEffect="
                                        + (applied ? "APPLIED" : "DUPLICATE")
                                        + " entries="
                                        + ledger.count());
                        System.out.flush();
                        if (o.flag("crash") && applied) Runtime.getRuntime().halt(17);

                        session.commit();
                        Messages.log(scenario, worker, "COMMIT", message);
                    }
                }
            }
        }
    }
}
