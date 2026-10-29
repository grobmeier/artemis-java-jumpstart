package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/** Exercise F. Only this module intentionally contains unfinished implementations. */
public final class FExercise {
    private FExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Topic destination = session.createTopic(s.name("s07", "orders.events"));
                // TODO(F): Set JMS properties consistently with the JSON and use
                // createConsumer(destination, selector).
                // Use o.word(1,"consume"), o.number("count",10,1,1000) and a bounded
                // receive(timeout).
                throw new UsageException(
                        "Exercise F is not implemented yet. Hints: docs/exercises.md.");
            }
        }
    }
}
