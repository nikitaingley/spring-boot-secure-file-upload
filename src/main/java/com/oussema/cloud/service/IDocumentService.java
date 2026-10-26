package com.oussema.cloud.service;

import com.oussema.cloud.dto.DocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IDocumentService {
    DocumentDto save(MultipartFile multipartFile);


    void deleteById(UUID id);
}
