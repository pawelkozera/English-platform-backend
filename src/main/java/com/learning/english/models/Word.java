package com.learning.english.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "word")
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String word;
    private String translation;
    private String audioFilePath;
    private String imageFilePath;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "word")
    private List<RepetitionWord> repetitionWords;

    @ManyToMany(mappedBy = "words")
    private List<Task> tasks;
}
