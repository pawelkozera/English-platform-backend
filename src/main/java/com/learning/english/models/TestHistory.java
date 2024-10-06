package com.learning.english.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_history")
@Setter
@Getter
public class TestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "test_instance_id", nullable = false)
    private TestInstance testInstance;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime completedAt;

    private Integer score;

    private boolean suspiciousActivityDetected;

    @OneToMany(mappedBy = "testHistory", cascade = CascadeType.ALL)
    private List<SuspiciousActivity> suspiciousActivities;
}
