package com.example.quiz.service;

import com.example.quiz.entity.Question;
import com.example.quiz.entity.Quiz;
import com.example.quiz.exception.BusinessException;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizService quizService;

    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Вопрос", id));
    }

    public List<Question> getQuestionsByQuiz(Long quizId) {
        quizService.getQuizById(quizId); // проверяем, что викторина существует
        return questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
    }

    @Transactional
    public Question addQuestion(Long quizId, Question data) {
        Quiz quiz = quizService.getQuizById(quizId);
        quizService.assertQuizEditable(quiz);

        if (data.getText() == null || data.getText().isBlank()) {
            throw new BusinessException("Текст вопроса обязателен");
        }

        Question question = new Question();
        question.setText(data.getText().trim());
        if (data.getType() != null) {
            question.setType(data.getType());
        }
        if (data.getPoints() != null) {
            question.setPoints(validPoints(data.getPoints()));
        }

        int orderIndex = (data.getOrderIndex() != null)
                ? data.getOrderIndex()
                : quiz.getQuestions().stream().mapToInt(Question::getOrderIndex).max().orElse(0) + 1;
        question.setOrderIndex(orderIndex);

        quiz.addQuestion(question);
        return questionRepository.save(question);
    }

    @Transactional
    public Question updateQuestion(Long id, Question data) {
        Question question = getQuestionById(id);
        quizService.assertQuizEditable(question.getQuiz());

        if (data.getText() != null) {
            if (data.getText().isBlank()) {
                throw new BusinessException("Текст вопроса не может быть пустым");
            }
            question.setText(data.getText().trim());
        }
        if (data.getType() != null) {
            question.setType(data.getType());
        }
        if (data.getPoints() != null) {
            question.setPoints(validPoints(data.getPoints()));
        }
        if (data.getOrderIndex() != null) {
            question.setOrderIndex(data.getOrderIndex());
        }
        return question;
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        Quiz quiz = question.getQuiz();
        quizService.assertQuizEditable(quiz);
        quiz.removeQuestion(question);
    }

    private int validPoints(Integer points) {
        if (points < 1) {
            throw new BusinessException("Баллы за вопрос должны быть не меньше 1");
        }
        return points;
    }
}