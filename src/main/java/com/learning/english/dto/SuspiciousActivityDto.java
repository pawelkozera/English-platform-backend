package com.learning.english.dto;

import java.time.LocalDateTime;

public record SuspiciousActivityDto(
        LocalDateTime timestamp,
        String description,
        Integer occurrenceCount
) {}
