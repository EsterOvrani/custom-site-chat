package com.example.backend.query.controller;

import com.example.backend.common.dto.ApiResponse;
import com.example.backend.common.exception.UnauthorizedException;  
import com.example.backend.query.dto.PublicQueryRequest;
import com.example.backend.query.dto.QueryResponse;
import com.example.backend.query.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;  
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryService queryService;

    // Public API: answer question using documents
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<QueryResponse>> askQuestion(
            @Valid @RequestBody PublicQueryRequest request) {

        log.info("📥 Query request received for secretKey: {}", request.getSecretKey());

        try {
            QueryResponse response = queryService.askQuestion(
                    request.getSecretKey(),
                    request.getQuestion(),
                    request.getHistory()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (UnauthorizedException e) {
            log.error("❌ Unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Error processing query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("שגיאה בעיבוד השאלה"));
        }
    }
}