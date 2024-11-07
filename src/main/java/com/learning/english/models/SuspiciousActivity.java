package com.learning.english.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspicious_activity")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "test_history_id", nullable = false)
    private TestHistory testHistory;

    private LocalDateTime timestamp;

    private String description;
}
