<template>
  <main class="user-page">
    <section class="user-shell">
      <el-card shadow="never" class="user-card">
        <div v-if="loading" class="loading-text">加载中...</div>
        <template v-else>
          <div class="user-head">
            <img :src="fullImageUrl(user.photo)" class="avatar" alt="avatar">
            <div>
              <div class="username">{{ user.username || "未知用户" }}</div>
              <el-tag size="small" :type="roleTagType(user.role)">{{ user.title || "普通用户" }}</el-tag>
              <div v-if="user.isSelf" class="private-info">
                <span>登录账号：{{ user.account || "未设置账号" }}</span>
                <span>绑定邮箱：{{ user.email || "未绑定邮箱" }}</span>
              </div>
            </div>
          </div>
        </template>
      </el-card>

      <section class="post-list">
        <div class="section-title">发布的帖子</div>
        <el-empty v-if="!loading && posts.length === 0" description="暂无可查看帖子" />
        <article
          v-for="post in posts"
          :key="post.pid"
          class="post-card"
          :class="{ pinned: post.isTop }"
          @click="openPost(post)"
        >
          <div class="post-meta">
            <el-tag size="small" effect="plain">{{ post.sectionName }}</el-tag>
            <el-tag v-if="post.visibility === 'PRIVATE'" size="small" type="warning">私密帖</el-tag>
            <el-tag v-if="post.isTop" size="small" type="danger">置顶</el-tag>
            <span>{{ post.timer }}</span>
          </div>
          <h2>{{ post.title || "无标题" }}</h2>
          <p>{{ contentPreview(post.content) }}</p>
          <div class="post-stats">
            <span>浏览 {{ post.viewCount || 1 }}</span>
            <span>点赞 {{ post.agreeCount || 0 }}</span>
            <span>评论 {{ post.commentCount || 0 }}</span>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script>
import { onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";

const API_BASE = "http://localhost:3000";

export default {
  name: "UserHomeView",
  setup() {
    const route = useRoute();
    const store = useStore();
    const user = reactive({});
    const posts = reactive([]);
    const loading = ref(false);

    const authHeaders = () => {
      if (!store.state.user.token) return {};
      return { Authorization: "Bearer " + store.state.user.token };
    };

    const fullImageUrl = (url) => {
      if (!url) return "";
      if (/^https?:\/\//i.test(url) || /^blob:/i.test(url)) return url;
      return API_BASE + url;
    };

    const roleTagType = (role) => {
      if (role === "OWNER") return "danger";
      if (role === "SUPER_ADMIN") return "success";
      if (role === "ADMIN") return "warning";
      return "info";
    };

    const normalizePost = (post) => ({
      ...post,
      viewCount: Number(post.viewCount || 1),
      agreeCount: Number(post.agreeCount || 0),
      commentCount: Number(post.commentCount || 0),
      isTop: post.isTop === true || post.isTop === "true",
      visibility: post.visibility === "PRIVATE" ? "PRIVATE" : "PUBLIC",
    });

    const contentPreview = (value) => {
      const text = value || "";
      return text.length > 50 ? `${text.slice(0, 50)}...` : text;
    };

    const loadPage = () => {
      const userId = route.params.userId;
      if (!userId) return;
      loading.value = true;
      $.ajax({
        url: `${API_BASE}/user/profile/info/`,
        type: "get",
        headers: authHeaders(),
        data: { userId },
        success(resp) {
          if (resp.error_message === "success") {
            Object.keys(user).forEach((key) => delete user[key]);
            Object.assign(user, resp);
          } else {
            ElMessage.error(resp.error_message || "用户不存在");
          }
        },
        complete() {
          loading.value = false;
        },
      });
      $.ajax({
        url: `${API_BASE}/user/post/user/`,
        type: "get",
        headers: authHeaders(),
        data: { userId },
        success(resp) {
          posts.splice(0, posts.length, ...resp.map(normalizePost));
        },
      });
    };

    const openPost = (post) => {
      openRouteInNewWindow(router, { name: "post-detail", params: { pid: post.pid } });
    };

    onMounted(loadPage);
    watch(() => route.params.userId, loadPage);

    return {
      user,
      posts,
      loading,
      fullImageUrl,
      roleTagType,
      contentPreview,
      openPost,
    };
  },
};
</script>

<style scoped>
.user-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 28px 16px 48px;
}

.user-shell {
  margin: 0 auto;
  max-width: 900px;
}

.user-card,
.post-card {
  border-radius: 8px;
}

.user-head {
  align-items: center;
  display: flex;
  gap: 16px;
}

.avatar {
  border-radius: 50%;
  height: 78px;
  object-fit: cover;
  width: 78px;
}

.username {
  color: #303133;
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 8px;
}

.private-info {
  color: #606266;
  display: grid;
  font-size: 13px;
  gap: 4px;
  margin-top: 10px;
}

.section-title {
  color: #303133;
  font-size: 18px;
  font-weight: 700;
  margin: 18px 0 12px;
}

.post-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  margin-bottom: 14px;
  padding: 16px;
}

.post-card.pinned {
  border-color: #f56c6c;
}

.post-meta,
.post-stats {
  align-items: center;
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  gap: 8px;
}

.post-card h2 {
  color: #303133;
  font-size: 20px;
  margin: 12px 0 8px;
  word-break: break-word;
}

.post-card p {
  color: #606266;
  line-height: 1.7;
  margin: 0 0 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.loading-text {
  color: #909399;
}
</style>
