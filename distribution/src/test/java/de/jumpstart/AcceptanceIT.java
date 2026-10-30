package de.jumpstart;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.zip.*;

/** Black-box acceptance of the packaged artifacts, real TCP and separate JVMs on every platform. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 90, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class AcceptanceIT {
    @TempDir static Path temporary;
    static Path source, home;
    static int port;
    static Job broker;
    static final List<Job> jobs = new CopyOnWriteArrayList<>();
    static final String java =
            Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java")
                    .toString();

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    static final class Job implements AutoCloseable {
        final Process process;
        final Path output;

        Job(List<String> command, Path root) throws Exception {
            this(command, root, Map.of());
        }

        Job(List<String> command, Path root, Map<String, String> environment) throws Exception {
            output = Files.createTempFile(temporary, "process-", ".log");
            ProcessBuilder builder =
                    new ProcessBuilder(command)
                            .directory(temporary.toFile())
                            .redirectErrorStream(true)
                            .redirectOutput(output.toFile());
            builder.environment().keySet().removeIf(k -> k.startsWith("LAB_"));
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
            builder.environment().putAll(environment);
            process = builder.start();
            jobs.add(this);
        }

        String text() throws IOException {
            String content = Files.readString(output);
            // The child can be between printf writes. Poll only complete records, not a partial ID.
            return content.substring(0, content.lastIndexOf('\n') + 1);
        }

        void ready(String signal) throws Exception {
            await(
                    () -> {
                        try {
                            return text().contains(signal) || !process.isAlive();
                        } catch (IOException e) {
                            return false;
                        }
                    },
                    Duration.ofSeconds(20));
            assertTrue(text().contains(signal), text());
        }

        String end(int expected) throws Exception {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Process did not exit: " + text());
            assertEquals(expected, process.exitValue(), text());
            return text();
        }

        @Override
        public void close() throws Exception {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    assertTrue(process.waitFor(5, TimeUnit.SECONDS));
                }
            }
        }
    }

    static void await(BooleanSupplier condition, Duration timeout) throws Exception {
        long end = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > end) fail("Begrenzte Wartefrist abgelaufen");
            Thread.sleep(40);
        }
    }

    static Job jar(String artifact, String namespace, String... args) throws Exception {
        return jarAt(home, artifact, namespace, args);
    }

    static Job jarAt(Path root, String artifact, String namespace, String... args)
            throws Exception {
        Path file = root.resolve("lib/" + artifact + ".jar");
        if (!Files.exists(file)) file = source.resolve(artifact + "/target/" + artifact + ".jar");
        List<String> command =
                new ArrayList<>(List.of(java, "-Dlab.home=" + root, "-jar", file.toString()));
        command.addAll(List.of(args));
        command.addAll(List.of("--port", "" + port, "--namespace", namespace));
        return new Job(command, root);
    }

    static String run(String ns, String... args) throws Exception {
        try (var job = jar("lab", ns, args)) {
            return job.end(0);
        }
    }

    static Set<String> ids(String logs, String action) {
        Set<String> ids = new HashSet<>();
        for (String line : logs.split("\n"))
            if (line.contains("action=" + action + " ")) {
                var matcher = Pattern.compile(" id=([^ ]+)").matcher(line);
                assertTrue(matcher.find(), line);
                assertTrue(ids.add(matcher.group(1)), "Doppelte ID: " + line);
            }
        return ids;
    }

    static Set<String> commits(String logs) {
        return ids(logs, "COMMIT");
    }

    static void startBroker() throws Exception {
        broker = jar("broker", "training", "start");
        broker.ready("BROKER_READY");
    }

    static void stopBroker() throws Exception {
        try (var stop = jar("broker", "training", "stop")) {
            stop.end(0);
        }
        broker.end(0);
        broker.close();
    }

    @BeforeAll
    static void setup() throws Exception {
        source = Path.of("..").toAbsolutePath().normalize();
        home = temporary.resolve("lab with spaces");
        Files.createDirectories(home.resolve("config"));
        Files.copy(
                source.resolve("config/local.properties"), home.resolve("config/local.properties"));
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        startBroker();
    }

    @AfterEach
    void cleanWorkers() throws Exception {
        for (Job j : jobs) if (j != broker) j.close();
    }

    @AfterAll
    static void cleanup() throws Exception {
        try {
            if (broker != null && broker.process.isAlive()) stopBroker();
        } finally {
            Path logs = Path.of("target/process-logs");
            Files.createDirectories(logs);
            for (Job j : jobs) {
                try {
                    j.close();
                } finally {
                    Files.copy(
                            j.output,
                            logs.resolve(j.output.getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    @Test
    @Order(1)
    void competingConsumersReceiveCompleteSetWithoutDuplicates() throws Exception {
        String ns = "competing";
        try (var alice =
                        jar(
                                "lab",
                                ns,
                                "s02",
                                "consume",
                                "--worker",
                                "alice",
                                "--continuous",
                                "--sleep",
                                "60");
                var bob =
                        jar(
                                "lab",
                                ns,
                                "s02",
                                "consume",
                                "--worker",
                                "bob",
                                "--continuous",
                                "--sleep",
                                "60")) {
            alice.ready("READY");
            bob.ready("READY");
            Set<String> expected = ids(run(ns, "s02", "produce", "--count", "20"), "SEND");
            await(
                    () -> {
                        try {
                            return commits(alice.text()).size() + commits(bob.text()).size() == 20;
                        } catch (Exception e) {
                            return false;
                        }
                    },
                    Duration.ofSeconds(15));
            Set<String> left = commits(alice.text()), right = commits(bob.text());
            assertFalse(left.isEmpty());
            assertFalse(right.isEmpty());
            assertTrue(Collections.disjoint(left, right));
            left.addAll(right);
            assertEquals(expected, left);
            try (var activeCleanup = jar("lab", ns, "s02", "cleanup", "--confirm")) {
                assertTrue(activeCleanup.end(2).contains("active consumers"));
            }
        }
        assertTrue(run(ns, "s02", "browse").contains("visible=0"));
    }

    @Test
    @Order(2)
    void independentTopicsAndLateSubscriber() throws Exception {
        String ns = "topics";
        try (var billing =
                        jar(
                                "lab",
                                ns,
                                "s03",
                                "subscribe",
                                "--worker",
                                "billing",
                                "--subscription",
                                "billing",
                                "--count",
                                "5");
                var shipping =
                        jar(
                                "lab",
                                ns,
                                "s03",
                                "subscribe",
                                "--worker",
                                "shipping",
                                "--subscription",
                                "shipping",
                                "--count",
                                "5")) {
            billing.ready("READY");
            shipping.ready("READY");
            Set<String> expected = ids(run(ns, "s03", "produce", "--count", "5"), "SEND");
            assertEquals(expected, commits(billing.end(0)));
            assertEquals(expected, commits(shipping.end(0)));
        }
        run(ns, "s03", "produce", "--count", "2", "--id", "past");
        try (var late = jar("lab", ns, "s03", "subscribe", "--count", "2")) {
            late.ready("READY");
            Set<String> future = ids(run(ns, "s03", "produce", "--count", "2"), "SEND");
            assertEquals(future, commits(late.end(0)));
        }
    }

    @Test
    @Order(3)
    void durableOfflineAndPersistentBrokerRestart() throws Exception {
        String ns = "durable";
        run(ns, "s04", "register", "--subscription", "shipping");
        Set<String> expected = ids(run(ns, "s04", "produce", "--count", "3"), "SEND");
        Set<String> queue = ids(run(ns, "s15", "produce", "--count", "3"), "SEND");
        stopBroker();
        startBroker();
        assertEquals(
                expected,
                commits(run(ns, "s04", "subscribe", "--subscription", "shipping", "--count", "3")));
        assertEquals(queue, commits(run(ns, "s15", "consume", "--count", "3")));
        run(ns, "s04", "unsubscribe", "--subscription", "shipping");
    }

    @Test
    @Order(4)
    void rollbackAndProcessCrashRedeliver() throws Exception {
        for (String operation : List.of("rollback", "crash")) {
            String ns = "redelivery-" + operation;
            run(ns, "s05", "produce", "--count", "1", "--id", "retry-id");
            try (var worker = jar("lab", ns, "s05", "consume", "--count", "1", "--" + operation)) {
                worker.end(operation.equals("crash") ? 17 : 0);
            }
            String result = run(ns, "s05", "consume", "--count", "1", "--timeout", "10000");
            assertEquals(Set.of("retry-id"), commits(result));
            assertTrue(result.contains("JMSRedelivered=true"), result);
            assertTrue(result.contains("attempt=2"), result);
        }
    }

    @Test
    @Order(5)
    void threeAttemptsDlqAndExplicitRepairReplay() throws Exception {
        String ns = "retry";
        Set<String> expected = ids(run(ns, "s06", "produce", "--count", "3"), "SEND");
        String result = run(ns, "s06", "consume", "--count", "5");
        assertEquals(2, commits(result).size());
        List<String> rollbacks = result.lines().filter(l -> l.contains("action=ROLLBACK")).toList();
        assertEquals(3, rollbacks.size());
        for (int i = 0; i < 3; i++)
            assertTrue(rollbacks.get(i).contains("attempt=" + (i + 1)), result);
        String errors = run(ns, "s06", "browse", "--target", "dlq");
        assertEquals(1, ids(errors, "BROWSE").size());
        Set<String> actual = commits(result);
        run(ns, "s06", "replay", "--count", "1");
        actual.addAll(commits(run(ns, "s06", "consume", "--count", "1")));
        assertEquals(expected, actual);
        assertTrue(run(ns, "s06", "browse", "--target", "dlq").contains("visible=0"));
    }

    @Test
    @Order(6)
    void messagingReceiveAndSendCommitOrRollbackTogether() throws Exception {
        String ns = "transfer";
        Set<String> expected = ids(run(ns, "s08", "produce", "--count", "1"), "SEND");
        run(ns, "s08", "transfer", "--count", "1", "--rollback");
        assertTrue(run(ns, "s08", "browse", "--target", "outgoing").contains("visible=0"));
        assertEquals(expected, commits(run(ns, "s08", "transfer", "--count", "1")));
        assertEquals(
                expected,
                commits(run(ns, "s08", "consume", "--target", "outgoing", "--count", "1")));
        assertTrue(run(ns, "s08", "browse").contains("visible=0"));
    }

    @Test
    @Order(7)
    void atomicLocalLedgerSurvivesCrashBeforeJmsCommit() throws Exception {
        String ns = "ledger";
        run(ns, "s09", "produce", "--id", "business-1", "--count", "2");
        try (var crash = jar("lab", ns, "s09", "consume", "--count", "1", "--crash")) {
            assertTrue(crash.end(17).contains("CRASH_AFTER_DB_COMMIT"));
        }
        String recovered = run(ns, "s09", "consume", "--count", "2", "--timeout", "10000");
        assertEquals(
                2,
                recovered.lines().filter(l -> l.contains("ledgerEffect=DUPLICATE")).count(),
                recovered);
        assertTrue(run(ns, "ledger", "show").contains("ledgerEntries=1"));
        List<String> ids =
                recovered
                        .lines()
                        .filter(l -> l.contains("action=COMMIT"))
                        .map(l -> l.split("JMSMessageID=")[1].split(" ")[0])
                        .toList();
        assertEquals(2, new HashSet<>(ids).size());
    }

    @Test
    @Order(8)
    void selectorsUsePropertiesWithPredictedSets() throws Exception {
        String ns = "selector";
        try (var eu =
                        jar(
                                "lab",
                                ns,
                                "s07",
                                "subscribe",
                                "--selector",
                                "region = 'EU'",
                                "--count",
                                "3");
                var cancel =
                        jar(
                                "lab",
                                ns,
                                "s07",
                                "subscribe",
                                "--selector",
                                "eventType = 'OrderCancelled'",
                                "--count",
                                "2")) {
            eu.ready("READY");
            cancel.ready("READY");
            String sent = run(ns, "s07", "produce", "--count", "6");
            Set<String> all = ids(sent, "SEND"),
                    euExpected = new HashSet<>(),
                    cancelExpected = new HashSet<>();
            for (String id : all) {
                int i = Integer.parseInt(id.substring(id.length() - 3, id.length() - 2));
                if (i % 2 == 1) euExpected.add(id);
                if (i % 3 == 0) cancelExpected.add(id);
            }
            assertEquals(euExpected, commits(eu.end(0)));
            assertEquals(cancelExpected, commits(cancel.end(0)));
        }
    }

    @Test
    @Order(9)
    void sharedDurableDividesWorkAndIndependentSubscriptionGetsCopy() throws Exception {
        String ns = "shared";
        try (var alice =
                        jar(
                                "lab",
                                ns,
                                "s14",
                                "subscribe",
                                "--subscription",
                                "team",
                                "--worker",
                                "alice",
                                "--continuous",
                                "--sleep",
                                "50");
                var bob =
                        jar(
                                "lab",
                                ns,
                                "s14",
                                "subscribe",
                                "--subscription",
                                "team",
                                "--worker",
                                "bob",
                                "--continuous",
                                "--sleep",
                                "50");
                var audit =
                        jar(
                                "lab",
                                ns,
                                "s14",
                                "subscribe",
                                "--subscription",
                                "audit",
                                "--count",
                                "12")) {
            alice.ready("READY");
            bob.ready("READY");
            audit.ready("READY");
            Set<String> expected = ids(run(ns, "s14", "produce", "--count", "12"), "SEND");
            assertEquals(expected, commits(audit.end(0)));
            await(
                    () -> {
                        try {
                            return commits(alice.text()).size() + commits(bob.text()).size() == 12;
                        } catch (Exception e) {
                            return false;
                        }
                    },
                    Duration.ofSeconds(15));
            Set<String> a = commits(alice.text()), b = commits(bob.text());
            assertFalse(a.isEmpty());
            assertFalse(b.isEmpty());
            assertTrue(Collections.disjoint(a, b));
            a.addAll(b);
            assertEquals(expected, a);
        }
        run(ns, "s14", "unsubscribe", "--subscription", "team");
        run(ns, "s14", "unsubscribe", "--subscription", "audit");
    }

    @Test
    @Order(10)
    void requestReplyCorrelationAndBoundedTimeout() throws Exception {
        String ns = "request";
        try (var service = jar("lab", ns, "s10", "reply", "--count", "1")) {
            service.ready("READY");
            String answer = run(ns, "s10", "request");
            assertTrue(answer.contains("correlation=ID:"), answer);
            service.end(0);
        }
        try (var noService = jar("lab", "no-service", "s10", "request", "--timeout", "300")) {
            noService.end(2);
        }
    }

    @Test
    @Order(11)
    void expiryAndDeliveryDelayHaveBoundedTiming() throws Exception {
        String ns = "time";
        Set<String> expired =
                ids(run(ns, "s11", "produce", "--count", "1", "--ttl", "300"), "SEND");
        // An actual expired-message destination is polled; no fixed long sleep.
        long end = System.nanoTime() + Duration.ofSeconds(12).toNanos();
        String view = "";
        do {
            view = run(ns, "s11", "browse", "--target", "expiry");
            if (!ids(view, "BROWSE").isEmpty()) break;
        } while (System.nanoTime() < end);
        assertEquals(expired, ids(view, "BROWSE"));
        assertTrue(run(ns, "s11", "browse").contains("visible=0"));
        try (var consumer =
                jar("lab", ns, "s12", "consume", "--count", "1", "--timeout", "10000")) {
            consumer.ready("READY");
            long start = System.nanoTime();
            Set<String> expected =
                    ids(run(ns, "s12", "produce", "--count", "1", "--delay", "1800"), "SEND");
            assertEquals(expected, commits(consumer.end(0)));
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            assertTrue(elapsed >= 1600 && elapsed < 12000, "delay elapsed=" + elapsed);
        }
    }

    @Test
    @Order(12)
    void groupsKeepOrderAndWorkerPerOrder() throws Exception {
        String ns = "groups";
        try (var a =
                        jar(
                                "lab",
                                ns,
                                "s13",
                                "consume",
                                "--worker",
                                "alice",
                                "--continuous",
                                "--sleep",
                                "40");
                var b =
                        jar(
                                "lab",
                                ns,
                                "s13",
                                "consume",
                                "--worker",
                                "bob",
                                "--continuous",
                                "--sleep",
                                "40")) {
            a.ready("READY");
            b.ready("READY");
            Set<String> expected =
                    ids(run(ns, "s13", "produce", "--count", "4", "--events", "3"), "SEND");
            await(
                    () -> {
                        try {
                            return commits(a.text()).size() + commits(b.text()).size() == 12;
                        } catch (Exception e) {
                            return false;
                        }
                    },
                    Duration.ofSeconds(15));
            await(
                    () -> {
                        try {
                            return a.text().lines().filter(l -> l.startsWith("group=")).count()
                                            + b.text()
                                                    .lines()
                                                    .filter(l -> l.startsWith("group="))
                                                    .count()
                                    == 12;
                        } catch (IOException e) {
                            return false;
                        }
                    },
                    Duration.ofSeconds(15));
            Map<String, String> owners = new HashMap<>();
            Map<String, List<Integer>> sequences = new HashMap<>();
            for (String logs : List.of(a.text(), b.text()))
                for (String line : logs.split("\n"))
                    if (line.startsWith("group=")) {
                        var parts = line.split(" ");
                        String group = parts[0];
                        String worker = parts[2];
                        assertEquals(worker, owners.computeIfAbsent(group, k -> worker));
                        sequences
                                .computeIfAbsent(group, k -> new ArrayList<>())
                                .add(Integer.parseInt(parts[1].split("=")[1]));
                    }
            assertEquals(4, sequences.size());
            for (var sequence : sequences.values()) assertEquals(List.of(1, 2, 3), sequence);
            var actual = commits(a.text());
            actual.addAll(commits(b.text()));
            assertEquals(expected, actual);
        }
    }

    @Test
    @Order(13)
    void boundedBacklogDrainsWithSecondWorker() throws Exception {
        String ns = "backlog";
        Set<String> expected = ids(run(ns, "s16", "produce", "--count", "20"), "SEND");
        assertEquals(20, ids(run(ns, "s16", "browse"), "BROWSE").size());
        try (var slow =
                        jar(
                                "lab",
                                ns,
                                "s16",
                                "consume",
                                "--worker",
                                "slow",
                                "--continuous",
                                "--sleep",
                                "100");
                var fast =
                        jar(
                                "lab",
                                ns,
                                "s16",
                                "consume",
                                "--worker",
                                "fast",
                                "--continuous",
                                "--sleep",
                                "30")) {
            slow.ready("READY");
            fast.ready("READY");
            await(
                    () -> {
                        try {
                            return commits(slow.text()).size() + commits(fast.text()).size() == 20;
                        } catch (Exception e) {
                            return false;
                        }
                    },
                    Duration.ofSeconds(15));
            var actual = commits(slow.text());
            actual.addAll(commits(fast.text()));
            assertEquals(expected, actual);
        }
        assertTrue(run(ns, "s16", "browse").contains("visible=0"));
    }

    @Test
    @Order(14)
    void configurationErrorsAndResetGuards() throws Exception {
        Path isolated = temporary.resolve("another lab");
        Files.createDirectories(isolated.resolve("config"));
        Files.copy(
                source.resolve("config/local.properties"),
                isolated.resolve("config/local.properties"));
        try (var conflict = jarAt(isolated, "broker", "guards", "start")) {
            assertTrue(conflict.end(2).contains("Port is occupied"));
        }
        try (var reset = jar("broker", "guards", "reset", "--confirm")) {
            assertTrue(reset.end(2).contains("Broker is running"));
        }
        try (var portTaken = jar("broker", "guards", "start", "--dataDir", "runtime/another")) {
            assertTrue(portTaken.end(2).contains("Broker is running"));
        }
        try (var bad = jar("lab", "guards", "s01", "browse", "--password", "definitely-wrong")) {
            String output = bad.end(2);
            assertFalse(output.contains("definitely-wrong"));
        }
        try (var outside = jar("broker", "guards", "reset", "--confirm", "--dataDir", "outside")) {
            assertTrue(outside.end(2).contains("under runtime"));
        }
        try (var invalid = jar("lab", "guards", "s01", "consume", "--unknown")) {
            assertTrue(invalid.end(2).contains("Unknown option"));
        }
        try (var remote = jar("lab", "guards", "s01", "browse", "--profile", "remote")) {
            assertTrue(remote.end(2).contains("Remote requires"));
        }
    }

    @Test
    @Order(15)
    void runtimeArchiveLaunchersAndSafeResetFromSpacePath() throws Exception {
        stopBroker();
        try {
            Path unpack = temporary.resolve("unpacked runtime with spaces");
            Files.createDirectories(unpack);
            Path zip = source.resolve("distribution/target/artemis-java-jumpstart-runtime.zip");
            try (var input = new ZipInputStream(Files.newInputStream(zip))) {
                for (ZipEntry e; (e = input.getNextEntry()) != null; ) {
                    Path file = unpack.resolve(e.getName()).normalize();
                    assertTrue(file.startsWith(unpack));
                    if (e.isDirectory()) Files.createDirectories(file);
                    else {
                        Files.createDirectories(file.getParent());
                        Files.copy(input, file);
                    }
                }
            }
            Path root = unpack.resolve("artemis-java-jumpstart");
            assertTrue(Files.isRegularFile(root.resolve("lib/broker.jar")));
            if (!isWindows())
                try (var files = Files.list(root.resolve("scripts"))) {
                    files.filter(p -> p.toString().endsWith(".sh"))
                            .forEach(p -> p.toFile().setExecutable(true));
                }
            try (var absent = launch(root, "doctor", "runtime")) {
                String diagnostic = absent.end(2);
                assertTrue(diagnostic.contains("Broker port is unreachable"), diagnostic);
            }
            try (var b = launch(root, "broker", "start")) {
                b.ready("BROKER_READY");
                try (var doctor = launch(root, "doctor", "runtime")) {
                    assertTrue(doctor.end(0).contains("authentication successful"));
                }
                try (var producer = launch(root, "lab", "s01", "produce", "--count", "2")) {
                    producer.end(0);
                }
                try (var consumer = launch(root, "lab", "s01", "consume", "--count", "2")) {
                    assertEquals(2, commits(consumer.end(0)).size());
                }
                try (var bad = launch(root, "lab", "s01", "consume", "--count", "0")) {
                    bad.end(2);
                }
                try (var stop = launch(root, "broker", "stop")) {
                    stop.end(0);
                }
                b.end(0);
            }
            // Simulate a junction on Windows, a symlink elsewhere; reset must refuse without
            // touching
            // the target.
            Path outside = temporary.resolve("preserved");
            Files.createDirectories(outside);
            Files.writeString(outside.resolve("keep.txt"), "keep");
            Path link = root.resolve("runtime/broker/foreign-link");
            if (isWindows()) {
                try (var mk =
                        new Job(
                                List.of(
                                        "cmd.exe",
                                        "/d",
                                        "/c",
                                        "mklink",
                                        "/J",
                                        link.toString(),
                                        outside.toString()),
                                root)) {
                    mk.end(0);
                }
            } else Files.createSymbolicLink(link, outside);
            try (var refused = launch(root, "broker", "reset", "--confirm")) {
                assertTrue(refused.end(2).contains("Symlink/Junction"));
            }
            assertEquals("keep", Files.readString(outside.resolve("keep.txt")));
            Files.delete(link);
            Path marker = root.resolve("runtime/broker/.lab-data");
            String identity = Files.readString(marker);
            Files.writeString(marker, "not-owned");
            try (var refused = launch(root, "broker", "reset", "--confirm")) {
                assertTrue(refused.end(2).contains("ownership marker"));
            }
            Files.writeString(marker, identity);
            try (var reset = launch(root, "broker", "reset", "--confirm")) {
                reset.end(0);
            }
            assertFalse(Files.exists(root.resolve("runtime/broker")));
            assertEquals("keep", Files.readString(outside.resolve("keep.txt")));
        } finally {
            // Restore the shared fixture even if an archive launcher fails on one platform.
            startBroker();
        }
    }

    @Test
    @Order(16)
    void separateSampleSolutionsAndCompilableScaffolds() throws Exception {
        Assumptions.assumeTrue(
                Boolean.getBoolean("training.enabled"), "Mit -Ptraining separat aktivieren");
        // Exercises are compiled and packaged by the training profile. Their behavior is
        // deliberately
        // owned by the trainees; completing a scaffold must not invalidate this acceptance suite.
        assertTrue(Files.isRegularFile(source.resolve("exercises/target/exercises.jar")));
        try (var a = jar("solutions", "solution-a", "A", "produce", "--count", "10")) {
            a.end(0);
        }
        try (var a = jar("solutions", "solution-a", "A", "consume", "--count", "10")) {
            assertEquals(10, commits(a.end(0)).size());
        }
        for (String exercise : List.of("B", "F")) {
            String ns = "solution-" + exercise;
            List<String> args =
                    new ArrayList<>(
                            List.of(
                                    exercise,
                                    "subscribe",
                                    "--count",
                                    exercise.equals("F") ? "2" : "4"));
            if (exercise.equals("F")) args.addAll(List.of("--selector", "region = 'EU'"));
            try (var sub = jar("solutions", ns, args.toArray(String[]::new));
                    var other =
                            jar(
                                    "solutions",
                                    ns,
                                    exercise,
                                    "subscribe",
                                    "--count",
                                    "4",
                                    "--subscription",
                                    "other")) {
                sub.ready("READY");
                other.ready("READY");
                try (var pub = jar("solutions", ns, exercise, "produce", "--count", "4")) {
                    pub.end(0);
                }
                assertEquals(exercise.equals("F") ? 2 : 4, commits(sub.end(0)).size());
                assertEquals(4, commits(other.end(0)).size());
            }
        }
        for (String action : List.of("register", "produce", "subscribe", "unsubscribe")) {
            try (var c = jar("solutions", "solution-c", "C", action, "--count", "3")) {
                c.end(0);
            }
        }
        try (var d = jar("solutions", "solution-d", "D", "produce", "--count", "3")) {
            d.end(0);
        }
        try (var d = jar("solutions", "solution-d", "D", "consume", "--count", "5")) {
            assertEquals(2, commits(d.end(0)).size());
        }
        try (var d = jar("solutions", "solution-d", "D", "replay", "--count", "1")) {
            d.end(0);
        }
        try (var d = jar("solutions", "solution-d", "D", "consume", "--count", "1")) {
            assertEquals(1, commits(d.end(0)).size());
        }
        try (var e =
                jar("solutions", "solution-e", "E", "produce", "--count", "2", "--id", "same")) {
            e.end(0);
        }
        try (var e = jar("solutions", "solution-e", "E", "consume", "--count", "1", "--crash")) {
            e.end(17);
        }
        try (var e = jar("solutions", "solution-e", "E", "consume", "--count", "2")) {
            assertEquals(
                    2, e.end(0).lines().filter(l -> l.contains("ledgerEffect=DUPLICATE")).count());
        }
        assertTrue(run("solution-e", "ledger", "show").contains("ledgerEntries=1"));
    }

    @Test
    @Order(17)
    void remoteTlsNamespacesGroupsAndErrorQueueAuthorization() throws Exception {
        stopBroker();
        Path remote = temporary.resolve("remote trainer");
        Files.createDirectories(remote.resolve("config"));
        Path keytool =
                Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        isWindows() ? "keytool.exe" : "keytool");
        String storePassword = "synthetic-test-store";
        try (var generate =
                new Job(
                        List.of(
                                keytool.toString(),
                                "-genkeypair",
                                "-alias",
                                "broker",
                                "-keyalg",
                                "RSA",
                                "-keysize",
                                "2048",
                                "-validity",
                                "2",
                                "-dname",
                                "CN=localhost",
                                "-ext",
                                "SAN=dns:localhost",
                                "-storetype",
                                "PKCS12",
                                "-keystore",
                                remote.resolve("config/server.p12").toString(),
                                "-storepass",
                                storePassword),
                        remote)) {
            generate.end(0);
        }
        try (var export =
                new Job(
                        List.of(
                                keytool.toString(),
                                "-exportcert",
                                "-alias",
                                "broker",
                                "-keystore",
                                remote.resolve("config/server.p12").toString(),
                                "-storepass",
                                storePassword,
                                "-file",
                                remote.resolve("config/server.cer").toString()),
                        remote)) {
            export.end(0);
        }
        try (var trust =
                new Job(
                        List.of(
                                keytool.toString(),
                                "-importcert",
                                "-noprompt",
                                "-alias",
                                "broker",
                                "-keystore",
                                remote.resolve("config/trust.p12").toString(),
                                "-storetype",
                                "PKCS12",
                                "-storepass",
                                storePassword,
                                "-file",
                                remote.resolve("config/server.cer").toString()),
                        remote)) {
            trust.end(0);
        }
        Files.writeString(
                remote.resolve("config/users.properties"),
                "alice.password=synthetic-alice\n"
                        + "alice.namespace=alice\n"
                        + "alice.groups=team\n"
                        + "bob.password=synthetic-bob\n"
                        + "bob.namespace=bob\n"
                        + "bob.groups=team\n");
        Files.writeString(
                remote.resolve("config/local.properties"),
                "profile=remote\n"
                        + "host=localhost\n"
                        + "ssl=true\n"
                        + "namespace=alice\n"
                        + "user=alice\n"
                        + "password=synthetic-alice\n"
                        + "bind=127.0.0.1\n"
                        + "keyStore=config/server.p12\n"
                        + "keyStorePassword="
                        + storePassword
                        + "\ntrustStore=config/trust.p12\ntrustStorePassword="
                        + storePassword
                        + "\n");
        try (var secureBroker = jarAt(remote, "broker", "alice", "start")) {
            secureBroker.ready("BROKER_READY");
            try (var producer = jarAt(remote, "lab", "alice", "s01", "produce", "--count", "1")) {
                producer.end(0);
            }
            try (var consumer = jarAt(remote, "lab", "alice", "s01", "consume", "--count", "1")) {
                assertEquals(1, commits(consumer.end(0)).size());
            }
            try (var forbidden = jarAt(remote, "lab", "bob", "s01", "produce", "--count", "1")) {
                forbidden.end(2);
            }
            try (var group = jarAt(remote, "lab", "team", "s01", "produce", "--count", "1")) {
                group.end(0);
            }
            try (var group =
                    jarAt(
                            remote,
                            "lab",
                            "team",
                            "s01",
                            "consume",
                            "--count",
                            "1",
                            "--user",
                            "bob",
                            "--password",
                            "synthetic-bob")) {
                group.end(0);
            }
            try (var producer = jarAt(remote, "lab", "alice", "s06", "produce", "--count", "2")) {
                producer.end(0);
            }
            try (var worker = jarAt(remote, "lab", "alice", "s06", "consume", "--count", "4")) {
                worker.end(0);
            }
            try (var dlq = jarAt(remote, "lab", "alice", "s06", "browse", "--target", "dlq")) {
                assertEquals(1, ids(dlq.end(0), "BROWSE").size());
            }
            try (var forbidden =
                    jarAt(
                            remote,
                            "lab",
                            "alice",
                            "s06",
                            "browse",
                            "--target",
                            "dlq",
                            "--user",
                            "bob",
                            "--password",
                            "synthetic-bob")) {
                forbidden.end(2);
            }
            try (var service = jarAt(remote, "lab", "alice", "s10", "reply", "--count", "1")) {
                service.ready("READY");
                try (var request = jarAt(remote, "lab", "alice", "s10", "request")) {
                    request.end(0);
                }
                service.end(0);
            }
            try (var mismatch =
                    jarAt(remote, "lab", "alice", "s01", "browse", "--host", "127.0.0.1")) {
                mismatch.end(2);
            }
            try (var stop = jarAt(remote, "broker", "alice", "stop")) {
                stop.end(0);
            }
            secureBroker.end(0);
        } finally {
            startBroker();
        }
    }

    @Test
    @Order(18)
    void doctorBuildChecksAndConfigurationPrecedence() throws Exception {
        Path root = temporary.resolve("doctor without broker");
        Files.createDirectories(root.resolve("config"));
        Files.copy(
                source.resolve("config/local.properties"), root.resolve("config/local.properties"));
        Files.createDirectories(root.resolve(".mvn/wrapper"));
        Files.copy(
                source.resolve(".mvn/wrapper/maven-wrapper.jar"),
                root.resolve(".mvn/wrapper/maven-wrapper.jar"));
        Path client = source.resolve("lab/target/lab.jar");
        List<String> base =
                List.of(java, "-Dlab.home=" + root, "-jar", client.toString(), "doctor", "build");
        var cli = new ArrayList<>(base);
        cli.addAll(List.of("--namespace", "cli-wins"));
        try (var result = new Job(cli, root, Map.of("LAB_NAMESPACE", "env-value"))) {
            assertTrue(result.end(0).contains("namespace=cli-wins"));
        }
        try (var result = new Job(base, root, Map.of("LAB_NAMESPACE", "env-value"))) {
            assertTrue(result.end(0).contains("namespace=env-value"));
        }
        try (var result = new Job(base, root)) {
            assertTrue(result.end(0).contains("namespace=training"));
        }
        try (var missing = jarAt(root, "lab", "training", "doctor", "runtime")) {
            assertTrue(missing.end(2).contains("Build artifact is missing"));
        }
        // Launcher fails before invoking a JAR when JAVA_HOME points to an absent JDK.
        Path script = source.resolve("scripts/lab" + (isWindows() ? ".cmd" : ".sh"));
        List<String> launch =
                isWindows()
                        ? List.of("cmd.exe", "/d", "/s", "/c", "\"\"" + script + "\" help\"")
                        : List.of(script.toString(), "help");
        try (var missing =
                new Job(launch, root, Map.of("JAVA_HOME", root.resolve("no jdk").toString()))) {
            assertTrue(missing.end(2).contains("Java is missing"));
        }
        run("competing", "s02", "cleanup", "--confirm");
    }

    @Test
    @Order(19)
    void buildDoctorWorksBeforeAnyLabJarExists() throws Exception {
        Path root = temporary.resolve("unbuilt source with spaces");
        Files.createDirectories(root.resolve("scripts"));
        try (var scripts = Files.list(source.resolve("scripts"))) {
            for (Path script : scripts.toList()) {
                Path copy = root.resolve("scripts").resolve(script.getFileName());
                Files.copy(script, copy);
                if (copy.toString().endsWith(".sh")) assertTrue(copy.toFile().setExecutable(true));
            }
        }
        Files.copy(source.resolve("pom.xml"), root.resolve("pom.xml"));
        Files.createDirectories(root.resolve(".mvn/wrapper"));
        for (String file : List.of("maven-wrapper.jar", "maven-wrapper.properties"))
            Files.copy(source.resolve(".mvn/wrapper/" + file), root.resolve(".mvn/wrapper/" + file));
        assertFalse(Files.exists(root.resolve("lab/target/lab.jar")));
        try (var doctor = launch(root, "doctor", "build")) {
            assertTrue(doctor.end(0).contains("BUILD_READY"));
        }
        Files.delete(root.resolve("pom.xml"));
        try (var runtime = launch(root, "doctor", "build")) {
            assertTrue(runtime.end(2).contains("runtime ZIP runs demos only"));
        }
    }

    @Test
    @Order(20)
    void firstWorkerAllowsTimeToOpenTheProducerTerminal() throws Exception {
        // The quickstart starts its worker first. A reader needs more than the old five seconds.
        try (var worker = jar("lab", "slow-quickstart", "s01", "consume", "--count", "10",
                "--worker", "alice", "--timeout", "300000")) {
            worker.ready("READY");
            Thread.sleep(6500);
            assertTrue(worker.process.isAlive(), worker.text());
            Set<String> sent = ids(run("slow-quickstart", "s01", "produce", "--count", "10"), "SEND");
            assertEquals(sent, commits(worker.end(0)));
        }
        assertTrue(run("slow-quickstart", "s01", "browse").contains("visible=0"));
    }

    static Job launch(Path root, String script, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        Path file = root.resolve("scripts/" + script + (isWindows() ? ".cmd" : ".sh"));
        if (isWindows()) {
            command.addAll(List.of("cmd.exe", "/d", "/s", "/c"));
            StringBuilder line = new StringBuilder("\"").append("\"").append(file).append("\"");
            for (String arg : args)
                line.append(" \"").append(arg.replace("\"", "\"\"")).append("\"");
            line.append(" --port ").append(port).append("\"");
            command.add(line.toString());
        } else {
            command.add(file.toString());
            command.addAll(List.of(args));
            command.addAll(List.of("--port", "" + port));
        }
        return new Job(command, root);
    }
}
