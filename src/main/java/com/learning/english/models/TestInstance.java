package com.learning.english.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "test_instance")
public class TestInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime activationTime;

    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "test_template_id", nullable = false)
    private TestTemplate testTemplate;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
}
