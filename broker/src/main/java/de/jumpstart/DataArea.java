package de.jumpstart;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Fail-closed data ownership and cross-process exclusion, also during reset. */
public final class DataArea implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    public final Path data;
    private final String identity;

    public DataArea(Settings settings) throws IOException {
        Path runtime = settings.home.resolve("runtime");
        data = settings.data().toAbsolutePath().normalize();
        if (!data.startsWith(runtime) || data.equals(runtime))
            throw new UsageException("Datenverzeichnis muss unter runtime/ liegen.");
        checkPath(runtime);
        checkPath(data);
        Files.createDirectories(runtime);
        Path lockPath = runtime.resolve(".broker.lock");
        checkPath(lockPath);
        channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock candidate;
        try {
            candidate = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            candidate = null;
        }
        if (candidate == null) {
            channel.close();
            throw new UsageException("Broker läuft oder Reset ist aktiv; zuerst Broker stoppen.");
        }
        lock = candidate;
        identity = "artemis-java-jumpstart-v1\n" + settings.home + "\n" + data + "\n";
    }

    private static void checkPath(Path p) throws IOException {
        for (Path part = p; part != null; part = part.getParent()) {
            if (Files.exists(part, LinkOption.NOFOLLOW_LINKS)) {
                // toRealPath follows Windows junctions as well as symbolic links.
                if (Files.isSymbolicLink(part)
                        || !part.toRealPath().equals(part.toAbsolutePath().normalize()))
                    throw new UsageException(
                            "Symlink/Junction oder nichtkanonischer Datenpfad verweigert.");
            }
        }
    }

    public void initialize() throws IOException {
        if (Files.exists(data)) {
            try (var entries = Files.list(data)) {
                if (entries.findAny().isPresent()) verifyMarker();
            }
        }
        Files.createDirectories(data);
        Path marker = data.resolve(".lab-data");
        checkPath(marker);
        Files.writeString(
                marker, identity, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void verifyMarker() throws IOException {
        Path marker = data.resolve(".lab-data");
        checkPath(marker);
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)
                || !Files.readString(marker).equals(identity))
            throw new UsageException(
                    "Reset/Start verweigert: Eigentumsmarker fehlt oder passt nicht zum"
                            + " Datenpfad.");
    }

    public void reset() throws IOException {
        checkPath(data);
        verifyMarker();
        // Preflight the complete tree before deleting anything. No link traversal.
        try (var paths = Files.walk(data)) {
            for (Path p : paths.toList()) checkPath(p);
        }
        Files.walkFileTree(
                data,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path p, BasicFileAttributes a)
                            throws IOException {
                        checkPath(p);
                        Files.delete(p);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path p, IOException e)
                            throws IOException {
                        if (e != null) throw e;
                        checkPath(p);
                        Files.delete(p);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            channel.close();
        }
    }
}
