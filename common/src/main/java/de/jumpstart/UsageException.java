package de.jumpstart;

/** Safe application-owned diagnostic, containing no provider configuration or credentials. */
public final class UsageException extends IllegalArgumentException {
    public UsageException(String message) {
        super(message);
    }
}
