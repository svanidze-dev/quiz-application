package com.example.quiz.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_answer_result_question", columnNames = {"result_id", "question_id"})
})
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class UserAnswer extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    @ToString.Exclude
    private QuizResult result;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @ToString.Exclude
    private Question question;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_answer_option",
            joinColumns = @JoinColumn(name = "user_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "answer_option_id")
    )
    @ToString.Exclude
    private Set<AnswerOption> selectedOptions = new HashSet<>();
}