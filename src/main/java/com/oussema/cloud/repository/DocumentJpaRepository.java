package com.oussema.cloud.repository;

import com.oussema.cloud.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentJpaRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerOrderByCreatedAtDesc(String owner);

}