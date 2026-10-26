package com.oussema.cloud.validator;

import com.oussema.cloud.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirusScanService {

    private final ClamavClient clamavClient;

    /**
     * Scans the provided input stream for potential viruses using the ClamAV scanner.
     * If a virus is detected, a {@link StorageException} is thrown.
     *
     * @param inputStream the input stream of the file to be scanned for viruses
     * @throws StorageException if the file contains a virus
     */
    public void scan(InputStream inputStream) throws StorageException {
        ScanResult result = clamavClient.scan(inputStream);
        if (result instanceof ScanResult.VirusFound) {
            log.info("Virus detected in the file.");
            throw new StorageException(StorageException.CloudStorageExceptionType.FILE_CONTAINS_VIRUS, "Virus detected in the file.");
        }
    }
}
