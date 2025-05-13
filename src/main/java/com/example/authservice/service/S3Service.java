package com.example.authservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.authservice.exception.FileEmptyException;
import com.example.authservice.exception.ImageSizeExceededException;
import com.example.authservice.exception.ResumeSizeExceededException;
import com.example.authservice.type.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // userId가 있을 때 (고정 파일명)
    public String upload(MultipartFile file, FileType fileType, Long userId) throws IOException {
        validateFile(file, fileType);

        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = switch (fileType) {
            case IMAGE -> String.format("%s/Profile_%d.%s", fileType.getDirectory(), userId, extension);
            case INSTRUCTOR_IMAGE -> String.format("%s/InstructorProfile_%d.%s", fileType.getDirectory(), userId, extension);
            case RESUME -> String.format("%s/Resume_%d.%s", fileType.getDirectory(), userId, extension);
        };

        uploadToS3(file, fileName);
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    // userId가 없을 때 (UUID 파일명)
    public String upload(MultipartFile file, FileType fileType) throws IOException {
        validateFile(file, fileType);

        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = String.format("%s/%s.%s", fileType.getDirectory(), UUID.randomUUID(), extension);

        uploadToS3(file, fileName);
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    private void validateFile(MultipartFile file, FileType fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            if (fileType == FileType.IMAGE || fileType == FileType.INSTRUCTOR_IMAGE) return;
            throw new FileEmptyException("파일이 비어 있습니다.");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!fileType.getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 확장자입니다: " + extension);
        }

        long fileSize = file.getSize();
        if (fileSize > fileType.getMaxSize()) {
            if (fileType == FileType.IMAGE || fileType == FileType.INSTRUCTOR_IMAGE) {
                throw new ImageSizeExceededException("이미지는 최대 1MB까지 업로드할 수 있습니다.");
            } else {
                throw new ResumeSizeExceededException("이력서는 최대 10MB까지 업로드할 수 있습니다.");
            }
        }
    }

    private void uploadToS3(MultipartFile file, String fileName) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest request = new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata);
        amazonS3.putObject(request);
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        String key = extractKeyFromUrl(fileUrl, bucket);
        if (amazonS3.doesObjectExist(bucket, key)) {
            amazonS3.deleteObject(bucket, key);
            log.info("기존 이미지 삭제 완료: {}", key);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String extractKeyFromUrl(String url, String bucketName) {
        String hostPrefix = "https://" + bucketName + ".s3.";

        if (url.startsWith(hostPrefix)) {
            int regionEndIndex = url.indexOf(".amazonaws.com/") + ".amazonaws.com/".length();
            return url.substring(regionEndIndex);
        }

        int pathIndex = url.indexOf(bucketName + "/");
        if (pathIndex != -1) {
            return url.substring(pathIndex + bucketName.length() + 1);
        }

        throw new IllegalArgumentException("지원되지 않는 URL 형식이거나 버킷 이름이 포함되지 않음: " + url);
    }
}
