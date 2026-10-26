package com.oussema.cloud.adapter;

import com.oussema.cloud.model.Blob;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface for cloud storage services
 */
public interface Storage {

    /**
     * Uploads a file to the cloud storage
     *
     * @param file the file to upload
     * @return the URL of the uploaded file
     */
    Blob uploadFile(MultipartFile file);

    /**
     * Uploads multiple files to the cloud storage
     *
     * @param files the files to upload
     * @return the URL of the uploaded files
     */
    List<Blob> uploadFiles(MultipartFile[] files);

    /**
     * Deletes a file from the cloud storage
     *
     * @param key the URL of the file to delete @Param contentType
     */
    void deleteFile(String key, String contentType);

    String generatePresignedUrl(String blobName, String contentType);
}
