package com.learning.english.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupResponse {
    private Integer id;
    private String groupName;
    private String groupCode;
    private boolean isOwner;
}

