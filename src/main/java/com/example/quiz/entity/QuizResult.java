package com.example.quiz.entity;

import com.example.quiz.entity.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_results")
@Getter @Setter @NoArgsConstructor @ToString(callSuper = true)
public class QuizResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @ToString.Exclude
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    private Integer score;
    @Column(name = "max_score")
    private Integer maxScore;
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<UserAnswer> answers = new ArrayList<>();

    public void addAnswer(UserAnswer answer) {
        answers.add(answer);
        answer.setResult(this);
    }
}