package com.example.imageservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

@Setter
@Getter
public class ImageMetadata {

    @JsonProperty("id")
    private String id;
    @JsonProperty("filename")
    private String filename;
    @JsonProperty("uploadDate")
    private Date uploadDate;
    @JsonProperty("length")
    private long length;

    public ImageMetadata(String id, String filename, Date uploadDate, long length) {
        this.id = id;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.length = length;
    }
}
