package com.learning.english.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TestResultDto {
    private Integer userId;
    private String userName;
    private Integer score;
    private LocalDateTime completedAt;
    private boolean suspiciousActivityDetected;
    private List<SuspiciousActivityDto> suspiciousActivities;
    private Integer totalScore;
}

