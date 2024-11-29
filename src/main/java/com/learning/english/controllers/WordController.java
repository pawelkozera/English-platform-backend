package com.learning.english.controllers;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.User;
import com.learning.english.service.WordService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/word")
@RequiredArgsConstructor
public class WordController {
    private final WordService wordService;
    private final AuthUtil authUtil;


    @GetMapping("/all/owned/by/user")
    public ResponseEntity<PagedModel<WordResponse>> getWordsOwnedByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request,
            PagedResourcesAssembler<WordResponse> assembler
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        Page<WordResponse> wordPage = wordService.getWordsOwnedByUser(user, page, size);

        PagedModel<EntityModel<WordResponse>> pagedModel = assembler.toModel(wordPage, wordResponse ->
                EntityModel.of(wordResponse,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WordController.class).getWordsOwnedByUser(page, size, request, assembler)).withSelfRel())
        );

        PagedModel<WordResponse> result = PagedModel.of(
                wordPage.getContent(),
                pagedModel.getMetadata()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addWord(HttpServletRequest request,
                                          @ModelAttribute("wordAddRequest") WordAddRequest wordAddRequest,
                                          @RequestPart(value = "audioFile", required = false) MultipartFile audioFile,
                                          @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {

        User user = authUtil.getAuthenticatedUser(request);

        String audioFilePath = null;
        String imageFilePath = null;

        if (audioFile != null && !audioFile.isEmpty()) {
            audioFilePath = saveFile(audioFile, "audio");
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            imageFilePath = saveFile(imageFile, "images");
        }

        wordAddRequest.setAudioFilePath(audioFilePath);
        wordAddRequest.setImageFilePath(imageFilePath);

        wordService.addWord(wordAddRequest, user);

        return ResponseEntity.ok("Word created successfully");
    }

    @PutMapping("/update/{wordId}")
    public ResponseEntity<String> updateWord(
            HttpServletRequest request,
            @PathVariable Long wordId,
            @ModelAttribute("wordUpdateRequest") WordAddRequest wordUpdateRequest,
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) throws IOException {
        User user = authUtil.getAuthenticatedUser(request);

        String audioFilePath = null;
        String imageFilePath = null;

        if (audioFile != null && !audioFile.isEmpty()) {
            audioFilePath = saveFile(audioFile, "audio");
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            imageFilePath = saveFile(imageFile, "images");
        }

        wordUpdateRequest.setAudioFilePath(audioFilePath);
        wordUpdateRequest.setImageFilePath(imageFilePath);

        wordService.updateWord(wordId, wordUpdateRequest, user);

        return ResponseEntity.ok("Word updated successfully");
    }

    private String saveFile(MultipartFile file, String folder) throws IOException {
        String uploadDir = "uploads/" + folder + "/";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String filePath = uploadDir + System.currentTimeMillis() + "_" + originalFilename;
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        return filePath;
    }

    @DeleteMapping("/delete/{wordId}")
    public ResponseEntity<String> deleteWord(
            HttpServletRequest request,
            @PathVariable Long wordId
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        try {
            wordService.deleteWord(wordId, user);
            return ResponseEntity.ok().body("Word deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
