package com.sareekart.storage;

import com.sareekart.config.FileStorageProperties;
import com.sareekart.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Server-side image validation: content-type allow-list, magic-byte sniffing,
 * size and empty-file checks. Content-Type header alone is NOT trusted.
 */
@Component
@RequiredArgsConstructor
public class ImageValidator {

    private final FileStorageProperties properties;

    /** Magic-byte signatures for supported formats. */
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png",  new byte[]{(byte) 0x89, 'P', 'N', 'G'},
        "image/gif",  new byte[]{'G', 'I', 'F'},
        "image/webp", new byte[]{'R', 'I', 'F', 'F'}
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", "jpg",
        "image/png",  "png",
        "image/gif",  "gif",
        "image/webp", "webp"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BadRequestException(String.format(
                "File exceeds maximum size of %d bytes", properties.getMaxFileSize()));
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.getAllowedContentTypes().contains(contentType)) {
            throw new BadRequestException("Unsupported content type: " + contentType);
        }

        // Magic-byte sniffing — don't trust Content-Type alone.
        byte[] magic = MAGIC_BYTES.get(contentType);
        if (magic != null && !magicBytesMatch(file, magic)) {
            throw new BadRequestException("File content does not match declared type " + contentType);
        }
    }

    /** Returns safe extension derived from the VALIDATED content type. */
    public String extensionFor(String contentType) {
        return EXTENSIONS.getOrDefault(contentType, "bin");
    }

    private boolean magicBytesMatch(MultipartFile file, byte[] expected) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[expected.length];
            int read = is.read(header);
            if (read < expected.length) return false;
            for (int i = 0; i < expected.length; i++) {
                if (header[i] != expected[i]) return false;
            }
            // WebP: RIFF header + 4-byte size + 'WEBP'
            if ("image/webp".equals(file.getContentType()) && read >= 8) {
                try (InputStream is2 = file.getInputStream()) {
                    byte[] webpMarker = new byte[4];
                    is2.skipNBytes(8);
                    int r = is2.read(webpMarker);
                    return r == 4 && webpMarker[0] == 'W' && webpMarker[1] == 'E'
                        && webpMarker[2] == 'B' && webpMarker[3] == 'P';
                } catch (IOException e) { return false; }
            }
            return true;
        } catch (IOException e) {
            throw new BadRequestException("Unable to read file contents");
        }
    }
}
