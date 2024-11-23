package com.learning.english.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RepetitionDisplayResponse {
    private Integer repetitionWordId;
    private String word;
    private String translation;
    private String audioFilePath;
    private String imageFilePath;
}
