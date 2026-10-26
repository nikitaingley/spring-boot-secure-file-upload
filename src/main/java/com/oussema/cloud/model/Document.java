package com.oussema.cloud.model;


import com.oussema.cloud.encryption.StringCryptoConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    @Convert(converter = StringCryptoConverter.class)
    private String name;

    @Column(name = "file_type", nullable = false)
    private String type;

    @Column(name = "file_key", nullable = false)
    @Convert(converter = StringCryptoConverter.class)
    private String key;

    @Column(name = "file_size", nullable = false)
    private Long size;

    @Column(name = "owner")
    private String owner;

    @Column(name = "created_at")
    private Instant createdAt;

    // Cloud that holds the main copy: AWS or AZURE
    @Column(name = "provider")
    private String provider;

    // Key of the copy in the other cloud (plain text: the encryption converter is not used on nullable values)
    @Column(name = "replica_key")
    private String replicaKey;

    // PENDING, DONE or FAILED
    @Column(name = "replica_status")
    private String replicaStatus;

}