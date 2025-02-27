package com.example.imageservice.controller;

import com.example.imageservice.dto.ImageData;
import com.example.imageservice.dto.ImageMetadata;
import com.example.imageservice.service.ImageService;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<List<ImageMetadata>> getAllImagesMetadata() {
        return ResponseEntity.ok(imageService.getAllImagesMetadata());
    }

    // Обновлённый эндпоинт: возвращает список картинок с метаданными и содержимым
    @GetMapping("/all")
    public ResponseEntity<List<ImageData>> getAllImages() {
        try {
            List<ImageData> images = imageService.getAllImages();
            if (images.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(images);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> getImageByFilename(@PathVariable String filename) {
        GridFsResource resource = imageService.getImageByFilename(filename);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String id = imageService.uploadImage(file);
            return ResponseEntity.ok(id);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка загрузки файла");
        }
    }

    @DeleteMapping("/delete/{filename:.+}")
    public ResponseEntity<Void> deleteImageByFilename(@PathVariable String filename) {
        imageService.deleteImageByFilename(filename);
        return ResponseEntity.noContent().build();
    }
}