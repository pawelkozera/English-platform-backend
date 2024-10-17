package com.learning.english.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_history")
public class TestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "test_instance_id", nullable = false)
    private TestInstance testInstance;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private LocalDateTime completedAt;

    private Integer score;

    private boolean suspiciousActivityDetected;
}
