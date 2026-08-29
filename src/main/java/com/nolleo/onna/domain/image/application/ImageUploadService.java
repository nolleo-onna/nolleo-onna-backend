package com.nolleo.onna.domain.image.application;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.infrastructure.s3.ImageStoragePort;
import com.nolleo.onna.domain.image.domain.exception.ImageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_IMAGE_COUNT = 5;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ImageStoragePort imageStoragePort;

    public List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return List.of();

        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ImageErrorCode.TOO_MANY_IMAGES);
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile image : images) {
            validateFile(image);

            try {
                String url = imageStoragePort.upload(
                        "posts",
                        image.getOriginalFilename(),
                        image.getInputStream(),
                        image.getSize(),
                        image.getContentType()
                );
                imageUrls.add(url);
            } catch (IOException e) {
                throw new BusinessException(ImageErrorCode.UPLOAD_FAILED);
            }
        }

        return imageUrls;
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ImageErrorCode.FILE_SIZE_EXCEEDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ImageErrorCode.INVALID_FILE_TYPE);
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ImageErrorCode.INVALID_FILE_TYPE);
        }
    }
}
