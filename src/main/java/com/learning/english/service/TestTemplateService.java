package com.learning.english.service;

import com.learning.english.dto.TaskResponse;
import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.dto.TestTemplateResponse;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.Task;
import com.learning.english.models.TestInstance;
import com.learning.english.models.TestTemplate;
import com.learning.english.models.User;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.TaskRepository;
import com.learning.english.repository.TestTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestTemplateService {
    private final TaskRepository taskRepository;
    private final TestTemplateRepository testTemplateRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public void addTemplate(TemplateAddRequest templateAddRequest, User user) {
        List<Task> tasks = (List<Task>) taskRepository.findAllById(templateAddRequest.getTasksIds());

        List<Task> userTasks = tasks.stream()
                .filter(task -> task.getOwner().equals(user))
                .collect(Collectors.toList());

        if (userTasks.size() != templateAddRequest.getTasksIds().size()) {
            throw new IllegalArgumentException("Some tasks do not belong to the user");
        }

        Integer totalScore = userTasks.stream()
                .mapToInt(Task::getScore)
                .sum();

        TestTemplate testTemplate = TestTemplate.builder()
                .name(templateAddRequest.getName())
                .tasks(userTasks)
                .owner(user)
                .totalScore(totalScore)
                .build();

        testTemplateRepository.save(testTemplate);
    }

    public Page<TestTemplateResponse> getTemplatesOwnedByUser(User user, int page, int size) {
        Page<TestTemplate> testTemplates = testTemplateRepository.findByOwner(user, PageRequest.of(page, size));

        return testTemplates.map(testTemplate -> {
            List<TaskResponse> taskResponses = testTemplate.getTasks().stream()
                    .map(task -> {
                        List<WordResponse> wordResponses = task.getWords().stream()
                                .map(word -> new WordResponse(word.getId(), word.getWord(), word.getTranslation(), word.getAudioFilePath(), word.getImageFilePath()))
                                .collect(Collectors.toList());

                        return TaskResponse.builder()
                                .id(task.getId())
                                .taskTypeName(task.getTaskType().getTypeName())
                                .taskSubTypeName(task.getTaskSubType().getSubTypeName())
                                .content(task.getContent())
                                .correctAnswer(task.getCorrectAnswer())
                                .words(wordResponses)
                                .build();
                    })
                    .collect(Collectors.toList());

            return new TestTemplateResponse(testTemplate.getName(), testTemplate.getId(), taskResponses);
        });
    }

    public void deleteTestTemplate(Integer testTemplateId, User user) {
        TestTemplate testTemplate = testTemplateRepository.findById(testTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Test template not found"));

        if (!testTemplate.getOwner().equals(user)) {
            throw new SecurityException("You are not authorized to delete this test template");
        }

        testTemplateRepository.delete(testTemplate);
    }
}

