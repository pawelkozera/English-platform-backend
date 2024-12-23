package com.learning.english.unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learning.english.dto.TaskResponse;
import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.dto.TestTemplateResponse;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.*;
import com.learning.english.repository.TaskRepository;
import com.learning.english.repository.TestTemplateRepository;
import com.learning.english.repository.GroupRepository;
import com.learning.english.service.TestTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class TestTemplateServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TestTemplateRepository testTemplateRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private TestTemplateService testTemplateService;

    @Test
    void shouldAddTemplateWhenTasksBelongToUser() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        Task task1 = new Task();
        task1.setId(1);
        task1.setOwner(user);
        task1.setScore(10);

        Task task2 = new Task();
        task2.setId(2);
        task2.setOwner(user);
        task2.setScore(20);

        List<Integer> taskIds = Arrays.asList(1, 2);
        TemplateAddRequest templateAddRequest = new TemplateAddRequest();
        templateAddRequest.setName("Test Template 1");
        templateAddRequest.setTasksIds(taskIds);

        when(taskRepository.findAllById(taskIds)).thenReturn(Arrays.asList(task1, task2));

        testTemplateService.addTemplate(templateAddRequest, user);

        verify(testTemplateRepository).save(argThat(testTemplate ->
                "Test Template 1".equals(testTemplate.getName()) &&
                        testTemplate.getTotalScore() == 30 &&
                        testTemplate.getTasks().containsAll(Arrays.asList(task1, task2))
        ));
    }

    @Test
    void shouldThrowExceptionIfTaskDoesNotBelongToUser() {
        User user = new User();
        user.setId(1);

        User otherUser = new User();
        otherUser.setId(2);

        Task task1 = new Task();
        task1.setId(1);
        task1.setOwner(user);
        task1.setScore(10);

        Task task2 = new Task();
        task2.setId(2);
        task2.setOwner(otherUser);
        task2.setScore(20);

        List<Integer> taskIds = Arrays.asList(1, 2);
        TemplateAddRequest templateAddRequest = new TemplateAddRequest();
        templateAddRequest.setName("Test Template 1");
        templateAddRequest.setTasksIds(taskIds);

        when(taskRepository.findAllById(taskIds)).thenReturn(Arrays.asList(task1, task2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            testTemplateService.addTemplate(templateAddRequest, user);
        });

        assertEquals("Some tasks do not belong to the user", exception.getMessage());
    }

    @Test
    void shouldReturnTemplatesOwnedByUser() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        Task task1 = new Task();
        task1.setId(1);
        task1.setTaskType(new TaskType("translation"));
        task1.setTaskSubType(new TaskSubType("image"));
        task1.setContent("Translate the word");
        task1.setCorrectAnswer("Correct Answer");

        Word word1 = new Word();
        word1.setId(1L);
        word1.setWord("word1");
        word1.setTranslation("translation1");
        word1.setAudioFilePath("/audio/path1");
        word1.setImageFilePath("/image/path1");
        task1.setWords(List.of(word1));

        TestTemplate testTemplate1 = new TestTemplate();
        testTemplate1.setId(1);
        testTemplate1.setName("Template 1");
        testTemplate1.setOwner(user);
        testTemplate1.setTasks(List.of(task1));

        Page<TestTemplate> templatesPage = new PageImpl<>(List.of(testTemplate1));

        when(testTemplateRepository.findByOwner(user, PageRequest.of(0, 10))).thenReturn(templatesPage);

        Page<TestTemplateResponse> result = testTemplateService.getTemplatesOwnedByUser(user, 0, 10);

        assertEquals(1, result.getTotalElements());

        TestTemplateResponse templateResponse = result.getContent().get(0);
        assertEquals("Template 1", templateResponse.getName());
        assertEquals(1, templateResponse.getTasks().size());

        TaskResponse taskResponse = templateResponse.getTasks().get(0);
        assertEquals("translation", taskResponse.getTaskTypeName());
        assertEquals("image", taskResponse.getTaskSubTypeName());
        assertEquals("Translate the word", taskResponse.getContent());
        assertEquals("Correct Answer", taskResponse.getCorrectAnswer());

        WordResponse wordResponse = taskResponse.getWords().get(0);
        assertEquals("word1", wordResponse.getWord());
        assertEquals("translation1", wordResponse.getTranslation());
        assertEquals("/audio/path1", wordResponse.getAudioFilePath());
        assertEquals("/image/path1", wordResponse.getImageFilePath());
    }

    @Test
    void shouldReturnEmptyPageIfNoTemplatesFound() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        Page<TestTemplate> templatesPage = new PageImpl<>(List.of());

        when(testTemplateRepository.findByOwner(user, PageRequest.of(0, 10))).thenReturn(templatesPage);

        Page<TestTemplateResponse> result = testTemplateService.getTemplatesOwnedByUser(user, 0, 10);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldDeleteTestTemplateIfUserIsOwner() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setId(1);
        testTemplate.setName("Template 1");
        testTemplate.setOwner(user);

        when(testTemplateRepository.findById(1)).thenReturn(java.util.Optional.of(testTemplate));

        testTemplateService.deleteTestTemplate(1, user);

        verify(testTemplateRepository, times(1)).delete(testTemplate);
    }

    @Test
    void shouldThrowExceptionIfTestTemplateNotFound() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");

        when(testTemplateRepository.findById(1)).thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            testTemplateService.deleteTestTemplate(1, user);
        });

        assertEquals("Test template not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfUserIsNotOwner() {
        User owner = new User();
        owner.setId(1);
        owner.setFirstName("John");
        owner.setLastName("Doe");

        User nonOwner = new User();
        nonOwner.setId(2);
        nonOwner.setFirstName("Jane");
        nonOwner.setLastName("Doe");

        TestTemplate testTemplate = new TestTemplate();
        testTemplate.setId(1);
        testTemplate.setName("Template 1");
        testTemplate.setOwner(owner);

        when(testTemplateRepository.findById(1)).thenReturn(java.util.Optional.of(testTemplate));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            testTemplateService.deleteTestTemplate(1, nonOwner);
        });

        assertEquals("You are not authorized to delete this test template", exception.getMessage());
    }
}

