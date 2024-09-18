package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAddRequest {
    private String taskTypeName;
    private String content;
    private String correctAnswer;
    private Integer lessonId;
}
