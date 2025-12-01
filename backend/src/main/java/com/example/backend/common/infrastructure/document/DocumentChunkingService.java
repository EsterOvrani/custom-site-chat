package com.example.backend.common.infrastructure.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentChunkingService {

    // Split document into overlapping chunks
    public List<TextSegment> chunkDocument(String content, String fileName, Long documentId) {
        log.info("Chunking document: {} (length: {})", fileName, content.length());

        // Splitting with overlap
        DocumentSplitter splitter = DocumentSplitters.recursive(
            500,  // chunk size
            50   // overlap - maintains context between chunks
        );

        Document document = Document.from(
            content,
            Metadata.from(Map.of(
                "fileName", fileName,
                "documentId", documentId.toString(),
                "timestamp", Instant.now().toString()
            ))
        );

        List<TextSegment> segments = splitter.split(document)
            .stream()
            .map(doc -> TextSegment.from(doc.text(), doc.metadata()))
            .collect(Collectors.toList());

        log.info("Created {} chunks from document", segments.size());
        return segments;
    }
}
