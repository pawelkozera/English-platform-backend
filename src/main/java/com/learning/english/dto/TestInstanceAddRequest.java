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
public class TestInstanceAddRequest {
    private Integer testTemplateId;
    private Integer groupId;
    private LocalDateTime activationTime;
    private LocalDateTime endTime;
    private Integer timeDuration;
}
