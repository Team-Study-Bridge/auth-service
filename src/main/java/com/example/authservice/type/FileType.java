package com.example.authservice.type;

import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public enum FileType {
    IMAGE("profile", Set.of("jpg", "jpeg", "png", "gif", "webp"), 1 * 1024 * 1024),
    RESUME("resume", Set.of("pdf", "docx"), 10 * 1024 * 1024),
    INSTRUCTOR_IMAGE("instructorProfile", Set.of("jpg", "jpeg", "png"), 1 * 1024 * 1024);

    private final String directory;
    private final Set<String> allowedExtensions;
    private final long maxSize;


    public String getDirectory() {
        return directory;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public long getMaxSize() {
        return maxSize;
    }
}
