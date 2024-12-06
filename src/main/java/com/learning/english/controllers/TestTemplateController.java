package com.learning.english.controllers;

import com.learning.english.dto.TaskResponse;
import com.learning.english.dto.TemplateAddRequest;
import com.learning.english.dto.TestTemplateResponse;
import com.learning.english.models.User;
import com.learning.english.service.TestTemplateService;
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
@RequestMapping("/api/v1/test/template")
@RequiredArgsConstructor
public class TestTemplateController {
    private final AuthUtil authUtil;
    private final TestTemplateService testTemplateService;

    @PostMapping("/add")
    public ResponseEntity<String> createTemplate(HttpServletRequest request, @RequestBody TemplateAddRequest templateAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        testTemplateService.addTemplate(templateAddRequest, user);
        return ResponseEntity.ok("Template created successfully");
    }

    @GetMapping("/all/owned/by/user")
    public ResponseEntity<PagedModel<TestTemplateResponse>> getTemplatesOwnedByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            PagedResourcesAssembler<TestTemplateResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<TestTemplateResponse> taskPage = testTemplateService.getTemplatesOwnedByUser(user, page, size);

        PagedModel<EntityModel<TestTemplateResponse>> pagedModel = assembler.toModel(taskPage, testTemplateResponse ->
                EntityModel.of(testTemplateResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(TestTemplateController.class).getTemplatesOwnedByUser(page, size, request, assembler)).withSelfRel())
        );

        PagedModel<TestTemplateResponse> result = PagedModel.of(
                taskPage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("{testTemplateId}/delete")
    public ResponseEntity<String> deleteTestTemplate(
            HttpServletRequest request,
            @PathVariable Integer testTemplateId
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        testTemplateService.deleteTestTemplate(testTemplateId, user);
        return ResponseEntity.ok("Test instance deleted successfully");
    }
}