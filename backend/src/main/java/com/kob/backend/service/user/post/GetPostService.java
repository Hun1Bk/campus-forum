package com.kob.backend.service.user.post;

import java.util.List;
import java.util.Map;

public interface GetPostService {
    List<Map<String, Object>> get_post(Integer sectionId, String keyword);

    List<Map<String, Object>> hot();

    List<Map<String, Object>> pinned();

    List<Map<String, Object>> mine();

    List<Map<String, Object>> userPosts(Integer userId);

    Map<String, Object> detail(Integer pid);

    Map<String, Object> userInfo(Integer userId);

    Map<String, String> view(Integer pid);

    Map<String, String> delete(Integer pid);
}
