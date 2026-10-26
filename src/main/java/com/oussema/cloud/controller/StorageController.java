package com.oussema.cloud.controller;


import com.oussema.cloud.dto.DocumentDto;
import com.oussema.cloud.multicloud.Cloud;
import com.oussema.cloud.multicloud.CloudRouter;
import com.oussema.cloud.service.DocumentService;
import com.oussema.cloud.validator.MimeTypeConstants;
import com.oussema.cloud.validator.ValidFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Storage Management", description = "APIs for managing cloud storage")
@RequiredArgsConstructor
public class StorageController {

    private final DocumentService documentService;
    private final CloudRouter router;


    @Operation(summary = "List my files")
    @GetMapping("/files")
    public ResponseEntity<List<DocumentDto>> listMyFiles() {
        return ResponseEntity.ok(documentService.listMine());
    }

    @Operation(summary = "Which clouds are reachable right now")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Boolean>> cloudHealth() {
        return ResponseEntity.ok(Map.of(
                "aws", router.isHealthy(Cloud.AWS),
                "azure", router.isHealthy(Cloud.AZURE)));
    }

    @Operation(summary = "Upload a file to cloud storage (provider: aws or azure)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> uploadFile(
            @Parameter(description = "File to upload") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Main cloud: aws or azure") @RequestParam(name = "provider", required = false) String provider) {
        return ResponseEntity.ok(documentService.save(file, provider));
    }

    //  Annotation level validation
    @Operation(summary = "Upload a file to cloud storage")
    @PostMapping(value = "/upload/valid", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> uploadFileAnnotationValidation(
            @Parameter(description = "File to upload") @RequestPart("file") @ValidFile(allowedTypes = {
                    MimeTypeConstants.APPLICATION_PDF,
                    MimeTypeConstants.IMAGE_JPEG,
                    MimeTypeConstants.IMAGE_PNG
            }) MultipartFile file) {
        return ResponseEntity.ok(documentService.save(file));
    }

    @Operation(summary = "Delete a file from cloud storage")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "ID of the file to delete") @PathVariable("id") UUID id) {
        documentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}