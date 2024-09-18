package com.learning.english.service;

import com.learning.english.dto.LessonAddRequest;
import com.learning.english.models.Group;
import com.learning.english.models.Lesson;
import com.learning.english.models.User;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.LessonRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    public void addLesson(LessonAddRequest lessonAddRequest, User user) {
        Group group = groupRepository.findById(lessonAddRequest.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean isOwner = userGroupRepository.existsByUserAndGroupAndIsOwnerTrue(user, group);
        if (!isOwner) {
            throw new IllegalArgumentException("User is not the owner of the group");
        }

        boolean lessonExists = group.getLessons().stream()
                .anyMatch(existingLesson -> existingLesson.getTitle().equalsIgnoreCase(lessonAddRequest.getTitle()));

        if (lessonExists) {
            throw new IllegalArgumentException("A lesson with this title already exists in the group");
        }

        Lesson lesson = Lesson.builder()
                .title(lessonAddRequest.getTitle())
                .build();

        group.getLessons().add(lesson);
        lesson.getGroups().add(group);

        lessonRepository.save(lesson);
    }
}
