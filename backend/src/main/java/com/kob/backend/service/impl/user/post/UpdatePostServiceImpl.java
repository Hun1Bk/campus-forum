package com.kob.backend.service.impl.user.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kob.backend.mapper.PostMapper;
import com.kob.backend.mapper.SectionMapper;
import com.kob.backend.pojo.Post;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.post.UpdatePostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UpdatePostServiceImpl implements UpdatePostService {
    private static final int MAX_IMAGE_COUNT = 9;
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private SectionMapper sectionMapper;

    @Override
    public Map<String, String> update(Integer pid, String title, String content, Integer sectionId, String visibility, String keepImageUrls, MultipartFile[] images) {
        Map<String, String> map = new HashMap<>();
        Integer currentUserId = currentUserId();
        if (currentUserId == null) {
            map.put("error_message", "请先登录");
            return map;
        }
        Post post = postMapper.selectById(pid);
        if (post == null) {
            map.put("error_message", "帖子不存在");
            return map;
        }
        if (!currentUserId.equals(post.getId())) {
            map.put("error_message", "只能编辑自己发布的帖子");
            return map;
        }
        String titleValue = title == null ? "" : title.trim();
        if (titleValue.isEmpty()) {
            map.put("error_message", "标题不能为空");
            return map;
        }
        if (titleValue.length() > 80) {
            map.put("error_message", "标题不能超过 80 个字符");
            return map;
        }
        String value = content == null ? "" : content.trim();
        if (value.isEmpty()) {
            map.put("error_message", "内容不能为空");
            return map;
        }
        if (sectionId == null || sectionMapper.selectById(sectionId) == null) {
            map.put("error_message", "分区不存在");
            return map;
        }

        List<String> finalImageUrls = parseKeepImageUrls(keepImageUrls, map);
        if (map.containsKey("error_message")) {
            return map;
        }
        List<MultipartFile> validImages = collectValidImages(images, map);
        if (map.containsKey("error_message")) {
            return map;
        }
        if (finalImageUrls.size() + validImages.size() > MAX_IMAGE_COUNT) {
            map.put("error_message", "每条帖子最多保留 9 张图片");
            return map;
        }
        try {
            finalImageUrls.addAll(saveImages(validImages));
            post.setTitle(titleValue);
            post.setContent(value);
            post.setSectionId(sectionId);
            post.setVisibility(normalizeVisibility(visibility));
            post.setImageUrls(OBJECT_MAPPER.writeValueAsString(finalImageUrls));
            postMapper.updateById(post);
        } catch (IOException e) {
            map.put("error_message", "图片保存失败");
            return map;
        }

        map.put("error_message", "success");
        return map;
    }

    private String normalizeVisibility(String visibility) {
        return "PRIVATE".equals(visibility) ? "PRIVATE" : "PUBLIC";
    }

    private Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
        }
        return null;
    }

    private List<String> parseKeepImageUrls(String keepImageUrls, Map<String, String> result) {
        if (keepImageUrls == null || keepImageUrls.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<String> urls = OBJECT_MAPPER.readValue(keepImageUrls, new TypeReference<List<String>>() {});
            List<String> safeUrls = new ArrayList<>();
            for (String url : urls) {
                if (url != null && !url.trim().isEmpty()) {
                    safeUrls.add(url.trim());
                }
            }
            return safeUrls;
        } catch (Exception e) {
            result.put("error_message", "图片数据格式不正确");
            return new ArrayList<>();
        }
    }

    private List<MultipartFile> collectValidImages(MultipartFile[] images, Map<String, String> result) {
        List<MultipartFile> validImages = new ArrayList<>();
        if (images == null) {
            return validImages;
        }
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            validImages.add(image);
        }
        for (MultipartFile image : validImages) {
            if (image.getSize() > MAX_IMAGE_SIZE) {
                result.put("error_message", "单张图片不能超过 5MB");
                return validImages;
            }
            String extension = getExtension(image.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                result.put("error_message", "只支持 jpg、jpeg、png、webp、gif 图片");
                return validImages;
            }
            String contentType = image.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                result.put("error_message", "请上传图片文件");
                return validImages;
            }
        }
        return validImages;
    }

    private List<String> saveImages(List<MultipartFile> images) throws IOException {
        List<String> urls = new ArrayList<>();
        if (images.isEmpty()) {
            return urls;
        }
        String date = LocalDate.now().toString();
        Path uploadDir = Paths.get("uploads", "posts", date).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        for (MultipartFile image : images) {
            String extension = getExtension(image.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = uploadDir.resolve(filename).normalize();
            image.transferTo(target.toFile());
            urls.add("/uploads/posts/" + date + "/" + filename);
        }
        return urls;
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
