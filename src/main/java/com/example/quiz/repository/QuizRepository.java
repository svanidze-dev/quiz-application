package com.example.quiz.repository;

import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.enums.Difficulty;
import com.example.quiz.entity.enums.QuizStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Page<Quiz> findByStatus(QuizStatus status, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Quiz> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    java.util.Optional<Quiz> findById(Long id);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select q from Quiz q
            where q.status = :status
              and (:title is null or lower(q.title) like lower(concat('%', :title, '%')))
              and (:categoryId is null or q.category.id = :categoryId)
              and (:difficulty is null or q.difficulty = :difficulty)
            """)
    Page<Quiz> search(@Param("status") QuizStatus status,
                      @Param("title") String title,
                      @Param("categoryId") Long categoryId,
                      @Param("difficulty") Difficulty difficulty,
                      Pageable pageable);
}