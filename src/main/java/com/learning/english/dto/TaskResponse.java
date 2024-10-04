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
public class TaskResponse {
    private Integer id;
    private String taskTypeName;
    private String taskSubTypeName;
    private String content;
    private String correctAnswer;
    private boolean completed;
    private List<WordResponse> words;
    private Integer score;
}