package com.example.backend.query.controller;

import com.example.backend.query.service.QueryService;
import com.example.backend.query.dto.PublicQueryRequest;
import com.example.backend.query.dto.AnswerResponse;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryService queryService;

    /**
     * שאילת שאלה ציבורית (ללא אימות!)
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askPublicQuestion(
            @Valid @RequestBody PublicQueryRequest request) {

        log.info("Public query received (session: {})", request.getSessionId());

        AnswerResponse answer = queryService.askPublicQuestion(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", answer.getSuccess());
        response.put("data", answer);

        return ResponseEntity.ok(response);
    }
}