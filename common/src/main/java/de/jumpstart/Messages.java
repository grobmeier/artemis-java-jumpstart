package de.jumpstart;

import com.fasterxml.jackson.databind.*;

import jakarta.jms.*;

import java.time.Instant;

public final class Messages {
    public static final ObjectMapper JSON = new ObjectMapper();

    private Messages() {}

    public static TextMessage create(
            Session session,
            boolean event,
            String id,
            String order,
            long cents,
            String type,
            String region,
            String priority)
            throws Exception {
        Object payload =
                event
                        ? new OrderCreated(
                                1,
                                id,
                                order,
                                cents,
                                Instant.now().toString(),
                                type,
                                region,
                                priority)
                        : new OrderCommand(
                                1,
                                id,
                                order,
                                cents,
                                Instant.now().toString(),
                                type,
                                region,
                                priority);
        TextMessage m = session.createTextMessage(JSON.writeValueAsString(payload));
        m.setStringProperty("eventType", type);
        m.setStringProperty("region", region);
        m.setStringProperty("priorityClass", priority);
        return m;
    }

    public static JsonNode body(Message m) throws Exception {
        return JSON.readTree(((TextMessage) m).getText());
    }

    public static String id(Message m) throws Exception {
        var b = body(m);
        return b.path(b.has("eventId") ? "eventId" : "commandId").asText();
    }

    public static void log(String scenario, String worker, String action, Message m)
            throws Exception {
        var b = body(m);
        System.out.printf(
                "scenario=%s worker=%s action=%s id=%s order=%s JMSMessageID=%s JMSRedelivered=%s"
                        + " attempt=%s%n",
                scenario,
                worker,
                action,
                id(m),
                b.path("orderId").asText(),
                m.getJMSMessageID(),
                m.getJMSRedelivered(),
                m.propertyExists("JMSXDeliveryCount")
                        ? m.getIntProperty("JMSXDeliveryCount")
                        : "unknown");
        System.out.flush();
    }
}
