package com.ssarvis.backend.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class BootstrapVoiceSampleLoader {

    private static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();

    public Path resolveSamplePath(List<String> samplePaths, int index) {
        String configuredPath = samplePaths.get(Math.min(index, samplePaths.size() - 1));
        Path rawPath = Path.of(configuredPath).normalize();
        if (rawPath.isAbsolute()) {
            return rawPath;
        }

        Path workingDirCandidate = WORKING_DIRECTORY.resolve(rawPath).normalize();
        if (Files.exists(workingDirCandidate)) {
            return workingDirCandidate;
        }

        Path backendDirCandidate = WORKING_DIRECTORY.resolve("backend").resolve(rawPath).normalize();
        if (Files.exists(backendDirCandidate)) {
            return backendDirCandidate;
        }

        return backendDirCandidate;
    }

    public MultipartFile loadSampleFile(Path samplePath) {
        try {
            if (!Files.exists(samplePath)) {
                throw new IllegalStateException("Bootstrap voice sample file not found: " + samplePath);
            }

            String originalFilename = samplePath.getFileName().toString();
            String contentType = MediaTypeFactory.getMediaType(originalFilename)
                    .map(MediaType::toString)
                    .orElseGet(() -> probeContentType(samplePath));

            return new SeedMultipartFile(
                    "sampleFile",
                    originalFilename,
                    contentType,
                    Files.readAllBytes(samplePath)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bootstrap voice sample file: " + samplePath, exception);
        }
    }

    private String probeContentType(Path samplePath) {
        try {
            String detected = Files.probeContentType(samplePath);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
            // Fall back to a common audio type below.
        }
        if (samplePath.getFileName().toString().toLowerCase().endsWith(".wav")) {
            return "audio/wav";
        }
        return "application/octet-stream";
    }

    private record SeedMultipartFile(
            String name,
            String originalFilename,
            String contentType,
            byte[] bytes
    ) implements MultipartFile {

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
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), bytes);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            Files.write(dest, bytes);
        }
    }
}
