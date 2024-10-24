package com.learning.english.service;

import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.models.Task;
import com.learning.english.models.TestTemplate;
import com.learning.english.models.User;
import com.learning.english.repository.TaskRepository;
import com.learning.english.repository.TestTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestTemplateService {
    private final TaskRepository taskRepository;
    private final TestTemplateRepository testTemplateRepository;

    @Transactional
    public void addTemplate(TemplateAddRequest templateAddRequest, User user) {
        List<Task> tasks = (List<Task>) taskRepository.findAllById(templateAddRequest.getTasksIds());

        List<Task> userTasks = tasks.stream()
                .filter(task -> task.getOwner().equals(user))
                .collect(Collectors.toList());

        if (userTasks.size() != templateAddRequest.getTasksIds().size()) {
            throw new IllegalArgumentException("Some tasks do not belong to the user");
        }

        TestTemplate testTemplate = TestTemplate.builder()
                .name(templateAddRequest.getName())
                .tasks(userTasks)
                .owner(user)
                .build();

        testTemplateRepository.save(testTemplate);
    }
}

