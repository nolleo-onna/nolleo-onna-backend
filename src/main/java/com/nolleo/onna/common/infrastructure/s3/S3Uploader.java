package com.nolleo.onna.common.infrastructure.s3;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.image.domain.exception.ImageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader implements ImageStoragePort {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public String upload(String folder, String originalFilename, InputStream inputStream, long size, String contentType) {
        String ext = extractExtension(originalFilename);
        String key = folder + "/" + UUID.randomUUID() + "." + ext;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
        } catch (Exception e) {
            throw new BusinessException(ImageErrorCode.UPLOAD_FAILED);
        }

        return "https://" + s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        int idx = imageUrl.indexOf("posts/");
        if (idx == -1) {
            idx = imageUrl.indexOf("images/");
        }
        if (idx == -1) return;
        String key = imageUrl.substring(idx);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
