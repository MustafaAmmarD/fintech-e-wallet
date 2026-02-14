package com.fintech.ewallet.kyc.domain;

import java.io.IOException;
import java.util.UUID;

/**
 * File storage service port.
 * <p>
 * Abstracts file storage to allow swapping between local filesystem,
 * AWS S3, Azure Blob Storage, etc.
 */
public interface FileStorageService {

    /**
     * Store a file and return its path.
     *
     * @param fileData File content as bytes
     * @param fileName Original filename
     * @param userId   User ID for organizing files
     * @return File path (relative or absolute depending on implementation)
     * @throws IOException If file storage fails
     */
    String storeFile(byte[] fileData, String fileName, UUID userId) throws IOException;

    /**
     * Retrieve file data.
     *
     * @param filePath Path returned by storeFile()
     * @return File content as bytes
     * @throws IOException If file not found or read fails
     */
    byte[] retrieveFile(String filePath) throws IOException;

    /**
     * Delete a file.
     *
     * @param filePath Path returned by storeFile()
     * @throws IOException If deletion fails
     */
    void deleteFile(String filePath) throws IOException;
}
