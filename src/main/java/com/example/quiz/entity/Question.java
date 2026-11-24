package com.example.quiz.entity;

import com.example.quiz.entity.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor @ToString(callSuper = true)
public class Question extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type = QuestionType.SINGLE_CHOICE;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private Integer points = 1;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @ToString.Exclude
    private Quiz quiz;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    private List<AnswerOption> options = new ArrayList<>();

    public void addOption(AnswerOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void removeOption(AnswerOption option) {
        options.remove(option);
        option.setQuestion(null);
    }
}