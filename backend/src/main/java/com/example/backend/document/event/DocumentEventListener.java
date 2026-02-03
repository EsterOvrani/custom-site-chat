package com.example.backend.document.event;

import com.example.backend.document.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private final DocumentProcessingService documentProcessingService;

    /**
     * ✅ זה ירוץ אחרי שה-transaction מסתיים!
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("documentProcessingExecutor")
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        log.info("====================================================");
        log.info("📢 DocumentCreatedEvent received for ID: {}", event.getDocumentId());
        log.info("Transaction committed - starting async processing");
        log.info("====================================================");
        
        // עכשיו המסמך כבר committed ב-DB!
        documentProcessingService.processDocumentAsync(
            event.getDocumentId(),
            event.getFileBytes(),
            event.getOriginalFilename(),
            event.getContentType(),
            event.getFileSize(),
            event.getFilePath(),
            event.getUserId(),
            event.getCollectionName()
        );
    }
}