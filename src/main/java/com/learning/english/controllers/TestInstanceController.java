package com.learning.english.controllers;

import com.learning.english.dto.TestInstanceAddRequest;
import com.learning.english.dto.TestInstanceDisplayResponse;
import com.learning.english.models.User;
import com.learning.english.service.TestInstanceService;
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
@RequestMapping("/api/v1/test/instance")
@RequiredArgsConstructor
public class TestInstanceController {
    private final AuthUtil authUtil;
    private final TestInstanceService testInstanceService;

    @PostMapping("/add")
    public ResponseEntity<String> launchTestInstance(HttpServletRequest request, @RequestBody TestInstanceAddRequest testInstanceAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        testInstanceService.addTestInstance(testInstanceAddRequest, user);
        return ResponseEntity.ok("Test instance created successfully");
    }

    @GetMapping("all/for/display/{groupId}")
    public ResponseEntity<PagedModel<TestInstanceDisplayResponse>> getTestInstancesForDisplay(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            @PathVariable Integer groupId,
            PagedResourcesAssembler<TestInstanceDisplayResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<TestInstanceDisplayResponse> testInstancePage = testInstanceService.getTestInstancesForDisplay(user, groupId, page, size);

        PagedModel<EntityModel<TestInstanceDisplayResponse>> pagedModel = assembler.toModel(testInstancePage, testDisplayResponse ->
                EntityModel.of(testDisplayResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TestInstanceController.class).getTestInstancesForDisplay(page, size, request, groupId, assembler)).withSelfRel())
        );

        PagedModel<TestInstanceDisplayResponse> result = PagedModel.of(
                testInstancePage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{testInstanceId}/tasks")
    public ResponseEntity<List<Integer>> getTasksForTestInstance(
            HttpServletRequest request,
            @PathVariable Integer testInstanceId
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        List<Integer> tasks = testInstanceService.getTasksForTestInstance(user, testInstanceId);
        return ResponseEntity.ok(tasks);
    }
}
