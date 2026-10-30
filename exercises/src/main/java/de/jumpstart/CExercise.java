package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/** Exercise C. Only this module intentionally contains unfinished implementations. */
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
                // TODO(C): Implement register, produce, subscribe and unsubscribe with
                // the same client ID and subscription.
                // Use o.word(1,"consume"), o.number("count",10,1,1000) and a bounded
                // receive(timeout).
                throw new UsageException(
                        "Exercise C is not implemented yet. Hints: docs/exercises.md.");
            }
        }
    }
}
