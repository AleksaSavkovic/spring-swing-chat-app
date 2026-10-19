package org.chatapp.client.service;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultipartBodyPublisher {

    private final String boundary;
    private final List<byte[]> parts = new ArrayList<>();

    public MultipartBodyPublisher() {
        this.boundary = "----ChatAppBoundary" + UUID.randomUUID();
    }

    public MultipartBodyPublisher addFile(
            String fieldName,
            Path file,
            String contentType
    ) throws IOException {

        String filename = file.getFileName().toString();

        String headers =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"" + fieldName +
                        "\"; filename=\"" + filename + "\"\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "\r\n";

        parts.add(headers.getBytes(StandardCharsets.UTF_8));
        parts.add(Files.readAllBytes(file));
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));

        return this;
    }

    public HttpRequest.BodyPublisher build() {

        parts.add(
                ("--" + boundary + "--\r\n")
                        .getBytes(StandardCharsets.UTF_8)
        );

        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }
}
