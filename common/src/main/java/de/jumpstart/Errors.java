package de.jumpstart;

public final class Errors {
    private Errors() {}

    public static void report(Exception e) {
        // Provider messages may contain connection configuration: never print raw exception
        // messages.
        String text =
                e instanceof UsageException
                        ? e.getMessage()
                        : "Vorgang fehlgeschlagen ("
                                + e.getClass().getSimpleName()
                                + "). Broker/Port, TLS, Zugangsdaten und Berechtigungen prüfen;"
                                + " scripts/doctor verwenden.";
        System.err.println("Fehler: " + text);
    }
}
