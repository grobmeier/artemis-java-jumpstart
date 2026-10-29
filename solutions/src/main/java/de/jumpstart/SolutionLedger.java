package de.jumpstart;

import java.nio.file.*;
import java.sql.*;

/** One unique business ID and its local effect are stored by the SAME DB transaction. */
public final class SolutionLedger implements AutoCloseable {
    private final Connection db;

    public SolutionLedger(Settings settings) throws Exception {
        Path dir = settings.home.resolve("runtime/ledger");
        Files.createDirectories(dir);
        Path path = dir.resolve(settings.namespace());
        if (!dir.toRealPath().equals(dir)) throw new UsageException("Ledger-Symlink verweigert.");
        for (String suffix : new String[] {"", ".mv.db", ".trace.db", ".lock.db"}) {
            Path file = Path.of(path + suffix);
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(file) || !file.toRealPath().equals(file)))
                throw new UsageException("Ledger-Symlink/Junction verweigert.");
        }
        db =
                DriverManager.getConnection(
                        "jdbc:h2:file:" + path + ";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0", "sa", "");
        try {
            try (var statement = db.createStatement()) {
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS ledger (business_id VARCHAR(160) PRIMARY KEY,"
                            + " order_id VARCHAR(160) NOT NULL, amount_cents BIGINT NOT NULL)");
            }
            db.setAutoCommit(false);
        } catch (SQLException e) {
            db.close();
            throw e;
        }
    }

    public boolean apply(String id, String order, long cents) throws SQLException {
        try (var insert = db.prepareStatement("INSERT INTO ledger VALUES (?,?,?)")) {
            insert.setString(1, id);
            insert.setString(2, order);
            insert.setLong(3, cents);
            insert.executeUpdate();
            db.commit();
            return true;
        } catch (SQLException e) {
            db.rollback();
            if ("23505".equals(e.getSQLState())) return false;
            throw e;
        }
    }

    public long count() throws SQLException {
        try (var s = db.createStatement();
                var rows = s.executeQuery("SELECT COUNT(*) FROM ledger")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    public void clear() throws SQLException {
        try (var s = db.createStatement()) {
            s.executeUpdate("DELETE FROM ledger");
            db.commit();
        }
    }

    @Override
    public void close() throws SQLException {
        db.close();
    }
}
