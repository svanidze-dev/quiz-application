package com.example.quiz.controller;

import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.QuizResult;
import com.example.quiz.entity.User;
import com.example.quiz.entity.enums.UserRole;
import com.example.quiz.service.QuizResultService;
import com.example.quiz.service.QuizService;
import com.example.quiz.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final QuizResultService quizResultService;
    private final QuizService quizService;
    private final UserService userService;

    private void requireAdmin(HttpSession session) {
        if (session == null || session.getAttribute("userId") == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Требуется авторизация");
        }
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Только для администратора");
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(HttpSession session) {
        requireAdmin(session);
        return quizResultService.getAdminStats();
    }

    @GetMapping("/results")
    public Page<QuizResult> allResults(HttpSession session,
                                       @PageableDefault(size = 20) Pageable pageable) {
        requireAdmin(session);
        return quizResultService.getAllResults(pageable);
    }

    @GetMapping("/quizzes/{quizId}/results")
    public Page<QuizResult> resultsByQuiz(HttpSession session,
                                          @PathVariable Long quizId,
                                          @PageableDefault(size = 20) Pageable pageable) {
        requireAdmin(session);
        return quizResultService.getResultsByQuiz(quizId, pageable);
    }

    @GetMapping("/quizzes/popular")
    public List<Quiz> popularQuizzes(HttpSession session,
                                     @RequestParam(defaultValue = "10") int limit) {
        requireAdmin(session);
        return quizService.getPopularQuizzes(limit);
    }
}