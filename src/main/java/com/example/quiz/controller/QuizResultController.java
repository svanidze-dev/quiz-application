package com.example.quiz.controller;

import com.example.quiz.entity.Question;
import com.example.quiz.entity.QuizResult;
import com.example.quiz.entity.UserAnswer;
import com.example.quiz.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizResultController {

    private final QuizResultService quizResultService;

    @PostMapping("/users/{userId}/quizzes/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResult startAttempt(@PathVariable Long userId, @PathVariable Long quizId) {
        return quizResultService.startAttempt(userId, quizId);
    }

    @GetMapping("/users/{userId}/attempts/{resultId}")
    public QuizResult getAttempt(@PathVariable Long userId, @PathVariable Long resultId) {
        return quizResultService.getAttempt(userId, resultId);
    }

    @GetMapping("/users/{userId}/attempts/{resultId}/questions-safe")
    public List<Map<String, Object>> getSafeQuestions(
            @PathVariable Long userId, @PathVariable Long resultId) {
        List<Question> questions = quizResultService.getAttemptQuestions(userId, resultId);
        return questions.stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("text", q.getText());
            m.put("type", q.getType());
            m.put("points", q.getPoints());
            m.put("orderIndex", q.getOrderIndex());
            m.put("options", q.getOptions().stream().map(o -> {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("id", o.getId());
                om.put("text", o.getText());
                om.put("orderIndex", o.getOrderIndex());
                return om;
            }).toList());
            return m;
        }).toList();
    }

    @GetMapping("/users/{userId}/attempts/{resultId}/answers")
    public List<UserAnswer> getAnswers(@PathVariable Long userId, @PathVariable Long resultId) {
        return quizResultService.getAnswers(userId, resultId);
    }

    @PutMapping("/users/{userId}/attempts/{resultId}/questions/{questionId}/answer")
    public UserAnswer saveAnswer(@PathVariable Long userId,
                                 @PathVariable Long resultId,
                                 @PathVariable Long questionId,
                                 @RequestBody Set<Long> optionIds) {
        return quizResultService.saveAnswer(userId, resultId, questionId, optionIds);
    }

    @DeleteMapping("/users/{userId}/attempts/{resultId}/questions/{questionId}/answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skipQuestion(@PathVariable Long userId,
                             @PathVariable Long resultId,
                             @PathVariable Long questionId) {
        quizResultService.skipQuestion(userId, resultId, questionId);
    }

    @PatchMapping("/users/{userId}/attempts/{resultId}/finish")
    public QuizResult finish(@PathVariable Long userId, @PathVariable Long resultId) {
        return quizResultService.finishAttempt(userId, resultId);
    }

    @GetMapping("/users/{userId}/attempts/{resultId}/review")
    public QuizResult review(@PathVariable Long userId, @PathVariable Long resultId) {
        return quizResultService.getResultForReview(userId, resultId);
    }


    @GetMapping("/users/{userId}/history")
    public Page<QuizResult> history(@PathVariable Long userId,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return quizResultService.getUserHistory(userId, pageable);
    }

    @GetMapping("/users/{userId}/statistics")
    public Map<String, Object> statistics(@PathVariable Long userId) {
        return quizResultService.getUserStatistics(userId);
    }


    @GetMapping("/leaderboard")
    public List<QuizResult> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        return quizResultService.getLeaderboard(limit);
    }

    @GetMapping("/quizzes/{quizId}/leaderboard")
    public List<QuizResult> leaderboardByQuiz(@PathVariable Long quizId,
                                              @RequestParam(defaultValue = "10") int limit) {
        return quizResultService.getLeaderboardByQuiz(quizId, limit);
    }


    @GetMapping("/results")
    public Page<QuizResult> getAllResults(@PageableDefault(size = 20) Pageable pageable) {
        return quizResultService.getAllResults(pageable);
    }

    @GetMapping("/results/{id}")
    public QuizResult getResultById(@PathVariable Long id) {
        return quizResultService.getResultById(id);
    }

    @GetMapping("/quizzes/{quizId}/results")
    public Page<QuizResult> getResultsByQuiz(@PathVariable Long quizId,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return quizResultService.getResultsByQuiz(quizId, pageable);
    }

    @PostMapping("/results/close-expired")
    public int closeExpired() {
        return quizResultService.finishExpiredAttempts();
    }
}