package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestInstanceDisplayResponse {
    private Integer testInstanceId;
    private String testName;
    private LocalDateTime activationTime;
    private LocalDateTime endTime;
}
