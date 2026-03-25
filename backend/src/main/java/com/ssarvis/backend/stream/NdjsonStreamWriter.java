package com.ssarvis.backend.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NdjsonStreamWriter {

    private final OutputStream outputStream;
    private final ObjectMapper objectMapper;

    public NdjsonStreamWriter(OutputStream outputStream, ObjectMapper objectMapper) {
        this.outputStream = outputStream;
        this.objectMapper = objectMapper;
    }

    public synchronized void write(Map<String, ?> value) throws IOException {
        outputStream.write(objectMapper.writeValueAsBytes(value));
        outputStream.write('\n');
        outputStream.flush();
    }

    public synchronized void writeError(String message) throws IOException {
        write(Map.of("type", "error", "message", message));
    }
}
