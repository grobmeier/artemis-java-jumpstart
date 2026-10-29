package de.jumpstart;

public record OrderCreated(
        int schemaVersion,
        String eventId,
        String orderId,
        long amountCents,
        String timestamp,
        String eventType,
        String region,
        String priorityClass) {}
