package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDisplayResponse {
    private Integer taskId;
    private boolean completed;
}