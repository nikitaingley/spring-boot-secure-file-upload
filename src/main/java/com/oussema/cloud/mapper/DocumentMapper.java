package com.oussema.cloud.mapper;


import com.oussema.cloud.dto.DocumentDto;
import com.oussema.cloud.model.Document;
import com.oussema.cloud.multicloud.Cloud;
import com.oussema.cloud.multicloud.CloudRouter;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {
    private final CloudRouter router;

    public DocumentMapper(final CloudRouter router) {
        this.router = router;
    }


    public DocumentDto mapBlobToBlobResponse(Document document) {
        if (document == null || document.getKey() == null) {
            return null;
        }

        Cloud primary = Cloud.from(document.getProvider(), "azure");
        Cloud other = primary.other();
        String url = null;
        Cloud servedFrom = null;

        // 1. normal case: link from the main cloud
        if (router.isHealthy(primary)) {
            url = router.presignedUrl(primary, document.getKey(), document.getType());
            if (url != null) servedFrom = primary;
        }

        // 2. failover: main cloud is down, use the replica if one exists
        if (url == null && "DONE".equals(document.getReplicaStatus())
                && document.getReplicaKey() != null && router.isHealthy(other)) {
            url = router.presignedUrl(other, document.getReplicaKey(), document.getType());
            if (url != null) servedFrom = other;
        }

        return new DocumentDto(
                document.getId(),
                document.getName(),
                document.getType(),
                url,
                document.getSize(),
                primary.name(),
                servedFrom == null ? null : servedFrom.name(),
                document.getReplicaStatus()
        );
    }
}