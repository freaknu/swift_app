package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.shared.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

/** Private on-disk document storage. The owning document row gates every read. */
@Component
public class DriverDocumentFileStore {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final String PUBLIC_PREFIX = "/api/v1/document/files/";
    private final Path root;

    public DriverDocumentFileStore(
            @Value("${driver.documents.storage-dir:${user.home}/.swiftapp/driver-documents}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public Stored store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Document must be between 1 byte and 10 MiB");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw ApiException.badRequest("Document must be between 1 byte and 10 MiB");
            }
            FileType type = detect(bytes);
            String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (type == null || !(type.mime().equals(declared)
                    || "application/octet-stream".equals(declared)
                    || "image/jpg".equals(declared) && "image/jpeg".equals(type.mime()))) {
                throw ApiException.badRequest("Only PDF, JPEG, PNG, and WebP documents are supported");
            }
            Files.createDirectories(root);
            UUID id = UUID.randomUUID();
            Files.write(path(id, type.extension()), bytes, StandardOpenOption.CREATE_NEW);
            return new Stored(id, PUBLIC_PREFIX + id, type.mime());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Document storage failed", ex);
        }
    }

    public FileContent load(UUID id) {
        for (FileType type : FileType.values()) {
            Path path = path(id, type.extension());
            if (Files.isRegularFile(path)) {
                return new FileContent(new FileSystemResource(path), type.mime());
            }
        }
        throw ApiException.notFound("Document file not found");
    }

    public void deleteIfStoredUrl(String url) {
        if (url == null || !url.startsWith(PUBLIC_PREFIX)) return;
        try {
            UUID id = UUID.fromString(url.substring(PUBLIC_PREFIX.length()));
            for (FileType type : FileType.values()) {
                Files.deleteIfExists(path(id, type.extension()));
            }
        } catch (IllegalArgumentException | IOException ignored) {
            // A failed cleanup cannot invalidate the newly saved document row.
        }
    }

    private Path path(UUID id, String extension) {
        return root.resolve(id + "." + extension);
    }

    private FileType detect(byte[] bytes) {
        if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P'
                && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') return FileType.PDF;
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return FileType.JPEG;
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return FileType.PNG;
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return FileType.WEBP;
        return null;
    }

    private enum FileType {
        PDF("pdf", "application/pdf"), JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"), WEBP("webp", "image/webp");
        private final String extension;
        private final String mime;
        FileType(String extension, String mime) {
            this.extension = extension;
            this.mime = mime;
        }
        String extension() { return extension; }
        String mime() { return mime; }
    }

    public record Stored(UUID id, String url, String mimeType) {}
    public record FileContent(FileSystemResource resource, String mimeType) {}
}
