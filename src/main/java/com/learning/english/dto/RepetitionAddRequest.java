package com.learning.english.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RepetitionAddRequest {
    private Long wordId;
    private Integer groupId;
}
