package de.jumpstart;

public final class Errors {
    private Errors() {}

    public static void report(Exception e) {
        // Provider messages may contain connection configuration: never print raw exception
        // messages.
        String text =
                e instanceof UsageException
                        ? e.getMessage()
                        : "Operation failed ("
                                + e.getClass().getSimpleName()
                                + "). Check the broker/port, TLS, credentials and permissions;"
                                + " use scripts/doctor.";
        System.err.println("Error: " + text);
    }
}
