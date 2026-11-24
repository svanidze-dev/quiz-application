package com.example.quiz.repository;

import com.example.quiz.entity.FavoriteQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteQuizRepository extends JpaRepository<FavoriteQuiz, Long> {

    Page<FavoriteQuiz> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<FavoriteQuiz> findByUserIdAndQuizId(Long userId, Long quizId);

    boolean existsByUserIdAndQuizId(Long userId, Long quizId);

    void deleteByUserIdAndQuizId(Long userId, Long quizId);

    void deleteByQuizId(Long quizId);

    boolean existsByQuizId(Long quizId);
}