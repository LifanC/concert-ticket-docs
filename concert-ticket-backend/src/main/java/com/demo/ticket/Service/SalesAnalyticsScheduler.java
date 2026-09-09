package com.demo.ticket.Service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true", matchIfMissing = true)
public class SalesAnalyticsScheduler {
    private static final Logger log = LoggerFactory.getLogger(SalesAnalyticsScheduler.class);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "sales-analytics");
        thread.setDaemon(true);
        return thread;
    });
    private final Path directory;
    private final String python;
    private final URI database;
    private final String username;
    private final String password;
    private final long delayMs;
    private final long timeoutSeconds;
    private boolean started;

    public SalesAnalyticsScheduler(
            @Value("${analytics.directory:}") String directory,
            @Value("${analytics.python:}") String python,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${analytics.delay-ms:1800000}") long delayMs,
            @Value("${analytics.timeout-seconds:120}") long timeoutSeconds) {
        Path local = Path.of("concert-ticket-analytics").toAbsolutePath();
        this.directory = (directory.isBlank()
                ? (Files.isDirectory(local) ? local : Path.of("../concert-ticket-analytics"))
                : Path.of(directory)).toAbsolutePath().normalize();
        this.python = python.isBlank() ? this.directory.resolve(
                System.getProperty("os.name").startsWith("Windows")
                        ? ".venv/Scripts/python.exe" : ".venv/bin/python").toString() : python;
        this.database = URI.create(url.substring("jdbc:".length()));
        this.username = username;
        this.password = password;
        this.delayMs = Math.max(1000, delayMs);
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!started) {
            started = true;
            executor.scheduleWithFixedDelay(this::runReport, 0, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    void runReport() {
        Process process = null;
        try {
            Path reports = directory.resolve("reports");
            Files.createDirectories(reports);
            ProcessBuilder builder = new ProcessBuilder(python, directory.resolve("analyze.py").toString());
            builder.directory(directory.toFile());
            builder.environment().put("PGHOST", database.getHost());
            builder.environment().put("PGPORT", Integer.toString(database.getPort() == -1 ? 5432 : database.getPort()));
            builder.environment().put("PGDATABASE", database.getPath().substring(1));
            builder.environment().put("PGUSER", username);
            builder.environment().put("POSTGRES_PASSWORD", password);
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.redirectErrorStream(true);
            builder.redirectOutput(reports.resolve("analytics-latest.log").toFile());
            process = builder.start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Python sales analytics timed out; next scheduled run will retry");
            } else if (process.exitValue() != 0) {
                log.warn("Python sales analytics failed (exit {}); see reports/analytics-latest.log", process.exitValue());
            } else {
                log.info("Python sales report generated in {}", reports);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            log.warn("Cannot run Python sales analytics; check analytics.directory and analytics.python ({})",
                    exception.getClass().getSimpleName());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }
}
