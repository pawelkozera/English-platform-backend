package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestInstanceDisplayResponse {
    private Integer testInstanceId;
    private UUID testInstanceUUID;
    private String testName;
    private LocalDateTime activationTime;
    private LocalDateTime endTime;
}
