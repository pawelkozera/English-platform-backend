package com.learning.english.models;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "test_template")
public class TestTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToMany
    @JoinTable(
            name = "test_template_task",
            joinColumns = @JoinColumn(name = "test_template_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private List<Task> tasks;
}
