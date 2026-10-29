package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Queue;
import jakarta.jms.Session;

/** Übungsstelle E. Nur dieses Modul enthält absichtlich offene Implementierungen. */
public final class EExercise {
    private EExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Queue destination = session.createQueue(s.name("s09", "orders.incoming"));
                // TODO(E): Ergänze H2 im exercises/pom.xml: Unique-ID und Effekt in derselben
                // DB-Transaktion; Crash vor JMS-Commit.
                // Verwende o.word(1,"consume"), o.number("count",10,1,1000) und einen begrenzten
                // receive(timeout).
                throw new UsageException(
                        "Übungsstelle E ist noch offen. Hinweise: docs/exercises.md.");
            }
        }
    }
}
