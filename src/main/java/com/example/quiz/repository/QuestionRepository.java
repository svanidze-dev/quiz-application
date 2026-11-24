package com.example.quiz.repository;

import com.example.quiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByOrderIndexAsc(Long quizId);

    long countByQuizId(Long quizId);
}