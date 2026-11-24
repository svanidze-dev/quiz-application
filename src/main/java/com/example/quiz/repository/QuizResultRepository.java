package com.example.quiz.repository;

import com.example.quiz.entity.QuizResult;
import com.example.quiz.entity.enums.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    @EntityGraph(attributePaths = {"user", "quiz", "quiz.category"})
    Page<QuizResult> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Optional<QuizResult> findByIdAndUserId(Long id, Long userId);

    Optional<QuizResult> findFirstByUserIdAndQuizIdAndStatus(Long userId, Long quizId, AttemptStatus status);

    boolean existsByUserIdAndQuizIdAndStatus(Long userId, Long quizId, AttemptStatus status);

    List<QuizResult> findByStatusAndDeadlineAtBefore(AttemptStatus status, LocalDateTime time);

    @EntityGraph(attributePaths = {"user", "quiz", "quiz.category"})
    Page<QuizResult> findByQuizIdOrderByStartedAtDesc(Long quizId, Pageable pageable);

    boolean existsByQuizId(Long quizId);

    long countByQuizId(Long quizId);

    @EntityGraph(attributePaths = {"user", "quiz"})
    @Query("""
            SELECT r FROM QuizResult r
            WHERE r.status <> com.example.quiz.entity.enums.AttemptStatus.IN_PROGRESS
              AND r.percentage IS NOT NULL
            ORDER BY r.percentage DESC, r.score DESC, r.finishedAt ASC
            """)
    List<QuizResult> findLeaderboard(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "quiz"})
    @Query("""
            SELECT r FROM QuizResult r
            WHERE r.quiz.id = :quizId
              AND r.status <> com.example.quiz.entity.enums.AttemptStatus.IN_PROGRESS
              AND r.percentage IS NOT NULL
            ORDER BY r.percentage DESC, r.score DESC, r.finishedAt ASC
            """)
    List<QuizResult> findLeaderboardByQuiz(@Param("quizId") Long quizId, Pageable pageable);

    @Query("""
            SELECT COUNT(r) FROM QuizResult r
            WHERE r.status <> com.example.quiz.entity.enums.AttemptStatus.IN_PROGRESS
            """)
    long countCompleted();

    @Query("""
            SELECT AVG(r.percentage) FROM QuizResult r
            WHERE r.status <> com.example.quiz.entity.enums.AttemptStatus.IN_PROGRESS
              AND r.percentage IS NOT NULL
            """)
    Double averagePercentage();

    @EntityGraph(attributePaths = {"user", "quiz"})
    @Query("SELECT r FROM QuizResult r")
    Page<QuizResult> findAllWithUserAndQuiz(Pageable pageable);

    @Query("""
            SELECT r.quiz.id, COUNT(r)
            FROM QuizResult r
            GROUP BY r.quiz.id
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> findQuizPopularity(Pageable pageable);
}