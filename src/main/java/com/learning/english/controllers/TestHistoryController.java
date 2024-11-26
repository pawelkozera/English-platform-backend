package com.learning.english.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.english.dto.TestHistoryAddRequest;
import com.learning.english.dto.TestHistoryDisplayResponse;
import com.learning.english.dto.TestInstanceDisplayResponse;
import com.learning.english.models.User;
import com.learning.english.service.TestHistoryService;
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

@RestController
@RequestMapping("/api/v1/test/history")
@RequiredArgsConstructor
public class TestHistoryController {
    private final AuthUtil authUtil;
    private final TestHistoryService testHistoryService;

    @PostMapping("/add")
    public ResponseEntity<String> createTestHistory(HttpServletRequest request, @RequestBody TestHistoryAddRequest testHistoryAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        return testHistoryService.addTestHistory(testHistoryAddRequest, user);
    }

    @PostMapping("/addTestHistoryBeacon")
    public ResponseEntity<String> createTestHistoryViaBeacon(HttpServletRequest request, @RequestBody String payload) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TestHistoryAddRequest testHistoryAddRequest = objectMapper.readValue(payload, TestHistoryAddRequest.class);

            User user = authUtil.getAuthenticatedUser(request);
            return testHistoryService.addTestHistory(testHistoryAddRequest, user);

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid JSON format");
        }
    }

    @GetMapping("/check/completion/{testInstanceId}")
    public ResponseEntity<String> checkTestCompletion(HttpServletRequest request, @PathVariable Integer testInstanceId) {
        User user = authUtil.getAuthenticatedUser(request);
        return testHistoryService.checkTestCompletion(testInstanceId, user);
    }

    @GetMapping("all/for/display/{groupId}")
    public ResponseEntity<PagedModel<TestHistoryDisplayResponse>> getTestHistoryForDisplay(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            @PathVariable Integer groupId,
            PagedResourcesAssembler<TestHistoryDisplayResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<TestHistoryDisplayResponse> testInstancePage = testHistoryService.getTestHistoryForDisplay(user, groupId, page, size);

        PagedModel<EntityModel<TestHistoryDisplayResponse>> pagedModel = assembler.toModel(testInstancePage, testDisplayResponse ->
                EntityModel.of(testDisplayResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TestHistoryController.class).getTestHistoryForDisplay(page, size, request, groupId, assembler)).withSelfRel())
        );

        PagedModel<TestHistoryDisplayResponse> result = PagedModel.of(
                testInstancePage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }
}