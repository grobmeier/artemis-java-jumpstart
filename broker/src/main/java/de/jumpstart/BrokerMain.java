package de.jumpstart;

import java.nio.channels.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BrokerMain {
    private BrokerMain() {}

    public static void main(String[] args) {
        Output.configure();
        try {
            var options = new Options(args);
            var settings = new Settings(options);
            Path stop = settings.home.resolve("runtime/.broker.stop");
            switch (options.word(0, "start")) {
                case "reset" -> {
                    if (!options.flag("confirm"))
                        throw new UsageException(
                                "Reset benötigt --confirm und einen gestoppten Broker.");
                    try (var area = new DataArea(settings)) {
                        area.reset();
                    }
                    System.out.println(
                            "Eigener Broker-Datenbereich gelöscht. Ledger bleibt erhalten.");
                }
                case "stop" -> {
                    Path lock = settings.home.resolve("runtime/.broker.lock");
                    if (!Files.isRegularFile(lock, LinkOption.NOFOLLOW_LINKS)
                            || Files.isSymbolicLink(stop))
                        throw new UsageException("Kein eigener Broker gefunden.");
                    try (var channel = FileChannel.open(lock, StandardOpenOption.WRITE)) {
                        var probe = channel.tryLock();
                        if (probe != null) {
                            probe.close();
                            throw new UsageException("Broker ist bereits gestoppt.");
                        }
                    }
                    Files.writeString(stop, "stop");
                    System.out.println("Geordnetes Stoppen angefordert; auf Brokerende warten.");
                }
                case "start" -> {
                    var broker = new BrokerRuntime(settings);
                    AtomicBoolean closed = new AtomicBoolean();
                    Runnable close =
                            () -> {
                                if (closed.compareAndSet(false, true))
                                    try {
                                        broker.close();
                                    } catch (Exception e) {
                                        System.err.println(
                                                "Broker konnte nicht vollständig stoppen.");
                                    }
                            };
                    Thread hook = new Thread(close);
                    Runtime.getRuntime().addShutdownHook(hook);
                    Files.deleteIfExists(stop);
                    System.out.println(
                            "BROKER_READY host="
                                    + settings.host()
                                    + " port="
                                    + settings.port()
                                    + " journal=NIO persistent=true");
                    System.out.flush();
                    try {
                        long next = 0;
                        while (!Files.exists(stop)) {
                            if (System.nanoTime() >= next) {
                                broker.status();
                                next = System.nanoTime() + 2_000_000_000L;
                            }
                            Thread.sleep(100);
                        }
                    } finally {
                        close.run();
                        Runtime.getRuntime().removeShutdownHook(hook);
                        Files.deleteIfExists(stop);
                    }
                }
                default -> throw new UsageException("Broker: start | stop | reset --confirm");
            }
        } catch (Exception e) {
            Errors.report(e);
            System.exit(2);
        }
    }
}
