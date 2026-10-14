package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.shared.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverDocumentFileStoreTest {
    @TempDir Path folder;

    @Test
    void storesAndLoadsPdfByOpaqueIdThenDeletesOnReplacement() throws Exception {
        DriverDocumentFileStore store = new DriverDocumentFileStore(folder.toString());
        var saved = store.store(new MockMultipartFile("file", "licence.pdf",
                "application/pdf", "%PDF-1.7\nexample".getBytes()));

        assertEquals("/api/v1/document/files/" + saved.id(), saved.url());
        assertEquals("application/pdf", store.load(saved.id()).mimeType());
        assertTrue(Files.exists(store.load(saved.id()).resource().getFile().toPath()));
        store.deleteIfStoredUrl(saved.url());
        assertThrows(ApiException.class, () -> store.load(saved.id()));
    }

    @Test
    void rejectsSpoofedMimeAndOversizedUploadWithoutWritingFile() throws Exception {
        DriverDocumentFileStore store = new DriverDocumentFileStore(folder.toString());
        assertThrows(ApiException.class, () -> store.store(new MockMultipartFile("file",
                "malware.pdf", "application/pdf", "<html>not a PDF</html>".getBytes())));
        assertThrows(ApiException.class, () -> store.store(new MockMultipartFile("file",
                "large.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1])));
        try (var files = Files.list(folder)) {
            assertFalse(files.findAny().isPresent());
        }
    }
}
