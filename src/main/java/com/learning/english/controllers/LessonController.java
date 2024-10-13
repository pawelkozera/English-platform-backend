package com.learning.english.controllers;

import com.learning.english.dto.*;
import com.learning.english.models.User;
import com.learning.english.service.LessonService;
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
@RequestMapping("/api/v1/lesson")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final AuthUtil authUtil;

    @PostMapping("/add")
    public ResponseEntity<String> addLesson(HttpServletRequest request, @RequestBody LessonAddRequest lessonAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        lessonService.addLesson(lessonAddRequest, user);
        return ResponseEntity.ok("Task created successfully");
    }

    @GetMapping("all/from/group/{groupId}")
    public List<LessonResponse> getLessonsFromGroup(HttpServletRequest request, @PathVariable Integer groupId) {
        User user = authUtil.getAuthenticatedUser(request);
        return lessonService.getLessonsFromGroup(user, groupId);
    }

    @GetMapping("all/for/display/{groupId}")
    public ResponseEntity<PagedModel<LessonsDisplayResponse>> getLessonsForDisplay(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            @PathVariable Integer groupId,
            PagedResourcesAssembler<LessonsDisplayResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<LessonsDisplayResponse> lessonPage = lessonService.getLessonsForDisplay(user, groupId, page, size);

        PagedModel<EntityModel<LessonsDisplayResponse>> pagedModel = assembler.toModel(lessonPage, lessonsDisplayResponse ->
                EntityModel.of(lessonsDisplayResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LessonController.class).getLessonsForDisplay(page, size, request, groupId, assembler)).withSelfRel())
        );

        PagedModel<LessonsDisplayResponse> result = PagedModel.of(
                lessonPage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{lessonId}/tasks")
    public ResponseEntity<List<TaskDisplayResponse>> getTasksForLesson(
            HttpServletRequest request,
            @PathVariable Integer lessonId
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        List<TaskDisplayResponse> tasks = lessonService.getTasksForLesson(user, lessonId);
        return ResponseEntity.ok(tasks);
    }
}
