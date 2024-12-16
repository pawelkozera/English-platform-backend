package com.learning.english.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@Getter
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