/**
 * FileStorageService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private final UploadProperties uploadProperties;

    public String storeProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일을 선택해 주세요.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jpg, jpeg, png, webp 형식만 업로드할 수 있습니다.");
        }

        try {
            String configuredDir = StringUtils.hasText(uploadProperties.dir()) ? uploadProperties.dir() : "uploads";
            Path uploadRoot = Paths.get(configuredDir).toAbsolutePath().normalize();
            Path profileDir = uploadRoot.resolve("profile");
            Files.createDirectories(profileDir);

            String filename = "user-" + userId + "-" + UUID.randomUUID() + "." + extension;
            Path target = profileDir.resolve(filename).normalize();

            if (!target.startsWith(profileDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/profile/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }
    }

    private String extractExtension(String filename) {
        String cleaned = StringUtils.hasText(filename) ? filename : "";
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleaned.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 확장자를 확인할 수 없습니다.");
        }
        return cleaned.substring(dotIndex + 1).toLowerCase();
    }
}
