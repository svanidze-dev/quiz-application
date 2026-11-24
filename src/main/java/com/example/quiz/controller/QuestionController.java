package com.example.quiz.controller;

import com.example.quiz.entity.Question;
import com.example.quiz.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/quizzes/{quizId}/questions")
    public List<Question> getByQuiz(@PathVariable Long quizId) {
        return questionService.getQuestionsByQuiz(quizId);
    }

    @GetMapping("/questions/{id}")
    public Question getById(@PathVariable Long id) {
        return questionService.getQuestionById(id);
    }

    @PostMapping("/quizzes/{quizId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Question create(@PathVariable Long quizId, @RequestBody Question question) {
        return questionService.addQuestion(quizId, question);
    }

    @PutMapping("/questions/{id}")
    public Question update(@PathVariable Long id, @RequestBody Question question) {
        return questionService.updateQuestion(id, question);
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }
}