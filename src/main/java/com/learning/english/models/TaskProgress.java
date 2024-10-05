package com.learning.english.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_progress")
public class TaskProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "lesson_progress_id", nullable = false)
    private LessonProgress lessonProgress;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    private boolean completed;
}