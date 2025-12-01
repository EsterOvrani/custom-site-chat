package com.example.backend.document.repository;

import com.example.backend.document.model.Document;
import com.example.backend.document.model.Document.ProcessingStatus;
import com.example.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository למסמכים
 * 
 * כל המסמכים עכשיו שייכים ישירות למשתמש (User) ולא ל-Chat
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ==================== Basic Queries ====================

    /**
     * מציאת מסמך לפי ID (רק אקטיביים)
     */
    Optional<Document> findByIdAndActiveTrue(Long id);

    /**
     * מציאת כל המסמכים של משתמש (ממוינים לפי displayOrder)
     */
    List<Document> findByUserAndActiveTrueOrderByDisplayOrderAsc(User user);

    /**
     * ספירת מסמכים אקטיביים של משתמש
     */
    long countByUserAndActiveTrue(User user);

    // ==================== Status Queries ====================

    /**
     * מציאת מסמכים לפי סטטוס עיבוד
     */
    List<Document> findByUserAndProcessingStatusAndActiveTrue(
        User user, 
        ProcessingStatus status
    );

    /**
     * מציאת כל המסמכים שהושלמו
     */
    default List<Document> findCompletedDocuments(User user) {
        return findByUserAndProcessingStatusAndActiveTrue(user, ProcessingStatus.COMPLETED);
    }

    /**
     * מציאת כל המסמכים שנכשלו
     */
    default List<Document> findFailedDocuments(User user) {
        return findByUserAndProcessingStatusAndActiveTrue(user, ProcessingStatus.FAILED);
    }

    /**
     * מציאת כל המסמכים שבעיבוד
     */
    default List<Document> findProcessingDocuments(User user) {
        return findByUserAndProcessingStatusAndActiveTrue(user, ProcessingStatus.PROCESSING);
    }

    /**
     * מציאת כל המסמכים שממתינים
     */
    default List<Document> findPendingDocuments(User user) {
        return findByUserAndProcessingStatusAndActiveTrue(user, ProcessingStatus.PENDING);
    }

    /**
     * ספירת מסמכים לפי משתמש וסטטוס (אקטיביים בלבד)
     */
    long countByUserAndProcessingStatusAndActiveTrue(User user, ProcessingStatus status);

    // ==================== Display Order ====================

    /**
     * קבלת ה-displayOrder הגבוה ביותר של משתמש
     * (כדי להוסיף מסמך חדש בסוף)
     */
    @Query("SELECT COALESCE(MAX(d.displayOrder), 0) FROM Document d " +
           "WHERE d.user = :user AND d.active = true")
    Integer getMaxDisplayOrderByUser(@Param("user") User user);

    // ==================== File Hash ====================

    /**
     * בדיקה אם קיים מסמך עם אותו hash (למניעת כפילויות)
     */
    @Query("SELECT d FROM Document d " +
           "WHERE d.user = :user " +
           "AND d.contentHash = :contentHash " +
           "AND d.active = true")
    Optional<Document> findByUserAndContentHash(
        @Param("user") User user,
        @Param("contentHash") String contentHash
    );

    /**
     * בדיקה אם קיים מסמך עם אותו שם קובץ
     */
    @Query("SELECT d FROM Document d " +
           "WHERE d.user = :user " +
           "AND d.originalFileName = :fileName " +
           "AND d.active = true")
    Optional<Document> findByUserAndFileName(
        @Param("user") User user,
        @Param("fileName") String fileName
    );
}