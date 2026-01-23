package io.lumen.web.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Represents a file uploaded in a multipart request.
 */
public interface MultipartFile {

    String getName();

    String getOriginalFilename();

    String getContentType();

    boolean isEmpty();

    long getSize();

    byte[] getBytes() throws IOException;

    InputStream getInputStream() throws IOException;

    void transferTo(Path dest) throws IOException;
}
