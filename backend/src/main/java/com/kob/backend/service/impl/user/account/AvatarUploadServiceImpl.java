package com.kob.backend.service.impl.user.account;

import com.kob.backend.service.user.account.AvatarUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AvatarUploadServiceImpl implements AvatarUploadService {
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");

    @Override
    public Map<String, String> upload(MultipartFile avatar) {
        Map<String, String> map = new HashMap<>();
        if (avatar == null || avatar.isEmpty()) {
            map.put("error_message", "请选择头像图片");
            return map;
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            map.put("error_message", "头像不能超过 2MB");
            return map;
        }
        String extension = getExtension(avatar.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            map.put("error_message", "只支持 jpg、jpeg、png、webp、gif 图片");
            return map;
        }
        String contentType = avatar.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            map.put("error_message", "请上传图片文件");
            return map;
        }

        String date = LocalDate.now().toString();
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path uploadDir = Paths.get("uploads", "avatars", date).toAbsolutePath().normalize();
        Path target = uploadDir.resolve(filename).normalize();
        try {
            Files.createDirectories(uploadDir);
            avatar.transferTo(target.toFile());
        } catch (IOException e) {
            map.put("error_message", "头像保存失败");
            return map;
        }

        map.put("error_message", "success");
        map.put("url", "/uploads/avatars/" + date + "/" + filename);
        return map;
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase();
    }
}
