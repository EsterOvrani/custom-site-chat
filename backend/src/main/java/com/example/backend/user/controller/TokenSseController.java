package com.example.backend.user.controller;

import com.example.backend.user.event.TokenUpdateEvent;
import com.example.backend.user.model.User;
import com.example.backend.auth.service.JwtService;
import com.example.backend.user.service.UserService;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.exception.UnauthorizedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Slf4j
public class TokenSseController {

    // מפה של כל ה-emitters לפי userId
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final JwtService jwtService; // ✅ הוסף
    private final UserRepository userRepository; // ✅ שונה



    /**
     * Endpoint להתחברות ל-SSE
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTokenUpdates(@RequestParam(required = false) String token) {
        
        // ✅ אימות מה-token שבשאילתה
        User currentUser;
        try {
            if (token != null && !token.isEmpty()) {
                // אימות ה-token
                currentUser = authenticateFromToken(token);
            } else {
                // נסה לקבל מה-SecurityContext (אם יש session)
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof User) {
                    currentUser = (User) authentication.getPrincipal();
                } else {
                    throw new UnauthorizedException("No valid authentication");
                }
            }
        } catch (Exception e) {
            log.error("Failed to authenticate SSE connection", e);
            throw new UnauthorizedException("Invalid token");
        }

        Long userId = currentUser.getId();
        log.info("📡 User {} connected to token SSE stream", userId);

        // יצירת emitter עם timeout של שעה
        SseEmitter emitter = new SseEmitter(3600000L);

        // הוספת ה-emitter לרשימה של המשתמש
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // טיפול בהשלמה או שגיאה
        emitter.onCompletion(() -> {
            log.info("✅ SSE completed for user {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("⏱️ SSE timeout for user {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onError((ex) -> {
            log.error("❌ SSE error for user {}", userId, ex);
            removeEmitter(userId, emitter);
        });

        // שליחת הודעת חיבור ראשונית
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Token stream connected"));
        } catch (IOException e) {
            log.error("Error sending initial message", e);
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    /**
     * אימות token ידנית
     */
    private User authenticateFromToken(String token) {
        try {
            // השתמש ב-JwtService לאימות
            String username = jwtService.extractUsername(token);
            
            // ✅ תיקון: השתמש ב-UserRepository במקום UserService
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
            
            if (jwtService.isTokenValid(token, user)) {
                return user;
            }
            
            throw new UnauthorizedException("Invalid token");
        } catch (Exception e) {
            log.error("Token authentication failed", e);
            throw new UnauthorizedException("Invalid token");
        }
    }

    /**
     * מאזין ל-TokenUpdateEvent ושולח עדכון למשתמש הרלוונטי
     */
    @EventListener
    public void handleTokenUpdate(TokenUpdateEvent event) {
        Long userId = event.getUserId();
        
        log.info("📢 Received token update event for user {}", userId);

        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE connections for user {}", userId);
            return;
        }

        // הכנת הנתונים לשליחה
        Map<String, Object> data = Map.of(
            "used", event.getTokensUsed(),
            "remaining", event.getTokensRemaining(),
            "usagePercentage", event.getUsagePercentage()
        );

        // שליחה לכל ה-emitters של המשתמש
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("token-update")
                    .data(data));
                
                log.debug("✅ Sent token update to user {}", userId);
            } catch (IOException e) {
                log.error("Failed to send token update to user {}", userId, e);
                removeEmitter(userId, emitter);
            }
        });
    }

    /**
     * הסרת emitter מהרשימה
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
        emitter.complete();
    }

    /**
     * קבלת מספר החיבורים הפעילים (לניטור)
     */
    @GetMapping("/connections/count")
    public Map<String, Object> getConnectionsCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        int totalConnections = userEmitters.values().stream()
            .mapToInt(CopyOnWriteArrayList::size)
            .sum();
        
        return Map.of(
            "userId", currentUser.getId(),
            "userConnections", userEmitters.getOrDefault(currentUser.getId(), new CopyOnWriteArrayList<>()).size(),
            "totalConnections", totalConnections
        );
    }
}