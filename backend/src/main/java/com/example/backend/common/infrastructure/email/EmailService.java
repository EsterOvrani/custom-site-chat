// backend/src/main/java/com/example/backend/common/infrastructure/email/EmailService.java
package com.example.backend.common.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {
    @Autowired
    private JavaMailSender emailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // ==================== Verification Email (קיים) ====================
    
    public void sendVerificationEmail(String to, String subject, String verificationCode) throws MessagingException {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String verificationLink = frontendUrl + "/verify?email=" + to + "&code=" + verificationCode;

            String htmlMessage = "<!DOCTYPE html>" +
                    "<html dir='rtl'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; direction: rtl; }" +
                    ".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }" +
                    ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }" +
                    ".header h1 { margin: 0; font-size: 28px; }" +
                    ".content { padding: 40px 30px; text-align: center; }" +
                    ".content p { font-size: 16px; color: #333; line-height: 1.6; margin-bottom: 30px; }" +
                    ".verify-button { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white !important; text-decoration: none; padding: 15px 40px; border-radius: 8px; font-size: 18px; font-weight: bold; margin: 20px 0; }" +
                    ".verify-button:hover { opacity: 0.9; }" +
                    ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e1e8ed; }" +
                    ".divider { margin: 30px 0; text-align: center; color: #999; }" +
                    ".code-box { background-color: #f8f9ff; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; margin: 20px 0; }" +
                    ".code-box .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; font-family: monospace; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='header'>" +
                    "<h1>📚 custom site chat</h1>" +
                    "</div>" +
                    "<div class='content'>" +
                    "<h2 style='color: #333; margin-bottom: 20px;'>ברוך הבא למערכת!</h2>" +
                    "<p>תודה שנרשמת למערכת ניהול המסמכים החכמה שלנו.</p>" +
                    "<p>כדי להשלים את תהליך הרשמה, אנא אמת את כתובת המייל שלך על ידי לחיצה על הכפתור:</p>" +
                    "<a href='" + verificationLink + "' class='verify-button'>✓ אמת את המייל שלי</a>" +
                    "<div class='divider'>או</div>" +
                    "<p style='font-size: 14px; color: #666;'>העתק והדבק את הקישור הבא בדפדפן:</p>" +
                    "<div class='code-box'>" +
                    "<a href='" + verificationLink + "' style='color: #667eea; word-break: break-all;'>" + verificationLink + "</a>" +
                    "</div>" +
                    "<div class='divider'>או השתמש בקוד האימות הידני:</div>" +
                    "<div class='code-box'>" +
                    "<div class='code'>" + verificationCode + "</div>" +
                    "</div>" +
                    "<p style='font-size: 13px; color: #999; margin-top: 30px;'>הקישור והקוד תקפים ל-15 דקות בלבד</p>" +
                    "</div>" +
                    "<div class='footer'>" +
                    "<p>אם לא נרשמת למערכת, אנא התעלם ממייל זה.</p>" +
                    "<p>© 2025 custom site chat. כל הזכויות שמורות.</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlMessage, true);

            emailSender.send(message);     

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new MessagingException("נכשל בשליחת אימייל אימות", e);
        }
    }

    // ==================== 🆕 Password Reset Email ====================
    
    public void sendPasswordResetEmail(String to, String resetCode) throws MessagingException {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String resetLink = frontendUrl + "/reset-password?email=" + to + "&code=" + resetCode;

            String htmlMessage = "<!DOCTYPE html>" +
                    "<html dir='rtl'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; direction: rtl; }" +
                    ".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }" +
                    ".header { background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%); color: white; padding: 30px; text-align: center; }" +
                    ".header h1 { margin: 0; font-size: 28px; }" +
                    ".content { padding: 40px 30px; text-align: center; }" +
                    ".content p { font-size: 16px; color: #333; line-height: 1.6; margin-bottom: 30px; }" +
                    ".reset-button { display: inline-block; background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%); color: white !important; text-decoration: none; padding: 15px 40px; border-radius: 8px; font-size: 18px; font-weight: bold; margin: 20px 0; }" +
                    ".reset-button:hover { opacity: 0.9; }" +
                    ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e1e8ed; }" +
                    ".divider { margin: 30px 0; text-align: center; color: #999; }" +
                    ".code-box { background-color: #fff5f5; border: 2px dashed #ff6b6b; border-radius: 8px; padding: 20px; margin: 20px 0; }" +
                    ".code-box .code { font-size: 32px; font-weight: bold; color: #ff6b6b; letter-spacing: 8px; font-family: monospace; }" +
                    ".warning { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 8px; padding: 15px; margin: 20px 0; color: #856404; font-size: 14px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='header'>" +
                    "<h1>🔐 איפוס סיסמה</h1>" +
                    "</div>" +
                    "<div class='content'>" +
                    "<h2 style='color: #333; margin-bottom: 20px;'>בקשה לאיפוס סיסמה</h2>" +
                    "<p>קיבלנו בקשה לאיפוס הסיסמה של החשבון שלך.</p>" +
                    "<p>לחץ על הכפתור למטה כדי ליצור סיסמה חדשה:</p>" +
                    "<a href='" + resetLink + "' class='reset-button'>🔄 איפוס סיסמה</a>" +
                    "<div class='divider'>או</div>" +
                    "<p style='font-size: 14px; color: #666;'>השתמש בקוד האיפוס הבא:</p>" +
                    "<div class='code-box'>" +
                    "<div class='code'>" + resetCode + "</div>" +
                    "</div>" +
                    "<div class='warning'>" +
                    "⚠️ <strong>חשוב:</strong> הקוד תקף ל-15 דקות בלבד. אם לא ביקשת איפוס סיסמה, התעלם ממייל זה." +
                    "</div>" +
                    "<p style='font-size: 13px; color: #999; margin-top: 30px;'>הקישור והקוד תקפים ל-15 דקות בלבד</p>" +
                    "</div>" +
                    "<div class='footer'>" +
                    "<p>אם לא ביקשת איפוס סיסמה, אנא התעלם ממייל זה.</p>" +
                    "<p>© 2025 custom site chat. כל הזכויות שמורות.</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setTo(to);
            helper.setSubject("איפוס סיסמה - Custom Site Chat");
            helper.setText(htmlMessage, true);

            emailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new MessagingException("נכשל בשליחת אימייל איפוס סיסמה", e);
        }
    }

    // ==================== 🆕 Google User Credentials Email ====================
    
    public void sendGoogleUserCredentials(String to, String username, String tempPassword) throws MessagingException {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String loginLink = frontendUrl + "/login";
            String changePasswordLink = frontendUrl + "/login"; // או עמוד מיוחד

            String htmlMessage = "<!DOCTYPE html>" +
                    "<html dir='rtl'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; direction: rtl; }" +
                    ".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }" +
                    ".header { background: linear-gradient(135deg, #4285f4 0%, #34a853 100%); color: white; padding: 30px; text-align: center; }" +
                    ".header h1 { margin: 0; font-size: 28px; }" +
                    ".content { padding: 40px 30px; text-align: center; }" +
                    ".content p { font-size: 16px; color: #333; line-height: 1.6; margin-bottom: 20px; }" +
                    ".credentials-box { background-color: #e8f5e9; border: 2px solid #4caf50; border-radius: 8px; padding: 25px; margin: 30px 0; text-align: right; }" +
                    ".credentials-box .label { font-weight: 600; color: #2e7d32; margin-bottom: 8px; }" +
                    ".credentials-box .value { font-size: 20px; font-family: monospace; background: white; padding: 12px; border-radius: 6px; margin-bottom: 15px; color: #1b5e20; border: 1px solid #4caf50; }" +
                    ".login-button { display: inline-block; background: linear-gradient(135deg, #4285f4 0%, #34a853 100%); color: white !important; text-decoration: none; padding: 15px 40px; border-radius: 8px; font-size: 18px; font-weight: bold; margin: 20px 0; }" +
                    ".login-button:hover { opacity: 0.9; }" +
                    ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e1e8ed; }" +
                    ".warning { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 8px; padding: 15px; margin: 20px 0; color: #856404; font-size: 14px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='header'>" +
                    "<h1>🎉 ברוך הבא!</h1>" +
                    "</div>" +
                    "<div class='content'>" +
                    "<h2 style='color: #333; margin-bottom: 20px;'>נרשמת בהצלחה דרך Google</h2>" +
                    "<p>היי! נרשמת למערכת שלנו באמצעות חשבון Google שלך.</p>" +
                    "<p>כדי שתוכל להתחבר גם <strong>בלי Google</strong> (עם שם משתמש וסיסמה), הנה פרטי ההתחברות שלך:</p>" +
                    "<div class='credentials-box'>" +
                    "<div class='label'>👤 שם משתמש:</div>" +
                    "<div class='value'>" + username + "</div>" +
                    "<div class='label'>🔑 סיסמה זמנית:</div>" +
                    "<div class='value'>" + tempPassword + "</div>" +
                    "</div>" +
                    "<p><strong>איך להתחבר?</strong></p>" +
                    "<p style='font-size: 14px; color: #666;'>יש לך 2 אפשרויות:</p>" +
                    "<p style='font-size: 14px; color: #666;'>1️⃣ להתחבר דרך Google (כרגיל)<br/>2️⃣ להתחבר עם שם המשתמש והסיסמה שלמעלה</p>" +
                    "<div class='warning'>" +
                    "⚠️ <strong>מומלץ:</strong> שנה את הסיסמה הזמנית לסיסמה קבועה שתזכור בקלות!" +
                    "</div>" +
                    "<a href='" + loginLink + "' class='login-button'>🚀 התחבר עכשיו</a>" +
                    "<p style='font-size: 13px; color: #999; margin-top: 30px;'>שמור את הפרטים האלה במקום בטוח</p>" +
                    "</div>" +
                    "<div class='footer'>" +
                    "<p>אם לא נרשמת למערכת, אנא התעלם ממייל זה.</p>" +
                    "<p>© 2025 custom site chat. כל הזכויות שמורות.</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setTo(to);
            helper.setSubject("פרטי התחברות למערכת - Custom Site Chat");
            helper.setText(htmlMessage, true);

            emailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send Google user credentials email to: {}", to, e);
            throw new MessagingException("נכשל בשליחת אימייל פרטי התחברות", e);
        }
    }
}