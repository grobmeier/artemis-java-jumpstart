package de.jumpstart;

import java.util.*;

/** Small strict command-line parser; no hidden messaging lifecycle. */
public final class Options {
    private final Map<String, String> values = new HashMap<>();
    private final List<String> words = new ArrayList<>();
    private static final Set<String> FLAGS =
            Set.of("continuous", "confirm", "rollback", "crash", "duplicate", "shared", "durable");
    private static final Set<String> KEYS =
            Set.of(
                    "config",
                    "profile",
                    "host",
                    "bind",
                    "port",
                    "namespace",
                    "user",
                    "password",
                    "dataDir",
                    "ssl",
                    "trustStore",
                    "trustStorePassword",
                    "keyStore",
                    "keyStorePassword",
                    "usersFile",
                    "worker",
                    "count",
                    "timeout",
                    "sleep",
                    "subscription",
                    "selector",
                    "scenario",
                    "order",
                    "id",
                    "ttl",
                    "delay",
                    "region",
                    "eventType",
                    "priorityClass",
                    "events",
                    "target");

    public Options(String... args) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("--")) {
                words.add(a);
                continue;
            }
            String k = a.substring(2);
            if (FLAGS.contains(k)) values.put(k, "true");
            else if (KEYS.contains(k) && i + 1 < args.length && !args[i + 1].startsWith("--"))
                values.put(k, args[++i]);
            else
                throw new UsageException("Unbekannte Option oder fehlender Wert. Hilfe: lab help.");
        }
    }

    public String get(String k, String fallback) {
        return values.getOrDefault(k, fallback);
    }

    public boolean has(String k) {
        return values.containsKey(k);
    }

    public boolean flag(String k) {
        return Boolean.parseBoolean(get(k, "false"));
    }

    public String word(int n, String fallback) {
        return words.size() > n ? words.get(n) : fallback;
    }

    public int number(String k, int fallback, int min, int max) {
        int n;
        try {
            n = Integer.parseInt(get(k, "" + fallback));
        } catch (NumberFormatException e) {
            throw new UsageException("--" + k + " muss eine ganze Zahl sein.");
        }
        if (n < min || n > max)
            throw new UsageException(
                    "--" + k + " muss zwischen " + min + " und " + max + " liegen.");
        return n;
    }
}
