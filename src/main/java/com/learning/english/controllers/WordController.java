package com.learning.english.controllers;

import com.learning.english.dto.WordAddRequest;
import com.learning.english.dto.WordResponse;
import com.learning.english.models.User;
import com.learning.english.service.WordService;
import com.learning.english.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public Page<WordResponse> getWordsOwnedByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        User user = authUtil.getAuthenticatedUser(request);
        return wordService.getWordsOwnedByUser(user, page, size);
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
}
