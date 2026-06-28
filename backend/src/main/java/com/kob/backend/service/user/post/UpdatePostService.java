package com.kob.backend.service.user.post;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UpdatePostService {
    Map<String, String> update(Integer pid, String title, String content, Integer sectionId, String visibility, String keepImageUrls, MultipartFile[] images);
}
