package de.jumpstart;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Keep German diagnostics consistent in redirected logs and on Windows consoles. */
public final class Output {
    private Output() {}

    public static void configure() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }
}
