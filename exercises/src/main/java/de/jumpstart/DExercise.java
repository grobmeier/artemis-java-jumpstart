package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Queue;
import jakarta.jms.Session;

/** Exercise D. Only this module intentionally contains unfinished implementations. */
public final class DExercise {
    private DExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Queue destination = session.createQueue(s.name("s06", "orders.incoming"));
                // TODO(D): Implement rollback for priorityClass=invalid, otherwise commit; then
                // DLQ inspection and deliberate replay.
                // Use o.word(1,"consume"), o.number("count",10,1,1000) and a bounded
                // receive(timeout).
                throw new UsageException(
                        "Exercise D is not implemented yet. Hints: docs/exercises.md.");
            }
        }
    }
}
