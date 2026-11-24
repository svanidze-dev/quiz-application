package com.example.quiz.controller;

import com.example.quiz.entity.FavoriteQuiz;
import com.example.quiz.service.FavoriteQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteQuizController {

    private final FavoriteQuizService favoriteQuizService;

    @GetMapping
    public Page<FavoriteQuiz> list(@PathVariable Long userId,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return favoriteQuizService.getUserFavorites(userId, pageable);
    }

    @PostMapping("/{quizId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteQuiz add(@PathVariable Long userId, @PathVariable Long quizId) {
        return favoriteQuizService.addToFavorites(userId, quizId);
    }

    @DeleteMapping("/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long userId, @PathVariable Long quizId) {
        favoriteQuizService.removeFromFavorites(userId, quizId);
    }

    @GetMapping("/{quizId}/status")
    public boolean isFavorite(@PathVariable Long userId, @PathVariable Long quizId) {
        return favoriteQuizService.isFavorite(userId, quizId);
    }
}