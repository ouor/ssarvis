package com.ssarvis.backend.voice;

import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.config.StorageConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class AudioStorageService {

    private static final String STORAGE_PROVIDER = "S3";
    private static final String STORED_AUDIO_MIME_TYPE = "audio/mpeg";

    private final AppProperties appProperties;
    private final S3Client s3Client;
    private final GeneratedAudioAssetRepository generatedAudioAssetRepository;

    public AudioStorageService(
            AppProperties appProperties,
            S3Client s3Client,
            GeneratedAudioAssetRepository generatedAudioAssetRepository
    ) {
        this.appProperties = appProperties;
        this.s3Client = s3Client;
        this.generatedAudioAssetRepository = generatedAudioAssetRepository;
    }

    public GeneratedAudioAsset storeDashscopeAudio(
            UserAccount user,
            byte[] sourceAudioBytes,
            String sourceAudioMimeType,
            String providerVoiceId
    ) {
        AppProperties.S3 s3 = appProperties.getStorage().getS3();
        if (!s3.isEnabled()) {
            return null;
        }
        if (!StringUtils.hasText(s3.getBucket())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3_BUCKET is not configured.");
        }

        try {
            byte[] encodedAudioBytes = encodeToMp3(sourceAudioBytes);
            String objectKey = buildObjectKey(s3.getKeyPrefix());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3.getBucket())
                    .key(objectKey)
                    .contentType(STORED_AUDIO_MIME_TYPE)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(encodedAudioBytes));

            return generatedAudioAssetRepository.save(new GeneratedAudioAsset(
                    user,
                    STORAGE_PROVIDER,
                    s3.getBucket(),
                    objectKey,
                    buildObjectUrl(s3, objectKey),
                    sourceAudioMimeType,
                    STORED_AUDIO_MIME_TYPE,
                    sourceAudioBytes.length,
                    encodedAudioBytes.length,
                    providerVoiceId
            ));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encode TTS audio with ffmpeg.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ffmpeg encoding was interrupted.", exception);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload encoded TTS audio to S3.", exception);
        }
    }

    private byte[] encodeToMp3(byte[] sourceAudioBytes) throws IOException, InterruptedException {
        Path inputPath = Files.createTempFile("dashscope-tts-", ".input");
        Path outputPath = Files.createTempFile("dashscope-tts-", ".mp3");

        try {
            Files.write(inputPath, sourceAudioBytes);

            Process process = new ProcessBuilder(
                    appProperties.getMedia().getFfmpegPath(),
                    "-y",
                    "-i",
                    inputPath.toString(),
                    "-vn",
                    "-codec:a",
                    "libmp3lame",
                    "-b:a",
                    "128k",
                    outputPath.toString()
            ).redirectErrorStream(true).start();

            String ffmpegOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "ffmpeg encoding failed with exit code " + exitCode + ". Output: " + abbreviate(ffmpegOutput, 500)
                );
            }

            return Files.readAllBytes(outputPath);
        } finally {
            Files.deleteIfExists(inputPath);
            Files.deleteIfExists(outputPath);
        }
    }

    private String buildObjectKey(String keyPrefix) {
        LocalDate today = LocalDate.now();
        String normalizedPrefix = StringUtils.hasText(keyPrefix) ? keyPrefix.replaceAll("/+$", "") : "ssarvis/tts";
        return "%s/%d/%02d/%02d/%s.mp3".formatted(
                normalizedPrefix,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID()
        );
    }

    private String buildObjectUrl(AppProperties.S3 s3, String objectKey) {
        if (StringUtils.hasText(s3.getPublicBaseUrl())) {
            return s3.getPublicBaseUrl().replaceAll("/+$", "") + "/" + objectKey;
        }
        if (StringUtils.hasText(s3.getEndpoint())) {
            String endpoint = s3.getEndpoint().replaceAll("/+$", "");
            if (StorageConfig.shouldUsePathStyleAccess(s3)) {
                return endpoint + "/" + s3.getBucket() + "/" + objectKey;
            }
            return endpoint + "/" + objectKey;
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(s3.getBucket(), s3.getRegion(), objectKey);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
