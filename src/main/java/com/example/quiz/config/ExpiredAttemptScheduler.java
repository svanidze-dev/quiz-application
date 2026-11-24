package com.example.quiz.config;

import com.example.quiz.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExpiredAttemptScheduler {

    private final QuizResultService quizResultService;

    @Scheduled(fixedDelay = 60_000)
    public void closeExpired() {
        int closed = quizResultService.finishExpiredAttempts();
        if (closed > 0) {
            log.info("Автоматически закрыто просроченных попыток: {}", closed);
        }
    }
}