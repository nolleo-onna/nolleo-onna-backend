package com.nolleo.onna.common.infrastructure.s3;

import java.io.InputStream;

public interface ImageStoragePort {
    String upload(String folder, String originalFilename, InputStream inputStream, long size, String contentType);
    void delete(String imageUrl);
}
