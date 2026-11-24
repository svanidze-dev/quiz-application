package com.example.quiz.service;

import com.example.quiz.entity.FavoriteQuiz;
import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.User;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.FavoriteQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteQuizService {

    private final FavoriteQuizRepository favoriteQuizRepository;
    private final UserService userService;
    private final QuizService quizService;

    @Transactional
    public FavoriteQuiz addToFavorites(Long userId, Long quizId) {
        User user = userService.getUserById(userId);
        Quiz quiz = quizService.getPublishedQuizById(quizId);

        return favoriteQuizRepository.findByUserIdAndQuizId(userId, quizId)
                .orElseGet(() -> {
                    FavoriteQuiz favorite = new FavoriteQuiz();
                    favorite.setUser(user);
                    favorite.setQuiz(quiz);
                    return favoriteQuizRepository.save(favorite);
                });
    }

    @Transactional
    public void removeFromFavorites(Long userId, Long quizId) {
        if (!favoriteQuizRepository.existsByUserIdAndQuizId(userId, quizId)) {
            throw new ResourceNotFoundException("Викторина (id=" + quizId + ") не найдена в избранном");
        }
        favoriteQuizRepository.deleteByUserIdAndQuizId(userId, quizId);
    }

    public Page<FavoriteQuiz> getUserFavorites(Long userId, Pageable pageable) {
        userService.getUserById(userId);
        return favoriteQuizRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public boolean isFavorite(Long userId, Long quizId) {
        return favoriteQuizRepository.existsByUserIdAndQuizId(userId, quizId);
    }
}