package com.learning.english.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupJoinRequest {
    private String groupCode;
    private String password;
}
