package com.example.quiz.repository;

import com.example.quiz.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestionIdOrderByOrderIndexAsc(Long questionId);

    List<AnswerOption> findByQuestionIdIn(Collection<Long> questionIds);
}