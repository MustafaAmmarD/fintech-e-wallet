package com.fintech.ewallet.kyc.infrastructure.storage;

import com.fintech.ewallet.kyc.domain.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService.
 * <p>
 * Stores files in ./uploads/kyc/{userId}/ directory.
 * For production, replace with S3/Azure/GCS implementation.
 */
@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final String UPLOAD_DIR = "./uploads/kyc";

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            log.info("KYC upload directory initialized: {}", UPLOAD_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create KYC upload directory", e);
        }
    }

    @Override
    public String storeFile(byte[] fileData, String fileName, UUID userId) throws IOException {
        // Create user-specific directory
        Path userDir = Paths.get(UPLOAD_DIR, userId.toString());
        Files.createDirectories(userDir);

        // Generate unique filename to avoid collisions
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        Path filePath = userDir.resolve(uniqueFileName);

        // Write file
        Files.write(filePath, fileData);

        log.info("Stored KYC document: {} (size: {} bytes)", filePath, fileData.length);
        return filePath.toString();
    }

    @Override
    public byte[] retrieveFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("KYC document not found: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        boolean deleted = Files.deleteIfExists(Paths.get(filePath));
        if (deleted) {
            log.info("Deleted KYC document: {}", filePath);
        }
    }
}
