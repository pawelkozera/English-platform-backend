package com.learning.english.service;

import com.learning.english.dao.GroupResponse;
import com.learning.english.dao.UserProfileResponse;
import com.learning.english.models.User;
import com.learning.english.repository.UserGroupRepository;
import com.learning.english.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserProfileResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<GroupResponse> getAllUserGroups(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return userGroupRepository.findByUser(user).stream()
                .map(userGroup -> GroupResponse.builder()
                        .id(userGroup.getGroup().getId())
                        .groupName(userGroup.getGroup().getGroupName())
                        .groupCode(userGroup.getGroup().getGroupCode())
                        .isOwner(userGroup.isOwner())
                        .build())
                .collect(Collectors.toList());
    }
}
