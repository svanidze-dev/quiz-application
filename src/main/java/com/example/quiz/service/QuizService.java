package com.example.quiz.service;

import com.example.quiz.entity.AnswerOption;
import com.example.quiz.entity.Category;
import com.example.quiz.entity.Question;
import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.enums.Difficulty;
import com.example.quiz.entity.enums.QuestionType;
import com.example.quiz.entity.enums.QuizStatus;
import com.example.quiz.exception.BusinessException;
import com.example.quiz.exception.ConflictException;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.FavoriteQuizRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.quiz.repository.QuizResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final FavoriteQuizRepository favoriteQuizRepository;
    private final CategoryService categoryService;


    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Викторина", id));
    }

    public Quiz getPublishedQuizById(Long id) {
        Quiz quiz = getQuizById(id);
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Викторина", id);
        }
        return quiz;
    }

    public Page<Quiz> getAllQuizzes(Pageable pageable) {
        return quizRepository.findAll(pageable);
    }

    public Page<Quiz> getPublishedQuizzes(String title, Long categoryId, Difficulty difficulty, Pageable pageable) {
        String normalizedTitle = (title == null || title.isBlank()) ? null : title.trim();
        return quizRepository.search(QuizStatus.PUBLISHED, normalizedTitle, categoryId, difficulty, pageable);
    }

    public List<Quiz> getPopularQuizzes(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Object[]> rows = quizResultRepository.findQuizPopularity(PageRequest.of(0, safeLimit));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(r -> (Long) r[0]).toList();
        Map<Long, Quiz> byId = quizRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Quiz::getId, q -> q));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }


    @Transactional
    public Quiz createQuiz(Quiz data, Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId);

        Quiz quiz = new Quiz();
        applyFields(quiz, data);
        quiz.setCategory(category);
        quiz.setStatus(QuizStatus.DRAFT);
        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz updateQuiz(Long id, Quiz data, Long categoryId) {
        Quiz quiz = getQuizById(id);
        applyFields(quiz, data);
        if (categoryId != null) {
            quiz.setCategory(categoryService.getCategoryById(categoryId));
        }
        return quiz;
    }

    @Transactional
    public void deleteQuiz(Long id) {
        Quiz quiz = getQuizById(id);
        if (quizResultRepository.existsByQuizId(id)) {
            throw new ConflictException(
                    "Нельзя удалить викторину: по ней есть прохождения. Снимите её с публикации.");
        }
        favoriteQuizRepository.deleteByQuizId(id);
        quizRepository.delete(quiz);
    }


    @Transactional
    public Quiz publishQuiz(Long id) {
        Quiz quiz = getQuizById(id);
        validateReadyToPublish(quiz);
        quiz.setStatus(QuizStatus.PUBLISHED);
        return quiz;
    }

    @Transactional
    public Quiz unpublishQuiz(Long id) {
        Quiz quiz = getQuizById(id);
        quiz.setStatus(QuizStatus.DRAFT);
        return quiz;
    }


    public void assertQuizEditable(Quiz quiz) {
        if (quiz.getStatus() == QuizStatus.PUBLISHED) {
            throw new BusinessException(
                    "Викторина опубликована. Снимите её с публикации, чтобы менять вопросы.");
        }
        if (quizResultRepository.existsByQuizId(quiz.getId())) {
            throw new ConflictException(
                    "По викторине уже есть прохождения, менять вопросы нельзя.");
        }
    }


    private void applyFields(Quiz target, Quiz source) {
        if (source.getTitle() == null || source.getTitle().isBlank()) {
            throw new BusinessException("Название викторины обязательно");
        }
        target.setTitle(source.getTitle().trim());
        target.setDescription(source.getDescription());

        if (source.getDifficulty() != null) {
            target.setDifficulty(source.getDifficulty());
        }

        Integer limit = source.getTimeLimitSeconds();
        if (limit != null && limit <= 0) {
            throw new BusinessException("Лимит времени должен быть больше нуля (или пустым — без лимита)");
        }
        target.setTimeLimitSeconds(limit);
    }

    private void validateReadyToPublish(Quiz quiz) {
        List<Question> questions = quiz.getQuestions();
        if (questions.isEmpty()) {
            throw new BusinessException("В викторине нет вопросов");
        }

        for (Question question : questions) {
            int number = question.getOrderIndex();
            List<AnswerOption> options = question.getOptions();

            if (options.size() < 2) {
                throw new BusinessException("Вопрос №" + number + ": нужно минимум 2 варианта ответа");
            }

            long correctCount = options.stream().filter(AnswerOption::isCorrect).count();
            if (correctCount == 0) {
                throw new BusinessException("Вопрос №" + number + ": не выбран правильный ответ");
            }
            if (question.getType() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
                throw new BusinessException(
                        "Вопрос №" + number + ": для одиночного выбора должен быть ровно один правильный ответ");
            }
        }
    }
}