package com.oussema.cloud.validator;


import com.oussema.cloud.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * The {@code FileValidator} class validates files based on their actual content type.
 * It uses Apache Tika to detect the MIME type of the file and ensures that the file type
 * is within a specified whitelist.
 * <p>
 * The validation ensures files are not empty and their type matches allowed types. This
 * functionality can be leveraged to enforce file upload rules in applications.
 * <p>
 * Dependencies:
 * - Apache Tika for MIME type detection.
 * <p>
 * Exceptions:
 * - {@link StorageException} is thrown if the file is empty or the type is unauthorized.
 * - {@link IOException} is thrown in case of file stream reading issues.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class FileValidator {


    private final Tika tika = new Tika();

    /**
     * Validates a file based on its actual content type using Apache Tika.
     *
     * @param file The file to validate.
     * @throws StorageException if the file is empty or its type is not in the whitelist.
     * @throws IOException      if there's an error reading the file stream.
     */
    public void validate(MultipartFile file, List<String> allowedTypes) throws IOException {
        if (file.isEmpty()) {
            throw new StorageException(StorageException.CloudStorageExceptionType.GENERIC);
        }

        String detectedType = tika.detect(file.getInputStream());

        if (!allowedTypes.contains(detectedType)) {
            log.info("Detected file type: {} is not in the allowed list: {}", detectedType, allowedTypes);
            throw new StorageException(StorageException.CloudStorageExceptionType.INVALID_FILE_TYPE);
        }
    }

}
