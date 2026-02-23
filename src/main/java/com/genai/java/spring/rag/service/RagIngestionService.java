package com.genai.java.spring.rag.service;

import com.genai.java.spring.rag.config.data.PgVectorStoreConfigData;
import com.genai.java.spring.rag.config.data.RagConfigData;
import com.genai.java.spring.rag.config.data.RagConstants;
import com.genai.java.spring.rag.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class RagIngestionService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final RagConfigData ragConfigData;
    private final PdfDocumentReaderConfig pdfDocumentReaderConfig;
    private final TokenTextSplitter tokenTextSplitter;
    private final String ragVectorStoreTableName;

    public RagIngestionService(@Qualifier("ragVectorStore") VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate,
                               RagConfigData ragConfigData,
                               TokenTextSplitter tokenTextSplitter,
                               PgVectorStoreConfigData pgVectorStoreConfigData) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.ragConfigData = ragConfigData;
        //Build a simple PDF reading config
        RagConfigData.PdfProperties pdfProperties = ragConfigData.getPdf();
        this.pdfDocumentReaderConfig = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(pdfProperties.getPagesPerDocument())
                .withPageExtractedTextFormatter(
                        ExtractedTextFormatter.builder()
                                .withLeftAlignment(pdfProperties.isLeftAlignment())
                                .withNumberOfTopTextLinesToDelete(pdfProperties.getNumberOfTopTextLinesToDelete())
                                .withNumberOfBottomTextLinesToDelete(pdfProperties.getNumberOfBottomTextLinesToDelete())
                                .build()
                )
                .build();
        this.tokenTextSplitter = tokenTextSplitter;
        this.ragVectorStoreTableName = pgVectorStoreConfigData.getTableNameForRag();
    }

    public void initializePgVectorStore() throws RagException {
        if (skipIngest(jdbcTemplate)) {
            return;
        }
        var pdfResources = getResources();
        if (pdfResources == null) {
            return;
        }
        ingestDocumentChunksToVectorStore(pdfResources);
    }

    public void upsertOneByPath(Path path) {
        log.info("Processing new path for ingestion: {}", path);
        FileSystemResource resource = new FileSystemResource(path.toFile());
        String source = resource.getFilename();
        String checksum = sha256Hex(resource);
        // If same checksum already in DB, skip
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + ragVectorStoreTableName + " WHERE metadata->>'source'=? AND metadata->>'checksum'=?",
                Integer.class, source, checksum);

        if (count != null && count > 0) {
            log.warn("Skipping new entry: Checksum already in DB!");
            return;
        }
        deleteBySource(source);
        ingestDocumentChunksToVectorStore(new Resource[] {resource});
    }

    public void deleteBySource(String source) {
        try {
            jdbcTemplate.update("DELETE FROM " + ragVectorStoreTableName + " WHERE metadata->>'source' = ?", source);
            log.info("Deleted source: {} from vector store", source);
        } catch (DataAccessException e) {
            throw new RagException("Error deleting source: " + source + " from vector store!", e);
        }
    }

    private boolean skipIngest(JdbcTemplate jdbcTemplate) {
        if (ragConfigData.isForceRebuild()) {
            log.info("force-rebuild=true -> truncating {}", ragVectorStoreTableName);
            jdbcTemplate.update("TRUNCATE TABLE " + ragVectorStoreTableName);
        } else {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + ragVectorStoreTableName, Integer.class);
            if (count != null && count > 0) {
                log.info("Vector store already populated ({} rows). Skipping ingest. Set app.rag.force-rebuild=true to rebuild", count);
                return true;
            }
        }
        return false;
    }

    private Resource[] getResources() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            String pdfPath = ragConfigData.getPdf().getPath();
            Resource[] pdfResources = resolver.getResources(pdfPath);
            if (pdfResources.length == 0) {
                log.warn("No PDFs found at {}", pdfPath);
                return null;
            }
            return pdfResources;
        } catch (IOException e) {
            throw new RagException("Could not get pdf resources!", e);
        }
    }

    private void ingestDocumentChunksToVectorStore(Resource[] pdfResources) {
        var documents = getDocuments(pdfResources);
        var chunks = getChunks(documents);
        addChunkIndex(chunks);
        vectorStore.add(chunks);
        log.info("Ingested {} chunks into PgVector", chunks.size());
    }

    private List<Document> getDocuments(Resource[] pdfResources) {
        List<Document> documents = new ArrayList<>();
        for (Resource pdfResource: pdfResources) {
            List<Document> parts = getDocumentParts(pdfResource);
            addMetadata(pdfResource, parts);
            documents.addAll(parts);
        }
        return documents;
    }

    private List<Document> getDocumentParts(Resource pdfResource) {
        List<Document> parts;
        if (RagConstants.PARAGRAPH.equalsIgnoreCase(ragConfigData.getPdf().getMode())) {
            //Paragraph mode relies on PDF Outline/TOC; not all PDFS have it
            parts = new ParagraphPdfDocumentReader(pdfResource, pdfDocumentReaderConfig).read();
        } else {
            parts = new PagePdfDocumentReader(pdfResource, pdfDocumentReaderConfig).read();
        }
        return parts;
    }

    private void addMetadata(Resource pdfResource, List<Document> parts) {
        for (var part : parts) {
            part.getMetadata().putIfAbsent(RagConstants.SOURCE, pdfResource.getFilename());
            part.getMetadata().putIfAbsent(RagConstants.DOC_TYPE,
                    Objects.requireNonNull(pdfResource.getFilename()).substring(0, pdfResource.getFilename().indexOf(".")));
            part.getMetadata().putIfAbsent(RagConstants.UPDATED_AT, ZonedDateTime.now().toLocalDate().toString());
            String checksum = sha256Hex(pdfResource);
            part.getMetadata().put(RagConstants.CHECKSUM, checksum);
        }
    }

    private List<Document> getChunks(List<Document> documents) {
        return tokenTextSplitter.apply(documents);
    }

    private void addChunkIndex(List<Document> chunks) {
        Map<String, Integer> counters = new HashMap<>();
        for (var chunk : chunks) {
            var source = String.valueOf(chunk.getMetadata().getOrDefault(RagConstants.SOURCE, RagConstants.UNKNOWN));
            var index = counters.merge(source, 1, Integer::sum) - 1;
            chunk.getMetadata().put(RagConstants.CHUNK_INDEX, index);
        }
    }

    /**
     * Computes the SHA-256 hex digest of the given resource via DigestInputStream.
     */
    private String sha256Hex(Resource resource) {
        try {
            final MessageDigest messageDigest = newMessageDigest("SHA-256");

            try (InputStream inputStream = resource.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest)) {
                //Drain the stream -> DigestInputStream updated messageDigest under the hood
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }

            return HexFormat.of().formatHex(messageDigest.digest());
        } catch (IOException e) {
            throw new RagException("Error creating sha256Hex for resource " + resource.getFilename(), e);
        }

    }

    private MessageDigest newMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RagException("Missing JCA provider for " + algorithm, e);
        }
    }

}
