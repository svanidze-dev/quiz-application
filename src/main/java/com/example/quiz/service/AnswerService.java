package com.example.quiz.service;

import com.example.quiz.entity.AnswerOption;
import com.example.quiz.entity.Question;
import com.example.quiz.entity.enums.QuestionType;
import com.example.quiz.exception.BusinessException;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.AnswerOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {

    private final AnswerOptionRepository answerOptionRepository;
    private final QuestionService questionService;
    private final QuizService quizService;

    public AnswerOption getOptionById(Long id) {
        return answerOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Вариант ответа", id));
    }

    public List<AnswerOption> getOptionsByQuestion(Long questionId) {
        questionService.getQuestionById(questionId); // проверяем, что вопрос существует
        return answerOptionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
    }

    @Transactional
    public AnswerOption addOption(Long questionId, AnswerOption data) {
        Question question = questionService.getQuestionById(questionId);
        quizService.assertQuizEditable(question.getQuiz());

        if (data.getText() == null || data.getText().isBlank()) {
            throw new BusinessException("Текст варианта ответа обязателен");
        }

        AnswerOption option = new AnswerOption();
        option.setText(data.getText().trim());
        option.setCorrect(data.isCorrect());

        int orderIndex = (data.getOrderIndex() != null)
                ? data.getOrderIndex()
                : question.getOptions().stream().mapToInt(AnswerOption::getOrderIndex).max().orElse(0) + 1;
        option.setOrderIndex(orderIndex);

        question.addOption(option);
        return answerOptionRepository.save(option);
    }

    @Transactional
    public AnswerOption updateOption(Long optionId, AnswerOption data) {
        AnswerOption option = getOptionById(optionId);
        quizService.assertQuizEditable(option.getQuestion().getQuiz());

        if (data.getText() != null) {
            if (data.getText().isBlank()) {
                throw new BusinessException("Текст варианта ответа не может быть пустым");
            }
            option.setText(data.getText().trim());
        }
        option.setCorrect(data.isCorrect());
        if (data.getOrderIndex() != null) {
            option.setOrderIndex(data.getOrderIndex());
        }
        return option;
    }

    @Transactional
    public void deleteOption(Long optionId) {
        AnswerOption option = getOptionById(optionId);
        Question question = option.getQuestion();
        quizService.assertQuizEditable(question.getQuiz());
        question.removeOption(option); // orphanRemoval удалит вариант из БД
    }


    @Transactional
    public List<AnswerOption> setCorrectOptions(Long questionId, Set<Long> correctOptionIds) {
        Question question = questionService.getQuestionById(questionId);
        quizService.assertQuizEditable(question.getQuiz());

        if (correctOptionIds == null || correctOptionIds.isEmpty()) {
            throw new BusinessException("Нужно указать хотя бы один правильный вариант");
        }
        if (question.getType() == QuestionType.SINGLE_CHOICE && correctOptionIds.size() > 1) {
            throw new BusinessException("В вопросе с одним ответом может быть только один правильный вариант");
        }

        Set<Long> existingIds = question.getOptions().stream()
                .map(AnswerOption::getId)
                .collect(Collectors.toSet());
        if (!existingIds.containsAll(correctOptionIds)) {
            throw new BusinessException("Среди переданных вариантов есть не относящиеся к этому вопросу");
        }

        for (AnswerOption option : question.getOptions()) {
            option.setCorrect(correctOptionIds.contains(option.getId()));
        }
        return question.getOptions();
    }
}