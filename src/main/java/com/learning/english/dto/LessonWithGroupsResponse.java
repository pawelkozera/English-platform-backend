package com.learning.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LessonWithGroupsResponse {
    private Integer lessonId;
    private String title;
    private List<Integer> groupIds;
}

