package io.lumen.web.multipart;

import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class ServletMultipartFile implements MultipartFile {
    private final Part part;

    public ServletMultipartFile(Part part) {
        this.part = part;
    }

    @Override
    public String getName() {
        return part.getName();
    }

    @Override
    public String getOriginalFilename() {
        return part.getSubmittedFileName();
    }

    @Override
    public String getContentType() {
        return part.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public long getSize() {
        return part.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return getInputStream().readAllBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    @Override
    public void transferTo(Path dest) throws IOException {
        part.write(dest.toAbsolutePath().toString());
    }
}
