// backend/src/main/java/com/example/backend/auth/service/GoogleOAuthService.java
package com.example.backend.auth.service;

import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.infrastructure.email.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * אימות ויצירת/מציאת משתמש מטוקן Google
     */
    public User authenticateGoogleUser(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), 
                    new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String pictureUrl = (String) payload.get("picture");
            boolean emailVerified = payload.getEmailVerified();

            log.info("Google user authenticated: {}", email);

            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                // עדכן תמונת פרופיל אם השתנתה
                if (pictureUrl != null && !pictureUrl.equals(user.getProfilePictureUrl())) {
                    user.setProfilePictureUrl(pictureUrl);
                    user = userRepository.save(user);
                }
                return user;
            } else {
                // 🆕 משתמש חדש - צור ושלח פרטים למייל
                User newUser = createGoogleUser(email, googleId, firstName, lastName, emailVerified);
                if (pictureUrl != null) {
                    newUser.setProfilePictureUrl(pictureUrl);
                    newUser = userRepository.save(newUser);
                }
                return newUser;
            }
            
        } catch (Exception e) {
            log.error("Failed to authenticate Google user", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * יצירת משתמש חדש מחשבון Google
     */
    private User createGoogleUser(String email, String googleId, 
                                String firstName, String lastName, 
                                boolean emailVerified) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName != null ? firstName : "User");
        user.setLastName(lastName != null ? lastName : "");
        user.setGoogleId(googleId);
        user.setAuthProvider(User.AuthProvider.GOOGLE);
        
        // 🆕 יצירת username ייחודי
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }
        user.setUsername(username);
        
        // 🆕 יצירת סיסמה קריאה (לא UUID!)
        String tempPassword = generateReadablePassword();
        user.setTempPassword(tempPassword); // שמירה לפני hash
        user.setPassword(passwordEncoder.encode(tempPassword));
        
        user.setEnabled(emailVerified);
        
        user = userRepository.save(user);
        log.info("✅ Created new Google user: {} with username: {}", email, username);
        
        // 🆕 שליחת מייל עם פרטי התחברות
        try {
            emailService.sendGoogleUserCredentials(email, username, tempPassword);
            log.info("✅ Sent credentials email to: {}", email);
        } catch (MessagingException e) {
            log.error("❌ Failed to send credentials email to: {}", email, e);
            // לא זורקים exception - המשתמש כבר נוצר בהצלחה
        }
        
        return user;
    }

    /**
     * 🆕 יצירת סיסמה קריאה (8 תווים: אותיות גדולות+קטנות+מספרים)
     */
    private String generateReadablePassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String allChars = upperCase + lowerCase + numbers;
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(8);
        
        // וודא שיש לפחות אחד מכל סוג
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        
        // השלם עד 8 תווים
        for (int i = 3; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // ערבב
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
}