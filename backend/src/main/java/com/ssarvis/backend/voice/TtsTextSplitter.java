package com.ssarvis.backend.voice;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TtsTextSplitter {

    private static final int MAX_TTS_TEXT_LENGTH = 600;

    public List<String> split(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        ArrayList<String> chunks = new ArrayList<>();
        String remaining = normalized;

        while (StringUtils.hasText(remaining)) {
            if (utf8Length(remaining) <= MAX_TTS_TEXT_LENGTH) {
                chunks.add(remaining.trim());
                break;
            }

            int splitIndex = findLastPeriodWithinLimit(remaining);
            if (splitIndex < 0) {
                splitIndex = findLastWhitespaceWithinLimit(remaining);
            }
            if (splitIndex < 0) {
                splitIndex = findMaxCharBoundaryWithinLimit(remaining);
            }

            if (splitIndex <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Failed to split long TTS text within the provider byte limit."
                );
            }

            chunks.add(remaining.substring(0, splitIndex).trim());
            remaining = remaining.substring(splitIndex).trim();
        }

        return chunks.stream().filter(StringUtils::hasText).toList();
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private int findLastPeriodWithinLimit(String text) {
        int lastPeriodIndex = -1;
        int byteLength = 0;
        for (int index = 0; index < text.length(); index++) {
            byteLength += utf8Length(Character.toString(text.charAt(index)));
            if (byteLength > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            if (text.charAt(index) == '.') {
                lastPeriodIndex = index + 1;
            }
        }
        return lastPeriodIndex;
    }

    private int findLastWhitespaceWithinLimit(String text) {
        int lastWhitespaceIndex = -1;
        int byteLength = 0;
        for (int index = 0; index < text.length(); index++) {
            byteLength += utf8Length(Character.toString(text.charAt(index)));
            if (byteLength > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            if (Character.isWhitespace(text.charAt(index))) {
                lastWhitespaceIndex = index + 1;
            }
        }
        return lastWhitespaceIndex;
    }

    private int findMaxCharBoundaryWithinLimit(String text) {
        int lastValidIndex = -1;
        int byteLength = 0;
        for (int index = 0; index < text.length(); index++) {
            byteLength += utf8Length(Character.toString(text.charAt(index)));
            if (byteLength > MAX_TTS_TEXT_LENGTH) {
                break;
            }
            lastValidIndex = index + 1;
        }
        return lastValidIndex;
    }
}
