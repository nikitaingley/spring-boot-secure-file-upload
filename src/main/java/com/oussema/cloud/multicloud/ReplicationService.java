package com.oussema.cloud.multicloud;

import com.oussema.cloud.model.Blob;
import com.oussema.cloud.model.Document;
import com.oussema.cloud.repository.DocumentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReplicationService {

    private final CloudRouter router;
    private final DocumentJpaRepository repository;

    /** Copies a freshly uploaded file to the other cloud, in the background. */
    @Async
    public void replicate(UUID documentId, Cloud target, byte[] bytes, String name, String type) {
        Document doc = repository.findById(documentId).orElse(null);
        if (doc == null) return; // deleted before the copy ran
        try {
            Blob blob = router.storage(target)
                    .uploadFile(new InMemoryMultipartFile(name, type, bytes));
            doc.setReplicaKey(blob.key());
            doc.setReplicaStatus("DONE");
            log.info("Replicated '{}' to {}", name, target);
        } catch (Exception e) {
            doc.setReplicaStatus("FAILED");
            log.error("Replication of '{}' to {} failed: {}", name, target, e.getMessage());
        }
        repository.save(doc);
    }
}