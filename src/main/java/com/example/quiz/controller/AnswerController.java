package com.example.quiz.controller;

import com.example.quiz.entity.AnswerOption;
import com.example.quiz.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @GetMapping("/questions/{questionId}/options")
    public List<AnswerOption> getByQuestion(@PathVariable Long questionId) {
        return answerService.getOptionsByQuestion(questionId);
    }

    @GetMapping("/options/{id}")
    public AnswerOption getById(@PathVariable Long id) {
        return answerService.getOptionById(id);
    }

    @PostMapping("/questions/{questionId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    public AnswerOption create(@PathVariable Long questionId, @RequestBody AnswerOption option) {
        return answerService.addOption(questionId, option);
    }

    @PutMapping("/options/{id}")
    public AnswerOption update(@PathVariable Long id, @RequestBody AnswerOption option) {
        return answerService.updateOption(id, option);
    }

    @DeleteMapping("/options/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        answerService.deleteOption(id);
    }

    @PatchMapping("/questions/{questionId}/options/correct")
    public List<AnswerOption> setCorrect(@PathVariable Long questionId,
                                         @RequestBody Set<Long> correctOptionIds) {
        return answerService.setCorrectOptions(questionId, correctOptionIds);
    }
}