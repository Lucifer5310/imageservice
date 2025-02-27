package com.example.imageservice.service;

import com.example.imageservice.dto.ImageData;
import com.example.imageservice.dto.ImageMetadata;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ImageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFSBucket gridFSBucket;
    private final KafkaProducerService kafkaProducerService;

    public ImageService(GridFsTemplate gridFsTemplate, GridFSBucket gridFSBucket, KafkaProducerService kafkaProducerService) {
        this.gridFsTemplate = gridFsTemplate;
        this.gridFSBucket = gridFSBucket;
        this.kafkaProducerService = kafkaProducerService;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");

        if (file.isEmpty() || !allowedContentTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("Файл должен быть изображением (JPEG, PNG, GIF, WEBP)");
        }

        ObjectId fileId = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        kafkaProducerService.sendMessage("Image is uploaded");
        return fileId.toHexString();
    }

    public List<ImageMetadata> getAllImagesMetadata() {
        kafkaProducerService.sendMessage("Image's metadata is shown");
        return StreamSupport.stream(gridFsTemplate.find(new Query()).spliterator(), false)
                .map(file -> new ImageMetadata(
                        file.getObjectId().toHexString(),
                        file.getFilename(),
                        file.getUploadDate(),
                        file.getLength()))
                .collect(Collectors.toList());
    }

    // Новый метод: получение всех картинок с метаданными и содержимым в виде byte[]
    public List<ImageData> getAllImages() throws IOException {
        kafkaProducerService.sendMessage("All images retrieved");
        return StreamSupport.stream(gridFsTemplate.find(new Query()).spliterator(), false)
                .map(file -> {
                    try {
                        GridFsResource resource = new GridFsResource(file, gridFSBucket.openDownloadStream(file.getFilename()));
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try (InputStream inputStream = resource.getInputStream()) {
                            inputStream.transferTo(outputStream);
                        }
                        return new ImageData(
                                file.getObjectId().toHexString(),
                                file.getFilename(),
                                file.getUploadDate(),
                                file.getLength(),
                                resource.getContentType(),
                                outputStream.toByteArray()
                        );
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка чтения изображения: " + file.getFilename(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    public GridFsResource getImageByFilename(String filename) {
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("filename").is(filename)));

        if (gridFSFile == null) {
            return null;
        }

        kafkaProducerService.sendMessage("Image is downloaded: " + filename);
        return new GridFsResource(gridFSFile, gridFSBucket.openDownloadStream(gridFSFile.getFilename()));
    }

    public void deleteImageByFilename(String filename) {
        gridFsTemplate.delete(Query.query(Criteria.where("filename").is(filename)));
        kafkaProducerService.sendMessage("Image is deleted: " + filename);
    }
}