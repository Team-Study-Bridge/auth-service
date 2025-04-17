package com.example.authservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_IMAGE_SIZE = 1 * 1024 * 1024; // 1MB
    private static final long MAX_RESUME_SIZE = 10 * 1024 * 1024; // 10MB

    public String upload(MultipartFile file, String directory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        boolean isImage = isImageExtension(extension);
        long fileSize = file.getSize();

        if (isImage && fileSize > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지 파일은 최대 1MB까지 업로드 가능합니다.");
        }

        if (!isImage && fileSize > MAX_RESUME_SIZE) {
            throw new IllegalArgumentException("이력서 파일은 최대 10MB까지 업로드 가능합니다.");
        }

        String uniqueFileName = String.format("%s/%s.%s", directory, UUID.randomUUID(), extension);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileSize);

        if (isImage) {
            metadata.setContentType("image/" + extension.toLowerCase());
        } else {
            metadata.setContentType(file.getContentType());
        }

        PutObjectRequest request = new PutObjectRequest(bucket, uniqueFileName, file.getInputStream(), metadata);
        amazonS3.putObject(request);

        return amazonS3.getUrl(bucket, uniqueFileName).toString();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isImageExtension(String extension) {
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }
}
