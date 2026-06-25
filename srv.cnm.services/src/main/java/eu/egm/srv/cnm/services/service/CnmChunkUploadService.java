package eu.egm.srv.cnm.services.service;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stages bounded upload chunks on disk and assembles them for import.
 */
@Service
public class CnmChunkUploadService {
    public static final long MAX_FILE_SIZE = 1024L * 1024L * 1024L;
    private final Path stagingRoot;
    private final Map<String, Map<String, UploadFile>> sessions = new ConcurrentHashMap<>();

    public CnmChunkUploadService() throws IOException {
        stagingRoot = Files.createTempDirectory("cnm-upload-");
    }

    public void storeChunk(
            String importId,
            String fileId,
            String fileName,
            int chunkIndex,
            int totalChunks,
            long fileSize,
            byte[] bytes) throws IOException {
        validateId(importId);
        validateId(fileId);
        if (fileSize < 0 || fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds the 1 GB import limit");
        }
        if (chunkIndex < 0 || totalChunks <= 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("Invalid upload chunk coordinates");
        }
        Path fileDirectory = stagingRoot.resolve(importId).resolve(fileId);
        Files.createDirectories(fileDirectory);
        Files.write(fileDirectory.resolve(chunkIndex + ".part"), bytes);
        sessions.computeIfAbsent(importId, ignored -> new ConcurrentHashMap<>())
                .put(fileId, new UploadFile(fileId, safeName(fileName), totalChunks, fileSize, fileDirectory));
    }

    public List<MultipartFile> complete(String importId) throws IOException {
        Map<String, UploadFile> files = sessions.get(importId);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No chunks found for import " + importId);
        }
        List<MultipartFile> completed = new ArrayList<>();
        for (UploadFile upload : files.values().stream().sorted(Comparator.comparing(UploadFile::fileName)).toList()) {
            Path assembled = upload.directory().resolve("assembled");
            try (var output = Files.newOutputStream(assembled)) {
                for (int index = 0; index < upload.totalChunks(); index++) {
                    Path chunk = upload.directory().resolve(index + ".part");
                    if (!Files.exists(chunk)) {
                        throw new IllegalStateException("Missing chunk " + index + " for " + upload.fileName());
                    }
                    Files.copy(chunk, output);
                }
            }
            if (Files.size(assembled) != upload.fileSize()) {
                throw new IllegalStateException("Assembled file size does not match " + upload.fileName());
            }
            completed.add(new StagedMultipartFile(upload.fileId(), upload.fileName(), assembled));
        }
        return completed;
    }

    public void discard(String importId) {
        sessions.remove(importId);
        deleteRecursively(stagingRoot.resolve(importId));
    }

    @PreDestroy
    void cleanup() {
        deleteRecursively(stagingRoot);
    }

    private void validateId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{1,100}")) {
            throw new IllegalArgumentException("Invalid upload identifier");
        }
    }

    private String safeName(String fileName) {
        return fileName == null || fileName.isBlank() ? "upload" : Path.of(fileName).getFileName().toString();
    }

    private void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException ignored) {
                    // Temporary upload cleanup is best effort.
                }
            });
        } catch (IOException ignored) {
            // Temporary upload cleanup is best effort.
        }
    }

    private record UploadFile(String fileId, String fileName, int totalChunks, long fileSize, Path directory) {
    }

    private record StagedMultipartFile(String name, String originalFilename, Path path) implements MultipartFile {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            try {
                return Files.probeContentType(path);
            } catch (IOException ignored) {
                return "application/octet-stream";
            }
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to inspect staged upload", exception);
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(File destination) throws IOException {
            transferTo(destination.toPath());
        }

        @Override
        public void transferTo(Path destination) throws IOException {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
