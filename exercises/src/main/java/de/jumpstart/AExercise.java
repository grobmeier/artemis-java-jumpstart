package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Queue;
import jakarta.jms.Session;

/** Exercise A. Only this module intentionally contains unfinished implementations. */
public final class AExercise {
    private AExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Queue destination = session.createQueue(s.name("s02", "orders.incoming"));
                // TODO(A): Implement produce/consume here: send a TextMessage or receive,
                // process and commit.
                // Use o.word(1,"consume"), o.number("count",10,1,1000) and a bounded
                // receive(timeout).
                throw new UsageException(
                        "Exercise A is not implemented yet. Hints: docs/exercises.md.");
            }
        }
    }
}
