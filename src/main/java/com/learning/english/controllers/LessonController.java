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
        return ResponseEntity.ok("Lesson created successfully");
    }

    @GetMapping("all/from/group/{groupId}")
    public ResponseEntity<PagedModel<LessonResponse>> getLessonsFromGroup(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            @PathVariable Integer groupId,
            PagedResourcesAssembler<LessonResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<LessonResponse> lessonPage = lessonService.getLessonsFromGroup(user, groupId, page, size);

        PagedModel<EntityModel<LessonResponse>> pagedModel = assembler.toModel(lessonPage, lessonResponse ->
                EntityModel.of(lessonResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LessonController.class).getLessonsFromGroup(page, size, request, groupId, assembler)).withSelfRel())
        );

        return ResponseEntity.ok(PagedModel.of(
                lessonPage.getContent(),
                pagedModel.getMetadata()
        ));
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

    @GetMapping("all/not/assigned/to/group/{groupId}")
    public ResponseEntity<PagedModel<LessonResponse>> getLessonsNotAssignedToGroup(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            @PathVariable Integer groupId,
            PagedResourcesAssembler<LessonResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<LessonResponse> lessonPage = lessonService.getLessonsNotAssignedToGroup(user, groupId, page, size);

        PagedModel<EntityModel<LessonResponse>> pagedModel = assembler.toModel(lessonPage, lessonResponse ->
                EntityModel.of(lessonResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LessonController.class).getLessonsNotAssignedToGroup(page, size, request, groupId, assembler)).withSelfRel())
        );

        return ResponseEntity.ok(PagedModel.of(
                lessonPage.getContent(),
                pagedModel.getMetadata()
        ));
    }

    @GetMapping("all/owned/by/user")
    public ResponseEntity<PagedModel<LessonWithGroupsResponse>> getLessonsOwnedByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            PagedResourcesAssembler<LessonWithGroupsResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<LessonWithGroupsResponse> lessonPage = lessonService.getLessonsOwnedByUser(user, page, size);

        PagedModel<EntityModel<LessonWithGroupsResponse>> pagedModel = assembler.toModel(lessonPage, lessonResponse ->
                EntityModel.of(lessonResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LessonController.class).getLessonsOwnedByUser(page, size, request, assembler)).withSelfRel())
        );

        return ResponseEntity.ok(PagedModel.of(
                lessonPage.getContent(),
                pagedModel.getMetadata()
        ));
    }

    @PutMapping("/{lessonId}/update")
    public ResponseEntity<String> updateLesson(
            HttpServletRequest request,
            @PathVariable Integer lessonId,
            @RequestBody LessonUpdateRequest lessonUpdateRequest
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        boolean success = lessonService.updateLesson(lessonId, lessonUpdateRequest, user);

        if (success) {
            return ResponseEntity.ok("Lesson updated successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to update lesson");
        }
    }

    @DeleteMapping("/{lessonId}/delete")
    public ResponseEntity<String> deleteLesson(
            HttpServletRequest request,
            @PathVariable Integer lessonId
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        boolean success = lessonService.deleteLesson(lessonId, user);

        if (success) {
            return ResponseEntity.ok("Lesson deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to delete lesson");
        }
    }
}