package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/** Übungsstelle C. Nur dieses Modul enthält absichtlich offene Implementierungen. */
public final class CExercise {
    private CExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            connection.setClientID(
                    s.name(
                            "s04",
                            "client-"
                                    + Settings.token(
                                            o.get("subscription", "shipping"), "Subscription")));
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Topic destination = session.createTopic(s.name("s04", "orders.events"));
                // TODO(C): Implementiere register, produce, subscribe und unsubscribe mit
                // identischer Client-ID und Subscription.
                // Verwende o.word(1,"consume"), o.number("count",10,1,1000) und einen begrenzten
                // receive(timeout).
                throw new UsageException(
                        "Übungsstelle C ist noch offen. Hinweise: docs/exercises.md.");
            }
        }
    }
}
