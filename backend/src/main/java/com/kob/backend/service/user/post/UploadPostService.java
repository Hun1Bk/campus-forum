package com.kob.backend.service.user.post;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UploadPostService {
    Map<String, String> upload_a_post(String title, String content, String create_time, Integer id, Integer sectionId, String visibility, MultipartFile[] images);
}
