package com.genai.java.spring.rag.service;

import com.genai.java.spring.rag.config.data.RagConfigData;
import com.genai.java.spring.rag.exception.RagException;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class PdfWatcherService implements SmartLifecycle {

    private final RagIngestionService ingestion;
    private final RagConfigData ragConfig;
    private final Executor watchLoop;
    private final ScheduledExecutorService debounce;

    private volatile boolean running = false;
    private java.nio.file.WatchService watchService;
    private final Map<Path, ScheduledFuture<?>> scheduled = new java.util.concurrent.ConcurrentHashMap<>();


    public PdfWatcherService(RagIngestionService ingestion,
                             RagConfigData ragConfig,
                             @Qualifier("traceableWatchLoopExecutor") Executor watchLoop,
                             @Qualifier("traceableScheduledExecutorService") ScheduledExecutorService debounce) {
        this.ingestion = ingestion;
        this.ragConfig = ragConfig;
        this.watchLoop = watchLoop;
        this.debounce = debounce;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void start() {
        try {
            Path dir = resolveWatchedDir();       // the directory, not the *.pdf glob
            log.info("Starting PDF folder watcher for dir: {}", dir);
            this.watchService = dir.getFileSystem().newWatchService();
            dir.register(watchService,
                    java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
                    java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY,
                    java.nio.file.StandardWatchEventKinds.ENTRY_DELETE);
            running = true;
            watchLoop.execute(() -> {
                try {
                    log.info("PDF watcher loop started for dir: {}", dir);
                    this.watchDir();
                } catch (Exception e) {
                    log.error("PDF watcher loop terminated with error", e);
                }
            });
        } catch (Exception e) {
            throw new RagException("Failed to start PDF watcher", e);
        }
    }

    private Path resolveWatchedDir() {
        // ragConfig.getPdf().getPath() is like: file:./rag/qa-over-internal-docs/*.pdf
        String pattern = ragConfig.getPdf().getPath().replaceFirst("^file:", "");
        Path path = java.nio.file.Paths.get(pattern).toAbsolutePath().normalize();
        // If a glob is present, watch its parent; otherwise if a file is given, watch its parent
        if (pattern.contains("*") || !java.nio.file.Files.isDirectory(path)) {
            return path.getParent();
        }
        return path;
    }

    private void watchDir() {
        while (running) {
            try {
                var key = watchService.take();
                Path dir = (Path) key.watchable();
                for (var event : key.pollEvents()) {
                    var kind = event.kind();
                    if (kind == java.nio.file.StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Path relative = ((java.nio.file.WatchEvent<Path>) event).context();
                    Path path = dir.resolve(relative);

                    // Only care about .pdf files
                    if (!relative.toString().toLowerCase().endsWith(".pdf")) {
                        continue;
                    }

                    // Debounce per file (e.g., 1s)
                    scheduled.compute(path, (p, pending) -> {
                        if (pending != null) pending.cancel(false);  // cancel any timer already queued for this file
                        return debounce.schedule(() -> handleEvent(kind, p), 1, java.util.concurrent.TimeUnit.SECONDS); // schedule a new one and run only if no new events for 1s
                    });
                }
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey no longer valid for {}", dir);
                    break; // or try to re-register the directory
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Watcher is interrupted!");
                break;
            } catch (Exception e) {
                log.warn("Error in watcher loop!", e);
            }
        }
    }

    private void handleEvent(java.nio.file.WatchEvent.Kind<?> kind, Path path) {
        try {
            log.info("Received event:{} -> for file:{}", kind.name(), path.getFileName());
            if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_DELETE) {
                // remove from store
                ingestion.deleteBySource(path.getFileName().toString());
            } else {
                // create/modify → upsert this one file
                ingestion.upsertOneByPath(path);
            }
        } catch (Exception e) {
            // log error
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {
        } finally {
            debounce.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
