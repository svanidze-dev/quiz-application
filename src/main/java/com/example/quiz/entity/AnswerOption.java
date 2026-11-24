package com.example.quiz.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "answer_options", indexes = {
        @Index(name = "idx_option_question", columnList = "question_id")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class AnswerOption extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @ToString.Exclude
    private Question question;
}