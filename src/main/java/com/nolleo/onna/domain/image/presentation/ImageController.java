package com.nolleo.onna.domain.image.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.image.application.ImageUploadService;
import com.nolleo.onna.domain.image.presentation.dto.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Tag(name = "Image", description = "이미지 업로드 API")
public class ImageController {

    private final ImageUploadService imageUploadService;

    @PostMapping
    @Operation(summary = "이미지 업로드", description = "게시글에 첨부할 이미지를 업로드합니다. (최대 5장, 각 10MB 이하)")
    public ResponseEntity<ApiResponseDto<ImageUploadResponse>> uploadImages(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam List<MultipartFile> images
    ) {
        List<String> imageUrls = imageUploadService.uploadImages(images);
        return ApiResponseDto.success(200, "이미지 업로드 성공", new ImageUploadResponse(imageUrls));
    }
}
