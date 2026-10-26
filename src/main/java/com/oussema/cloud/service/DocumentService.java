package com.oussema.cloud.service;


import com.oussema.cloud.dto.DocumentDto;
import com.oussema.cloud.exception.StorageException;
import com.oussema.cloud.mapper.DocumentMapper;
import com.oussema.cloud.model.Blob;
import com.oussema.cloud.model.Document;
import com.oussema.cloud.multicloud.Cloud;
import com.oussema.cloud.multicloud.CloudRouter;
import com.oussema.cloud.multicloud.ReplicationService;
import com.oussema.cloud.repository.DocumentJpaRepository;
import com.oussema.cloud.validator.FileValidator;
import com.oussema.cloud.validator.VirusScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.oussema.cloud.validator.MimeTypeConstants.ALLOWED_DOCUMENT_TYPES;

@RequiredArgsConstructor
@Service
@Log4j2
public class DocumentService implements IDocumentService {

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentMapper documentMapper;
    private final CloudRouter router;
    private final ReplicationService replicationService;
    private final FileValidator fileValidator;
    private final VirusScanService virusScanService;

    @Value("${cloud.storage.provider:azure}")
    private String defaultProvider;


    @Override
    public DocumentDto save(MultipartFile file) {
        return save(file, null);
    }

    public DocumentDto save(MultipartFile file, String provider) {
        try {
            // Validates actual file content using Apache Tika to prevent MIME type spoofing attacks
            fileValidator.validate(file, ALLOWED_DOCUMENT_TYPES);

            // Scan for viruses and malware with clamav
            virusScanService.scan(file.getInputStream());

            // Keep a copy of the bytes: the background replica runs after this request has ended
            byte[] bytes = file.getBytes();

            // Main cloud = the one the user chose, or the other one if it is unreachable
            Cloud used = router.chooseForUpload(Cloud.from(provider, defaultProvider));
            Blob blob = router.storage(used).uploadFile(file);

            Document document = Document.builder()
                    .key(blob.key())
                    .name(blob.name())
                    .size(blob.size())
                    .type(blob.type())
                    .owner(currentUser())
                    .createdAt(Instant.now())
                    .provider(used.name())
                    .replicaStatus("PENDING")
                    .build();

            Document savedDocument = documentJpaRepository.save(document);

            // Copy to the other cloud in the background
            replicationService.replicate(savedDocument.getId(), used.other(), bytes, blob.name(), blob.type());

            return documentMapper.mapBlobToBlobResponse(savedDocument);
        } catch (StorageException | IOException e) {
            throw new StorageException(StorageException.CloudStorageExceptionType.FILE_UPLOAD_FAILED, e.getMessage());
        } catch (RuntimeException e) {
            // e.g. a cloud SDK error
            throw new StorageException(StorageException.CloudStorageExceptionType.FILE_UPLOAD_FAILED, e.getMessage());
        }
    }


    @Override
    public void deleteById(UUID id) {
        Document document = documentJpaRepository.findById(id).orElseThrow(
                () -> new StorageException(StorageException.CloudStorageExceptionType.FILE_NOT_FOUND)
        );
        // Only the owner may delete. Other users get "not found".
        if (!currentUser().equals(document.getOwner())) {
            throw new StorageException(StorageException.CloudStorageExceptionType.FILE_NOT_FOUND);
        }
        documentJpaRepository.deleteById(id);

        // Remove the file from both clouds
        Cloud primary = Cloud.from(document.getProvider(), "azure");
        deleteQuietly(primary, document.getKey(), document.getType());
        if (document.getReplicaKey() != null) {
            deleteQuietly(primary.other(), document.getReplicaKey(), document.getType());
        }
    }

    private void deleteQuietly(Cloud cloud, String key, String type) {
        try {
            router.storage(cloud).deleteFile(key, type);
        } catch (Exception e) {
            log.error("Could not delete {} from {}: {}", key, cloud, e.getMessage());
        }
    }


    public List<DocumentDto> listMine() {
        return documentJpaRepository.findByOwnerOrderByCreatedAtDesc(currentUser())
                .stream()
                .map(documentMapper::mapBlobToBlobResponse)
                .toList();
    }

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}