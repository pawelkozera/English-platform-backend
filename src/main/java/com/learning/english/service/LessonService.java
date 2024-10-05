package com.learning.english.service;

import com.learning.english.dto.LessonAddRequest;
import com.learning.english.dto.LessonResponse;
import com.learning.english.dto.LessonsDisplayResponse;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.*;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.LessonProgressRepository;
import com.learning.english.repository.LessonRepository;
import com.learning.english.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final LessonProgressRepository lessonProgressRepository;

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

    public List<LessonResponse> getLessonsFromGroup(User user, Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean isMember = userGroupRepository.existsByUserAndGroup(user, group);
        if (!isMember) {
            throw new IllegalArgumentException("User is not a member of the group");
        }

        List<Lesson> lessons = lessonRepository.findAllByGroupsContaining(group);

        return lessons.stream()
                .map(lesson -> LessonResponse.builder()
                        .lessonId(lesson.getId())
                        .title(lesson.getTitle())
                        .build())
                .collect(Collectors.toList());
    }

    public Page<LessonsDisplayResponse> getLessonsForDisplay(User user, int page, int size) {
        List<Group> userGroups = userGroupRepository.findAllByUser(user).stream()
                .map(UserGroup::getGroup)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);

        Page<Lesson> lessons = lessonRepository.findAllByGroupsIn(userGroups, pageable);

        return lessons.map(lesson -> {
            LessonProgress lessonProgress = lessonProgressRepository
                    .findByUserAndLesson(user, lesson)
                    .orElse(null);

            int completedTasks = lessonProgress != null ? lessonProgress.getCompletedTaskCount() : 0;
            int totalTasks = lesson.getTasks().size();

            return LessonsDisplayResponse.builder()
                    .lessonId(lesson.getId())
                    .title(lesson.getTitle())
                    .completedTasks(completedTasks)
                    .totalTasks(totalTasks)
                    .build();
        });
    }
}
