package com.petmgmt.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

public class FileUtils {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final String UPLOAD_BASE = "src/main/resources/static/uploads";

    public static boolean isAllowedFile(String filename) {
        if (filename == null) return false;
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        return ALLOWED_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase());
    }

    public static String saveUpload(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) return null;
        if (!isAllowedFile(file.getOriginalFilename())) return null;

        try {
            String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path dir = Paths.get(UPLOAD_BASE, subfolder);
            Files.createDirectories(dir);
            Path filepath = dir.resolve(filename);
            Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);
            return "uploads/" + subfolder + "/" + filename;
        } catch (IOException e) {
            return null;
        }
    }

    public static void deleteUpload(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return;
        try {
            Path path = Paths.get("src/main/resources/static", relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // ignore
        }
    }
}
