package com.genai.java.spring;

import com.genai.java.spring.rag.service.PdfWatcherService;
import com.genai.java.spring.rag.service.RagIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenAIJavaSpringApplication implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;
    private final PdfWatcherService pdfWatcherService;

    public GenAIJavaSpringApplication(RagIngestionService ragIngestionService, PdfWatcherService pdfWatcherService) {
        this.ragIngestionService = ragIngestionService;
        this.pdfWatcherService = pdfWatcherService;
    }

    static void main(String[] args) {
        SpringApplication.run(GenAIJavaSpringApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ragIngestionService.initializePgVectorStore();
        pdfWatcherService.start();
    }
}
