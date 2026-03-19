package com.genai.java.spring;

import com.genai.java.spring.rag.service.PdfWatcherService;
import com.genai.java.spring.rag.service.RagIngestionService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenAIJavaSpringApplication implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;
    private final PdfWatcherService pdfWatcherService;
    private final ObservationRegistry registry;

    public GenAIJavaSpringApplication(RagIngestionService ragIngestionService,
                                      PdfWatcherService pdfWatcherService,
                                      ObservationRegistry registry) {
        this.ragIngestionService = ragIngestionService;
        this.pdfWatcherService = pdfWatcherService;
        this.registry = registry;
    }

    static void main(String[] args) {
        SpringApplication.run(GenAIJavaSpringApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Observation ragObservation = Observation.start("rag.ingestion.flow.run", registry);
        try (Observation.Scope scope = ragObservation.openScope()) {
            ragIngestionService.initializePgVectorStore();
        } finally {
            ragObservation.stop();
        }
        pdfWatcherService.start();
    }
}
