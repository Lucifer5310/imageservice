package com.example.imageservice.service;

import com.example.imageservice.dto.ImageMetadata;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFSBucket gridFSBucket;
    private final KafkaProducerService kafkaProducerService;

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

    public GridFsResource getImageById(String id) {
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(id))));

        if (gridFSFile == null) {
            return null;
        }

        kafkaProducerService.sendMessage("Image is downloaded");
        return new GridFsResource(gridFSFile, gridFSBucket.openDownloadStream(gridFSFile.getObjectId()));
    }

    public void deleteImage(String id) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id)));
        kafkaProducerService.sendMessage("Image is deleted");
    }
}
