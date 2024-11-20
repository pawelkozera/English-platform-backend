package com.learning.english.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "repetition_word")
public class RepetitionWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "repetition_id")
    private Repetition repetition;

    @ManyToOne
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(name = "efactor", nullable = false)
    private double eFactor = 2.5;

    @Column(name = "interval", nullable = false)
    private int interval = 1;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;
}