package de.jumpstart;

import jakarta.jms.*;

import org.apache.activemq.artemis.api.core.*;
import org.apache.activemq.artemis.jms.client.*;

/** Provider-specific naming only: keep temporary reply addresses inside authorized namespaces. */
public final class Replies {
    private Replies() {}

    public static TemporaryQueue temporary(Session session, String prefix) throws Exception {
        ActiveMQSession artemis = (ActiveMQSession) session;
        var queue = ActiveMQDestination.createTemporaryQueue(artemis, prefix + ".");
        var address = queue.getSimpleAddress();
        artemis.getCoreSession()
                .createQueue(
                        QueueConfiguration.of(address)
                                .setRoutingType(RoutingType.ANYCAST)
                                .setDurable(false)
                                .setTemporary(true));
        artemis.getConnection().addTemporaryQueue(address);
        queue.setCreated(true);
        return queue;
    }
}
