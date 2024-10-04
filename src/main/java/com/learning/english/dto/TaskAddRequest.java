package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAddRequest {
    private String taskTypeName;
    private String taskSubTypeName;
    private String content;
    private String correctAnswer;
    private Integer lessonId;
    private List<Integer> wordIds;
    private Integer score;
}
