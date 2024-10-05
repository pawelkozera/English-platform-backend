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
@Table(name = "lesson_progress")
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    private boolean completed;

    @OneToMany(mappedBy = "lessonProgress", cascade = CascadeType.ALL)
    private List<TaskProgress> taskProgresses;

    public int getCompletedTaskCount() {
        return (int) taskProgresses.stream().filter(TaskProgress::isCompleted).count();
    }
}