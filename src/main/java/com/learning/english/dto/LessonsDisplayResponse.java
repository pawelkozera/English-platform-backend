package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonsDisplayResponse {
    private String title;
    private Integer lessonId;
    private Integer completedTasks;
    private Integer totalTasks;
}
