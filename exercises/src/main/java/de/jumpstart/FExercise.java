package de.jumpstart;

import jakarta.jms.Connection;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/** Übungsstelle F. Nur dieses Modul enthält absichtlich offene Implementierungen. */
public final class FExercise {
    private FExercise() {}

    public static void run(Settings s, Options o) throws Exception {
        try (var factory = s.factory();
                Connection connection = factory.createConnection(s.user(), s.password())) {
            try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
                Topic destination = session.createTopic(s.name("s07", "orders.events"));
                // TODO(F): Setze JMS-Properties konsistent zum JSON und verwende
                // createConsumer(destination, selector).
                // Verwende o.word(1,"consume"), o.number("count",10,1,1000) und einen begrenzten
                // receive(timeout).
                throw new UsageException(
                        "Übungsstelle F ist noch offen. Hinweise: docs/exercises.md.");
            }
        }
    }
}
