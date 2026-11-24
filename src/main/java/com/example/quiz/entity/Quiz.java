package com.example.quiz.entity;

import com.example.quiz.entity.enums.Difficulty;
import com.example.quiz.entity.enums.QuizStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes", indexes = {
        @Index(name = "idx_quiz_title", columnList = "title"),
        @Index(name = "idx_quiz_status", columnList = "status"),
        @Index(name = "idx_quiz_difficulty", columnList = "difficulty")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class Quiz extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private Category category;

    @JsonIgnore
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    private List<Question> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuiz(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setQuiz(null);
    }
}