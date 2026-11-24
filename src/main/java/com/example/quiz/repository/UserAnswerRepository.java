package com.example.quiz.repository;

import com.example.quiz.entity.UserAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    @EntityGraph(attributePaths = "selectedOptions")
    List<UserAnswer> findByResultId(Long resultId);

    @EntityGraph(attributePaths = "selectedOptions")
    Optional<UserAnswer> findByResultIdAndQuestionId(Long resultId, Long questionId);
}