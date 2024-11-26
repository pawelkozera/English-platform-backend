package com.learning.english.controllers;

import com.learning.english.dto.AnnouncementAddRequest;
import com.learning.english.dto.AnnouncementDisplayResponse;
import com.learning.english.models.User;
import com.learning.english.service.AnnouncementService;
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
@RequestMapping("/api/v1/announcement")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final AuthUtil authUtil;

    @PostMapping("/add")
    public ResponseEntity<String> addAnnouncement(HttpServletRequest request, @RequestBody AnnouncementAddRequest announcementAddRequest) {
        User user = authUtil.getAuthenticatedUser(request);
        announcementService.addAnnouncement(announcementAddRequest, user);
        return ResponseEntity.ok("Announcement created successfully");
    }

    @GetMapping("all/for/display/{groupId}")
    public ResponseEntity<PagedModel<AnnouncementDisplayResponse>> getAnnouncementsForDisplay(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1") int size,
            HttpServletRequest request,
            @PathVariable Integer groupId,
            PagedResourcesAssembler<AnnouncementDisplayResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<AnnouncementDisplayResponse> announcementPage = announcementService.getAnnouncementsForDisplay(user, groupId, page, size);

        PagedModel<EntityModel<AnnouncementDisplayResponse>> pagedModel = assembler.toModel(announcementPage, announcementDisplayResponse ->
                EntityModel.of(announcementDisplayResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AnnouncementController.class).getAnnouncementsForDisplay(page, size, request, groupId, assembler)).withSelfRel())
        );

        PagedModel<AnnouncementDisplayResponse> result = PagedModel.of(
                announcementPage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }
}
