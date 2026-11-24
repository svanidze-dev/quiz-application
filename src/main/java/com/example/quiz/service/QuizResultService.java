package com.example.quiz.service;

import com.example.quiz.entity.AnswerOption;
import com.example.quiz.entity.Question;
import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.QuizResult;
import com.example.quiz.entity.User;
import com.example.quiz.entity.UserAnswer;
import com.example.quiz.entity.enums.AttemptStatus;
import com.example.quiz.entity.enums.QuestionType;
import com.example.quiz.exception.BusinessException;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.AnswerOptionRepository;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.QuizResultRepository;
import com.example.quiz.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizResultService {

    private final QuizResultRepository quizResultRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserService userService;
    private final QuizService quizService;


    @Transactional
    public QuizResult startAttempt(Long userId, Long quizId) {
        User user = userService.getUserById(userId);
        Quiz quiz = quizService.getPublishedQuizById(quizId);

        boolean alreadyCompleted = quizResultRepository
                .existsByUserIdAndQuizIdAndStatus(userId, quizId, AttemptStatus.COMPLETED)
                || quizResultRepository
                .existsByUserIdAndQuizIdAndStatus(userId, quizId, AttemptStatus.TIMED_OUT);

        if (alreadyCompleted) {
            throw new BusinessException("Вы уже проходили эту викторину. Повторное прохождение запрещено.");
        }

        Optional<QuizResult> active = quizResultRepository
                .findFirstByUserIdAndQuizIdAndStatus(userId, quizId, AttemptStatus.IN_PROGRESS);
        if (active.isPresent()) {
            QuizResult existing = active.get();
            if (!closeIfExpired(existing)) {
                return existing;
            }
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
        if (questions.isEmpty()) {
            throw new BusinessException("В этой викторине пока нет вопросов");
        }

        QuizResult result = new QuizResult();
        result.setUser(user);
        result.setQuiz(quiz);
        result.setStatus(AttemptStatus.IN_PROGRESS);
        result.setMaxScore(questions.stream().mapToInt(Question::getPoints).sum());
        if (quiz.getTimeLimitSeconds() != null) {
            result.setDeadlineAt(LocalDateTime.now().plusSeconds(quiz.getTimeLimitSeconds()));
        }
        return quizResultRepository.save(result);
    }


    @Transactional
    public QuizResult getAttempt(Long userId, Long resultId) {
        QuizResult result = findUserResult(userId, resultId);
        closeIfExpired(result);
        return result;
    }

    @Transactional
    public List<Question> getAttemptQuestions(Long userId, Long resultId) {
        QuizResult result = getAttempt(userId, resultId);
        return questionRepository.findByQuizIdOrderByOrderIndexAsc(result.getQuiz().getId());
    }

    @Transactional
    public List<UserAnswer> getAnswers(Long userId, Long resultId) {
        QuizResult result = getAttempt(userId, resultId);
        return userAnswerRepository.findByResultId(result.getId());
    }


    @Transactional
    public UserAnswer saveAnswer(Long userId, Long resultId, Long questionId, Set<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            throw new BusinessException("Выберите хотя бы один вариант или пропустите вопрос");
        }

        QuizResult result = getActiveAttempt(userId, resultId);
        Question question = getQuestionOfQuiz(result, questionId);

        if (question.getType() == QuestionType.SINGLE_CHOICE && optionIds.size() > 1) {
            throw new BusinessException("В этом вопросе можно выбрать только один вариант");
        }

        List<AnswerOption> options = answerOptionRepository.findAllById(optionIds);
        boolean allBelongToQuestion = options.stream()
                .allMatch(o -> o.getQuestion().getId().equals(questionId));
        if (options.size() != optionIds.size() || !allBelongToQuestion) {
            throw new BusinessException("Переданы некорректные варианты ответа");
        }

        UserAnswer answer = findAnswer(result, questionId);
        if (answer == null) {
            answer = new UserAnswer();
            answer.setQuestion(question);
            result.addAnswer(answer);
        }
        answer.getSelectedOptions().clear();
        answer.getSelectedOptions().addAll(options);
        return userAnswerRepository.save(answer);
    }

    @Transactional
    public void skipQuestion(Long userId, Long resultId, Long questionId) {
        QuizResult result = getActiveAttempt(userId, resultId);
        getQuestionOfQuiz(result, questionId);
        result.getAnswers().removeIf(a -> a.getQuestion().getId().equals(questionId));
    }


    @Transactional
    public QuizResult finishAttempt(Long userId, Long resultId) {
        QuizResult result = findUserResult(userId, resultId);
        if (result.getStatus() != AttemptStatus.IN_PROGRESS) {
            return result;
        }
        AttemptStatus finalStatus = isExpired(result) ? AttemptStatus.TIMED_OUT : AttemptStatus.COMPLETED;
        closeAttempt(result, finalStatus);
        return result;
    }

    @Transactional
    public int finishExpiredAttempts() {
        List<QuizResult> expired = quizResultRepository
                .findByStatusAndDeadlineAtBefore(AttemptStatus.IN_PROGRESS, LocalDateTime.now());
        expired.forEach(result -> closeAttempt(result, AttemptStatus.TIMED_OUT));
        return expired.size();
    }


    @Transactional
    public QuizResult getResultForReview(Long userId, Long resultId) {
        QuizResult result = getAttempt(userId, resultId);
        if (result.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("Правильные ответы доступны только после завершения");
        }
        return result;
    }

    public Page<QuizResult> getUserHistory(Long userId, Pageable pageable) {
        userService.getUserById(userId);
        return quizResultRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
    }

    public Map<String, Object> getUserStatistics(Long userId) {
        userService.getUserById(userId);

        List<QuizResult> all = quizResultRepository
                .findByUserIdOrderByStartedAtDesc(userId, Pageable.unpaged())
                .getContent();

        long completed = all.stream()
                .filter(r -> r.getStatus() != AttemptStatus.IN_PROGRESS)
                .count();

        long inProgress = all.stream()
                .filter(r -> r.getStatus() == AttemptStatus.IN_PROGRESS)
                .count();

        double avgPercentage = all.stream()
                .filter(r -> r.getPercentage() != null)
                .mapToDouble(r -> r.getPercentage().doubleValue())
                .average()
                .orElse(0.0);

        int bestScore = all.stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(QuizResult::getScore)
                .max()
                .orElse(0);

        return Map.of(
                "userId", userId,
                "totalAttempts", all.size(),
                "completedAttempts", completed,
                "inProgressAttempts", inProgress,
                "averagePercentage", Math.round(avgPercentage * 100.0) / 100.0,
                "bestScore", bestScore
        );
    }


    public List<QuizResult> getLeaderboard(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return quizResultRepository.findLeaderboard(PageRequest.of(0, safeLimit));
    }

    public List<QuizResult> getLeaderboardByQuiz(Long quizId, int limit) {
        quizService.getQuizById(quizId);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return quizResultRepository.findLeaderboardByQuiz(quizId, PageRequest.of(0, safeLimit));
    }


    public QuizResult getResultById(Long resultId) {
        return quizResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Результат", resultId));
    }

    public Page<QuizResult> getAllResults(Pageable pageable) {
        return quizResultRepository.findAllWithUserAndQuiz(pageable);
    }

    public Page<QuizResult> getResultsByQuiz(Long quizId, Pageable pageable) {
        quizService.getQuizById(quizId);
        return quizResultRepository.findByQuizIdOrderByStartedAtDesc(quizId, pageable);
    }

    public Map<String, Object> getAdminStats() {
        long completed = quizResultRepository.countCompleted();
        Double avg = quizResultRepository.averagePercentage();
        return Map.of(
                "completedAttempts", completed,
                "averagePercentage", avg == null ? 0.0 : Math.round(avg * 100.0) / 100.0
        );
    }


    private QuizResult findUserResult(Long userId, Long resultId) {
        return quizResultRepository.findByIdAndUserId(resultId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Прохождение", resultId));
    }

    private QuizResult getActiveAttempt(Long userId, Long resultId) {
        QuizResult result = findUserResult(userId, resultId);
        if (result.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("Прохождение уже завершено");
        }
        if (isExpired(result)) {
            throw new BusinessException("Время вышло. Завершите прохождение, чтобы увидеть результат");
        }
        return result;
    }

    private Question getQuestionOfQuiz(QuizResult result, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Вопрос", questionId));
        if (!question.getQuiz().getId().equals(result.getQuiz().getId())) {
            throw new BusinessException("Этот вопрос не относится к проходимой викторине");
        }
        return question;
    }

    private UserAnswer findAnswer(QuizResult result, Long questionId) {
        return result.getAnswers().stream()
                .filter(a -> a.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElse(null);
    }

    private boolean isExpired(QuizResult result) {
        return result.getStatus() == AttemptStatus.IN_PROGRESS
                && result.getDeadlineAt() != null
                && LocalDateTime.now().isAfter(result.getDeadlineAt());
    }

    private boolean closeIfExpired(QuizResult result) {
        if (isExpired(result)) {
            closeAttempt(result, AttemptStatus.TIMED_OUT);
            return true;
        }
        return false;
    }

    private void closeAttempt(QuizResult result, AttemptStatus finalStatus) {
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(result.getQuiz().getId());
        List<Long> questionIds = questions.stream().map(Question::getId).toList();

        Map<Long, Set<Long>> correctByQuestion = new HashMap<>();
        for (AnswerOption option : answerOptionRepository.findByQuestionIdIn(questionIds)) {
            if (option.isCorrect()) {
                correctByQuestion
                        .computeIfAbsent(option.getQuestion().getId(), k -> new HashSet<>())
                        .add(option.getId());
            }
        }

        Map<Long, Set<Long>> selectedByQuestion = new HashMap<>();
        for (UserAnswer answer : userAnswerRepository.findByResultId(result.getId())) {
            Set<Long> selectedIds = answer.getSelectedOptions().stream()
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());
            selectedByQuestion.put(answer.getQuestion().getId(), selectedIds);
        }

        int score = 0;
        int maxScore = 0;
        for (Question question : questions) {
            maxScore += question.getPoints();
            Set<Long> correct = correctByQuestion.getOrDefault(question.getId(), Set.of());
            Set<Long> selected = selectedByQuestion.getOrDefault(question.getId(), Set.of());
            if (!correct.isEmpty() && correct.equals(selected)) {
                score += question.getPoints();
            }
        }

        BigDecimal percentage = (maxScore == 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(score)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxScore), 2, RoundingMode.HALF_UP);

        result.setScore(score);
        result.setMaxScore(maxScore);
        result.setPercentage(percentage);
        result.setStatus(finalStatus);
        result.setFinishedAt(finalStatus == AttemptStatus.TIMED_OUT && result.getDeadlineAt() != null
                ? result.getDeadlineAt()
                : LocalDateTime.now());
    }
}