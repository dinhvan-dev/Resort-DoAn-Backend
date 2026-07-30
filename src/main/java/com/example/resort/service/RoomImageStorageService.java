package com.example.resort.service;

import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RoomImageStorageService {
    private static final int MIN_IMAGES_PER_UPLOAD = 1;
    private static final int MAX_IMAGES = 5;

    private final Path roomImageDir;

    public RoomImageStorageService(@Value("${app.upload.room-image-dir:uploads/rooms}") String roomImageDir) {
        this.roomImageDir = Path.of(roomImageDir).toAbsolutePath().normalize();
    }

    public List<String> storeRoomImages(List<MultipartFile> files) {
        if (files == null || files.size() < MIN_IMAGES_PER_UPLOAD || files.size() > MAX_IMAGES) {
            throw new AppException(ErrorCode.INVALID_ROOM_IMAGES);
        }

        try {
            Files.createDirectories(roomImageDir);
            return files.stream()
                    .map(this::storeRoomImage)
                    .toList();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_ROOM_IMAGES);
        }
    }

    private String storeRoomImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_ROOM_IMAGES);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;
        Path target = roomImageDir.resolve(storedFilename).normalize();

        if (!target.startsWith(roomImageDir)) {
            throw new AppException(ErrorCode.INVALID_ROOM_IMAGES);
        }

        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_ROOM_IMAGES);
        }

        return "/uploads/rooms/" + storedFilename;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return ".jpg";
        }

        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
