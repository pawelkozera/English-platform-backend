package com.learning.english.controllers;

import com.learning.english.dto.*;
import com.learning.english.models.Task;
import com.learning.english.models.User;
import com.learning.english.service.TaskService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final AuthUtil authUtil;

    @PostMapping("/add")
    public ResponseEntity<String> createTask(HttpServletRequest request, @RequestBody TaskAddRequest taskAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        taskService.addTask(taskAddRequest, user);
        return ResponseEntity.ok("Task created successfully");
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(HttpServletRequest request, @PathVariable Integer taskId) {
        User user = authUtil.getAuthenticatedUser(request);
        TaskResponse taskResponse = taskService.getTaskById(user, taskId);
        return ResponseEntity.ok(taskResponse);
    }

    @GetMapping("/batch")
    public ResponseEntity<List<TaskResponse>> getTasksByIds(HttpServletRequest request, @RequestParam List<Integer> taskIds) {
        User user = authUtil.getAuthenticatedUser(request);
        List<TaskResponse> tasks = taskService.getTasksByIds(user, taskIds);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> completeTask(HttpServletRequest request, @RequestBody TaskCompleteRequest taskCompleteRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        taskService.completeTask(user, taskCompleteRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all/owned/by/user")
    public ResponseEntity<PagedModel<TaskResponse>> getTasksOwnedByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            PagedResourcesAssembler<TaskResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<TaskResponse> taskPage = taskService.getTasksOwnedByUser(user, page, size);

        PagedModel<EntityModel<TaskResponse>> pagedModel = assembler.toModel(taskPage, taskResponse ->
                EntityModel.of(taskResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TaskController.class).getTasksOwnedByUser(page, size, request, assembler)).withSelfRel())
        );

        PagedModel<TaskResponse> result = PagedModel.of(
                taskPage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{taskId}/delete")
    public ResponseEntity<String> deleteTask(HttpServletRequest request, @PathVariable Integer taskId) {
        User user = authUtil.getAuthenticatedUser(request);
        taskService.deleteTask(user, taskId);
        return ResponseEntity.ok("Task deleted successfully");
    }
}

