package de.jumpstart;

public record OrderCommand(
        int schemaVersion,
        String commandId,
        String orderId,
        long amountCents,
        String timestamp,
        String eventType,
        String region,
        String priorityClass) {}
