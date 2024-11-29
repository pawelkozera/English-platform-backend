package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordResponse {
    private Long id;
    private String word;
    private String translation;
    private String audioFilePath;
    private String imageFilePath;
}
