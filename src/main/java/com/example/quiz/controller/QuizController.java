package com.example.quiz.controller;

import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.enums.Difficulty;
import com.example.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public Page<Quiz> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Difficulty difficulty,
            @PageableDefault(size = 20) Pageable pageable) {
        return quizService.getPublishedQuizzes(title, categoryId, difficulty, pageable);
    }

    @GetMapping("/all")
    public Page<Quiz> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return quizService.getAllQuizzes(pageable);
    }

    @GetMapping("/{id}")
    public Quiz getById(@PathVariable Long id) {
        return quizService.getQuizById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Quiz create(@RequestParam Long categoryId, @RequestBody Quiz quiz) {
        return quizService.createQuiz(quiz, categoryId);
    }

    @PutMapping("/{id}")
    public Quiz update(@PathVariable Long id,
                       @RequestParam(required = false) Long categoryId,
                       @RequestBody Quiz quiz) {
        return quizService.updateQuiz(id, quiz, categoryId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        quizService.deleteQuiz(id);
    }

    @PatchMapping("/{id}/publish")
    public Quiz publish(@PathVariable Long id) {
        return quizService.publishQuiz(id);
    }

    @PatchMapping("/{id}/unpublish")
    public Quiz unpublish(@PathVariable Long id) {
        return quizService.unpublishQuiz(id);
    }
}