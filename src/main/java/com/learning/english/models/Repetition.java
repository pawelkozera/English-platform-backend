package com.learning.english.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "repetition")
public class Repetition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToMany
    @JoinTable(
            name = "repetition_word",
            joinColumns = @JoinColumn(name = "repetition_id"),
            inverseJoinColumns = @JoinColumn(name = "word_id")
    )
    @Builder.Default
    private List<Word> words = new ArrayList<>();

    @Column(name = "efactor", nullable = false)
    private double eFactor = 2.5;

    @Column(name = "interval", nullable = false)
    private int interval = 1;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;
}
