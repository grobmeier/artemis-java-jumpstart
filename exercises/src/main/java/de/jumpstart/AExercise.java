package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Queue;
import jakarta.jms.Session;

/** Übungsstelle A. Nur dieses Modul enthält absichtlich offene Implementierungen. */
public final class AExercise {
    private AExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Queue destination = session.createQueue(s.name("s02", "orders.incoming"));
                // TODO(A): Implementiere hier produce/consume: TextMessage senden bzw. receive,
                // Bearbeitung und commit.
                // Verwende o.word(1,"consume"), o.number("count",10,1,1000) und einen begrenzten
                // receive(timeout).
                throw new UsageException(
                        "Übungsstelle A ist noch offen. Hinweise: docs/exercises.md.");
            }
        }
    }
}
